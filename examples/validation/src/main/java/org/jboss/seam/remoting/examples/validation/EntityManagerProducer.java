package org.jboss.seam.remoting.examples.validation;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jboss.solder.core.ExtensionManaged;

public class EntityManagerProducer {
    @PersistenceUnit
    @ConversationScoped
    @Produces
    @ExtensionManaged
    EntityManagerFactory emf;
}
