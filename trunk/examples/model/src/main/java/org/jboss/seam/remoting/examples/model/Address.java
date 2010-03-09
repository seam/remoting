package org.jboss.seam.remoting.examples.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
public class Address implements Serializable
{
   private static final long serialVersionUID = 6871342053292770838L;
   
   private Integer addressId;
   private Person person;
   private Integer streetNo;
   private String streetName;
   private String suburb;
   private String postCode;
   private String country;

   @Id @GeneratedValue
   public Integer getAddressId()
   {
      return addressId;
   }
   
   public void setAddressId(Integer addressId)
   {
      this.addressId = addressId;
   }
   
   @ManyToOne(optional = false)
   @JoinColumn(name = "PERSON_ID")
   public Person getPerson()
   {
      return person;
   }
   
   public void setPerson(Person person)
   {
      this.person = person;
   }
   
   public Integer getStreetNo()
   {
      return streetNo;
   }
   
   public void setStreetNo(Integer streetNo)
   {
      this.streetNo = streetNo;
   }
   
   public String getStreetName()
   {
      return streetName;
   }
   
   public void setStreetName(String streetName)
   {
      this.streetName = streetName;
   }
   
   public String getSuburb()
   {
      return suburb;
   }
   
   public void setSuburb(String suburb)
   {
      this.suburb = suburb;
   }
   
   public String getPostCode()
   {
      return postCode;
   }
   
   public void setPostCode(String postCode)
   {
      this.postCode = postCode;
   }
   
   public String getCountry()
   {
      return country;
   }
   
   public void setCountry(String country)
   {
      this.country = country;
   }
}
