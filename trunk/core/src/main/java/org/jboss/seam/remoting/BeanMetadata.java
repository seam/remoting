package org.jboss.seam.remoting;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains the metadata (either the accessible properties or invokable methods)
 * for a single action or state bean.
 *  
 * @author Shane Bryzak
 */
public class BeanMetadata
{
   public enum BeanType {action, state}
      
   private BeanType beanType;
   
   private String name;
   private Map<String,Integer> methods;
   private Map<String,String> properties;
   
   public BeanMetadata(BeanType beanType, String name)
   {
      this.beanType = beanType;
      this.name = name;
      
      if (beanType == BeanType.action)
      {
         methods = new HashMap<String,Integer>();
      }
      else
      {
         properties = new HashMap<String,String>();
      }
   }
   
   public BeanType getBeanType()
   {
      return beanType;
   }
   
   public String getName()
   {
      return name;
   }
   
   public void addMethod(String name, int paramCount)
   {
      methods.put(name, paramCount);
   }
   
   public void addProperty(String name, String remotingType)
   {
      properties.put(name, remotingType);
   }
   
   public Map<String,Integer> getMethods()
   {
      return methods;
   }
   
   public Map<String,String> getProperties()
   {
      return properties;
   }
}
