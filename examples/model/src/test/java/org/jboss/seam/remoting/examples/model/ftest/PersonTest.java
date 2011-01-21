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
package org.jboss.seam.remoting.examples.model.ftest;

import org.jboss.test.selenium.AbstractTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class PersonTest extends AbstractTestCase
{
   private ModelPage page;
   
   @BeforeMethod
   public void init()
   {
      page = new ModelPage(selenium, contextPath);
   }
   
   @Test
   public void testImportedDetails()
   {
      page.selectPerson("Shane Bryzak");
      assertEquals(page.getFirstName(), "Shane");
      assertEquals(page.getSurname(), "Bryzak");
      assertTrue(page.getBirthdate().length() > 1); // avoid possible problems with locales
   }
   
   @Test(dependsOnMethods = "testImportedDetails")
   public void testUpdatingPersonDetails()
   {
      page.selectPerson("Shane Bryzak");
      page.setFirstName("John");
      page.setSurname("Doe");
      page.applyChanges();
      
      page.selectPerson("John Doe");
      assertEquals(page.getFirstName(), "John");
      assertEquals(page.getSurname(), "Doe");
      assertTrue(page.getBirthdate().length() > 0);
   }
   
   @Test
   public void testAddingNewPerson()
   {
      page.createPerson();
      page.setFirstName("Martin");
      page.setSurname("Gencur");
      page.setBirthdate("1901/01/01");
      page.applyChanges();
      
      assertEquals(page.getSelenium().getAlert(), "Changes applied");
      
      page.selectPerson("Martin Gencur");
      assertEquals(page.getFirstName(), "Martin");
      assertEquals(page.getSurname(), "Gencur");
      assertTrue(page.getBirthdate().length() > 0);
   }
}
