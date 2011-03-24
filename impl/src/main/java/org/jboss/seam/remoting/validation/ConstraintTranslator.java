package org.jboss.seam.remoting.validation;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.Conversation;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.PropertyDescriptor;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.logging.Logger;
import org.jboss.seam.remoting.AnnotationsParser;
import org.jboss.seam.remoting.RequestHandler;
import org.jboss.seam.remoting.util.JsConverter;
import org.jboss.seam.remoting.util.Strings;

/**
 * This class reads constraints metadata from all requested beans, translate them and send them back to client
 *  
 * @author Amir Sadri
 */
public class ConstraintTranslator implements RequestHandler{
                                                   /////////TODO what if client wanted to use a customized ValidatorFactory
	private static final ValidatorFactory Factory = Validation.buildDefaultValidatorFactory();
	private static final Logger log = Logger.getLogger(ConstraintTranslator.class);
	
	private static Annotation[] EMPTY_ANNOTATIONS = new Annotation[]{};

	private static final byte[] VALIDATION_TAG_OPEN  = "<validation>".getBytes();
	private static final byte[] VALIDATION_TAG_CLOSE = "</validation>".getBytes();

	private static final byte[] MESSAGES_TAG_OPEN  = "<messages>".getBytes();
	private static final byte[] MESSAGES_TAG_CLOSE = "</messages>".getBytes();

	private static final byte[] BEAN_TAG_OPEN_START = "<b n=\"".getBytes();
	private static final byte[] BEAN_TAG_OPEN_END   = "\">".getBytes();
	private static final byte[] BEAN_TAG_CLOSE      = "</b>".getBytes();

	private static final byte[] PROPERTY_TAG_OPEN_START = "<p n=\"".getBytes();
	private static final byte[] PROPERTY_TAG_OPEN_END   = "\">".getBytes();
	private static final byte[] PROPERTY_TAG_CLOSE      = "</p>".getBytes();

	private static final byte[] CONSTRAINT_TAG_OPEN_START = "<c n=\"".getBytes();
	private static final byte[] CONSTRAINT_TAG_MIDDLE     = "\" parent=\"".getBytes();
	private static final byte[] CONSTRAINT_TAG_OPEN_END   = "\">".getBytes();
	private static final byte[] CONSTRAINT_TAG_CLOSE      = "</c>".getBytes();

	private static final byte[] GROUP_TAG_OPEN  = "<g id=\"".getBytes();
	private static final byte[] GROUP_TAG_CLOSE = "\" />".getBytes();

	private static final String GROUP_HIERARCHY_TAG_OPEN     = "<gh id=\"";
	private static final String GROUP_HIERARCHY_TAG_MIDDLE   = "\" n=\"";
	private static final String GROUP_HIERARCHY_TAG_OPEN_END = "\">";
	private static final String GROUP_HIERARCHY_CLOSE    = "</gh>";

	private static final String GROUP_PARENT_TAG_OPEN        = "<gp ";
	private static final String GROUP_PARENT_TAG_MIDDLE_ID   = " id=\"";
	private static final String GROUP_PARENT_TAG_MIDDLE_NAME = " n=\"";
	private static final String GROUP_PARENT_TAG_CLOSE       = "\" />";

	private static final byte[] MESSAGE_TAG_OPEN_START  = "<m c=\"".getBytes();
	private static final byte[] MESSAGE_TAG_OPEN_MIDDLE = "\" msg=\"".getBytes();
	private static final byte[] MESSAGE_TAG_OPEN_END    = "\" />".getBytes();

	private static final byte[] PARAMETER_TAG_OPEN_START  = "<pm n=\"".getBytes();
	private static final byte[] PARAMETER_TAG_OPEN_MIDDLE = "\" v=\"".getBytes();
	private static final byte[] PARAMETER_TAG_OPEN_END    = "\" />".getBytes();

	static ArrayList<String> DEFAULT_ATTRIBUTES = new ArrayList<String>();
	static {
		DEFAULT_ATTRIBUTES.add("message");
		DEFAULT_ATTRIBUTES.add("groups");
		DEFAULT_ATTRIBUTES.add("payload");
    }

	private HashMap<String , SpecialConsideration> specialConsiderations = new HashMap<String, SpecialConsideration>();

	@Inject BeanManager beanManager;
	@Inject Conversation conversation;


	public ConstraintTranslator(){
	  specialConsiderations.put("Pattern", new RegexpConsideration());
	  //////TODO implement a mechanism which would allow developers to add their own SpecialConsiderations dynamically 
	}

