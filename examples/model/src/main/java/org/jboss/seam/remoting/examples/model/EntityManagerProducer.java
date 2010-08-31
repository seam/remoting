package org.jboss.seam.remoting.examples.model;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jboss.seam.persistence.SeamManaged;

public class EntityManagerProducer
{
   @PersistenceUnit
   @ConversationScoped
   @Produces
   @SeamManaged
   EntityManagerFactory emf;  
}
