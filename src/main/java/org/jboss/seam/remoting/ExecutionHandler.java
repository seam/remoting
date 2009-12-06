package org.jboss.seam.remoting;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.enterprise.context.Conversation;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.seam.remoting.wrapper.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unmarshals the calls from an HttpServletRequest, executes them in order and
 * marshals the responses.
 * 
 * @author Shane Bryzak
 */
public class ExecutionHandler implements RequestHandler
{
   private static final Logger log = LoggerFactory
         .getLogger(ExecutionHandler.class);

   private static final byte[] HEADER_OPEN = "<header>".getBytes();
   private static final byte[] HEADER_CLOSE = "</header>".getBytes();
   private static final byte[] CONVERSATION_ID_TAG_OPEN = "<conversationId>"
         .getBytes();
   private static final byte[] CONVERSATION_ID_TAG_CLOSE = "</conversationId>"
         .getBytes();

   private static final byte[] CONTEXT_TAG_OPEN = "<context>".getBytes();
   private static final byte[] CONTEXT_TAG_CLOSE = "</context>".getBytes();

   @Inject BeanManager beanManager;
   @Inject Conversation conversation;

   /**
    * The entry point for handling a request.
    * 
    * @param request
    *           HttpServletRequest
    * @param response
    *           HttpServletResponse
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
      final RequestContext ctx = unmarshalContext(env);
      
      if (!ctx.getConversationId().isEmpty())
      { 
         // TODO restore the conversation if there is a conversation ID in the context
         //conversation.
      }

      // Extract the calls from the request
      List<Call> calls = unmarshalCalls(env);

      // Execute each of the calls
      for (Call call : calls)
      {
         call.execute();
      }

      // Store the conversation ID in the outgoing context
      ctx.setConversationId(conversation.getId());

      // Package up the response
      marshalResponse(calls, ctx, response.getOutputStream());
   }

   /**
    * Unmarshals the context from the request envelope header.
    * 
    * @param env
    *           Element
    * @return RequestContext
    */
   private RequestContext unmarshalContext(Element env)
   {
      RequestContext ctx = new RequestContext();

      Element header = env.element("header");
      if (header != null)
      {
         Element context = header.element("context");
         if (context != null)
         {

            Element convId = context.element("conversationId");
            if (convId != null)
            {
               ctx.setConversationId(convId.getText());
            }
         }
      }

      return ctx;
   }

   /**
    * Unmarshal the request into a list of Calls.
    * 
    * @param env
    *           Element
    * @throws Exception
    */
   @SuppressWarnings("unchecked")
   private List<Call> unmarshalCalls(Element env) throws Exception
   {
      try
      {
         List<Call> calls = new ArrayList<Call>();

         List<Element> callElements = env.element("body").elements("call");

         for (Element e : callElements)
         {           
            Element targetNode = e.element("target");
            Element qualifiersNode = e.element("qualifiers");
            Element methodNode = e.element("method");
            
            Call call = new Call(beanManager, e.attributeValue("id"), 
                  targetNode.getText(), 
                  qualifiersNode != null ? qualifiersNode.getText() : null, 
                  methodNode.getText());

            // First reconstruct all the references
            Element refsNode = e.element("refs");

            Iterator iter = refsNode.elementIterator("ref");
            while (iter.hasNext())
            {
               call.getContext()
                     .createWrapperFromElement((Element) iter.next());
            }

            // Now unmarshal the ref values
            for (Wrapper w : call.getContext().getInRefs().values())
            {
               w.unmarshal();
            }

            Element paramsNode = e.element("params");

            // Then process the param values
            iter = paramsNode.elementIterator("param");
            while (iter.hasNext())
            {
               Element param = (Element) iter.next();

               call.addParameter(call.getContext().createWrapperFromElement(
                     (Element) param.elementIterator().next()));
            }

            calls.add(call);
         }

         return calls;
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
    * @param calls
    *           List The list of calls to write
    * @param out
    *           OutputStream The stream to write to
    * @throws IOException
    */
   private void marshalResponse(List<Call> calls, RequestContext ctx,
         OutputStream out) throws IOException
   {
      out.write(ENVELOPE_TAG_OPEN);

      if (ctx.getConversationId() != null)
      {
         out.write(HEADER_OPEN);
         out.write(CONTEXT_TAG_OPEN);
         out.write(CONVERSATION_ID_TAG_OPEN);
         out.write(ctx.getConversationId().getBytes());
         out.write(CONVERSATION_ID_TAG_CLOSE);
         out.write(CONTEXT_TAG_CLOSE);
         out.write(HEADER_CLOSE);
      }

      out.write(BODY_TAG_OPEN);

      for (Call call : calls)
      {
         MarshalUtils.marshalResult(call, out);
      }

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }
}
