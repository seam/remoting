package org.jboss.seam.remoting.wrapper;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

/**
 * 
 * @author Shane Bryzak
 */
public class WrapperFactory
{
   /**
    * Singleton instance.
    */
   private static final WrapperFactory factory = new WrapperFactory();

   /**
    * A registry of wrapper types
    */
   private Map<String, Class<?>> wrapperRegistry = new HashMap<String, Class<?>>();

   @SuppressWarnings("unchecked")
   private Map<Class, Class> classRegistry = new HashMap<Class, Class>();

   /**
    * Private constructor
    */
   private WrapperFactory()
   {
      // Register the defaults
      registerWrapper("str", StringWrapper.class);
      registerWrapper("bool", BooleanWrapper.class);
      registerWrapper("bean", BeanWrapper.class);
      registerWrapper("number", NumberWrapper.class);
      registerWrapper("null", NullWrapper.class);
      registerWrapper("bag", BagWrapper.class);
      registerWrapper("map", MapWrapper.class);
      registerWrapper("date", DateWrapper.class);

      // String types
      registerWrapperClass(String.class, StringWrapper.class);
      registerWrapperClass(StringBuilder.class, StringWrapper.class);
      registerWrapperClass(StringBuffer.class, StringWrapper.class);
      registerWrapperClass(Character.class, StringWrapper.class);

      // Big numbers are handled by StringWrapper
      registerWrapperClass(BigDecimal.class, StringWrapper.class);
      registerWrapperClass(BigInteger.class, StringWrapper.class);

      // Number types
      registerWrapperClass(Integer.class, NumberWrapper.class);
      registerWrapperClass(Long.class, NumberWrapper.class);
      registerWrapperClass(Short.class, NumberWrapper.class);
      registerWrapperClass(Double.class, NumberWrapper.class);
      registerWrapperClass(Float.class, NumberWrapper.class);
      registerWrapperClass(Byte.class, NumberWrapper.class);
   }

   public void registerWrapper(String type, Class<?> wrapperClass)
   {
      wrapperRegistry.put(type, wrapperClass);
   }

   public void registerWrapperClass(Class<?> cls, Class<?> wrapperClass)
   {
      classRegistry.put(cls, wrapperClass);
   }

   public static WrapperFactory getInstance()
   {
      return factory;
   }

   @SuppressWarnings("unchecked")
   public Wrapper createWrapper(String type, BeanManager beanManager)
   {
      Class wrapperClass = wrapperRegistry.get(type);

      if (wrapperClass != null)
      {
         try
         {
            Constructor<Wrapper> c = wrapperClass
                  .getConstructor(BeanManager.class);
            Wrapper wrapper = c.newInstance(beanManager);
            return wrapper;
         } catch (Exception ex)
         {
         }
      }

      throw new RuntimeException(String.format(
            "Failed to create wrapper for type: %s", type));
   }

   @SuppressWarnings("unchecked")
   public Wrapper getWrapperForObject(Object obj, BeanManager beanManager)
   {
      if (obj == null)
         return new NullWrapper(beanManager);

      Wrapper w = null;

      if (Map.class.isAssignableFrom(obj.getClass()))
         w = new MapWrapper(beanManager);
      else if (obj.getClass().isArray()
            || Collection.class.isAssignableFrom(obj.getClass()))
         w = new BagWrapper(beanManager);
      else if (obj.getClass().equals(Boolean.class)
            || obj.getClass().equals(Boolean.TYPE))
         w = new BooleanWrapper(beanManager);
      else if (obj.getClass().isEnum())
         w = new StringWrapper(beanManager);
      else if (Date.class.isAssignableFrom(obj.getClass())
            || Calendar.class.isAssignableFrom(obj.getClass()))
         w = new DateWrapper(beanManager);
      else if (classRegistry.containsKey(obj.getClass()))
      {
         try
         {
            Constructor<Wrapper> c = classRegistry.get(obj.getClass())
                  .getConstructor(BeanManager.class);
            w = c.newInstance(beanManager);
         } catch (Exception ex)
         {
            throw new RuntimeException("Failed to create wrapper instance.");
         }
      } else
         w = new BeanWrapper(beanManager);

      w.setValue(obj);
      return w;
   }
}
