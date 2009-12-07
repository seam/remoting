package org.jboss.seam.remoting.examples.model;

import java.io.Serializable;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.seam.remoting.annotations.WebRemote;

@ConversationScoped
public class CustomerAction implements Serializable
{
   private static final long serialVersionUID = 8350706339578435242L;
   
   @PersistenceContext EntityManager entityManager;
   @Inject Conversation conversation;
   
   private Customer customer;
      
   @WebRemote
   public void createCustomer()
   {
      conversation.begin();
      customer = new Customer();
   }
   
   @WebRemote
   public void editCustomer(Integer customerId)
   {
      conversation.begin();
      customer = entityManager.find(Customer.class, customerId);
   }
   
   @WebRemote
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
