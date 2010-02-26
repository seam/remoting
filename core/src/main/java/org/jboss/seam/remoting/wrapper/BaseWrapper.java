package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;

import javax.enterprise.inject.spi.BeanManager;

import org.dom4j.Element;
import org.jboss.seam.remoting.CallContext;

/**
 * Base class for all Wrapper implementations.
 * 
 * @author Shane Bryzak
 */
public abstract class BaseWrapper implements Wrapper
{
   protected BeanManager beanManager;

   public BaseWrapper(BeanManager beanManager)
   {
      this.beanManager = beanManager;
   }

   /**
    * The path of this object within the result object graph
    */
   protected String path;

   /**
    * The call context
    */
   protected CallContext context;

   /**
    * The DOM4J element containing the value
    */
   protected Element element;

   /**
    * The wrapped value
    */
   protected Object value;

   /**
    * Sets the path.
    * 
    * @param path
    *           String
    */
   public void setPath(String path)
   {
      this.path = path;
   }

   /**
    * Sets the wrapped value
    * 
    * @param value
    *           Object
    */
   public void setValue(Object value)
   {
      this.value = value;
   }

   /**
    * Returns the wrapped value
    * 
    * @return Object
    */
   public Object getValue()
   {
      return value;
   }

   /**
    * Sets the call context
    */
   public void setCallContext(CallContext context)
   {
      this.context = context;
   }

   /**
    * Extracts a value from a DOM4J Element
    * 
    * @param element
    *           Element
    */
   public void setElement(Element element)
   {
      this.element = element;
   }

   /**
    * Default implementation does nothing
    */
   public void unmarshal()
   {
   }

   /**
    * Default implementation does nothing
    * 
    * @param out
    *           OutputStream
    * @throws IOException
    */
   public void serialize(OutputStream out) throws IOException
   {
   }
}
