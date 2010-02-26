package org.jboss.seam.remoting.messaging;

import java.util.ArrayList;
import java.util.List;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

/**
 *
 * @author Shane Bryzak
 */
public class RemoteSubscriber
{
  private String token;
  private String topicName;

  private Topic topic;
  private TopicSession topicSession;
  private TopicSubscriber subscriber;
  private SubscriptionRegistry registry;

  public RemoteSubscriber(String token, String topicName, SubscriptionRegistry registry)
  {
    this.token = token;
    this.topicName = topicName;
    this.registry = registry;
  }

  public String getToken()
  {
    return token;
  }

  public String getTopicName()
  {
    return topicName;
  }

  public void subscribe(TopicConnection conn)
      throws JMSException
  {
    topicSession = conn.createTopicSession(false, javax.jms.Session.AUTO_ACKNOWLEDGE);
    topic = topicSession.createTopic(topicName);
    subscriber = topicSession.createSubscriber(topic);
  }

  public void unsubscribe()
  {
    try {
      subscriber.close();

      // Remove the subscription's token from the user's session context
      registry.getUserTokens().remove(token);
    }
    catch (JMSException ex) { }

    try {
      topicSession.close();
    }
    catch (JMSException ex) { }
  }

  public void setTopicSubscriber(TopicSubscriber subscriber)
  {
    this.subscriber = subscriber;
  }

  public TopicSubscriber getTopicSubscriber()
  {
    return subscriber;
  }

  public List<Message> poll(int timeout)
      throws JMSException
  {
    List<Message> messages = null;

    Message m = null;

    synchronized(subscriber)
    {
      do {
          // Only timeout for the first message.. subsequent messages should be nowait
        if (messages == null && timeout > 0)
          m = subscriber.receive(timeout * 1000);
        else
          m = subscriber.receiveNoWait();

        if (m != null) {
          if (messages == null)
            messages = new ArrayList<Message> ();
          messages.add(m);
        }
      }
      while (m != null);
    }

    return messages;
  }
}
