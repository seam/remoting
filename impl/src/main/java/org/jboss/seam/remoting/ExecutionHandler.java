package org.jboss.seam.remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Iterator;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.Conversation;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.logging.Logger;
import org.jboss.seam.remoting.wrapper.Wrapper;

/**
 * Unmarshals the calls from an HttpServletRequest, executes them in order and
 * marshals the responses.
 * 
 * @author Shane Bryzak
 */
public class ExecutionHandler extends AbstractRequestHandler implements RequestHandler
{
   private static final Logger log = Logger.getLogger(ExecutionHandler.class);

   @Inject BeanManager beanManager;
   @Inject Conversation conversation;

   /**
    * The entry point for handling a request.
    * 
    * @param request HttpServletRequest
    * @param response HttpServletResponse
    * @throws Exception
    */
   public void handle(HttpServletRequest request,
         final HttpServletResponse response) throws Exception
   {
      // We're sending an XML response, so set the response content type to
      // text/xml
      response.setContentType("text/xml");

      ByteArrayOutputStream out = new ByteArrayOutputStream();

      byte[] buffer = new byte[256];
      int read = request.getInputStream().read(buffer);
      while (read != -1)
      {
         out.write(buffer, 0, read);
         read = request.getInputStream().read(buffer);
      }

      String requestData = new String(out.toByteArray());
      log.debug("Processing remote request: " + requestData);

      // Parse the incoming request as XML
      SAXReader xmlReader = new SAXReader();
      Document doc = xmlReader.read(new StringReader(requestData));
      final Element env = doc.getRootElement();
      final RequestContext ctx = new RequestContext(env.element("header"));
      
      try
      {
         activateConversationContext(request, ctx.getConversationId());
   
         // Extract the calls from the request
         Call call = unmarshalCall(env);
         call.execute();
         
         // Store the conversation ID in the outgoing context
         try
         {
            ctx.setConversationId(conversation.isTransient() ? null : conversation.getId());
         }
         catch (ContextNotActiveException ex)
         {
            // No active conversation context, ignore
         }
   
         // Package up the response
         marshalResponse(call, ctx, response.getOutputStream());
      }
      finally
      {
         deactivateConversationContext(request);
      }
   }
   


   /**
    * Unmarshal the request into a list of Calls.
    * 
    * @param env
    *           Element
    * @throws Exception
    *           for any error
    * @return
    *         Call
    */
   @SuppressWarnings("unchecked")
   private Call unmarshalCall(Element env) throws Exception
   {
      try
      {
         Element callElement = env.element("body").element("call");
  
         Element targetNode = callElement.element("target");
         Element qualifiersNode = callElement.element("qualifiers");
         Element methodNode = callElement.element("method");
         
         Call call = new Call(beanManager, targetNode.getText(), 
               qualifiersNode != null ? qualifiersNode.getText() : null, 
               methodNode.getText());

         // First reconstruct all the references
         Element refsNode = callElement.element("refs");
         
         Iterator<Element> iter = refsNode.elementIterator("ref");
         while (iter.hasNext())
         {
            call.getContext().createWrapperFromElement(iter.next());
         }

         // Now unmarshal the ref values
         for (Wrapper w : call.getContext().getInRefs().values())
         {
            w.unmarshal();
         }

         Element paramsNode = callElement.element("params");

         // Then process the param values
         iter = paramsNode.elementIterator("param");
         while (iter.hasNext())
         {
            Element param = iter.next();

            call.addParameter(call.getContext().createWrapperFromElement(
                  (Element) param.elementIterator().next()));
         }

         return call;
      }
      catch (Exception ex)
      {
         log.error("Error unmarshalling calls from request", ex);
         throw ex;
      }
   }

   /**
    * Write the results to the output stream.
    * 
    * @param call
    *           List The list of calls to write
    * @param ctx
    *           The current Request Context
    * @param out
    *           OutputStream The stream to write to
    * @throws IOException
    *           for any I/O error
    */
   private void marshalResponse(Call call, RequestContext ctx, OutputStream out)
      throws IOException
   {
      out.write(ENVELOPE_TAG_OPEN);
      out.write(HEADER_OPEN);
      out.write(CONTEXT_TAG_OPEN);
      if (ctx.getConversationId() != null)
      {
         out.write(CONVERSATION_ID_TAG_OPEN);
         out.write(ctx.getConversationId().getBytes());
         out.write(CONVERSATION_ID_TAG_CLOSE);
      }
      out.write(CALL_ID_TAG_OPEN);
      out.write(ctx.getCallId().toString().getBytes());
      out.write(CALL_ID_TAG_CLOSE);
      out.write(CONTEXT_TAG_CLOSE);
      out.write(HEADER_CLOSE);

      out.write(BODY_TAG_OPEN);
      MarshalUtils.marshalCallResult(call, out);

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }
}
