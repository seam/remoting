package org.jboss.seam.remoting.examples.model;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

@ConversationScoped
public class CustomerAction implements Serializable
{
   private static final long serialVersionUID = 8350706339578435242L;
   
   @Inject EntityManager entityManager;
   @Inject Conversation conversation;
   
   private Customer customer;
      
   public void createCustomer()
   {
      conversation.begin();
      customer = new Customer();
   }
   
   public void editCustomer(Integer customerId)
   {
      conversation.begin();
      customer = entityManager.find(Customer.class, customerId);
   }
   
   public void saveCustomer()
   {
      if (customer.getCustomerId() == null)
      {
         entityManager.persist(customer);
      }
      else
      {
         customer = entityManager.merge(customer);
      }
      conversation.end();
   }
   
   public Customer getCustomer()
   {
      return customer;
   }
}
