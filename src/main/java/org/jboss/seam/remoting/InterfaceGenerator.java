package org.jboss.seam.remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.remoting.annotations.WebRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates JavaScript interface code.
 * 
 * @author Shane Bryzak
 */
public class InterfaceGenerator implements RequestHandler
{
   private static final Logger log = LoggerFactory.getLogger(InterfaceGenerator.class);
   
   @Inject BeanManager beanManager;

   /**
    * Maintain a cache of the accessible fields
    */
   private static Map<Class<?>, Set<String>> accessibleProperties = new HashMap<Class<?>, Set<String>>();

   /**
    * A cache of component interfaces, keyed by name.
    */
   private Map<Class<?>, byte[]> interfaceCache = new HashMap<Class<?>, byte[]>();
   
   /**
    * 
    * @param request
    *           HttpServletRequest
    * @param response
    *           HttpServletResponse
    * @throws Exception
    */
   public void handle(final HttpServletRequest request,
         final HttpServletResponse response) throws Exception
   {
      if (request.getQueryString() == null)
      {
         throw new ServletException("Invalid request - no component specified");
      }

      Set<Class<?>> typesCached = new HashSet<Class<?>>();
      Set<Type> types = new HashSet<Type>();

      response.setContentType("text/javascript");

      Enumeration<?> e = request.getParameterNames();
      while (e.hasMoreElements())
      {
         String componentName = ((String) e.nextElement()).trim();
         Class<?> beanClass = null;

         Set<Bean<?>> beans = beanManager.getBeans(componentName);
         
         if (!beans.isEmpty())
         {
            beanClass = beans.iterator().next().getBeanClass();
         }
         else
         {
            try
            {
               beanClass = Class.forName(componentName);
            }
            catch (ClassNotFoundException ex) 
            {
               log.error(String.format("Component not found: [%s]",
                     componentName));
               throw new ServletException(
                     "Invalid request - component not found.");               
            }            
         }
         
         typesCached.add(beanClass);
      }

      generateBeanInterface(typesCached, response.getOutputStream(), types);
   }

   /**
    * Generates the JavaScript code required to invoke the methods of a
    * component/s.
    * 
    * @param components
    *           Component[] The components to generate javascript for
    * @param out
    *           OutputStream The OutputStream to write the generated javascript
    *           to
    * @throws IOException
    *            Thrown if there is an error writing to the OutputStream
    */
   public void generateBeanInterface(Set<Class<?>> classes, OutputStream out,
         Set<Type> types) throws IOException
   {
      for (Class<?> cls : classes)
      {
         if (cls != null)
         {
            if (!interfaceCache.containsKey(cls))
            {
               synchronized (interfaceCache)
               {
                  if (!interfaceCache.containsKey(cls))
                  {
                     ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                     appendBeanSource(bOut, cls, types);
                     interfaceCache.put(cls, bOut.toByteArray());
                  }
               }
            }
            out.write(interfaceCache.get(cls));
         }
      }
   }

   /**
    * A helper method, used internally by InterfaceGenerator and also when
    * serializing responses. Returns a list of the property names for the
    * specified class which should be included in the generated interface for
    * the type.
    * 
    * @param cls
    *           Class
    * @return List
    */
   public static Set<String> getAccessibleProperties(Class<?> cls)
   {
      /**
       * @todo This is a hack to get the "real" class - find out if there is an
       *       API method in CGLIB that can be used instead
       */
      if (cls.getName().contains("EnhancerByCGLIB"))
         cls = cls.getSuperclass();

      if (!accessibleProperties.containsKey(cls))
      {
         synchronized (accessibleProperties)
         {
            if (!accessibleProperties.containsKey(cls))
            {
               Set<String> properties = new HashSet<String>();

               Class<?> c = cls;
               while (c != null && !c.equals(Object.class))
               {
                  for (Field f : c.getDeclaredFields())
                  {
                     if (!Modifier.isTransient(f.getModifiers())
                           && !Modifier.isStatic(f.getModifiers()))
                     {
                        String fieldName = f.getName().substring(0, 1)
                              .toUpperCase()
                              + f.getName().substring(1);
                        String getterName = String.format("get%s", fieldName);
                        String setterName = String.format("set%s", fieldName);
                        Method getMethod = null;
                        Method setMethod = null;

                        try
                        {
                           getMethod = c.getMethod(getterName);
                        }
                        catch (SecurityException ex)
                        {
                        }
                        catch (NoSuchMethodException ex)
                        {
                           // it might be an "is" method...
                           getterName = String.format("is%s", fieldName);
                           try
                           {
                              getMethod = c.getMethod(getterName);
                           }
                           catch (NoSuchMethodException ex2)
                           { /* don't care */
                           }
                        }

                        try
                        {
                           setMethod = c.getMethod(setterName, new Class[] { f
                                 .getType() });
                        }
                        catch (SecurityException ex)
                        {
                        }
                        catch (NoSuchMethodException ex)
                        { /* don't care */
                        }

                        if (Modifier.isPublic(f.getModifiers())
                              || (getMethod != null
                                    && Modifier.isPublic(getMethod
                                          .getModifiers()) || (setMethod != null && Modifier
                                    .isPublic(setMethod.getModifiers()))))
                        {
                           properties.add(f.getName());
                        }
                     }
                  }

                  //
                  for (Method m : c.getDeclaredMethods())
                  {
                     if (m.getName().startsWith("get")
                           || m.getName().startsWith("is"))
                     {
                        int startIdx = m.getName().startsWith("get") ? 3 : 2;

                        try
                        {
                           c.getMethod(String.format("set%s", m.getName()
                                 .substring(startIdx)), m.getReturnType());
                        }
                        catch (NoSuchMethodException ex)
                        {
                           continue;
                        }

                        String propertyName = String.format("%s%s", Character
                              .toLowerCase(m.getName().charAt(startIdx)), m
                              .getName().substring(startIdx + 1));

                        if (!properties.contains(propertyName))
                        {
                           properties.add(propertyName);
                        }
                     }
                  }

                  c = c.getSuperclass();
               }

               accessibleProperties.put(cls, properties);
            }
         }
      }

      return accessibleProperties.get(cls);
   }

