package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.dom4j.Element;
import org.jboss.seam.remoting.MetadataCache;

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
   
   private MetadataCache metadataCache;
   
   @SuppressWarnings("unchecked")
   private MetadataCache getMetadataCache()
   {
      if (metadataCache == null)
      {
         Bean<MetadataCache> bean = (Bean<MetadataCache>) beanManager.getBeans(
               MetadataCache.class).iterator().next(); 
         metadataCache = bean.create(beanManager.createCreationalContext(bean));
      }
      return metadataCache;
   }
   
   @Override
   @SuppressWarnings("unchecked")
   public void setElement(Element element)
   {
      super.setElement(element);

      String beanType = element.attributeValue("type");

      Set<Bean<?>> beans = beanManager.getBeans(beanType); 
      if (beans.size() > 0)
      {
         Bean bean = beans.iterator().next();
         value = bean.create(beanManager.createCreationalContext(bean));
      }
      else
      {
         try
         {
            value = Class.forName(beanType).newInstance();
         }
         catch (Exception ex)
         {
            throw new RuntimeException("Could not unmarshal bean element: "
                  + element.getText(), ex);
         }
      }
   }
   
   public Type getBeanPropertyType(String propertyName)
   {
      Class<?> cls = value.getClass();
      
      String getter = "get" + Character.toUpperCase(propertyName.charAt(0)) +
          propertyName.substring(1);
      
      for (Method m : cls.getMethods())
      {
         if (getter.equals(m.getName())) return m.getGenericReturnType(); 
      }
      
      Field field = null;
      while (field == null && !cls.equals(Object.class))
      {
         try
         {
            field = cls.getDeclaredField(propertyName);
         }
         catch (NoSuchFieldException e)
         {
            cls = cls.getSuperclass();
         }
      }
      
      if (field == null)
      {
         throw new IllegalArgumentException("Invalid property name [" + propertyName +
               "] specified for target class [" + value.getClass() + "]");
      }
      
      return field.getGenericType();
   }
   
   public Wrapper getBeanProperty(String propertyName)
   {
      Class<?> cls = value.getClass();
      
      Field f = null;
      try
      {
         f = cls.getField(propertyName);
      }
      catch (NoSuchFieldException ex) { }

      boolean accessible = false;
      try
      {
         // Temporarily set the field's accessibility so we can read it
         if (f != null)
         {
            accessible = f.isAccessible();
            f.setAccessible(true);
            return context.createWrapperFromObject(f.get(value), null);
         }
         else
         {
            Method accessor = null;
            try
            {
               accessor = cls.getMethod(String.format("get%s%s",
                     Character.toUpperCase(propertyName.charAt(0)),
                     propertyName.substring(1)));
            }
            catch (NoSuchMethodException ex)
            {
               try
               {
                  accessor = cls.getMethod(String.format("is%s%s",
                        Character.toUpperCase(propertyName.charAt(0)),
                        propertyName.substring(1)));
               }
               catch (NoSuchMethodException ex2)
               {
                  // uh oh... continue with the next one
                  return null;
               }
            }

            try
            {
               return context.createWrapperFromObject(accessor.invoke(value), null);
            }
            catch (InvocationTargetException ex)
            {
               throw new RuntimeException(String.format(
                     "Failed to read property [%s] for object [%s]",
                     propertyName, value));
            }
         }
      }
      catch (IllegalAccessException ex)
      {
         throw new RuntimeException("Error reading value from field.");
      }
      finally
      {
         if (f != null)
            f.setAccessible(accessible);
      }      
   }
   
   public void setBeanProperty(String propertyName, Wrapper valueWrapper)
   {
      Class<?> cls = value.getClass();

      // We're going to try a combination of ways to set the property value
      Method method = null;
      Field field = null;

      // First try to find the best matching method
      String setter = "set" + Character.toUpperCase(propertyName.charAt(0))
            + propertyName.substring(1);

      ConversionScore score = ConversionScore.nomatch;
      for (Method m : cls.getMethods())
      {
         if (setter.equals(m.getName()) && m.getParameterTypes().length == 1)
         {
            ConversionScore s = valueWrapper.conversionScore(m.getParameterTypes()[0]);
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
               field = cls.getDeclaredField(propertyName);
            }
            catch (NoSuchFieldException ex)
            {
               // Couldn't find the field.. try the superclass
               cls = cls.getSuperclass();
            }
         }

         if (field == null)
         {
            throw new RuntimeException(String.format(
               "Error while unmarshalling - property [%s] not found in class [%s]",
               propertyName, value.getClass().getName()));
         }
      }

      // Now convert the field value to the correct target type
      Object fieldValue = null;
      try
      {
         fieldValue = valueWrapper.convert(method != null ? method
               .getGenericParameterTypes()[0] : field.getGenericType());
      }
      catch (ConversionException ex)
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
         }
         catch (Exception e)
         {
            throw new RuntimeException(String.format(
                  "Could not invoke setter method [%s]", method.getName()));
         }
      }
      else
      {
         // Otherwise try to set the field value directly
         boolean accessible = field.isAccessible();
         try
         {
            if (!accessible) field.setAccessible(true);
            field.set(value, fieldValue);
         }
         catch (Exception ex)
         {
            throw new RuntimeException("Could not set field value.", ex);
         }
         finally
         {
            field.setAccessible(accessible);
         }
      }
      
   }

   @SuppressWarnings("unchecked")
   @Override
   public void unmarshal()
   {
      List<Element> members = element.elements("member");

      for (Element member : members)
      {
         String name = member.attributeValue("name");

         Wrapper w = context.createWrapperFromElement((Element) member
               .elementIterator().next());
         setBeanProperty(name, w);
      }
   }

   public Object convert(Type type) throws ConversionException
   {
      if (type instanceof Class<?>
            && ((Class<?>) type).isAssignableFrom(value.getClass()))
      {
         return value;
      }
      else
      {
         throw new ConversionException(String.format(
               "Value [%s] cannot be converted to type [%s].", value, type));
      }
   }

   /**
    * Writes the object reference ID to the specified OutputStream
    */
   public void marshal(OutputStream out) throws IOException
   {
      context.addOutRef(this);

      out.write(REF_START_TAG_OPEN);
      out.write(Integer.toString(context.getOutRefs().indexOf(this)).getBytes());
      out.write(REF_START_TAG_END);
   }

   @Override
   public void serialize(OutputStream out) throws IOException
   {
      serialize(out, null);
   }

   /**
    * Writes a serialized representation of the object's properties to the specified
    * OutputStream.
    * 
    * @param out
    * @param constraints
    * @throws IOException
    */
   public void serialize(OutputStream out, List<String> constraints)
         throws IOException
   {
      out.write(BEAN_START_TAG_OPEN);

      Class<?> cls = value.getClass();

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

      String componentName = cls.getName();
      
      Set<Bean<?>> beans = beanManager.getBeans(cls);
      if (beans.size() > 0)
      {
         Bean<?> bean = beanManager.getBeans(cls).iterator().next();
         if (bean.getName() != null)
         {
            componentName = bean.getName();
         }
      }            

      out.write(componentName.getBytes());

      out.write(BEAN_START_TAG_CLOSE);

      for (String propertyName : getMetadataCache().getAccessibleProperties(cls).keySet())
      {
         String fieldPath = path != null && path.length() > 0 ? String.format(
               "%s.%s", path, propertyName) : propertyName;

         // Also exclude fields listed using wildcard notation:
         // [componentName].fieldName
         String wildCard = String.format("[%s].%s",
               componentName != null ? componentName : cls.getName(),
               propertyName);

         if (constraints == null || (!constraints.contains(fieldPath) && 
               !constraints.contains(wildCard)))
         {
            out.write(MEMBER_START_TAG_OPEN);
            out.write(propertyName.getBytes());
            out.write(MEMBER_START_TAG_CLOSE);

            Wrapper w = getBeanProperty(propertyName);
            if (w != null)
            {
               w.setPath(fieldPath);
               w.marshal(out);
            }

            out.write(MEMBER_CLOSE_TAG);
         }
      }

      out.write(BEAN_CLOSE_TAG);
   }

   public ConversionScore conversionScore(Class<?> cls)
   {
      if (cls.equals(value.getClass()))
      {
         return ConversionScore.exact;
      }
      else if (cls.isAssignableFrom(value.getClass())
            || cls.equals(Object.class))
      {
         return ConversionScore.compatible;
      }
      else
      {
         return ConversionScore.nomatch;
      }
   }
}
