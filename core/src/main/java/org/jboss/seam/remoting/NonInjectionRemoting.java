package org.jboss.seam.remoting;

import static org.jboss.weld.logging.messages.ServletMessage.BEAN_DEPLOYMENT_ARCHIVE_MISSING;
import static org.jboss.weld.logging.messages.ServletMessage.BEAN_MANAGER_FOR_ARCHIVE_NOT_FOUND;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.servlet.ServletContext;

import org.jboss.seam.remoting.ExecutionHandler;
import org.jboss.seam.remoting.InterfaceGenerator;
import org.jboss.seam.remoting.Remoting;
import org.jboss.seam.remoting.model.ModelHandler;
import org.jboss.weld.Container;
import org.jboss.weld.bootstrap.spi.BeanDeploymentArchive;
import org.jboss.weld.exceptions.IllegalStateException;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.servlet.api.ServletServices;

public class NonInjectionRemoting extends Remoting
{
   private static final long serialVersionUID = -8985912269669096603L;
     
   private static BeanManagerImpl getBeanManager(ServletContext ctx)
   {
      BeanDeploymentArchive war = Container.instance().services().get(ServletServices.class).getBeanDeploymentArchive(ctx);
      if (war == null)
      {
         throw new IllegalStateException(BEAN_DEPLOYMENT_ARCHIVE_MISSING, ctx);
      }
      BeanManagerImpl beanManager = Container.instance().beanDeploymentArchives().get(war);
      if (beanManager == null)
      {
         throw new IllegalStateException(BEAN_MANAGER_FOR_ARCHIVE_NOT_FOUND, ctx, war);
      }
      return beanManager;
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