	/*
	 * handles validation requests by converting Constraint metadata to a client-side readable, XML format
	 * 
	 * @see org.jboss.seam.remoting.RequestHandler#handle(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
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
	   log.debug("[ConstraintHandler] Processing remote request: " + requestData);

	   // Parse the incoming request as XML
	   SAXReader xmlReader = new SAXReader();
	   Document doc  = xmlReader.read(new StringReader(requestData));
	   Element body      = doc.getRootElement().element("body");
	   Element beans     = body.element("beans");
	   Element msgs      = body.element("messages");
	    if(beans == null){
	      log.debug("Recieved envelope is empty! [no 'beans' tag]");	
	      return; /////There is nothing to handle...
	    }
	   String[] loadedMsgs = msgs != null ? msgs.getText().split(",,") : null;
	   Iterator<Element> children = beans.elementIterator();

	  Locale locale = null; 
	  try{ 
	   Bean<Locale> bean = (Bean<Locale>) beanManager.getBeans(Locale.class).iterator().next();
	   locale = bean.create(beanManager.createCreationalContext(bean));
	  }catch(Exception exp){
		 ////// for some reason, which most likely would be not having International Module being used,
	    /////// we cannot get hold of the dynamic Locale instance, so we have to carry on with the server's default one
		locale = Locale.getDefault();  
	  }

     this.marshalResponse(children, loadedMsgs, response.getOutputStream(),locale);
    }

	/*
	 * convert and then write bean's constraint metadata into the response stream,
	 * it also returns a list of required validation messages
	 */
	protected Map<String, Object[]> translateConstraints(String beanName, String qualifiers, OutputStream out)
	throws IOException
	{      
	   Bean<?> targetBean = getTargetBean(beanName, qualifiers);
	   HashMap<String, Object[]> validationMessages = new HashMap<String, Object[]>(32);
	   HashMap<String , ArrayList<String>> groupHierarechy = new HashMap<String , ArrayList<String>>();

	   out.write(BEAN_TAG_OPEN_START);
	   out.write(beanName.getBytes());
	   out.write(BEAN_TAG_OPEN_END);
	   Validator validator = Factory.getValidator();
	   BeanDescriptor descriptor = validator.getConstraintsForClass(targetBean.getBeanClass());
	   Iterator<PropertyDescriptor> descriptors = descriptor.getConstrainedProperties().iterator();	   
		while(descriptors.hasNext()){
		   PropertyDescriptor prop = descriptors.next();   
		   out.write(PROPERTY_TAG_OPEN_START);
		   out.write(prop.getPropertyName().getBytes()); 
		   out.write(PROPERTY_TAG_OPEN_END);
		   Iterator<ConstraintDescriptor<?>> constraints = prop.getConstraintDescriptors().iterator();
			 while(constraints.hasNext()){
			   ConstraintDescriptor<?> constraint = constraints.next();
			   String[] outcome = convertConstraint(constraint, groupHierarechy , out, null);
			   validationMessages.put(outcome[0], new Object[] {constraint , outcome[1]});
			 }  
			out.write(PROPERTY_TAG_CLOSE);
		   }
		  if(groupHierarechy.size() > 0){
			Iterator<String> keys = groupHierarechy.keySet().iterator();
			while(keys.hasNext()){
			   String key = keys.next();	
			   String[] tokens = key.split(":"); ///// first token is name , second one is id	
			   out.write((GROUP_HIERARCHY_TAG_OPEN+tokens[1]).getBytes());
			   out.write((GROUP_HIERARCHY_TAG_MIDDLE+tokens[0]).getBytes());
			   out.write(GROUP_HIERARCHY_TAG_OPEN_END.getBytes());
			   ArrayList<String> parentsTags = groupHierarechy.get(key);
			    if(parentsTags.size() > 0){
			      for(String tag : parentsTags)
			    	out.write(tag.getBytes());  
			    }
			   out.write(GROUP_HIERARCHY_CLOSE.getBytes());
			}
		  }
	      out.write(BEAN_TAG_CLOSE);

	  return validationMessages;	
	}  

