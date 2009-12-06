package org.jboss.seam.remoting.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

public class Model implements Serializable
{
   private static final long serialVersionUID = 8318288750036758325L;
   
   private BeanManager beanManager;
   private String id;
   private Map<Integer,Object> refs;
   
   private Set<Bean<?>> beans;
   
   public Model(BeanManager beanManager)
   {
      this.beanManager = beanManager;
      id = UUID.randomUUID().toString();
      refs = new HashMap<Integer,Object>();
      beans = new HashSet<Bean<?>>();
   }
   
   public String getId()
   {
      return id;
   }
   
   public void addBean()
   
   public void applyChanges(Set<ChangeSet> delta)
   {
      
   }
}
