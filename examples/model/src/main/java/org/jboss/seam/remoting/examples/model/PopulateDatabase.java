package org.jboss.seam.remoting.examples.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.enterprise.event.Observes;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.solder.servlet.WebApplication;
import org.jboss.solder.servlet.event.Initialized;
import org.jboss.seam.transaction.Transactional;

/**
 * Populate a database with data. 
 *
 * @author Martin Gencur
 */
public class PopulateDatabase {
    @PersistenceContext
    private EntityManager entityManager;

    private Person p;
    private Address a;
    private final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

    @Transactional
    public void loadData(@Observes @Initialized WebApplication webapp) throws ParseException { 
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
    }
}
