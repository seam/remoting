package org.jboss.seam.remoting.model;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.Iterator;

import javax.enterprise.context.Conversation;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.seam.remoting.Call;
import org.jboss.seam.remoting.RequestContext;
import org.jboss.seam.remoting.RequestHandler;
import org.jboss.seam.remoting.wrapper.Wrapper;
import org.jboss.weld.conversation.ConversationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelHandler implements RequestHandler
{
   private static final Logger log = LoggerFactory.getLogger(ModelHandler.class); 
   
   @Inject BeanManager beanManager;
   @Inject ConversationManager conversationManager;
   @Inject Conversation conversation;
   @Inject ModelRegistry registry;
   
   public void handle(HttpServletRequest request, HttpServletResponse response)
         throws Exception
   {
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
      log.debug("Processing model request: " + requestData);

      SAXReader xmlReader = new SAXReader();
      Document doc = xmlReader.read(new StringReader(requestData));
      final Element env = doc.getRootElement();
      final RequestContext ctx = new RequestContext(env.element("header"));
      
      if (!ctx.getConversationId().isEmpty())
      { 
         conversationManager.beginOrRestoreConversation(ctx.getConversationId());
      }
      
      Element modelElement = env.element("body").element("model");
      
      String operation = modelElement.attributeValue("operation");
      String id = modelElement.attributeValue("id");
      
      if ("fetch".equals(operation))
      {
         Call action = null;
         if (modelElement.elements("action").size() > 0)
         {
            Element actionElement = modelElement.element("action");
            Element targetElement = actionElement.element("target");
            Element qualifiersElement = actionElement.element("qualifiers");
            Element methodElement = actionElement.element("method");
            Element paramsElement = actionElement.element("params");
            action = new Call(beanManager, id, targetElement.getTextTrim(), 
                 qualifiersElement.getTextTrim(), methodElement.getTextTrim());
            
            Element refsNode = modelElement.element("refs");

            Iterator<?> iter = refsNode.elementIterator("ref");
            while (iter.hasNext())
            {
               action.getContext().createWrapperFromElement((Element) iter.next());
            }

            for (Wrapper w : action.getContext().getInRefs().values())
            {
               w.unmarshal();
            }

            iter = paramsElement.elementIterator("param");
            while (iter.hasNext())
            {
               Element param = (Element) iter.next();

               action.addParameter(action.getContext().createWrapperFromElement(
                     (Element) param.elementIterator().next()));
            }
         }
         
         // Unmarshal beans
         
         // Unmarshal expressions
         
         if (action != null)
         {
            action.execute();
         }
         
      }

      // Store the conversation ID in the outgoing context
      ctx.setConversationId(conversation.getId());
   }
}
