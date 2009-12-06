package org.jboss.seam.remoting.examples.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Customer entity bean.  This class exposes a variety of property types for the
 * purpose of the example.
 *  
 * @author Shane Bryzak
 */
@Entity
public class Customer implements Serializable
{
   private static final long serialVersionUID = 4289723352398788625L;

   public enum Gender { male, female }
   
   private Integer customerId;
   private String firstName;
   private String lastName;
   private Date dateOfBirth;
   private Gender gender;
   
   @Id @GeneratedValue
   public Integer getCustomerId()
   {
      return customerId;
   }
   
   public void setCustomerId(Integer customerId)
   {
      this.customerId = customerId;
   }
   
   public String getFirstName()
   {
      return firstName;
   }
   
   public void setFirstName(String firstName)
   {
      this.firstName = firstName;
   }
   
   public String getLastName()
   {
      return lastName;
   }
   
   public void setLastName(String lastName)
   {
      this.lastName = lastName;
   }
   
   public Date getDateOfBirth()
   {
      return dateOfBirth;
   }
   
   public void setDateOfBirth(Date dateOfBirth)
   {
      this.dateOfBirth = dateOfBirth;
   }
   
   public Gender getGender()
   {
      return gender;
   }
   
   public void setGender(Gender gender)
   {
      this.gender = gender;
   }
}
