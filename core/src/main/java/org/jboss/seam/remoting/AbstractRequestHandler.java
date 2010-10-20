package org.jboss.seam.remoting;

import javax.enterprise.context.spi.Context;
import javax.enterprise.inject.Instance;

import org.jboss.seam.remoting.util.Strings;
import org.jboss.weld.Container;
import org.jboss.weld.context.http.HttpConversationContext;

/**
 * Abstract base class for remoting request handlers
 * 
 * @author Shane Bryzak
 *
 */
public abstract class AbstractRequestHandler implements RequestHandler
{
   public void activateConversationContext(String conversationId)
   {
      Instance<Context> instance = instance();
      HttpConversationContext conversationContext = instance.select(HttpConversationContext.class).get();
      
      if (conversationId != null && !Strings.isEmpty(conversationId))
      {
         conversationContext.activate(conversationId);
      }
   }
   
   private static Instance<Context> instance()
   {
      return Container.instance().deploymentManager().instance().select(Context.class);
   }
}
