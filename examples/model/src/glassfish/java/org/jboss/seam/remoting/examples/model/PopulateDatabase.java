package org.jboss.seam.remoting.examples.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Populate a database for Glassfish with data. This class is not necessary
 * when using JBoss AS due to use of Hibernate JPA provider in conjunction
 * with import.sql file. Hibernate will then populate the database
 * automatically right after deploying the application.
 *
 * @author Martin Gencur
 */
@Singleton
@Startup
public class PopulateDatabase {
    @PersistenceContext
    private EntityManager entityManager;

    private Person p;
    private Address a;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @PostConstruct
    public void startup() throws Exception {
        Query q = entityManager.createNativeQuery("DELETE from ADDRESS");
        q.executeUpdate();
        q = entityManager.createNativeQuery("DELETE from PERSON");
        q.executeUpdate();
        entityManager.flush();

        p = new Person();
        p.setFirstName("Shane");
        p.setLastName("Bryzak");
        p.setDateOfBirth(df.parse("1901-01-01"));
        p.setAddresses(new ArrayList<Address>());

        a = new Address();
        a.setPerson(p);
        a.setStreetNo(100);
        a.setStreetName("Main");
        a.setSuburb("Pleasantville");
        a.setPostCode("32123");
        a.setCountry("Australia");
        p.getAddresses().add(a);

        a = new Address();
        a.setPerson(p);
        a.setStreetNo(57);
        a.setStreetName("1st Avenue");
        a.setSuburb("Pittsville");
        a.setPostCode("32411");
        a.setCountry("Australia");
        p.getAddresses().add(a);
        entityManager.persist(p);
        entityManager.flush();

        p = new Person();
        p.setFirstName("Jozef");
        p.setLastName("Hartinger");
        p.setDateOfBirth(df.parse("1901-01-01"));
        p.setAddresses(new ArrayList<Address>());

        a = new Address();
        a.setPerson(p);
        a.setStreetNo(99);
        a.setStreetName("Purkynova");
        a.setSuburb("Kralovo pole");
        a.setPostCode("60200");
        a.setCountry("Czech republic");
        p.getAddresses().add(a);
        entityManager.persist(p);
        entityManager.flush();
    }
}
