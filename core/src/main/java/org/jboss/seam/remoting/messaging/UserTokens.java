package org.jboss.seam.remoting.messaging;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;

@SessionScoped
public class UserTokens implements Serializable
{
   Set<String> tokens = new HashSet<String>();
   
   @Inject SubscriptionRegistry registry;
   
   public void add(String token) {
      tokens.add(token);
   }
   
   public boolean contains(String token) {
      return tokens.contains(token); 
   }
   
   public void remove(String token) {
      tokens.remove(token);
   }
   
   @PreDestroy 
   public void cleanUp() {
       registry.cleanupTokens(tokens);
   }
}
