package org.jboss.seam.remoting;

import java.io.ByteArrayOutputStream;
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

import org.jboss.solder.logging.Logger;
import org.jboss.seam.remoting.model.ModelHandler;
import org.jboss.seam.remoting.validation.ConstraintTranslator;

/**
 * Serves JavaScript implementation of Seam Remoting
 *
 * @author Shane Bryzak
 */
public class Remoting extends HttpServlet {
    private static final long serialVersionUID = -3911197516105313424L;

    private static final String REQUEST_PATH_EXECUTE = "/execute";
    // private static final String REQUEST_PATH_SUBSCRIPTION = "/subscription";
    // private static final String REQUEST_PATH_POLL = "/poll";
    private static final String REQUEST_PATH_INTERFACE = "/interface.js";
    private static final String REQUEST_PATH_MODEL = "/model";
    private static final String REQUEST_PATH_VALIDATION = "/validate";

    @Inject
    Instance<ExecutionHandler> executionHandlerInstance;
    @Inject
    Instance<InterfaceGenerator> interfaceHandlerInstance;
    @Inject
    Instance<ModelHandler> modelHandlerInstance;
    @Inject
    Instance<ConstraintTranslator> translatorInstance;

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

    private Map<String, byte[]> resourceCache = new HashMap<String, byte[]>();

    private static final Logger log = Logger.getLogger(Remoting.class);

    private static final Pattern pathPattern = Pattern.compile("/(.*?)/([^/]+)");

    private static final String REMOTING_RESOURCE_PATH = "resource";

    private synchronized void initConfig(String contextPath, HttpServletRequest request) {
        if (!cachedConfig.containsKey(contextPath)) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nSeam.resourcePath = \"");
            sb.append(contextPath);
            sb.append(request.getServletPath());
            sb.append(servletConfig.getServletContext().getContextPath());
            sb.append("\";");
            sb.append("\nSeam.debug = ");
            sb.append(getDebug() ? "true" : "false");
            sb.append(";");
            /*
            * sb.append("\nSeam.pollInterval = "); sb.append(getPollInterval());
            * sb.append(";"); sb.append("\nSeam.pollTimeout = ");
            * sb.append(getPollTimeout()); sb.append(";");
            */

            cachedConfig.put(contextPath, sb.toString().getBytes());
        }
    }

    /**
     * Appends various configuration options to the remoting javascript client
     * api.
     *
     * @param out OutputStream
     */
    private void appendConfig(OutputStream out, String contextPath, HttpServletRequest request) throws IOException {
        if (!cachedConfig.containsKey(contextPath)) {
            initConfig(contextPath, request);
        }

        out.write(cachedConfig.get(contextPath));
    }

    /**
     * @param resourceName String The name of the resource to serve
     * @param out          OutputStream The OutputStream to write the resource to
     */
    private void writeResource(String resourceName, HttpServletResponse response, boolean compress) throws IOException {
        String cacheKey = resourceName + ":" + Boolean.toString(compress);
        if (!resourceCache.containsKey(cacheKey)) {
            synchronized (resourceCache) {
                if (!resourceCache.containsKey(cacheKey)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();

                    // Only allow resource requests for .js files
                    if (resourceName.endsWith(".js")) {
                        InputStream in = this.getClass().getClassLoader().getResourceAsStream("org/jboss/seam/remoting/" + resourceName);
                        try {
                            if (in != null) {
                                response.setContentType("text/javascript");

                                byte[] buffer = new byte[1024];
                                int read = in.read(buffer);
                                while (read != -1) {
                                    out.write(buffer, 0, read);
                                    read = in.read(buffer);
                                }

                                resourceCache.put(cacheKey, compress ? compressResource(out.toByteArray()) : out.toByteArray());

                                response.getOutputStream().write(resourceCache.get(cacheKey));
                            } else {
                                log.error(String.format("Resource [%s] not found.", resourceName));
                            }
                        } finally {
                            if (in != null)
                                in.close();
                        }
                    }
                }
            }
        } else {
            response.getOutputStream().write(resourceCache.get(cacheKey));
        }
    }

    /**
     * Compresses JavaScript resources by removing comments, cr/lf, leading and
     * trailing white space.
     *
     * @param resourceData The resource data to compress.
     * @return
     */
    private byte[] compressResource(byte[] resourceData) {
        String resource = new String(resourceData);

        // Remove comments
        resource = resource.replaceAll("/{2,}[^\\n\\r]*[\\n\\r]", "");
        resource = resource.replaceAll("/\\*([^*]|[\\r\\n]|(\\*+([^*/]|[\\r\\n])))*\\*/", "");

        // Remove leading and trailing space and CR/LF's for lines with a
        // statement terminator
        resource = resource.replaceAll(";\\s*[\\n\\r]+\\s*", ";");

        // Remove leading and trailing space and CR/LF's for lines with a block
        // terminator
        resource = resource.replaceAll("}\\s*[\\n\\r]+\\s*", "}");

        // Replace any remaining leading/trailing space and CR/LF with a single
        // space
        resource = resource.replaceAll("\\s*[\\n\\r]+\\s*", " ");

        return resource.getBytes();
    }

    public int getPollTimeout() {
        return pollTimeout;
    }

    public void setPollTimeout(int pollTimeout) {
        this.pollTimeout = pollTimeout;
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        this.pollInterval = pollInterval;
    }

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void destroy() {

    }

    public ServletConfig getServletConfig() {
        return servletConfig;
    }

    public String getServletInfo() {
        return null;
    }

    public void init(ServletConfig config) throws ServletException {
        this.servletConfig = config;
    }

    protected ExecutionHandler getExecutionHandler() {
        return executionHandlerInstance.get();
    }

    protected InterfaceGenerator getInterfaceHandler() {
        return interfaceHandlerInstance.get();
    }

    protected ModelHandler getModelHandler() {
        return modelHandlerInstance.get();
    }

    protected ConstraintTranslator getTranslatorHandler() {
        return translatorInstance.get();
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            String pathInfo = request.getPathInfo();

            // Nothing to do
            if (pathInfo == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "No path information provided");
                return;
            }

            if (pathInfo.startsWith(servletConfig.getServletContext().getContextPath())) {
                pathInfo = pathInfo.substring(servletConfig.getServletContext().getContextPath().length());
            }

            if (REQUEST_PATH_EXECUTE.equals(pathInfo)) {
                getExecutionHandler().handle(request, response);
            } else if (REQUEST_PATH_INTERFACE.equals(pathInfo)) {
                getInterfaceHandler().handle(request, response);
            } else if (REQUEST_PATH_MODEL.equals(pathInfo)) {
                getModelHandler().handle(request, response);
            } else if (REQUEST_PATH_VALIDATION.equals(pathInfo)) {
                getTranslatorHandler().handle(request, response);
            } else {
                Matcher m = pathPattern.matcher(pathInfo);
                if (m.matches()) {
                    String path = m.group(1);
                    String resource = m.group(2);

                    if (REMOTING_RESOURCE_PATH.equals(path)) {
                        String compressParam = request.getParameter("compress");
                        boolean compress = !(compressParam != null && "false".equals(compressParam));

                        writeResource(resource, response, compress);
                        if ("remote.js".equals(resource)) {
                            appendConfig(response.getOutputStream(), request.getContextPath(), request);
                        }
                    }
                    response.getOutputStream().flush();
                }
            }
        } catch (Exception ex) {
            log.error("Error", ex);
        }
    }
}
