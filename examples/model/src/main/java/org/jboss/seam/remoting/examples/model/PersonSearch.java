package org.jboss.seam.remoting.examples.model;

import java.util.List;

import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.seam.remoting.annotations.WebRemote;

/**
 * Returns a list of all the Person entities
 *  
 * @author Shane Bryzak
 */
@Named("personSearch")
public class PersonSearch
{
   @PersistenceContext EntityManager entityManager;
   
   @WebRemote @SuppressWarnings("unchecked")
   public List<Person> listPeople()
   {
      return entityManager.createQuery("select p from Person p").getResultList();
   }
}