   /**
    * Appends component interface code to an outputstream for a specified
    * component.
    * 
    * @param out
    *           OutputStream The OutputStream to write to
    * @param component
    *           Component The component to generate an interface for
    * @param types
    *           Set A list of types that have already been generated for this
    *           request. If this component has already been generated (i.e. it
    *           is in the list) then it won't be generated again
    * @throws IOException
    *            If there is an error writing to the OutputStream.
    */
   private void appendBeanSource(OutputStream out, Class<?> beanClass, Set<Type> types)
         throws IOException
   {
      StringBuilder componentSrc = new StringBuilder();

      Set<Class<?>> componentTypes = new HashSet<Class<?>>();

      // Check if any of the methods are annotated with @WebRemote, and if so
      // treat it as an "action" component instead of a type component
      for (Method m : beanClass.getDeclaredMethods())
      {
         if (m.getAnnotation(WebRemote.class) != null)
         {
            componentTypes.add(beanClass);
            break;
         }
      }

      if (componentTypes.isEmpty())
      {
         appendTypeSource(out, beanClass, types);
         return;
      }

      // If types already contains all the component types, then return
      boolean foundNew = false;
      for (Class<?> type : componentTypes)
      {
         if (!types.contains(type))
         {
            foundNew = true;
            break;
         }
      }

      if (!foundNew)
      {
         return;
      }

      String name = beanManager.getBeans(beanClass).iterator().next().getName();
      String beanName = name != null ? name : beanClass.getName();
      if (beanName.contains("."))
      {
         componentSrc.append("Seam.Remoting.createNamespace('");
         componentSrc.append(beanName.substring(0, beanName.lastIndexOf('.')));
         componentSrc.append("');\n");

      }

      componentSrc.append("Seam.Remoting.type.");
      componentSrc.append(beanName);
      componentSrc.append(" = function() {\n");
      componentSrc.append("  this.__callback = new Object();\n");

      for (Class<?> type : componentTypes)
      {
         if (types.contains(type))
         {
            break;
         }
         else
         {
            types.add(type);

            for (Method m : type.getDeclaredMethods())
            {
               if (m.getAnnotation(WebRemote.class) == null)
                  continue;

               // Append the return type to the source block
               appendTypeSource(out, m.getGenericReturnType(), types);

               componentSrc.append("  Seam.Remoting.type.");
               componentSrc.append(beanName);
               componentSrc.append(".prototype.");
               componentSrc.append(m.getName());
               componentSrc.append(" = function(");

               // Insert parameters p0..pN
               for (int i = 0; i < m.getGenericParameterTypes().length; i++)
               {
                  appendTypeSource(out, m.getGenericParameterTypes()[i], types);

                  if (i > 0)
                  {
                     componentSrc.append(", ");
                  }
                  componentSrc.append("p");
                  componentSrc.append(i);
               }

               if (m.getGenericParameterTypes().length > 0)
                  componentSrc.append(", ");

               componentSrc.append("callback, exceptionHandler) {\n");
               componentSrc.append("    return Seam.Remoting.execute(this, \"");
               componentSrc.append(m.getName());
               componentSrc.append("\", [");

               for (int i = 0; i < m.getParameterTypes().length; i++)
               {
                  if (i > 0)
                     componentSrc.append(", ");
                  componentSrc.append("p");
                  componentSrc.append(i);
               }

               componentSrc.append("], callback, exceptionHandler);\n");
               componentSrc.append("  }\n");
            }
         }
         componentSrc.append("}\n");

         // Set the component name
         componentSrc.append("Seam.Remoting.type.");
         componentSrc.append(beanName);
         componentSrc.append(".__name = \"");
         componentSrc.append(beanName);
         componentSrc.append("\";\n\n");

         // Register the component
         componentSrc.append("Seam.Remoting.registerBean(Seam.Remoting.type.");
         componentSrc.append(beanName);
         componentSrc.append(");\n\n");

         out.write(componentSrc.toString().getBytes());
      }
   }

