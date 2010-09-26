package org.jboss.seam.remoting.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
import org.jboss.seam.remoting.util.Strings;
import org.jboss.seam.remoting.wrapper.BagWrapper;
import org.jboss.seam.remoting.wrapper.BeanWrapper;
import org.jboss.seam.remoting.wrapper.MapWrapper;
import org.jboss.seam.remoting.wrapper.Wrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming model fetch/apply requests
 *  
 * @author Shane Bryzak
 */
public class ModelHandler implements RequestHandler
{
   private static final Logger log = LoggerFactory.getLogger(ModelHandler.class); 
   
   @Inject BeanManager beanManager;
   @Inject ModelRegistry registry;
   @Inject Conversation conversation;
   
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
            
      //ConversationManager2 conversationManager = BeanProvider.conversationManager(request.getServletContext());
      
      if (ctx.getConversationId() != null && !Strings.isEmpty(ctx.getConversationId()))
      {  
         //conversationManager.setupConversation(ctx.getConversationId());
      }
      
      Element modelElement = env.element("body").element("model");
      String operation = modelElement.attributeValue("operation");
        
      if ("expand".equals(operation))
      {
         processExpandRequest(modelElement, ctx, response.getOutputStream());  
      }
      else
      {
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
      //for (Element exprElement : (List<Element>) modelElement.elements("expression"))
      //{
         
      //}
      
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
   
   @SuppressWarnings({ "unchecked", "unused" })
   private Model processApplyRequest(Element modelElement)
      throws Exception
   {
      Model model = registry.getModel(modelElement.attributeValue("id"));
      model.setAction(null);
      
      // We clone the outRefs to the inRefs so that the context can locate
      // already-loaded refs when unmarshalling
      for (int i = 0; i < model.getCallContext().getOutRefs().size(); i++)
      {
         model.getCallContext().getInRefs().put("" + i, model.getCallContext().getOutRefs().get(i));
      }
                  
      Element refsElement = modelElement.element("refs");
      if (refsElement != null)
      {
         List<Wrapper> newRefs = new ArrayList<Wrapper>();
         
         for (Element ref : (List<Element>) refsElement.elements("ref"))
         {
            newRefs.add(model.getCallContext().createWrapperFromElement(ref));
         }
         
         // Unmarshal any new ref values
         for (Wrapper w : newRefs)
         {
            w.unmarshal();
         }
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
               Wrapper target = model.getCallContext().getOutRefs().get(refId);
               if (!(target instanceof BeanWrapper))
               {
                  throw new IllegalStateException("Changeset for refId [" +
                        refId + "] does not reference a valid bean object");                     
               }
               
               for (Element member : (List<Element>) changeset.elements("member"))
               {
                  String name = member.attributeValue("name");
                  
                  Wrapper source = model.getCallContext().createWrapperFromElement(
                        (Element) member.elementIterator().next());
                                                      
                  if (source instanceof BagWrapper)
                  {                  
                     Object targetBag = ((BeanWrapper) target).getBeanProperty(name);
                     if (targetBag == null)
                     {
                        ((BeanWrapper) target).setBeanProperty(name, source);
                     }
                     else
                     {
                        Type t = ((BeanWrapper) target).getBeanPropertyType(name);
                        if (!cloneBagContents(source.convert(t), ((Wrapper) targetBag).getValue()))
                        {
                           ((BeanWrapper) target).setBeanProperty(name, source);
                        }
                     }
                  }
                  else if (source instanceof MapWrapper)
                  {                     
                     Object targetMap = ((BeanWrapper) target).getBeanProperty(name);
                     if (!Map.class.isAssignableFrom(targetMap.getClass()))
                     {
                        throw new IllegalStateException("Cannot assign Map value " +
                              "to non Map property [" + target.getClass().getName() +
                              "." + name + "]");
                     }
                     
                     if (targetMap == null)
                     {
                        ((BeanWrapper) target).setBeanProperty(name, source);
                     }
                     else
                     {                        
                        Type t = ((BeanWrapper) target).getBeanPropertyType(name);                        
                        cloneMapContents((Map<Object,Object>) source.convert(t), 
                              (Map<Object,Object>) targetMap);
                     }
                  }                  
                  else
                  {                                          
                     ((BeanWrapper) target).setBeanProperty(name, source);
                  }
               }
            }
            
            if (changeset.elements("bag").size() > 0)
            {
               Wrapper target = model.getCallContext().getOutRefs().get(refId);               
               Wrapper source = model.getCallContext().createWrapperFromElement(
                     (Element) changeset.element("bag"));
               cloneBagContents(source.convert(target.getValue().getClass()), 
                     target.getValue());
            }            
            else if (changeset.elements("map").size() > 0)
            {
               Wrapper target = model.getCallContext().getOutRefs().get(refId);
               Wrapper source = model.getCallContext().createWrapperFromElement(
                     (Element) changeset.element("map"));
               cloneMapContents((Map<Object,Object>) source.convert(target.getValue().getClass()),
                     (Map<Object,Object>) target.getValue());
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
   
   private void processExpandRequest(Element modelElement, RequestContext ctx, OutputStream out)
      throws Exception
   {
      Model model = registry.getModel(modelElement.attributeValue("id"));
      model.setAction(null);
    
      Element refElement = modelElement.element("ref");
      if (refElement == null)
      {
         throw new IllegalStateException("Invalid request state - no object ref found");
      }
      
      int refId = Integer.parseInt(refElement.attributeValue("id"));
      Wrapper target = model.getCallContext().getOutRefs().get(refId);
      
      int newRefIdx = model.getCallContext().getOutRefs().size();
      
      Element memberElement = refElement.element("member");
      String memberName = memberElement.attributeValue("name");
      
      Wrapper value = ((BeanWrapper) target).getBeanProperty(memberName);
      if (value instanceof BagWrapper)
      {
         ((BagWrapper) value).setLoadLazy(true);
      }
    
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

      MarshalUtils.marshalModelExpand(model, value, out, newRefIdx);      

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();  
   }
   
   /**
    * Clones the contents of the specified source bag into the specified target
    * bag. If the contents can be cloned, this method returns true, otherwise it
    * returns false.
    *  
    * @param sourceBag
    * @param targetBag
    * @return
    */
   @SuppressWarnings("unchecked")
   private boolean cloneBagContents(Object sourceBag, Object targetBag)
   {
      Class<?> cls = sourceBag.getClass();
      if (cls.isArray())
      {
         int sourceLen = Array.getLength(sourceBag);
         int targetLen = Array.getLength(targetBag);
         if (targetLen != sourceLen) return false;
         for (int i = 0; i < sourceLen; i++)
         {
            Array.set(targetBag, i, Array.get(sourceBag, i));
         }
         return true;
      }
      else if (List.class.isAssignableFrom(cls))
      {
         List<Object> sourceList = (List<Object>) sourceBag;
         List<Object> targetList = (List<Object>) targetBag;
         
         targetList.clear();
         
         for (int i = 0; i < sourceList.size(); i++)
         {
            if (targetList.size() < i + 1)
            {
               targetList.add(i, sourceList.get(i));  
            }
            else if (targetList.get(i) != sourceList.get(i))
            {
               targetList.set(i, sourceList.get(i));
            }
         }
         return true;        
      }
      else if (Set.class.isAssignableFrom(cls))
      {
         Set<Object> sourceSet = (Set<Object>) sourceBag;
         Set<Object> targetSet = (Set<Object>) targetBag;
         
         for (Object e : sourceSet)
         {
            if (!targetSet.contains(e))
            {
               targetSet.add(e);
            }
         }
         
         for (Object e : targetSet)
         {
            if (!sourceSet.contains(e))
            {
               targetSet.remove(e);
            }
         }         
         return true;
      }
      
      return false;
   }
   
   /**
    * Clones the contents of one Map into another
    * 
    * @param sourceMap
    * @param targetMap
    */
   private void cloneMapContents(Map<Object,Object> sourceMap, Map<Object,Object> targetMap)
   {
      for (Object key : sourceMap.keySet())
      {
         if (!targetMap.containsKey(key))
         {
            targetMap.put(key, sourceMap.get(key));
         }
      }
      
      for (Object key : targetMap.keySet())
      {
         if (!sourceMap.containsKey(key))
         {
            targetMap.remove(key);
         }
      }      
   }
   
   private void marshalResponse(Model model, RequestContext ctx, 
         OutputStream out) throws IOException
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

      MarshalUtils.marshalModel(model, out);

      out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
      out.flush();
   }   
}
