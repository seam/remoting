package org.jboss.seam.remoting;

/**
 *
 * @author Shane Bryzak
 */
public class RequestContext
{
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
