package org.jboss.seam.remoting.model;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.seam.remoting.AnnotationsParser;
import org.jboss.seam.remoting.Call;
import org.jboss.seam.remoting.CallContext;

/**
 * Manages a model request
 *  
 * @author Shane Bryzak
 */
public class Model implements Serializable
{
   private static final long serialVersionUID = 8318288750036758325L;
   
   private BeanManager beanManager;
   private String id;
   private CallContext callContext;
   
   public class BeanProperty 
   {
      private Bean<?> bean;
      private String propertyName;
      private Object value;      
      
      public BeanProperty(Bean<?> bean, String propertyName)
      {
         this.bean = bean;
         this.propertyName = propertyName;
      }      
      
      public Bean<?> getBean()
      {
         return bean;
      }
      
      @SuppressWarnings("unchecked")
      public void evaluate(CreationalContext ctx)
      {
         Object instance = bean.create(ctx);
         
         if (propertyName != null)
         {
            try
            {
               Field f = bean.getBeanClass().getField(propertyName);
               boolean accessible = f.isAccessible();
               try
               {
                  f.setAccessible(true);
                  value = f.get(instance);
               }
               catch (Exception e)
               {
                  throw new RuntimeException(
                        "Exception reading model property " + propertyName +
                        " from bean [" + bean + "]");
               }
               finally
               {
                  f.setAccessible(accessible);
               }
            }
            catch (NoSuchFieldException ex)
            {
               // Try the getter method
               String methodName = "get" + propertyName.substring(0, 1).toUpperCase() +
                  propertyName.substring(1);
               
               try
               {
                  Method m = bean.getBeanClass().getMethod(methodName);
                  value = m.invoke(instance);
               }
               catch (Exception e)
               {
                  throw new RuntimeException(
                        "Exception reading model property " + propertyName +
                        " from bean [" + bean + "]");
               }
            }
         }
         else
         {
            this.value = instance;
         }
      }
      
      public Object getValue()
      {
         return value;
      }
   }
   
   private Map<String, BeanProperty> beanProperties;
   
   public Model(BeanManager beanManager)
   {
      this.beanManager = beanManager;
      id = UUID.randomUUID().toString();
      callContext = new CallContext(beanManager);
      beanProperties = new HashMap<String, BeanProperty>();
   }
   
   /**
    * Evaluate each of the model's bean properties, expressions, etc and
    * store the values in the BeanProperty map.
    */
   public void evaluate()
   {
      
      for (String alias : beanProperties.keySet())
      {         
         BeanProperty property = beanProperties.get(alias);
         property.evaluate(beanManager.createCreationalContext(property.getBean()));         
      }
   }
   
   public Map<String,BeanProperty> getBeanProperties()
   {
      return beanProperties;
   }
   
   public String getId()
   {
      return id;
   }
   
   public CallContext getCallContext()
   {
      return callContext;
   }
   
   public void addBean(String alias, String beanName, String qualifiers, String propertyName)
   {
      Set<Bean<?>> beans = beanManager.getBeans(beanName);
      
      if (beans.isEmpty())
      {
         try
         {
            Class<?> beanType = Class.forName(beanName);
            
            Annotation[] q = qualifiers != null && !qualifiers.isEmpty() ? 
                  new AnnotationsParser(beanType, qualifiers, beanManager).getAnnotations() : 
                     Call.EMPTY_ANNOTATIONS;

            beans = beanManager.getBeans(beanType, q);            
         }
         catch (ClassNotFoundException ex)
         {
            throw new IllegalArgumentException("Invalid bean class specified: " + beanName);
         }
                  
         if (beans.isEmpty())
         {
            throw new IllegalArgumentException(
                  "Could not find bean with bean with type/name " + beanName + 
                  ", qualifiers [" + qualifiers + "]");         
         }         
      }  
      
      Bean<?> bean = beans.iterator().next();
      beanProperties.put(alias, new BeanProperty(bean, propertyName));
   }
   
   public void applyChanges(Set<ChangeSet> delta)
   {
      
   }
}
