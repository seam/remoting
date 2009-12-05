package org.jboss.seam.remoting.messaging;

import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Shane Bryzak
 */
public class SubscriptionRequest
{
  private String topicName;
  private RemoteSubscriber subscriber;
  private SubscriptionRegistry registry;

  public SubscriptionRequest(String topicName, SubscriptionRegistry registry)
  {
    this.topicName = topicName;
    this.registry = registry;
  }

  public void subscribe()
  {
    subscriber = registry.subscribe(topicName);
  }

  public void marshal(OutputStream out)
      throws IOException
  {
    out.write("<subscription topic=\"".getBytes());
    out.write(topicName.getBytes());
    out.write("\" token=\"".getBytes());
    out.write(subscriber.getToken().getBytes());
    out.write("\"/>".getBytes());
    out.flush();
  }
}
