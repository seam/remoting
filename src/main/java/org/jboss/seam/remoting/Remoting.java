package org.jboss.seam.remoting;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serves JavaScript implementation of Seam Remoting
 * 
 * @author Shane Bryzak
 * 
 */
public class Remoting extends HttpServlet
{
   private static final long serialVersionUID = -3911197516105313424L;
   
   private static final String REQUEST_PATH_EXECUTE = "/execute";
   //private static final String REQUEST_PATH_SUBSCRIPTION = "/subscription";
   //private static final String REQUEST_PATH_POLL = "/poll";
   private static final String REQUEST_PATH_INTERFACE = "/interface.js";   
   
   @Inject Instance<ExecutionHandler> executionHandlerInstance;
   @Inject Instance<InterfaceGenerator> interfaceHandlerInstance;
   
   public static final int DEFAULT_POLL_TIMEOUT = 10; // 10 seconds
   public static final int DEFAULT_POLL_INTERVAL = 1; // 1 second

   private ServletConfig servletConfig;

   private int pollTimeout = DEFAULT_POLL_TIMEOUT;

   private int pollInterval = DEFAULT_POLL_INTERVAL;

   private boolean debug = false;

   /**
    * We use a Map for this because a Servlet can serve requests for more than
    * one context path.
    */
   private Map<String, byte[]> cachedConfig = new HashMap<String, byte[]>();

   private static final Logger log = LoggerFactory.getLogger(Remoting.class);

   private static final Pattern pathPattern = Pattern.compile("/(.*?)/([^/]+)");

   private static final String REMOTING_RESOURCE_PATH = "resource";

   private synchronized void initConfig(String contextPath,
         HttpServletRequest request)
   {
      if (!cachedConfig.containsKey(contextPath))
      {
         StringBuilder sb = new StringBuilder();
         sb.append("\nSeam.Remoting.resourcePath = \"");
         sb.append(contextPath);
         sb.append(request.getServletPath());
         sb.append(servletConfig.getServletContext().getContextPath());
         sb.append("\";");
         sb.append("\nSeam.Remoting.debug = ");
         sb.append(getDebug() ? "true" : "false");
         sb.append(";");
         sb.append("\nSeam.Remoting.pollInterval = ");
         sb.append(getPollInterval());
         sb.append(";");
         sb.append("\nSeam.Remoting.pollTimeout = ");
         sb.append(getPollTimeout());
         sb.append(";");

         cachedConfig.put(contextPath, sb.toString().getBytes());
      }
   }

   /**
    * Appends various configuration options to the remoting javascript client
    * api.
    * 
    * @param out
    *           OutputStream
    */
   private void appendConfig(OutputStream out, String contextPath,
         HttpServletRequest request) throws IOException
   {
      if (!cachedConfig.containsKey(contextPath))
      {
         initConfig(contextPath, request);
      }

      out.write(cachedConfig.get(contextPath));
   }

   /**
    * 
    * @param resourceName
    *           String
    * @param out
    *           OutputStream
    */
   private void writeResource(String resourceName, HttpServletResponse response)
         throws IOException
   {
      // Only allow resource requests for .js files
      if (resourceName.endsWith(".js"))
      {
         InputStream in = this.getClass().getClassLoader().getResourceAsStream(
               "org/jboss/seam/remoting/" + resourceName);
         try
         {
            if (in != null)
            {
               response.setContentType("text/javascript");

               byte[] buffer = new byte[1024];
               int read = in.read(buffer);
               while (read != -1)
               {
                  response.getOutputStream().write(buffer, 0, read);
                  read = in.read(buffer);
               }
            } else
            {
               log.error(String
                     .format("Resource [%s] not found.", resourceName));
            }
         } finally
         {
            if (in != null)
               in.close();
         }
      }
   }

   public int getPollTimeout()
   {
      return pollTimeout;
   }

   public void setPollTimeout(int pollTimeout)
   {
      this.pollTimeout = pollTimeout;
   }

   public int getPollInterval()
   {
      return pollInterval;
   }

   public void setPollInterval(int pollInterval)
   {
      this.pollInterval = pollInterval;
   }

   public boolean getDebug()
   {
      return debug;
   }

   public void setDebug(boolean debug)
   {
      this.debug = debug;
   }

   public void destroy()
   {

   }

   public ServletConfig getServletConfig()
   {
      return null;
   }

   public String getServletInfo()
   {
      return null;
   }

   public void init(ServletConfig config) throws ServletException
   {
      this.servletConfig = config;
   }

   public void service(HttpServletRequest request, HttpServletResponse response)
         throws ServletException, IOException
   {
      try
      {
         String pathInfo = request.getPathInfo();
         
         if (pathInfo.startsWith(servletConfig.getServletContext().getContextPath()))
         {
            pathInfo = pathInfo.substring(servletConfig.getServletContext().getContextPath().length());
         }               

         if (REQUEST_PATH_EXECUTE.equals(pathInfo))
         {
            executionHandlerInstance.get().handle(request, response);
         }
         else if (REQUEST_PATH_INTERFACE.equals(pathInfo))
         {
            interfaceHandlerInstance.get().handle(request, response);
         }
         else
         {
            Matcher m = pathPattern.matcher(pathInfo);
            if (m.matches())
            {
               String path = m.group(1);
               String resource = m.group(2);

               if (REMOTING_RESOURCE_PATH.equals(path))
               {
                  writeResource(resource, response);
                  if ("remote.js".equals(resource))
                  {
                     appendConfig(response.getOutputStream(), request
                           .getContextPath(), request);
                  }
               }
               response.getOutputStream().flush();
            }
         }
      } catch (Exception ex)
      {
         log.error("Error", ex);
      }
   }
}