	/*
	 * The core functionality 
	 */
	protected String[] convertConstraint(ConstraintDescriptor<?> constraint ,
			                             HashMap<String , ArrayList<String>> gh ,
			                             OutputStream out , 
			                             String parent)
	throws IOException
	{

	   String annotation  = constraint.getAnnotation().annotationType().toString();
	   annotation = annotation.substring(annotation.lastIndexOf(".")+1); ////I decided not to use the fully-qualified name
	   Map<String,Object> attrs = constraint.getAttributes();           //// in order to reduce the amount of data that is sent to client 
	   HashMap<String,Object> params = null;
	   if(attrs != null){
		 params = new HashMap<String,Object>();
		 for(String attr : attrs.keySet()){
	     if(!DEFAULT_ATTRIBUTES.contains(attr))
		   params.put(attr, attrs.get(attr));   
		  }
	    }
		if(this.specialConsiderations.containsKey(annotation)){
		  SpecialConsideration sc = this.specialConsiderations.get(annotation);
		  annotation = sc.reassessConstraintName(annotation);
		  params = sc.reassessParameters(params);
		}
		out.write(CONSTRAINT_TAG_OPEN_START);
		out.write(annotation.getBytes());
		if(parent != null){
		  out.write(CONSTRAINT_TAG_MIDDLE);
		  out.write(parent.getBytes()); 
		 }	 
		 out.write(CONSTRAINT_TAG_OPEN_END); 
		 ///////////////////////// Handling Groups if there is any
		 if(gh != null){
		 Set<Class<?>> groups = constraint.getGroups();
	     if(groups.size() > 0){
	       int counter = gh.size();	 
		   Iterator<Class<?>> groupIter = groups.iterator();
		   while(groupIter.hasNext()){
			  Class<?> groupClass = groupIter.next();
			  String name = groupClass.getName();
			  if(!name.equals("javax.validation.groups.Default")){
			    out.write(GROUP_TAG_OPEN);
			    out.write((BuildGroupHierarchy(groupClass, ++counter, gh)+"").getBytes());
			    out.write(GROUP_TAG_CLOSE);
			  }  
		    }
	       }
		 }
	     ///////////////////////////////////////////////////////
		 if(params != null && params.size() > 0){
		   StringBuilder builder =  new StringBuilder("_[");	 
		   for(String key : params.keySet()){	
		      Object obj = params.get(key);  
		      builder.append(key).append(":");
		      out.write(PARAMETER_TAG_OPEN_START);
		      out.write(key.getBytes());
		      out.write(PARAMETER_TAG_OPEN_MIDDLE);
		      String JSValue = obj.toString();
		      if(obj.getClass().isArray())
		        JSValue = JsConverter.convertArray((Object[])obj);
		      else if(obj instanceof Collection<?>)
		        JSValue = JsConverter.convertCollection((Collection<?>)obj);	 
		      else if(obj instanceof Map)
		        JSValue = JsConverter.convertMap((Map<? ,? >)obj);

		      out.write(JSValue.getBytes());
		      out.write(PARAMETER_TAG_OPEN_END);
		      builder.append(JSValue).append(",");	 
		    }	
		     builder.deleteCharAt(builder.length()-1);
			 builder.append("]");
			 annotation += builder.toString();

		 }
		 out.write(CONSTRAINT_TAG_CLOSE);

		 Set<ConstraintDescriptor<?>> compositeConstraints = constraint.getComposingConstraints();
		 if(compositeConstraints != null){
			Iterator<ConstraintDescriptor<?>> composites = compositeConstraints.iterator();
			while(composites.hasNext()){
			  convertConstraint(composites.next(), null , out, annotation); 
			 }
		  }              ///////////the outcome of all composed constraints is ignored, since their message
		                 ////////// will be overidden by their parent's anyway...   
	  return parent == null ? new String[] {annotation,(String)attrs.get("message")} : null;
	}

	/*
	 * extract the actual Bean
	 */
	protected Bean<?> getTargetBean(String beanName , String qualifiers)
	{
		Bean<?> targetBean = null;
		Set<Bean<?>> beans = beanManager.getBeans(beanName);     
	      if (beans.isEmpty())
	      {
	         try
	         {
	            Class<?> beanType = Class.forName(beanName);

	            Annotation[] q = qualifiers != null && !Strings.isEmpty(qualifiers) ? 
	                  new AnnotationsParser(beanType, qualifiers, beanManager).getAnnotations() : 
	                     EMPTY_ANNOTATIONS;

	            beans = beanManager.getBeans(beanType, q);            
	         }
	         catch (ClassNotFoundException ex)
	         {
	            throw new IllegalArgumentException("Invalid bean class specified: " + beanName);
	         }

	         if (beans.isEmpty())
	         {
	            throw new IllegalArgumentException(
	                  "Could not find bean with bean with type/name " + beanName + 
	                  ", qualifiers [" + qualifiers + "]");         
	         }         
	      }  

	     targetBean = beans.iterator().next();	
	   return targetBean;  
	}
   
