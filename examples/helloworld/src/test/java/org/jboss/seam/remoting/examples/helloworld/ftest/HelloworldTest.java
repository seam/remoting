/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.remoting.examples.helloworld.ftest;

import static org.testng.Assert.assertEquals;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.net.MalformedURLException;
import java.net.URL;
import org.jboss.test.selenium.AbstractTestCase;
import org.jboss.test.selenium.locator.XpathLocator;
import static org.jboss.test.selenium.locator.LocatorFactory.*;
import static org.jboss.test.selenium.guard.request.RequestTypeGuardFactory.*;

/**
 * A functional test for a Helloworld example
 * 
 * @author Martin Gencur
 * 
 */
public class HelloworldTest extends AbstractTestCase
{
   protected String MAIN_PAGE = "/helloworld.html";
   protected XpathLocator FORMALITY_CASUAL = xp("//input[contains(@value,'Casual')]");
   protected XpathLocator FORMALITY_FORMAL = xp("//input[contains(@value,'Formal')]");
   protected XpathLocator LOCAL_ENGLISH = xp("//input[contains(@value,'ENGLISH')]");
   protected XpathLocator LOCAL_RUSSIAN = xp("//input[contains(@value,'RUSSIAN')]");
   protected XpathLocator INVOKE_BUTTON = xp("//button[contains(text(),'Invoke')]");

   @BeforeMethod
   public void openStartUrl() throws MalformedURLException
   {
      selenium.setSpeed(300);
      selenium.open(new URL(contextPath.toString() + MAIN_PAGE));
   }

   @Test
   public void testCasualEnglish()
   {
      selenium.check(FORMALITY_CASUAL);
      selenium.check(LOCAL_ENGLISH);
      selenium.answerOnNextPrompt("Martin");
      waitXhr(selenium).click(INVOKE_BUTTON);    
      assertEquals(selenium.getAlert(), "Hi, Martin");
   }

   @Test
   public void testCasualRussian()
   {
      selenium.check(FORMALITY_CASUAL);
      selenium.check(LOCAL_RUSSIAN);
      selenium.answerOnNextPrompt("Martin");
      waitXhr(selenium).click(INVOKE_BUTTON);    
      assertEquals(selenium.getAlert(), "Privyet, Martin");
   }
   
   @Test
   public void testFormalEnglish()
   {
      selenium.check(FORMALITY_FORMAL);
      selenium.check(LOCAL_ENGLISH);
      selenium.answerOnNextPrompt("Martin");
      waitXhr(selenium).click(INVOKE_BUTTON);    
      assertEquals(selenium.getAlert(), "Hello, Martin");
   }
   
   @Test
   public void testFormalRussian()
   {
      selenium.check(FORMALITY_FORMAL);
      selenium.check(LOCAL_RUSSIAN);
      selenium.answerOnNextPrompt("Martin");
      waitXhr(selenium).click(INVOKE_BUTTON);    
      assertEquals(selenium.getAlert(), "Zdravstvuite, Martin");
   }
}
