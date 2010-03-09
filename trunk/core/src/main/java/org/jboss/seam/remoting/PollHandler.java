package org.jboss.seam.remoting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.seam.remoting.messaging.PollError;
import org.jboss.seam.remoting.messaging.PollRequest;
import org.jboss.seam.remoting.messaging.SubscriptionRegistry;
import org.jboss.seam.remoting.wrapper.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles JMS Message poll requests.
 * 
 * @author Shane Bryzak
 */
public class PollHandler implements RequestHandler
{
   private static final Logger log = LoggerFactory.getLogger(PollHandler.class);

   private static final byte[] ERRORS_TAG_OPEN_START = "<errors token=\""
         .getBytes();
   private static final byte[] ERRORS_TAG_OPEN_END = "\">".getBytes();

   private static final byte[] ERROR_TAG_OPEN_START = "<error code=\""
         .getBytes();
   private static final byte[] ERROR_TAG_OPEN_END = "\">".getBytes();
   private static final byte[] ERROR_TAG_CLOSE = "</error>".getBytes();

   private static final byte[] MESSAGES_TAG_OPEN_START = "<messages token=\""
         .getBytes();
   private static final byte[] MESSAGES_TAG_OPEN_END = "\">".getBytes();
   private static final byte[] MESSAGES_TAG_CLOSE = "</messages>".getBytes();

   private static final byte[] MESSAGE_TAG_OPEN_START = "<message type=\""
         .getBytes();
   private static final byte[] MESSAGE_TAG_OPEN_END = "\">".getBytes();
   private static final byte[] MESSAGE_TAG_CLOSE = "</message>".getBytes();

   private static final byte[] VALUE_TAG_OPEN = "<value>".getBytes();
   private static final byte[] VALUE_TAG_CLOSE = "</value>".getBytes();

   @Inject
   BeanManager beanManager;
   @Inject
   SubscriptionRegistry registry;

   public void handle(HttpServletRequest request,
         final HttpServletResponse response) throws Exception
   {
      // We're sending an XML response, so set the response content type to
      // text/xml
      response.setContentType("text/xml");

      // Parse the incoming request as XML
      SAXReader xmlReader = new SAXReader();
      Document doc = xmlReader.read(request.getInputStream());
      Element env = doc.getRootElement();

      final List<PollRequest> polls = unmarshalRequests(env);

      for (PollRequest req : polls)
      {
         req.poll();
      }

      // Package up the response
      marshalResponse(polls, response.getOutputStream());
   }

   @SuppressWarnings("unchecked")
   private List<PollRequest> unmarshalRequests(Element env) throws Exception
   {
      try
      {
         List<PollRequest> requests = new ArrayList<PollRequest>();

         List<Element> requestElements = env.element("body").elements("poll");
         for (Element e : requestElements)
         {
            requests.add(new PollRequest(e.attributeValue("token"), Integer
                  .parseInt(e.attributeValue("timeout")), registry));
         }

         return requests;
      }
      catch (Exception ex)
      {
         log.error("Error unmarshalling subscriptions from request", ex);
         throw ex;
      }
   }

   private void marshalResponse(List<PollRequest> reqs, OutputStream out)
         throws IOException
   {
      out.write(ENVELOPE_TAG_OPEN);
      out.write(BODY_TAG_OPEN);

      for (PollRequest req : reqs)
      {
         if (req.getErrors() != null && req.getErrors().size() > 0)
         {
            out.write(ERRORS_TAG_OPEN_START);
            out.write(req.getToken().getBytes());
            out.write(ERRORS_TAG_OPEN_END);
            for (PollError err : req.getErrors())
            {
               writeError(err, out);
            }
         }
         else if (req.getMessages() != null && req.getMessages().size() > 0)
         {
            out.write(MESSAGES_TAG_OPEN_START);
            out.write(req.getToken().getBytes());
            out.write(MESSAGES_TAG_OPEN_END);
            for (Message m : req.getMessages())
            {
               try
               {
                  writeMessage(m, out);
               }
               catch (JMSException ex)
               {
               }
               catch (IOException ex)
               {
               }
            }
            out.write(MESSAGES_TAG_CLOSE);
         }
      }

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }

   private void writeMessage(Message m, OutputStream out) throws IOException,
         JMSException
   {
      out.write(MESSAGE_TAG_OPEN_START);

      // We need one of these to maintain a list of outbound references
      CallContext ctx = new CallContext(beanManager);
      Object value = null;

      if (m instanceof TextMessage)
      {
         out.write("text".getBytes());
         value = ((TextMessage) m).getText();
      }
      else if (m instanceof ObjectMessage)
      {
         out.write("object".getBytes());
         value = ((ObjectMessage) m).getObject();
      }

      out.write(MESSAGE_TAG_OPEN_END);

      out.write(VALUE_TAG_OPEN);
      ctx.createWrapperFromObject(value, "").marshal(out);
      out.write(VALUE_TAG_CLOSE);

      out.write(REFS_TAG_OPEN);

      // Using a for-loop, because stuff can get added to outRefs as we recurse
      // the object graph
      for (int i = 0; i < ctx.getOutRefs().size(); i++)
      {
         Wrapper wrapper = ctx.getOutRefs().get(i);

         out.write(REF_TAG_OPEN_START);
         out.write(Integer.toString(i).getBytes());
         out.write(REF_TAG_OPEN_END);

         wrapper.serialize(out);

         out.write(REF_TAG_CLOSE);
      }

      out.write(REFS_TAG_CLOSE);

      out.write(MESSAGE_TAG_CLOSE);
   }

   private void writeError(PollError error, OutputStream out)
         throws IOException
   {
      out.write(ERROR_TAG_OPEN_START);
      out.write(error.getCode().getBytes());
      out.write(ERROR_TAG_OPEN_END);
      out.write(error.getMessage().getBytes());
      out.write(ERROR_TAG_CLOSE);
   }
}
