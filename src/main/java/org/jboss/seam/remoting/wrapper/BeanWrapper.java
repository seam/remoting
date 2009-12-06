package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.dom4j.Element;
import org.jboss.seam.remoting.InterfaceGenerator;

/**
 * @author Shane Bryzak
 */
public class BeanWrapper extends BaseWrapper implements Wrapper
{
   private static final byte[] REF_START_TAG_OPEN = "<ref id=\"".getBytes();
   private static final byte[] REF_START_TAG_END = "\"/>".getBytes();

   private static final byte[] BEAN_START_TAG_OPEN = "<bean type=\"".getBytes();
   private static final byte[] BEAN_START_TAG_CLOSE = "\">".getBytes();
   private static final byte[] BEAN_CLOSE_TAG = "</bean>".getBytes();

   private static final byte[] MEMBER_START_TAG_OPEN = "<member name=\""
         .getBytes();
   private static final byte[] MEMBER_START_TAG_CLOSE = "\">".getBytes();
   private static final byte[] MEMBER_CLOSE_TAG = "</member>".getBytes();

   public BeanWrapper(BeanManager beanManager)
   {
      super(beanManager);
   }

   @Override
   public void setElement(Element element)
   {
      super.setElement(element);

      String beanType = element.attributeValue("type");

      // TODO do it this way, it might not be a managed bean...
      Bean bean = beanManager.getBeans(beanType).iterator().next();

      if (bean != null)
      {
         value = bean.create(beanManager.createCreationalContext(bean));
      } else
      {
         try
         {
            value = Class.forName(beanType).newInstance();
         } catch (Exception ex)
         {
            throw new RuntimeException("Could not unmarshal bean element: "
                  + element.getText(), ex);
         }
      }
   }

   @Override
   public void unmarshal()
   {
      List members = element.elements("member");

      for (Element member : (List<Element>) members)
      {
         String name = member.attributeValue("name");

         Wrapper w = context.createWrapperFromElement((Element) member
               .elementIterator().next());

         Class cls = value.getClass();

         // We're going to try a combination of ways to set the property value
         // here
         Method method = null;
         Field field = null;

         // First try to find the best matching method
         String setter = "set" + Character.toUpperCase(name.charAt(0))
               + name.substring(1);

         ConversionScore score = ConversionScore.nomatch;
         for (Method m : cls.getMethods())
         {
            if (setter.equals(m.getName()) && m.getParameterTypes().length == 1)
            {
               ConversionScore s = w.conversionScore(m.getParameterTypes()[0]);
               if (s.getScore() > score.getScore())
               {
                  method = m;
                  score = s;
               }
            }
         }

         // If we can't find a method, look for a matching field name
         if (method == null)
         {
            while (field == null && !cls.equals(Object.class))
            {
               try
               {
                  // First check the declared fields
                  field = cls.getDeclaredField(name);
               } catch (NoSuchFieldException ex)
               {
                  // Couldn't find the field.. try the superclass
                  cls = cls.getSuperclass();
               }
            }

            if (field == null)
            {
               throw new RuntimeException(
                     String
                           .format(
                                 "Error while unmarshalling - property [%s] not found in class [%s]",
                                 name, value.getClass().getName()));
            }
         }

         // Now convert the field value to the correct target type
         Object fieldValue = null;
         try
         {
            fieldValue = w.convert(method != null ? method
                  .getGenericParameterTypes()[0] : field.getGenericType());
         } catch (ConversionException ex)
         {
            throw new RuntimeException(
                  "Could not convert value while unmarshaling", ex);
         }

         // If we have a setter method, invoke it
         if (method != null)
         {
            try
            {
               method.invoke(value, fieldValue);
            } catch (Exception e)
            {
               throw new RuntimeException(String.format(
                     "Could not invoke setter method [%s]", method.getName()));
            }
         } else
         {
            // Otherwise try to set the field value directly
            boolean accessible = field.isAccessible();
            try
            {
               if (!accessible)
                  field.setAccessible(true);
               field.set(value, fieldValue);
            } catch (Exception ex)
            {
               throw new RuntimeException("Could not set field value.", ex);
            } finally
            {
               field.setAccessible(accessible);
            }
         }
      }
   }

