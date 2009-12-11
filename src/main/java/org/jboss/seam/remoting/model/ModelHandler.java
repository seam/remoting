package org.jboss.seam.remoting.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jboss.seam.remoting.MarshalUtils;
import org.jboss.seam.remoting.RequestContext;
import org.jboss.seam.remoting.RequestHandler;
import org.jboss.seam.remoting.wrapper.Wrapper;
import org.jboss.weld.Container;
import org.jboss.weld.context.ContextLifecycle;
import org.jboss.weld.context.ConversationContext;
import org.jboss.weld.conversation.ConversationManager;
import org.jboss.weld.servlet.ConversationBeanStore;
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
            
      // Initialize the conversation context
      ConversationContext conversationContext = Container.instance().deploymentServices().get(ContextLifecycle.class).getConversationContext();
      conversationContext.setBeanStore(new ConversationBeanStore(request.getSession(), ctx.getConversationId()));
      conversationContext.setActive(true);
      
      if (ctx.getConversationId() != null && !ctx.getConversationId().isEmpty())
      { 
         conversationManager.beginOrRestoreConversation(ctx.getConversationId());
      }
      else
      {
         conversationManager.beginOrRestoreConversation(null);
      }
      
      Set<Model> models = new HashSet<Model>();
      Call action = null;      
      
      for (Element modelElement : (List<Element>) env.element("body").elements("model"))
      {     
         String operation = modelElement.attributeValue("operation");
         String callId = modelElement.attributeValue("callId");
         
         if ("fetch".equals(operation))
         {
            Model model = registry.createModel();
            models.add(model);
            model.setCallId(callId);           
            
            if (modelElement.elements("action").size() > 0)
            {
               Element actionElement = modelElement.element("action");
               Element targetElement = actionElement.element("target");
               Element qualifiersElement = actionElement.element("qualifiers");
               Element methodElement = actionElement.element("method");
               Element paramsElement = actionElement.element("params");
               Element refsElement = actionElement.element("refs");
               
               action = new Call(beanManager, callId, targetElement.getTextTrim(), 
                    qualifiersElement != null ? qualifiersElement.getTextTrim() : null, 
                    methodElement != null ? methodElement.getTextTrim() : null);                        
   
               if (refsElement != null)
               {
                  for (Element refElement : (List<Element>) refsElement.elements("ref"))
                  {
                     action.getContext().createWrapperFromElement(refElement);
                  }
      
                  for (Wrapper w : action.getContext().getInRefs().values())
                  {
                     w.unmarshal();
                  }
               }
   
               if (paramsElement != null)
               {
                  for (Element paramElement : (List<Element>) paramsElement.elements("param"))
                  {
                     action.addParameter(action.getContext().createWrapperFromElement(
                           (Element) paramElement.elements().get(0)));
                  }
               }
            }
            
            for (Element beanElement : (List<Element>) modelElement.elements("bean"))
            {
               Element beanNameElement = beanElement.element("name");
               Element beanQualifierElement = beanElement.element("qualifier");
               Element beanPropertyElement = beanElement.element("property");
               
               model.addBean(beanElement.attributeValue("alias"),
                     beanNameElement.getTextTrim(), 
                     beanQualifierElement != null ? beanQualifierElement.getTextTrim() : null, 
                     beanPropertyElement != null ? beanPropertyElement.getTextTrim() : null);
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

      if (action != null && action.getException() != null)
      {
         out.write(ENVELOPE_TAG_OPEN);
         out.write(BODY_TAG_OPEN);         
         MarshalUtils.marshalException(action.getException(), action.getContext(), out);
         out.write(BODY_TAG_CLOSE);
         out.write(ENVELOPE_TAG_CLOSE);
         out.flush();
      }      
      else
      {      
         for (Model model : models)
         {
            model.evaluate();
         }
         
         ctx.setConversationId(conversation.getId());      
         marshalResponse(models, ctx, response.getOutputStream());
      }
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
         MarshalUtils.marshalModel(model, out);
      }

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }   
}