   /**
    * Append Javascript interface code for a specified class to a block of code.
    * 
    * @param source
    *           StringBuilder The code block to append to
    * @param type
    *           Class The type to generate a Javascript interface for
    * @param types
    *           Set A list of the types already generated (only include each
    *           type once).
    */
   private void appendTypeSource(OutputStream out, Type type, Set<Type> types)
         throws IOException
   {
      if (type instanceof Class<?>)
      {
         Class<?> classType = (Class<?>) type;

         if (classType.isArray())
         {
            appendTypeSource(out, classType.getComponentType(), types);
            return;
         }

         if (classType.getName().startsWith("java.") || types.contains(type)
               || classType.isPrimitive())
         {
            return;
         }

         // Keep track of which types we've already added
         types.add(type);

         appendClassSource(out, classType, types);
      }
      else if (type instanceof ParameterizedType)
      {
         for (Type t : ((ParameterizedType) type).getActualTypeArguments())
         {
            appendTypeSource(out, t, types);
         }
      }
   }

   /**
    * Appends the interface code for a non-component class to an OutputStream.
    * 
    * @param out
    *           OutputStream
    * @param classType
    *           Class
    * @param types
    *           Set
    * @throws IOException
    */
   private void appendClassSource(OutputStream out, Class<?> classType,
         Set<Type> types) throws IOException
   {
      // Don't generate interfaces for enums
      if (classType.isEnum())
      {
         return;
      }

      StringBuilder typeSource = new StringBuilder();

      // Determine whether this class is a component; if so, use its name
      // otherwise use its class name.
      Bean<?> bean = beanManager.getBeans(classType).iterator().next();
      
      String componentName = bean.getName();
      if (componentName == null)
         componentName = classType.getName();

      String typeName = componentName.replace('.', '$');

      typeSource.append("Seam.Remoting.type.");
      typeSource.append(typeName);
      typeSource.append(" = function() {\n");

      StringBuilder fields = new StringBuilder();
      StringBuilder accessors = new StringBuilder();
      StringBuilder mutators = new StringBuilder();
      Map<String, String> metadata = new HashMap<String, String>();

      String getMethodName = null;
      String setMethodName = null;

      for (String propertyName : getAccessibleProperties(classType))
      {
         Type propertyType = null;

         Field f = null;
         try
         {
            f = classType.getDeclaredField(propertyName);
            propertyType = f.getGenericType();
         }
         catch (NoSuchFieldException ex)
         {
            setMethodName = String.format("set%s%s", Character
                  .toUpperCase(propertyName.charAt(0)), propertyName
                  .substring(1));

            try
            {
               getMethodName = String.format("get%s%s", Character
                     .toUpperCase(propertyName.charAt(0)), propertyName
                     .substring(1));
               propertyType = classType.getMethod(getMethodName)
                     .getGenericReturnType();
            }
            catch (NoSuchMethodException ex2)
            {
               try
               {
                  getMethodName = String.format("is%s%s", Character
                        .toUpperCase(propertyName.charAt(0)), propertyName
                        .substring(1));

                  propertyType = classType.getMethod(getMethodName)
                        .getGenericReturnType();
               }
               catch (NoSuchMethodException ex3)
               {
                  // ???
                  continue;
               }
            }
         }

         appendTypeSource(out, propertyType, types);

         // Include types referenced by generic declarations
         if (propertyType instanceof ParameterizedType)
         {
            for (Type t : ((ParameterizedType) propertyType)
                  .getActualTypeArguments())
            {
               if (t instanceof Class<?>)
               {
                  appendTypeSource(out, t, types);
               }
            }
         }

         if (f != null)
         {
            String fieldName = propertyName.substring(0, 1).toUpperCase()
                  + propertyName.substring(1);
            String getterName = String.format("get%s", fieldName);
            String setterName = String.format("set%s", fieldName);

            try
            {
               classType.getMethod(getterName);
               getMethodName = getterName;
            }
            catch (SecurityException ex)
            {
            }
            catch (NoSuchMethodException ex)
            {
               getterName = String.format("is%s", fieldName);
               try
               {
                  if (Modifier.isPublic(classType.getMethod(getterName)
                        .getModifiers()))
                     getMethodName = getterName;
               }
               catch (NoSuchMethodException ex2)
               { /* don't care */
               }
            }

            try
            {
               if (Modifier.isPublic(classType.getMethod(setterName,
                     f.getType()).getModifiers()))
                  setMethodName = setterName;
            }
            catch (SecurityException ex)
            {
            }
            catch (NoSuchMethodException ex)
            { /* don't care */
            }
         }

         // Construct the list of fields.
         if (getMethodName != null || setMethodName != null)
         {
            metadata.put(propertyName, getFieldType(propertyType));

            fields.append("  this.");
            fields.append(propertyName);
            fields.append(" = undefined;\n");

            if (getMethodName != null)
            {
               accessors.append("  Seam.Remoting.type.");
               accessors.append(typeName);
               accessors.append(".prototype.");
               accessors.append(getMethodName);
               accessors.append(" = function() { return this.");
               accessors.append(propertyName);
               accessors.append("; }\n");
            }

            if (setMethodName != null)
            {
               mutators.append("  Seam.Remoting.type.");
               mutators.append(typeName);
               mutators.append(".prototype.");
               mutators.append(setMethodName);
               mutators.append(" = function(");
               mutators.append(propertyName);
               mutators.append(") { this.");
               mutators.append(propertyName);
               mutators.append(" = ");
               mutators.append(propertyName);
               mutators.append("; }\n");
            }
         }
      }

      typeSource.append(fields);
      typeSource.append(accessors);
      typeSource.append(mutators);

      typeSource.append("}\n\n");

      // Append the type name
      typeSource.append("Seam.Remoting.type.");
      typeSource.append(typeName);
      typeSource.append(".__name = \"");
      typeSource.append(componentName);
      typeSource.append("\";\n");

      // Append the metadata
      typeSource.append("Seam.Remoting.type.");
      typeSource.append(typeName);
      typeSource.append(".__metadata = [\n");

      boolean first = true;

      for (String key : metadata.keySet())
      {
         if (!first)
            typeSource.append(",\n");

         typeSource.append("  {field: \"");
         typeSource.append(key);
         typeSource.append("\", type: \"");
         typeSource.append(metadata.get(key));
         typeSource.append("\"}");

         first = false;
      }

      typeSource.append("];\n\n");

      // Register the type under Seam.Component if it is a component, otherwise
      // register it under Seam.Remoting

      // TODO fix this - a bean might not be named
      if (classType.isAnnotationPresent(Named.class))
      {
         typeSource.append("Seam.Remoting.registerBean(Seam.Remoting.type.");
      }
      else
      {
         typeSource.append("Seam.Remoting.registerType(Seam.Remoting.type.");
      }

      typeSource.append(typeName);
      typeSource.append(");\n\n");

      out.write(typeSource.toString().getBytes());
   }

