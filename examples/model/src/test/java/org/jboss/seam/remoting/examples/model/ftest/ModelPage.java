package org.jboss.seam.remoting.examples.model.ftest;

import java.net.URL;

import org.jboss.test.selenium.framework.AjaxSelenium;
import org.jboss.test.selenium.locator.JQueryLocator;
import org.jboss.test.selenium.waiting.Wait;
import org.jboss.test.selenium.waiting.conditions.ElementPresent;
import org.jboss.test.selenium.waiting.selenium.SeleniumWaiting;

import static org.jboss.test.selenium.guard.request.RequestTypeGuardFactory.waitXhr;
import static org.jboss.test.selenium.locator.LocatorFactory.jq;
import static org.testng.Assert.assertEquals;

public class ModelPage {
    private static final JQueryLocator PERSON_LIST = jq("#personList");
    private static final JQueryLocator PERSON_LOAD_ADDRESSES = jq(".loadAddresses");
    private static final JQueryLocator PERSON_CREATE_ADDRESS = jq(".createAddress");
    private static final JQueryLocator PERSON_APPLY_CHANGES = jq(".applyChanges");
    private static final JQueryLocator PERSON_BY_NAME = jq("#personList div a:contains('{0}')");
    private static final JQueryLocator PERSON_CREATE_NEW = jq(".createPerson");
    private static final JQueryLocator ADDRESS_BY_ID = jq(".addressContainer:eq({0})");
    private static final JQueryLocator ADDRESSES = jq(".addressContainer");
    private static final JQueryLocator PERSON_FIRSTNAME = jq("#firstName");
    private static final JQueryLocator PERSON_SURNAME = jq("#lastName");
    private static final JQueryLocator PERSON_BIRTHDATE = jq("#dob");

    private static final int WAIT_INTERVAL = 500;
    private static final int WAIT_TIMEOUT = 10000;

    private AjaxSelenium selenium;
    private ElementPresent elementPresent = ElementPresent.getInstance();
    private SeleniumWaiting wait = Wait.waitSelenium().interval(WAIT_INTERVAL).timeout(WAIT_TIMEOUT);
    private URL contextPath;

    public ModelPage(AjaxSelenium selenium, URL contextPath) {
        this.selenium = selenium;
        this.contextPath = contextPath;
        selenium.open(this.contextPath);
        wait.until(elementPresent.locator(PERSON_LIST));
    }

    public ModelPage selectPerson(String name) {
        waitXhr(selenium).click(PERSON_BY_NAME.format(name));
        return this;
    }

    public ModelPage createPerson() {
        waitXhr(selenium).click(PERSON_CREATE_NEW);
        return this;
    }

    public String getFirstName() {
        return selenium.getValue(PERSON_FIRSTNAME);
    }

    public void setFirstName(String value) {
        selenium.type(PERSON_FIRSTNAME, value);
    }

    public String getSurname() {
        return selenium.getValue(PERSON_SURNAME);
    }

    public void setSurname(String value) {
        selenium.type(PERSON_SURNAME, value);
    }

    public String getBirthdate() {
        return selenium.getValue(PERSON_BIRTHDATE);
    }

    public void setBirthdate(String value) {
        selenium.type(PERSON_BIRTHDATE, value);
    }

    public ModelPage loadAddresses() {
        waitXhr(selenium).click(PERSON_LOAD_ADDRESSES);
        return this;
    }

    public ModelPage createAddress() {
        selenium.click(PERSON_CREATE_ADDRESS);// TODO this may cause race
        // conditions
        return this;
    }

    public ModelPage applyChanges() {
        waitXhr(selenium).click(PERSON_APPLY_CHANGES);
        if (selenium.isAlertPresent()) {
            assertEquals(selenium.getAlert(), "Changes applied");
        }
        return this;
    }

    public AjaxSelenium getSelenium() {
        return selenium;
    }

    public int getAddressCount() {
        return selenium.getCount(ADDRESSES);
    }

    public Address getAddress(int id) {
        return new Address(ADDRESS_BY_ID.format(id));
    }

    public class Address {
        private final JQueryLocator locator;
        private final String ADDRESS_STREET_NUMBER = ".streetNo";
        private final String ADDRESS_STREET_NAME = ".streetName";
        private final String ADDRESS_SUBURB = ".suburb";
        private final String ADDRESS_POSTCODE = ".postCode";
        private final String ADDRESS_COUNTRY = ".country";

        public Address(JQueryLocator locator) {
            this.locator = locator;
        }

        public String getStreetNumber() {
            return selenium.getValue(locator.getDescendant(jq(ADDRESS_STREET_NUMBER)));
        }

        public void setStreetNumber(String value) {
            selenium.type(locator.getDescendant(jq(ADDRESS_STREET_NUMBER)), value);
        }

        public String getStreetName() {
            return selenium.getValue(locator.getDescendant(jq(ADDRESS_STREET_NAME)));
        }

        public void setStreetName(String value) {
            selenium.type(locator.getDescendant(jq(ADDRESS_STREET_NAME)), value);
        }

        public String getSuburb() {
            return selenium.getValue(locator.getDescendant(jq(".suburb")));
        }

        public void setSuburb(String value) {
            selenium.type(locator.getDescendant(jq(ADDRESS_SUBURB)), value);
        }

        public String getPostcode() {
            return selenium.getValue(locator.getDescendant(jq(ADDRESS_POSTCODE)));
        }

        public void setPostcode(String value) {
            selenium.type(locator.getDescendant(jq(ADDRESS_POSTCODE)), value);
        }

        public String getCountry() {
            return selenium.getValue(locator.getDescendant(jq(".country")));
        }

        public void setCountry(String value) {
            selenium.type(locator.getDescendant(jq(ADDRESS_COUNTRY)), value);
        }

        public void delete() {
            selenium.click(locator.getDescendant(jq(".deleteAddress")));
        }
    }
}
