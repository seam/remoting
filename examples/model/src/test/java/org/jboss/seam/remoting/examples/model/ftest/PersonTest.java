package org.jboss.seam.remoting.examples.model.ftest;


import java.net.URL;

import org.jboss.arquillian.ajocado.framework.AjaxSelenium;
import org.jboss.arquillian.ajocado.utils.URLUtils;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class PersonTest extends AbstractTest {
    private ModelPage page;

    @Drone
    AjaxSelenium selenium;

    @ArquillianResource
    URL contextPath;
    
    @Before
    public void init() {
        page = new ModelPage(selenium, URLUtils.buildUrl(contextPath, MAIN_PAGE));
    }

    @Test
    public void testImportedDetails() {
        page.selectPerson("Shane Bryzak");
        assertEquals(page.getFirstName(), "Shane");
        assertEquals(page.getSurname(), "Bryzak");
        assertTrue(page.getBirthdate().length() > 1); // avoid possible problems with locales
    }

    @Test
    public void testAddingAndUpdatingNewPerson() {
        page.createPerson();
        page.setFirstName("Martin");
        page.setSurname("Gencur");
        page.setBirthdate("1901/01/01");
        page.applyChanges();

        page.selectPerson("Martin Gencur");
        assertEquals(page.getFirstName(), "Martin");
        assertEquals(page.getSurname(), "Gencur");
        assertTrue(page.getBirthdate().length() > 0);
        // update
        page.selectPerson("Martin Gencur");
        page.setFirstName("John");
        page.setSurname("Doe");
        page.applyChanges();

        page.selectPerson("John Doe");
        assertEquals(page.getFirstName(), "John");
        assertEquals(page.getSurname(), "Doe");
        assertTrue(page.getBirthdate().length() > 0);
    }
}
