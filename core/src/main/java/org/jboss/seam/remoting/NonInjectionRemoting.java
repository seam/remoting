package org.jboss.seam.remoting;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;

import org.jboss.seam.remoting.model.ModelHandler;
import org.jboss.weld.extensions.beanManager.BeanManagerAccessor;

public class NonInjectionRemoting extends Remoting
{
   private static final long serialVersionUID = -8985912269669096603L;
     
   private static BeanManager getBeanManager(ServletContext ctx)
   {
      return BeanManagerAccessor.getManager();
   }
   
   @SuppressWarnings("unchecked")
   @Override
   protected ExecutionHandler getExecutionHandler()
   {
      BeanManager beanManager = getBeanManager(getServletConfig().getServletContext());
      Bean<ExecutionHandler> bean = (Bean<ExecutionHandler>) beanManager.getBeans(
            ExecutionHandler.class).iterator().next();
      return bean.create(beanManager.createCreationalContext(bean));
   }
   
   @SuppressWarnings("unchecked")
   @Override
   protected InterfaceGenerator getInterfaceHandler()
   {
      BeanManager beanManager = getBeanManager(getServletConfig().getServletContext());
      Bean<InterfaceGenerator> bean = (Bean<InterfaceGenerator>) beanManager.getBeans(
            InterfaceGenerator.class).iterator().next();
      return bean.create(beanManager.createCreationalContext(bean));
   }
   
   @SuppressWarnings("unchecked")
   @Override
   protected ModelHandler getModelHandler()
   {
      BeanManager beanManager = getBeanManager(getServletConfig().getServletContext());
      Bean<ModelHandler> bean = (Bean<ModelHandler>) beanManager.getBeans(
            ModelHandler.class).iterator().next();
      return bean.create(beanManager.createCreationalContext(bean));
   }
}