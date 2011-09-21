package org.jboss.seam.remoting.examples.validation.ftest;

import static org.jboss.arquillian.ajocado.Ajocado.waitForXhr;
import static org.jboss.arquillian.ajocado.locator.LocatorFactory.id;
import static org.jboss.arquillian.ajocado.locator.LocatorFactory.xp;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.ajocado.framework.AjaxSelenium;
import org.jboss.arquillian.ajocado.locator.IdLocator;
import org.jboss.arquillian.ajocado.locator.XPathLocator;
import org.jboss.arquillian.ajocado.utils.URLUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A functional test for a Validation example
 *
 * @author Martin Gencur
 * @author Jozef Hartinger
 */
@RunWith(Arquillian.class)
public class ValidationTest {
    protected String MAIN_PAGE = "/remoting-validation/validation.html";
    protected XPathLocator SAVE_BUTTON = xp("//input[contains(@value,'Save')]");
    protected IdLocator FIRST_NAME_FIELD = id("firstName");
    protected IdLocator LAST_NAME_FIELD = id("lastName");
    protected IdLocator BIRTHDAY_FIELD = id("dateOfBirth");

    public static final String ARCHIVE_NAME = "remoting-validation.war";
    public static final String BUILD_DIRECTORY = "target";

    @Deployment(testable = false)
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(ZipImporter.class, ARCHIVE_NAME).importFrom(new File(BUILD_DIRECTORY + '/' + ARCHIVE_NAME))
                .as(WebArchive.class);
    }
    
    @Drone
    AjaxSelenium selenium;

    @ArquillianResource
    URL contextPath;
    
    @Before
    public void openStartUrl(){
        selenium.setSpeed(300);
        selenium.open(URLUtils.buildUrl(contextPath, MAIN_PAGE));
    }

    @Test
    public void testCorrectValues() {
        selenium.type(FIRST_NAME_FIELD, "Martin");
        selenium.type(LAST_NAME_FIELD, "Gencur");
        selenium.type(BIRTHDAY_FIELD, "1985/04/05");

        waitForXhr(selenium).click(SAVE_BUTTON);

        assertFalse("First Name and Last Name validation should pass", selenium.isTextPresent("size must be between 3 and 40"));
        assertFalse("Date of Birth validation should pass", selenium.isTextPresent("must be in the past"));
        assertFalse("All the fields should be filled in", selenium.isTextPresent("may not be null"));
        assertEquals("All validations passed!", selenium.getAlert());
    }

    @Test
    public void testIncorrectValues() {
        selenium.type(FIRST_NAME_FIELD, "Ma"); // too short name
        selenium.type(LAST_NAME_FIELD, ""); // empty surname
        selenium.type(BIRTHDAY_FIELD, "2015/01/01"); // the date in the future

        waitForXhr(selenium).click(SAVE_BUTTON);

        assertTrue("First Name validation should fail", selenium.isTextPresent("size must be between 3 and 40"));
        assertTrue("Last Name validation should fail", selenium.isTextPresent("may not be null"));
        assertTrue("Date of Birth validation should fail", selenium.isTextPresent("must be in the past"));
    }
}
