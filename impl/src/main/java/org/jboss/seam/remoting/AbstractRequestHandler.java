package org.jboss.seam.remoting;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.conversation.spi.SeamConversationContext;

/**
 * Abstract base class for remoting request handlers. 
 * 
 * Currently this class is non-portable
 * 
 * @author Shane Bryzak
 * @author Ales Justin
 */
public abstract class AbstractRequestHandler implements RequestHandler
{
   @Inject SeamConversationContext<HttpServletRequest> scc; // = SeamConversationContextFactory.getContext(HttpServletRequest.class);

   public void activateConversationContext(HttpServletRequest request, String conversationId)
   {
      scc.associate(request).activate(conversationId);
   }
   
   public void deactivateConversationContext(HttpServletRequest request)
   {
      scc.invalidate().deactivate().dissociate(request);
   }
}