   /**
    * Returns the remoting "type" for a specified class.
    * 
    * @param type
    *           Class
    * @return String
    */
   protected String getFieldType(Type type)
   {
      if (type.equals(String.class)
            || (type instanceof Class<?> && ((Class<?>) type).isEnum())
            || type.equals(BigInteger.class) || type.equals(BigDecimal.class))
      {
         return "str";
      }
      else if (type.equals(Boolean.class) || type.equals(Boolean.TYPE))
      {
         return "bool";
      }
      else if (type.equals(Short.class) || type.equals(Short.TYPE)
            || type.equals(Integer.class) || type.equals(Integer.TYPE)
            || type.equals(Long.class) || type.equals(Long.TYPE)
            || type.equals(Float.class) || type.equals(Float.TYPE)
            || type.equals(Double.class) || type.equals(Double.TYPE)
            || type.equals(Byte.class) || type.equals(Byte.TYPE))
      {
         return "number";
      }
      else if (type instanceof Class<?>)
      {
         Class<?> cls = (Class<?>) type;
         if (Date.class.isAssignableFrom(cls)
               || Calendar.class.isAssignableFrom(cls))
         {
            return "date";
         }
         else if (cls.isArray())
         {
            return "bag";
         }
         else if (cls.isAssignableFrom(Map.class))
         {
            return "map";
         }
         else if (cls.isAssignableFrom(Collection.class))
         {
            return "bag";
         }
      }
      else if (type instanceof ParameterizedType)
      {
         ParameterizedType pt = (ParameterizedType) type;

         if (pt.getRawType() instanceof Class<?>
               && Map.class.isAssignableFrom((Class<?>) pt.getRawType()))
         {
            return "map";
         }
         else if (pt.getRawType() instanceof Class<?>
               && Collection.class.isAssignableFrom((Class<?>) pt.getRawType()))
         {
            return "bag";
         }
      }

      return "bean";
   }
}
