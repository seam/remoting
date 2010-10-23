package org.jboss.seam.remoting.examples.model;

import java.io.Serializable;
import java.util.ArrayList;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.seam.persistence.transaction.Transactional;
import org.jboss.seam.remoting.annotations.WebRemote;


public @ConversationScoped class PersonAction implements Serializable
{
   private static final long serialVersionUID = -1923705862231821692L;
   
   @Inject EntityManager entityManager;
   @Inject Conversation conversation;
   
   private Person person;
      
   @WebRemote
   public void createPerson()
   {
      conversation.begin();
      person = new Person();
      person.setAddresses(new ArrayList<Address>());
   }
   
   @WebRemote
   public void editPerson(Integer personId)
   {
      conversation.begin();
      person = entityManager.find(Person.class, personId);
   }
   
   @WebRemote @Transactional
   public void savePerson() throws Exception
   {
      if (person.getPersonId() == null)
      {
         entityManager.persist(person);
      }
      else
      {
         person = entityManager.merge(person);
      }
      
      conversation.end();
   }
   
   public Person getPerson()
   {
      return person;
   }
}
