package org.jboss.seam.remoting.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

@SessionScoped
public class ModelRegistry implements Serializable
{
   private static final long serialVersionUID = -2952670948046596460L;
   
   @Inject BeanManager beanManager;
   private Map<String,Model> models;
   
   public ModelRegistry()
   {
      models = new HashMap<String,Model>();
   }
   
   public Model createModel()
   {
      Model model = new Model(beanManager);
      models.put(model.getId(), model);
      return model;
   }
   
   public Model getModel(String id)
   {
      return models.get(id);
   }
   
   
}
