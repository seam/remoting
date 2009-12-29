package org.jboss.seam.remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.remoting.BeanMetadata.BeanType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates JavaScript interface code.
 * 
 * @author Shane Bryzak
 */
public class InterfaceGenerator implements RequestHandler
{
   private static final Logger log = LoggerFactory.getLogger(InterfaceGenerator.class);
   
   @Inject BeanManager beanManager;
   @Inject MetadataCache metadataCache;

   /**
    * A cache of component interfaces, keyed by name.
    */
   private Map<Class<?>, byte[]> interfaceCache = new HashMap<Class<?>, byte[]>();
   
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

      Set<Class<?>> typesCached = new HashSet<Class<?>>();
      response.setContentType("text/javascript");

      Enumeration<?> e = request.getParameterNames();
      while (e.hasMoreElements())
      {
         String componentName = ((String) e.nextElement()).trim();
         Class<?> beanClass = null;

         Set<Bean<?>> beans = beanManager.getBeans(componentName);
         
         if (!beans.isEmpty())
         {
            beanClass = beans.iterator().next().getBeanClass();
         }
         else
         {
            try
            {
               beanClass = Class.forName(componentName);
            }
            catch (ClassNotFoundException ex) 
            {
               log.error(String.format("Component not found: [%s]",
                     componentName));
               response.sendError(HttpServletResponse.SC_NOT_FOUND, 
                     String.format("Component not found: [%s]", componentName));              
            }            
         }
         
         typesCached.add(beanClass);
      }

      generateBeanInterface(typesCached, response.getOutputStream(), null);
   }

   /**
    * Generates the JavaScript code required to invoke the methods of a bean.
    * 
    * @param classes Set<Class<?>> The bean classes for which to generate JavaScript stubs
    * @param out OutputStream The OutputStream to write the generated JavaScript
    * @param types Set<Type> Used to keep track of which bean classes have been
    *        generated, can be null
    * @throws IOException Thrown if there is an error writing to the OutputStream
    */
   public void generateBeanInterface(Set<Class<?>> classes, OutputStream out,
         Set<BeanMetadata> types) throws IOException
   {
      if (types == null)
      {
         types = new HashSet<BeanMetadata>();
      }
      
      for (Class<?> beanClass : classes)
      {
         if (beanClass != null)
         {
            if (!interfaceCache.containsKey(beanClass))
            {
               synchronized (interfaceCache)
               {
                  if (!interfaceCache.containsKey(beanClass))
                  {
                     ByteArrayOutputStream bOut = new ByteArrayOutputStream();
                     addDependency(beanClass, types);
                     appendBeanSource(bOut, beanClass, types);
                     interfaceCache.put(beanClass, bOut.toByteArray());
                  }
               }
            }
            out.write(interfaceCache.get(beanClass));
         }
      }
   }

   /**
    * Appends component interface code to an OutputStream for a specified
    * component.
    * 
    * @param out OutputStream The OutputStream to write to
    * @param beanClass Class<?> The bean class to generate an interface for
    * @param types
    *           Set A list of types that have already been generated for this
    *           request. If this component has already been generated (i.e. it
    *           is in the list) then it won't be generated again
    * @throws IOException
    *            If there is an error writing to the OutputStream.
    */
   private void addDependency(Class<?> beanClass, Set<BeanMetadata> types)
         throws IOException
   {
      types.add(metadataCache.getMetadata(beanClass));
      
      for (Class<?> dependencyClass : metadataCache.getDependencies(beanClass))
      {
         if (!types.contains(dependencyClass))
         {
            addDependency(dependencyClass, types);
         }
      }      
   }

   /**
    * Appends the interface code for a JavaBean (state holding) class to an OutputStream.
    * 
    * @param out OutputStream
    * @param classType Class<?>
    * @param types Set<Type>
    * @throws IOException
    */
   private void appendBeanSource(OutputStream out, Class<?> classType,
         Set<BeanMetadata> types) throws IOException
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
