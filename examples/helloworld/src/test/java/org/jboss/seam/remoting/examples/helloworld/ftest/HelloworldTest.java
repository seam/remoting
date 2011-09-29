package org.jboss.seam.remoting.examples.helloworld.ftest;

import static org.jboss.arquillian.ajocado.locator.LocatorFactory.xp;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;

import org.jboss.arquillian.ajocado.framework.AjaxSelenium;
import org.jboss.arquillian.ajocado.locator.XPathLocator;
import org.jboss.arquillian.ajocado.utils.URLUtils;
import org.jboss.arquillian.ajocado.waiting.Wait;
import org.jboss.arquillian.ajocado.waiting.selenium.SeleniumCondition;
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
 * A functional test for a Helloworld example
 *
 * @author Martin Gencur
 * @author Jozef Hartinger
 */
@RunWith(Arquillian.class)
public class HelloworldTest {
    protected String MAIN_PAGE = "/remoting-helloworld/helloworld.html";
    protected XPathLocator FORMALITY_CASUAL = xp("//input[contains(@value,'Casual')]");
    protected XPathLocator FORMALITY_FORMAL = xp("//input[contains(@value,'Formal')]");
    protected XPathLocator LOCAL_ENGLISH = xp("//input[contains(@value,'ENGLISH')]");
    protected XPathLocator LOCAL_RUSSIAN = xp("//input[contains(@value,'RUSSIAN')]");
    protected XPathLocator INVOKE_BUTTON = xp("//button[contains(text(),'Invoke')]");

    public static final String ARCHIVE_NAME = "remoting-helloworld.war";
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
    public void openStartUrl() {
        selenium.setSpeed(300);
        selenium.open(URLUtils.buildUrl(contextPath, MAIN_PAGE));
    }

    @Test
    public void testCasualEnglish() {
        selenium.check(FORMALITY_CASUAL);
        selenium.check(LOCAL_ENGLISH);
        selenium.answerOnNextPrompt("Martin");
        selenium.click(INVOKE_BUTTON);
        assertEquals(waitForAlert(), "Hi, Martin");
    }

    @Test
    public void testCasualRussian() {
        selenium.check(FORMALITY_CASUAL);
        selenium.check(LOCAL_RUSSIAN);
        selenium.answerOnNextPrompt("Martin");
        selenium.click(INVOKE_BUTTON);
        assertEquals(waitForAlert(), "Privyet, Martin");
    }

    @Test
    public void testFormalEnglish() {
        selenium.check(FORMALITY_FORMAL);
        selenium.check(LOCAL_ENGLISH);
        selenium.answerOnNextPrompt("Martin");
        selenium.click(INVOKE_BUTTON);
        assertEquals(waitForAlert(), "Hello, Martin");
    }

    @Test
    public void testFormalRussian() {
        selenium.check(FORMALITY_FORMAL);
        selenium.check(LOCAL_RUSSIAN);
        selenium.answerOnNextPrompt("Martin");
        selenium.click(INVOKE_BUTTON);
        assertEquals(waitForAlert(), "Zdravstvuite, Martin");
    }
    
    /**
     * The method waits for confirmation to appear, consumes the confirmation and then waits until the condition passed as a
     * method parameter to become satisfied.
     */
    private String waitForAlert() {
        Wait.waitSelenium.timeout(10000).interval(50).until(new SeleniumCondition() {
            @Override
            public boolean isTrue() {
                return selenium.isAlertPresent();
            }
        });
        return selenium.getAlert();
    }
}
