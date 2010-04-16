package org.jboss.seam.remoting.util;

public class Strings
{
   public static boolean isEmpty(String string)
   {
      int len;
      if (string == null || (len = string.length()) == 0)
      {
         return true;
      }
      
      for (int i = 0; i < len; i++)
      {
         if ((Character.isWhitespace(string.charAt(i)) == false))
         {
            return false;
         }
      }
      return true;
   }
}
