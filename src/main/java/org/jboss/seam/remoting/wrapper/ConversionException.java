package org.jboss.seam.remoting.wrapper;

/**
 * Thrown for an invalid conversion.
 * 
 * @author Shane Bryzak
 */
public class ConversionException extends Exception
{
   private static final long serialVersionUID = 5584559762846984501L;

   public ConversionException(String message)
   {
      super(message);
   }

   public ConversionException(String message, Throwable cause)
   {
      super(message, cause);
   }
}
