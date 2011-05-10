package org.jboss.seam.remoting.examples.model.ftest;

import org.jboss.test.selenium.AbstractTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PersonTest extends AbstractTestCase {
    private ModelPage page;

    @BeforeMethod
    public void init() {
        page = new ModelPage(selenium, contextPath);
    }

    @Test
    public void testImportedDetails() {
        page.selectPerson("Shane Bryzak");
        assertEquals(page.getFirstName(), "Shane");
        assertEquals(page.getSurname(), "Bryzak");
        assertTrue(page.getBirthdate().length() > 1); // avoid possible problems with locales
    }

    @Test
    public void testAddingNewPerson() {
        page.createPerson();
        page.setFirstName("Martin");
        page.setSurname("Gencur");
        page.setBirthdate("1901/01/01");
        page.applyChanges();

        page.selectPerson("Martin Gencur");
        assertEquals(page.getFirstName(), "Martin");
        assertEquals(page.getSurname(), "Gencur");
        assertTrue(page.getBirthdate().length() > 0);
    }

    @Test(dependsOnMethods = "testAddingNewPerson")
    public void testUpdatingPersonDetails() {
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
