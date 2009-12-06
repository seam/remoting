package org.jboss.seam.remoting;

import org.dom4j.Element;

/**
 *
 * @author Shane Bryzak
 */
public class RequestContext
{
   public RequestContext(Element header)
   {
      if (header != null)
      {
         Element context = header.element("context");
         if (context != null)
         {
            Element convId = context.element("conversationId");
            if (convId != null)
            {
               setConversationId(convId.getText());
            }
         }
      }
   }
   
  private String conversationId;

  public String getConversationId()
  {
    return conversationId;
  }

  public void setConversationId(String conversationId)
  {
    this.conversationId = conversationId;
  }
}
