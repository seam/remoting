package org.jboss.seam.remoting.examples.model.ftest;

import org.jboss.test.selenium.AbstractTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class AddressTest extends AbstractTestCase {
    private ModelPage page;

    @BeforeMethod
    public void init() {
        page = new ModelPage(selenium, contextPath);
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
    }

    @Test(dependsOnMethods = "testAddressDetails")
    public void testUpdatingExistingAddress() {
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
        assertEquals(savedAddress.getStreetNumber(), "1000", "Address not updated.");
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
        assertEquals(page.getAddressCount(), 0, "Address not removed.");
    }
}
