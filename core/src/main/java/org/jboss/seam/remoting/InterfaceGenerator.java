package org.jboss.seam.remoting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.remoting.BeanMetadata.BeanType;

/**
 * Generates JavaScript interface code.
 * 
 * @author Shane Bryzak
 */
public class InterfaceGenerator implements RequestHandler
{  
   @Inject MetadataCache metadataCache;
   
   /**
    * Handles the request
    */
   public void handle(final HttpServletRequest request,
         final HttpServletResponse response) throws Exception
   {
      if (request.getQueryString() == null)
      {
         response.sendError(HttpServletResponse.SC_BAD_REQUEST, 
               "Invalid request - no component specified");
      }
      
      response.setContentType("text/javascript");

      Set<String> names = new HashSet<String>();     
      Enumeration<?> e = request.getParameterNames();
      while (e.hasMoreElements())
      {
         names.add(((String) e.nextElement()).trim());                 
      }

      appendBeanSource(response.getOutputStream(), metadataCache.loadBeans(names));
   }

   /**
    * Appends the interface code for a set of bean classes to an OutputStream.
    * 
    * @param out OutputStream
    * @param classType Class<?>
    * @param types Set<Type>
    * @throws IOException
    */
   private void appendBeanSource(OutputStream out, Set<BeanMetadata> types) 
      throws IOException
   {
      StringBuilder src = new StringBuilder();
      
      for (BeanMetadata meta : types)
      {          
         if (meta.getBeanType() == BeanType.action)
         {
            src.append("Seam.registerBean(\"");
            src.append(meta.getName());
            src.append("\", null, {");
            
            boolean first = true;
            for (String methodName : meta.getMethods().keySet())
            {      
               if (!first) 
               {
                  src.append(", ");               
               }
               else
               {
                  first = false;
               }
                              
               src.append(methodName);
               src.append(": ");
               src.append(meta.getMethods().get(methodName));
            }
            src.append("});\n");            
         }
         else
         {
            src.append("Seam.registerBean(\"");
            src.append(meta.getName());
            src.append("\", {");
            
            boolean first = true;
            for (String propertyName : meta.getProperties().keySet())
            {
               if (!first) 
               {
                  src.append(", ");
               }
               else
               {
                  first = false;
               }
               
               src.append(propertyName);
               src.append(": \"");
               src.append(meta.getProperties().get(propertyName));
               src.append("\"");
            }
            src.append("});\n");            
         }
      }      
      out.write(src.toString().getBytes());         
   }
}
