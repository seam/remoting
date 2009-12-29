package org.jboss.seam.remoting;

import java.io.IOException;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.remoting.BeanMetadata.BeanType;
import org.jboss.seam.remoting.annotations.WebRemote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Caches BeanMetadata instances
 *  
 * @author Shane Bryzak
 */
@ApplicationScoped
public class MetadataCache
{
   private static final Logger log = LoggerFactory.getLogger(MetadataCache.class);
   
   private Map<Class<?>,BeanMetadata> metadataCache;
   
   private Map<Class<?>, Set<Class<?>>> beanDependencies;
   
   /**
    * A cache of the accessible properties for a bean class
    */
   private Map<Class<?>, Map<String,Type>> accessibleProperties = new HashMap<Class<?>, Map<String,Type>>();   
   
   @Inject BeanManager beanManager;
   
   public MetadataCache()
   {
      metadataCache = new HashMap<Class<?>, BeanMetadata>();
      beanDependencies = new HashMap<Class<?>, Set<Class<?>>>();
   }
   
   public BeanMetadata getMetadata(Class<?> beanClass)
   {
      if (beanClass == null) throw new IllegalArgumentException("beanClass cannot be null");
      
      if (metadataCache.containsKey(beanClass))
      {
         return metadataCache.get(beanClass);
      }
      else
      {
         synchronized(metadataCache)
         {
            if (!metadataCache.containsKey(beanClass))
            {               
               metadataCache.put(beanClass, generateBeanMetadata(beanClass));
               
               Set<Class<?>> dependencies = beanDependencies.get(beanClass);
               if (dependencies != null)
               {
                  for (Class<?> dependency : dependencies)
                  {
                     getMetadata(dependency);
                  }
               }
            }
         }
         return metadataCache.get(beanClass);
      }
   }
   
   @WebRemote
   public Set<BeanMetadata> loadBeans(Set<String> names)
   {
      Set<BeanMetadata> meta = new HashSet<BeanMetadata>();
      
      for (String name : names)
      {      
         Class<?> beanClass = null;
   
         Set<Bean<?>> beans = beanManager.getBeans(name);
         
         if (!beans.isEmpty())
         {
            beanClass = beans.iterator().next().getBeanClass();
         }
         else
         {
            try
            {
               beanClass = Class.forName(name);
            }
            catch (ClassNotFoundException ex) 
            {
               log.error(String.format("Component not found: [%s]", name));
               throw new IllegalArgumentException(String.format("Component not found: [%s]", name));             
            }            
         }
         
         addBeanDependencies(beanClass, meta);
      }     
            
      return meta;
   }
   
   private void addBeanDependencies(Class<?> beanClass, Set<BeanMetadata> types)
   {
      types.add(getMetadata(beanClass));
   
      for (Class<?> dependencyClass : getDependencies(beanClass))
      {
         if (!types.contains(dependencyClass))
         {
            addBeanDependencies(dependencyClass, types);
         }
      }
   }
   
   private BeanMetadata generateBeanMetadata(Class<?> beanClass)
   {     
      BeanType beanType = BeanType.state;
      String name = beanClass.getName();
      
      // If any of the methods are annotated with @WebRemote, it's an action bean
      for (Method m : beanClass.getDeclaredMethods())
      {
         if (m.getAnnotation(WebRemote.class) != null)
         {
            beanType = BeanType.action;
            String beanName = beanManager.getBeans(beanClass).iterator().next().getName();
            if (beanName != null)
            {
               name = beanName;
            }           
            break;
         }
      }
      
      BeanMetadata meta = new BeanMetadata(beanType, name);
      
      if (beanType == BeanType.state)
      {
         populateMetadataProperties(beanClass, meta);
      }
      else
      {
         populateMetadataMethods(beanClass, meta);
      }
      
      return meta;
   }
   
   private void populateMetadataProperties(Class<?> beanClass, BeanMetadata meta)
   {
      Map<String,Type> props = getAccessibleProperties(beanClass);
      
      for (String propertyName : props.keySet())
      {
         Type propertyType = props.get(propertyName);
         
         meta.addProperty(propertyName, getFieldType(propertyType));
         addTypeDependency(beanClass, propertyType);      
      }
   }
   
   private void populateMetadataMethods(Class<?> beanClass, BeanMetadata meta)
   {
      for (Method m : beanClass.getDeclaredMethods())
      {
         if (m.getAnnotation(WebRemote.class) == null) continue;
         meta.addMethod(m.getName(), m.getParameterTypes().length);

         addTypeDependency(beanClass, m.getGenericReturnType());
         for (int i = 0; i < m.getGenericParameterTypes().length; i++)
         {            
            addTypeDependency(beanClass, m.getGenericParameterTypes()[i]);
         }                
      }      
   }
   
