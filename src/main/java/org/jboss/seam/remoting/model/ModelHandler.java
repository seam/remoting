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
import org.jboss.seam.remoting.CallContext;
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
   @Inject ModelRegistry registry;
   @Inject Conversation conversation;
   
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
            
      ConversationContext conversationContext = null;
      try
      {         
         // Initialize the conversation context
         conversationContext = Container.instance().deploymentServices().get(ContextLifecycle.class).getConversationContext();
         
         if (ctx.getConversationId() != null && !ctx.getConversationId().isEmpty())
         { 
            conversationContext.setBeanStore(new ConversationBeanStore(request.getSession(), ctx.getConversationId()));
            conversationContext.setActive(true);  
            conversationManager.beginOrRestoreConversation(ctx.getConversationId());
         }
         else
         {
            conversationContext.setBeanStore(new ConversationBeanStore(request.getSession(), 
                  ((org.jboss.weld.conversation.ConversationImpl) conversation).getUnderlyingId()));
            conversationContext.setActive(true);
         }
         
         Element modelElement = env.element("body").element("model");
         String operation = modelElement.attributeValue("operation");
         
         Model model = null;
           
         if ("fetch".equals(operation))
         {
            model = processFetchRequest(modelElement);
         }
         else if ("apply".equals(operation))
         {
            model = processApplyRequest(modelElement);
         }
   
         if (model.getAction() != null && model.getAction().getException() != null)
         {
            response.getOutputStream().write(ENVELOPE_TAG_OPEN);
            response.getOutputStream().write(BODY_TAG_OPEN);         
            MarshalUtils.marshalException(model.getAction().getException(), 
                  model.getAction().getContext(), response.getOutputStream());
            response.getOutputStream().write(BODY_TAG_CLOSE);
            response.getOutputStream().write(ENVELOPE_TAG_CLOSE);
            response.getOutputStream().flush();
            return;
         }
         
         model.evaluate();
         
         ctx.setConversationId(conversation.isTransient() ? null : conversation.getId());
         marshalResponse(model, ctx, response.getOutputStream());
      }
      finally
      {
         conversationManager.cleanupConversation();
         if (conversationContext != null)
         {
            conversationContext.setBeanStore(null);
            conversationContext.setActive(false);            
         }
      }
   }
   
   @SuppressWarnings({ "unchecked" }) 
   private Model processFetchRequest(Element modelElement)
      throws Exception
   {
      Model model = registry.createModel();
      
      if (modelElement.elements("action").size() > 0)
      {         
         unmarshalAction(modelElement.element("action"), model);
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
      
      if (model.getAction() != null)
      {
         model.getAction().execute();                              
      }
      
      return model;
   }
   
   @SuppressWarnings("unchecked")
   private void unmarshalAction(Element actionElement, Model model)
   {
      Element targetElement = actionElement.element("target");
      Element qualifiersElement = actionElement.element("qualifiers");
      Element methodElement = actionElement.element("method");
      Element paramsElement = actionElement.element("params");
      Element refsElement = actionElement.element("refs");
      
      model.setAction(new Call(beanManager, targetElement.getTextTrim(), 
           qualifiersElement != null ? qualifiersElement.getTextTrim() : null, 
           methodElement != null ? methodElement.getTextTrim() : null));                        

      if (refsElement != null)
      {
         for (Element refElement : (List<Element>) refsElement.elements("ref"))
         {
            model.getAction().getContext().createWrapperFromElement(refElement);
         }

         for (Wrapper w : model.getAction().getContext().getInRefs().values())
         {
            w.unmarshal();
         }
      }

      if (paramsElement != null)
      {
         for (Element paramElement : (List<Element>) paramsElement.elements("param"))
         {
            model.getAction().addParameter(model.getAction().getContext().createWrapperFromElement(
                  (Element) paramElement.elements().get(0)));
         }
      }      
   }
   
   @SuppressWarnings("unchecked")
   private Model processApplyRequest(Element modelElement)
      throws Exception
   {
      Model model = registry.getModel(modelElement.attributeValue("uid"));
      model.setAction(null);
      
      CallContext ctx = new CallContext(beanManager);
      
      Element refsElement = modelElement.element("refs");
      for (Element ref : (List<Element>) refsElement.elements("ref"))
      {
         ctx.createWrapperFromElement(ref);
      }
      
      Element delta = modelElement.element("delta");
      if (delta != null)
      {
         List<Element> changesets = delta.elements("changeset");
         for (Element changeset : changesets)
         {
            int refId = Integer.parseInt(changeset.attributeValue("refid"));
            
            if (changeset.elements("member").size() > 0)
            {
               for (Element member : (List<Element>) changeset.elements("member"))
               {
                  String name = member.attributeValue("name");
                  Wrapper w = model.getCallContext().createWrapperFromElement(
                        (Element) member.elementIterator().next());
                  model.setModelProperty(refId, name, w);                  
               }
            }
            
         }
      }
      
      if (modelElement.elements("action").size() > 0)
      {         
         unmarshalAction(modelElement.element("action"), model);
      }      
      
      if (model.getAction() != null)
      {
         model.getAction().execute();                              
      }    
      
      return model;
   }
   
   private void marshalResponse(Model model, RequestContext ctx, 
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
         out.write(CALL_ID_TAG_OPEN);
         out.write(ctx.getCallId().toString().getBytes());
         out.write(CALL_ID_TAG_CLOSE);
         out.write(CONTEXT_TAG_CLOSE);
         out.write(HEADER_CLOSE);
      }

      out.write(BODY_TAG_OPEN);

      MarshalUtils.marshalModel(model, out);

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }   
}
