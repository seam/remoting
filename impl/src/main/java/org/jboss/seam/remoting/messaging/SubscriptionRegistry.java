package org.jboss.seam.remoting.messaging;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.TopicConnection;

/**
 *
 * @author Shane Bryzak
 */
@ApplicationScoped
public class SubscriptionRegistry implements Serializable
{
   private static final long serialVersionUID = -3054539610480557675L;

   private String connectionProvider;

  private Object monitor = new Object();

  private Map<String,RemoteSubscriber> subscriptions = new HashMap<String,RemoteSubscriber>();
   
  @Inject Instance<UserTokens> userTokenInstance;
  @Inject Instance<TopicConnection> topicConnectionInstance;
  
  private volatile TopicConnection topicConnection;
  
  /**
   * Contains a list of all the topics that clients are allowed to subscribe to.
   */
  private Set<String> allowedTopics = new HashSet<String>();

  public Set<String> getAllowedTopics()
  {
    return allowedTopics;
  }

  public void setAllowedTopics(Set<String> allowedTopics)
  {
    this.allowedTopics = allowedTopics;
  }

  public String getConnectionProvider()
  {
    return connectionProvider;
  }

  public void setConnectionProvider(String connectionProvider)
  {
    this.connectionProvider = connectionProvider;
  }

  private TopicConnection getTopicConnection()
    throws Exception
  {
    if (topicConnection == null)
    {
      synchronized(monitor)
      {
        if (topicConnection == null)
        {
            topicConnection = topicConnectionInstance.get();
            
            topicConnection.setExceptionListener(new ExceptionListener() {
              public void onException(JMSException ex)
              {
                // swallow the exception for now - do we need to try and reconnect???
              }
            });
            topicConnection.start();
        }
      }
    }
    return topicConnection;
  }

  public RemoteSubscriber subscribe(String topicName)
  {
    if (!allowedTopics.contains(topicName)) {
      throw new IllegalArgumentException(String.format(
        "Cannot subscribe to a topic that is not allowed. Topic [%s] is not an " +
        "allowed topic.", topicName));
    }

    RemoteSubscriber sub = new RemoteSubscriber(UUID.randomUUID().toString(), 
          topicName, this);

    try {
      subscribe(sub);
      subscriptions.put(sub.getToken(), sub);

      // Save the client's token in their session context
      getUserTokens().add(sub.getToken());

      return sub;
    } catch (Exception ex) {
      // TODO should log this
      return null;
    }
  }

  private void subscribe(RemoteSubscriber sub) 
      throws JMSException, Exception
  {
     try {
        sub.subscribe(getTopicConnection()); 
     } catch (Exception e) {
        // Clear the topic connection and try again.         
        resetTopic(); 
        sub.subscribe(getTopicConnection()); 
     }
  }

  private void resetTopic()
  {
     TopicConnection savedTopic = null;
     
     synchronized(monitor) {
        if (topicConnection != null) { 
           savedTopic = topicConnection;
           topicConnection = null;
        }
     }
     
     if (savedTopic != null) {
        try { 
           savedTopic.close(); 
        } catch (Exception ignored) { }     
     }     
  }

  public UserTokens getUserTokens()
  {
     return userTokenInstance.get();
  }

  public RemoteSubscriber getSubscription(String token)
  {
    if (!getUserTokens().contains(token)) {
      throw new IllegalArgumentException("Invalid token argument - token not found in Session Context.");
    }
    
    return subscriptions.get(token);
  }
  
  public Set<String> getAllTokens() {
      return subscriptions.keySet();
  }

  public void cleanupTokens(Set<String> tokens)
  {
       for (String token: tokens) {
          RemoteSubscriber subscriber = subscriptions.remove(token);
          if (subscriber!=null) {
             try {
                 subscriber.unsubscribe();
             } catch (Exception e) {
                //log.debug("problem cleaning up subcription", e);
             }
          }          
       }
  }
}
