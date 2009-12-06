package org.jboss.seam.remoting.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

/**
 * Handles incoming model fetch/applyUpdate requests
 *  
 * @author Shane Bryzak
 */
public class ModelHandler implements RequestHandler
{
   private static final Logger log = LoggerFactory.getLogger(ModelHandler.class); 
   
   @Inject BeanManager beanManager;
   @Inject ConversationManager conversationManager;
   @Inject Conversation conversation;
   @Inject ModelRegistry registry;
   
   @SuppressWarnings("unchecked")
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
      
      Set<Model> models = new HashSet<Model>();
      
      for (Element modelElement : (List<Element>) env.element("body").elements("model"))
      {     
         String operation = modelElement.attributeValue("operation");
         String id = modelElement.attributeValue("id");
         
         if ("fetch".equals(operation))
         {
            Model model = registry.createModel();
            
            Call action = null;
            if (modelElement.elements("action").size() > 0)
            {
               Element actionElement = modelElement.element("action");
               Element targetElement = actionElement.element("target");
               Element qualifiersElement = actionElement.element("qualifiers");
               Element methodElement = actionElement.element("method");
               Element paramsElement = actionElement.element("params");
               Element refsElement = actionElement.element("refs");
               
               action = new Call(beanManager, id, targetElement.getTextTrim(), 
                    qualifiersElement.getTextTrim(), methodElement.getTextTrim());                        
   
               for (Element refElement : (List<Element>) refsElement.elements("ref"))
               {
                  action.getContext().createWrapperFromElement(refElement);
               }
   
               for (Wrapper w : action.getContext().getInRefs().values())
               {
                  w.unmarshal();
               }
   
               for (Element paramElement : (List<Element>) paramsElement.elements("param"))
               {
                  action.addParameter(action.getContext().createWrapperFromElement(
                        paramElement));
               }
            }
            
            for (Element beanElement : (List<Element>) modelElement.elements("bean"))
            {
               Element beanNameElement = beanElement.element("name");
               Element beanQualifierElement = beanElement.element("qualifier");
               Element beanPropertyElement = beanElement.element("property");
               
               model.addBean(beanElement.attributeValue("alias"),
                     beanNameElement.getTextTrim(), 
                     beanQualifierElement.getTextTrim(), 
                     beanPropertyElement.getTextTrim());
            }
            
            // TODO Unmarshal expressions - don't support this until security implications investigated
            for (Element exprElement : (List<Element>) modelElement.elements("expression"))
            {
               
            }
            
            if (action != null)
            {
               action.execute();
            }            
         }
      }

      ctx.setConversationId(conversation.getId());      
      marshalResponse(models, ctx, response.getOutputStream());
   }
   
   private void marshalResponse(Set<Model> models, RequestContext ctx, 
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

      for (Model model : models)
      {
         //MarshalUtils.marshalResult(call, out);
      }

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }   
}
