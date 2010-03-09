package org.jboss.seam.remoting.messaging;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * Wrapper for a single request for a specified subscription poll.
 *
 * @author Shane Bryzak
 */
public class PollRequest
{
  private String token;
  private int timeout;
  private List<Message> messages;
  private List<PollError> errors = new ArrayList<PollError>();
  private SubscriptionRegistry registry;

  public PollRequest(String token, int timeout, SubscriptionRegistry registry)
  {
    this.token = token;
    this.timeout = timeout;
    this.registry = registry;
  }

  public String getToken()
  {
    return token;
  }

  public List<Message> getMessages()
  {
    return messages;
  }

  public List<PollError> getErrors()
  {
    return errors;
  }

  public void poll()
  {
    RemoteSubscriber subscriber = registry.getSubscription(token);
    if (subscriber != null)
    {
      try
      {
        messages = subscriber.poll(timeout);
      }
      catch (JMSException ex)
      {
        errors.add(new PollError(PollError.ERROR_CODE_JMS_EXCEPTION,
                                 "Error polling for messages"));
      }
    }
    else
      errors.add(new PollError(PollError.ERROR_CODE_TOKEN_NOT_FOUND,
                               "No subscription was found for the specified token."));
  }
}