	/*
	 * response is made here by converting each bean's constraint metadata and sending both metadata and required 
	 * messages.
	 * 
	 */
	private void marshalResponse(Iterator<Element> beans, 
			                     String[] msgs ,
			                     OutputStream out ,
			                     Locale locale)throws IOException
	{     	 	
	   out.write(ENVELOPE_TAG_OPEN);	
	   out.write(BODY_TAG_OPEN);	
	   out.write(VALIDATION_TAG_OPEN);
	   if(beans == null)
		   out.write("<null/>".getBytes());
	   else{
		  HashMap<String, Object[]> requiredMsgs = new HashMap<String,Object[]>(); 
		  while(beans.hasNext()){
			 final Element bean        = beans.next();
			 final Attribute qualifier = bean.attribute("qualifiers");
			 requiredMsgs.putAll(this.translateConstraints(bean.attribute("target").getText(), 
					                                      qualifier != null ? qualifier.getText() : null ,  
					                                      out) );

		  }

		 out.write(MESSAGES_TAG_OPEN);
		 if(msgs != null){
		   for(String msg : msgs)        /////cross-checking already available validation messages with 
			  requiredMsgs.remove(msg);  ///// the ones we are about to send

		 }
		 MessageInterpolator messageHandler = Factory.getMessageInterpolator(); 
		 for(String key : requiredMsgs.keySet()){
			out.write(MESSAGE_TAG_OPEN_START);
			out.write(key.getBytes());
			out.write(MESSAGE_TAG_OPEN_MIDDLE);
			Object[] entry = requiredMsgs.get(key);
			FakeInterpolatorContext context = new FakeInterpolatorContext((ConstraintDescriptor<?>)entry[0]);
			out.write(messageHandler.interpolate((String)entry[1], context, locale)
					                .replace("\"", "'")
					                .replace("&" , "&amp;")
					                .replace("<", "&lt;")
					                .getBytes());
			out.write(MESSAGE_TAG_OPEN_END);                             
		 }
		 out.write(MESSAGES_TAG_CLOSE);
	   }
	  out.write(VALIDATION_TAG_CLOSE);
	  out.write(BODY_TAG_CLOSE);
      out.write(ENVELOPE_TAG_CLOSE);
	  out.flush();
	}

	/*
	 * make the group hierarchy, while ensuring that there is no duplicate entry
	 */
	private int BuildGroupHierarchy(Class<?> group , 
			                         int counter    ,
			                         final HashMap<String , ArrayList<String>> gh)
	{
	   String groupName = group.getName();	
	   Iterator<String> keys = gh.keySet().iterator();
	   int flag = -1;
	   while(keys.hasNext()){
		 String n = keys.next(); 
		 if(n.startsWith(groupName.substring(groupName.lastIndexOf(".")+1))){		
		   flag = Integer.parseInt(n.split(":")[1]);	 
		   break; 
		 }  
	   }
	   if(flag != -1)
		 return flag; 	
	   Class<?>[] parents = group.getInterfaces();
	   ArrayList<String> tags = new ArrayList<String>();  
	   for(int i=0;i<parents.length;i++) {
		  Class<?>[] grandParents = parents[i].getInterfaces();
		  String name = parents[i].getName();
		  name = name.substring(name.lastIndexOf(".")+1);
		  if(grandParents.length == 0)
			tags.add(GROUP_PARENT_TAG_OPEN + GROUP_PARENT_TAG_MIDDLE_NAME + name + GROUP_PARENT_TAG_CLOSE);  
		  else{
		   int temp = BuildGroupHierarchy(parents[i], counter+i+1 , gh);	  
		   tags.add(GROUP_PARENT_TAG_OPEN + GROUP_PARENT_TAG_MIDDLE_ID + temp + GROUP_PARENT_TAG_CLOSE);	

		  } 
	   }
	  gh.put(groupName.substring(groupName.lastIndexOf(".")+1)+":"+counter, tags); 

	 return counter;  
	}

  /*
   * we need a InterpolatorContext to fetch messages before really validating them. 	
   */
  private final class FakeInterpolatorContext implements MessageInterpolator.Context{

	private ConstraintDescriptor<?> cd;  

    public FakeInterpolatorContext(ConstraintDescriptor<?> descriptor){
    	this.cd = descriptor;
    }

	public ConstraintDescriptor<?> getConstraintDescriptor() {
		return this.cd;
	}

	public Object getValidatedValue() {
		return "?value?"; //////we dont know yet!!
	}

  }
  
}