   private void addTypeDependency(Class<?> beanType, Type dependency)
   {
      if (!beanDependencies.containsKey(beanType))
      {
         beanDependencies.put(beanType, new HashSet<Class<?>>());
      }
      
      Set<Class<?>> dependencies = beanDependencies.get(beanType);
      
      if (dependencies.contains(dependency)) return;
            
      if (dependency instanceof Class<?>)
      {
         Class<?> classType = (Class<?>) dependency;

         if (classType.isArray())
         {
            addTypeDependency(beanType, classType.getComponentType());
            return;
         }

         if (classType.getName().startsWith("java.") || classType.isPrimitive() 
               || classType.isEnum())
         {
            return;
         }

         dependencies.add(classType);
      }
      else if (dependency instanceof ParameterizedType)
      {
         for (Type t : ((ParameterizedType) dependency).getActualTypeArguments())
         {
            addTypeDependency(beanType, t);
         }
      }      
   }
   
   /**
    * Returns the metadata for the specified bean type and all of its reachable
    * dependencies
    * 
    * @param beanType
    * @return
    */
   public Set<Class<?>> getDependencies(Class<?> beanType)
   {
      return beanDependencies.get(beanType);
   }
   
   /**
    * Returns the remoting "type" for a specified class.
    * 
    * @param type Type The type for which to return the remoting type.
    * @return String The remoting type for the specified type.
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
   
   /**
    * A helper method, used internally by InterfaceGenerator and also when
    * serializing responses. Returns a list of the property names for the
    * specified class which should be included in the generated interface for
    * the type.
    * 
    * @param cls Class The class to scan for accessible properties
    * @return Set<String> A Set containing the accessible property names
    */
   public Map<String,Type> getAccessibleProperties(Class<?> cls)
   {      
      if (cls.getName().contains("EnhancerByCGLIB"))
         cls = cls.getSuperclass();

      if (!accessibleProperties.containsKey(cls))
      {
         synchronized (accessibleProperties)
         {
            if (!accessibleProperties.containsKey(cls))
            {
               Map<String, Type> properties = new HashMap<String, Type>();

               Class<?> c = cls;
               while (c != null && !c.equals(Object.class))
               {
                  // Scan the declared fields
                  for (Field f : c.getDeclaredFields())
                  {
                     // If we already have this field, continue processing
                     if (properties.containsKey(f.getName()))
                     {
                        continue;
                     }
                     
                     // Don't include transient or static fields
                     if (!Modifier.isTransient(f.getModifiers())
                           && !Modifier.isStatic(f.getModifiers()))
                     {
                        if (Modifier.isPublic(f.getModifiers()))
                        {
                           properties.put(f.getName(), f.getGenericType());
                        }
                        else
                        {                        
                           // Look for a public getter method
                           String fieldName = f.getName().substring(0, 1).toUpperCase()
                                 + f.getName().substring(1);
                                                   
                           String getterName = String.format("get%s", fieldName);
                           Method getMethod = null;
   
                           try
                           {
                              getMethod = c.getMethod(getterName);
                           }
                           catch (SecurityException ex) { }
                           catch (NoSuchMethodException ex)
                           {
                              // it might be an "is" method...
                              getterName = String.format("is%s", fieldName);
                              try
                              {
                                 getMethod = c.getMethod(getterName);
                              }
                              catch (NoSuchMethodException ex2) { }
                           }
                           
                           if (getMethod != null && Modifier.isPublic(getMethod.getModifiers()))
                           {
                              properties.put(f.getName(), getMethod.getGenericReturnType());
                           }
                           else
                           {   
                              // Last resort, look for a public setter method
                              String setterName = String.format("set%s", fieldName);
                              Method setMethod = null;
                              try
                              {
                                 setMethod = c.getMethod(setterName, new Class[] { f.getType() });
                              }
                              catch (SecurityException ex) { }
                              catch (NoSuchMethodException ex) { }
      
                              if (setMethod != null && Modifier.isPublic(setMethod.getModifiers()))
                              {
                                 properties.put(f.getName(), setMethod.getGenericParameterTypes()[0]);
                              }
                           }
                        }
                     }
                  }

                  // Scan the declared methods
                  for (Method m : c.getDeclaredMethods())
                  {
                     if (m.getName().startsWith("get") || m.getName().startsWith("is"))
                     {
                        int startIdx = m.getName().startsWith("get") ? 3 : 2;

                        try
                        {
                           c.getMethod(String.format("set%s", m.getName()
                                 .substring(startIdx)), m.getReturnType());
                        }
                        catch (NoSuchMethodException ex)
                        {
                           // If there is no setter method, ignore and continue processing
                           continue;
                        }

                        String propertyName = String.format("%s%s", Character
                              .toLowerCase(m.getName().charAt(startIdx)), m
                              .getName().substring(startIdx + 1));

                        if (!properties.containsKey(propertyName))
                        {
                           properties.put(propertyName, m.getGenericReturnType());
                        }
                     }
                  }

                  // Recursively scan the class hierarchy
                  c = c.getSuperclass();
               }

               accessibleProperties.put(cls, properties);
            }
         }
      }

      return accessibleProperties.get(cls);
   }   
   
}
