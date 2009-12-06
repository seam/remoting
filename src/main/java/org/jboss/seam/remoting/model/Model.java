package org.jboss.seam.remoting.model;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.seam.remoting.AnnotationsParser;
import org.jboss.seam.remoting.Call;
import org.jboss.seam.remoting.CallContext;

public class Model implements Serializable
{
   private static final long serialVersionUID = 8318288750036758325L;
   
   private BeanManager beanManager;
   private String id;
   private CallContext callContext;
   
   private class BeanProperty 
   {
      public Bean<?> bean;
      public String propertyName;
      
      public BeanProperty(Bean<?> bean, String propertyName)
      {
         this.bean = bean;
         this.propertyName = propertyName;
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
