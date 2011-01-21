package org.jboss.seam.remoting.examples.validation.ftest;

import static org.jboss.test.selenium.locator.LocatorFactory.id;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.test.selenium.AbstractTestCase;
import org.jboss.test.selenium.locator.IdLocator;
import org.jboss.test.selenium.locator.XpathLocator;
import static org.jboss.test.selenium.locator.LocatorFactory.*;
import static org.jboss.test.selenium.guard.request.RequestTypeGuardFactory.*;

/**
 * A functional test for a Validation example
 * 
 * @author Martin Gencur
 * 
 */
public class ValidationTest extends AbstractTestCase
{
   protected String MAIN_PAGE = "/validation.html";
   protected XpathLocator SAVE_BUTTON = xp("//input[contains(@value,'Save')]");
   protected IdLocator FIRST_NAME_FIELD = id("firstName");
   protected IdLocator LAST_NAME_FIELD = id("lastName");
   protected IdLocator BIRTHDAY_FIELD = id("dateOfBirth");

   @BeforeMethod
   public void openStartUrl() throws MalformedURLException
   {
      selenium.setSpeed(300);
      selenium.open(new URL(contextPath.toString() + MAIN_PAGE));
   }

   @Test
   public void testCorrectValues()
   {
      selenium.type(FIRST_NAME_FIELD, "Martin");
      selenium.type(LAST_NAME_FIELD, "Gencur");
      selenium.type(BIRTHDAY_FIELD, "1985/04/05");

      waitXhr(selenium).click(SAVE_BUTTON);

      assertFalse(selenium.isTextPresent("size must be between 3 and 40"), "First Name and Last Name validation should pass");
      assertFalse(selenium.isTextPresent("must be in the past"), "Date of Birth validation should pass");
      assertFalse(selenium.isTextPresent("may not be null"), "All the fields should be filled in");
      assertEquals(selenium.getAlert(), "All validations passed!");
   }

   @Test
   public void testIncorrectValues()
   {
      selenium.type(FIRST_NAME_FIELD, "Ma"); // too short name
      selenium.type(LAST_NAME_FIELD, ""); // empty surname
      selenium.type(BIRTHDAY_FIELD, "2015/01/01"); // the date in the future

      waitXhr(selenium).click(SAVE_BUTTON);

      assertTrue(selenium.isTextPresent("size must be between 3 and 40"), "First Name validation should fail");
      assertTrue(selenium.isTextPresent("may not be null"), "Last Name validation should fail");
      assertTrue(selenium.isTextPresent("must be in the past"), "Date of Birth validation should fail");
   }
}