   public Object convert(Type type) throws ConversionException
   {
      if (type instanceof Class
            && ((Class) type).isAssignableFrom(value.getClass()))
         return value;
      else
         throw new ConversionException(String.format(
               "Value [%s] cannot be converted to type [%s].", value, type));
   }

   public void marshal(OutputStream out) throws IOException
   {
      context.addOutRef(this);

      out.write(REF_START_TAG_OPEN);
      out
            .write(Integer.toString(context.getOutRefs().indexOf(this))
                  .getBytes());
      out.write(REF_START_TAG_END);
   }

   @Override
   public void serialize(OutputStream out) throws IOException
   {
      serialize(out, null);
   }

   public void serialize(OutputStream out, List<String> constraints)
         throws IOException
   {
      out.write(BEAN_START_TAG_OPEN);

      Class cls = value.getClass();

      /**
       * @todo This is a hack to get the "real" class - find out if there is an
       *       API method in CGLIB that can be used instead
       */
      if (cls.getName().contains("EnhancerByCGLIB"))
      {
         cls = cls.getSuperclass();
      }

      if (cls.getName().contains("_$$_javassist_"))
      {
         cls = cls.getSuperclass();
      }

      // TODO fix this, bean might not have a name
      Bean bean = beanManager.getBeans(cls).iterator().next();      
      String componentName = bean.getName();

      if (componentName != null)
         out.write(componentName.getBytes());
      else
         out.write(cls.getName().getBytes());

      out.write(BEAN_START_TAG_CLOSE);

      for (String propertyName : InterfaceGenerator
            .getAccessibleProperties(cls))
      {
         String fieldPath = path != null && path.length() > 0 ? String.format(
               "%s.%s", path, propertyName) : propertyName;

         // Also exclude fields listed using wildcard notation:
         // [componentName].fieldName
         String wildCard = String.format("[%s].%s",
               componentName != null ? componentName : cls.getName(),
               propertyName);

         if (constraints == null
               || (!constraints.contains(fieldPath) && !constraints
                     .contains(wildCard)))
         {
            out.write(MEMBER_START_TAG_OPEN);
            out.write(propertyName.getBytes());
            out.write(MEMBER_START_TAG_CLOSE);

            Field f = null;
            try
            {
               f = cls.getField(propertyName);
            } catch (NoSuchFieldException ex)
            {
            }

            boolean accessible = false;
            try
            {
               // Temporarily set the field's accessibility so we can read it
               if (f != null)
               {
                  accessible = f.isAccessible();
                  f.setAccessible(true);
                  context.createWrapperFromObject(f.get(value), fieldPath)
                        .marshal(out);
               } else
               {
                  Method accessor = null;
                  try
                  {
                     accessor = cls.getMethod(String.format("get%s%s",
                           Character.toUpperCase(propertyName.charAt(0)),
                           propertyName.substring(1)));
                  } catch (NoSuchMethodException ex)
                  {
                     try
                     {
                        accessor = cls.getMethod(String.format("is%s%s",
                              Character.toUpperCase(propertyName.charAt(0)),
                              propertyName.substring(1)));
                     } catch (NoSuchMethodException ex2)
                     {
                        // uh oh... continue with the next one
                        continue;
                     }
                  }

                  try
                  {
                     context.createWrapperFromObject(accessor.invoke(value),
                           fieldPath).marshal(out);
                  } catch (InvocationTargetException ex)
                  {
                     throw new RuntimeException(String.format(
                           "Failed to read property [%s] for object [%s]",
                           propertyName, value));
                  }
               }
            } catch (IllegalAccessException ex)
            {
               throw new RuntimeException("Error reading value from field.");
            } finally
            {
               if (f != null)
                  f.setAccessible(accessible);
            }

            out.write(MEMBER_CLOSE_TAG);
         }
      }

      out.write(BEAN_CLOSE_TAG);
   }

   public ConversionScore conversionScore(Class<?> cls)
   {
      if (cls.equals(value.getClass()))
         return ConversionScore.exact;
      else if (cls.isAssignableFrom(value.getClass())
            || cls.equals(Object.class))
         return ConversionScore.compatible;
      else
         return ConversionScore.nomatch;
   }
}
