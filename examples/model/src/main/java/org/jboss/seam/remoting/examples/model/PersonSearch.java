package org.jboss.seam.remoting.examples.model;

import java.util.List;

import javax.enterprise.inject.Model;
import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.seam.remoting.annotations.WebRemote;

/**
 * Returns a list of all the Person entities
 *  
 * @author Shane Bryzak
 */
public @Model class PersonSearch
{
   @Inject EntityManager entityManager;
   
   @WebRemote @SuppressWarnings("unchecked")
   public List<Person> listPeople() throws Exception
   {
      return entityManager.createQuery("select p from Person p").getResultList();
   }
}
