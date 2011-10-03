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

@RunWith(Arquillian.class)
public class AddressTest extends AbstractTest {
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
    public void testAddressDetails() {
        page.selectPerson("Shane Bryzak").loadAddresses();

        ModelPage.Address address1 = page.getAddress(0);
        assertEquals(address1.getStreetNumber(), "100");
        assertEquals(address1.getStreetName(), "Main");
        assertEquals(address1.getSuburb(), "Pleasantville");
        assertEquals(address1.getCountry(), "Australia");
        assertEquals(address1.getPostcode(), "32123");

        ModelPage.Address address2 = page.getAddress(1);
        assertEquals(address2.getStreetNumber(), "57");
        assertEquals(address2.getStreetName(), "1st Avenue");
        assertEquals(address2.getPostcode(), "32411");
        assertEquals(address2.getSuburb(), "Pittsville");
        assertEquals(address2.getCountry(), "Australia");
        
        // update address
        
        page.selectPerson("Shane Bryzak").loadAddresses();
        ModelPage.Address newAddress = page.getAddress(0);
        newAddress.setStreetNumber("1000");
        newAddress.setStreetName("Amber");
        newAddress.setPostcode("54321");
        newAddress.setSuburb("FooBar Drive");
        newAddress.setCountry("USA");

        page.applyChanges();
        // reload the page
        page.selectPerson("Shane Bryzak").loadAddresses();

        ModelPage.Address savedAddress = page.getAddress(0);
        assertEquals("Address not updated.", savedAddress.getStreetNumber(), "1000");
        assertEquals(savedAddress.getStreetName(), "Amber");
        assertEquals(savedAddress.getPostcode(), "54321");
        assertEquals(savedAddress.getSuburb(), "FooBar Drive");
        assertEquals(savedAddress.getCountry(), "USA");
    }

    @Test
    public void testAddingNewAddress() {
        page.selectPerson("Shane Bryzak").loadAddresses();
        page.createAddress();

        ModelPage.Address newAddress = page.getAddress(2);
        newAddress.setStreetNumber("1001");
        newAddress.setStreetName("Foo");
        newAddress.setPostcode("12345");
        newAddress.setSuburb("Bar");
        newAddress.setCountry("Spain");

        page.applyChanges();
        // reload the page
        page.selectPerson("Shane Bryzak").loadAddresses();

        ModelPage.Address savedAddress = page.getAddress(2);
        assertEquals(savedAddress.getStreetNumber(), "1001");
        assertEquals(savedAddress.getStreetName(), "Foo");
        assertEquals(savedAddress.getPostcode(), "12345");
        assertEquals(savedAddress.getSuburb(), "Bar");
        assertEquals(savedAddress.getCountry(), "Spain");
    }

    @Test
    public void testRemovingAddress() {
        page.selectPerson("Jozef Hartinger").loadAddresses();

        page.getAddress(0).delete();
        page.applyChanges();
        // reload the page
        page.selectPerson("Jozef Hartinger").loadAddresses();
        assertEquals("Address not removed.", page.getAddressCount(), 0);
    }
}
