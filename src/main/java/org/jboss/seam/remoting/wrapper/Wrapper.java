package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import org.dom4j.Element;
import org.jboss.seam.remoting.CallContext;

/**
 * Acts as a wrapper around parameter values passed within an AJAX call.
 *
 * @author Shane Bryzak
 */
public interface Wrapper
{
  /**
   * Sets the path of the wrapped object within the resulting object graph
   *
   * @param path String
   */
  public void setPath(String path);

  public void setCallContext(CallContext context);

  /**
   * Extracts a value from a DOM4J Element
   *
   * @param element Element
   */
  public void setElement(Element element);

  /**
   *
   * @param value Object
   */
  public void setValue(Object value);

  /**
   *
   * @return Object
   */
  public Object getValue();

  /**
   *
   */
  public void unmarshal();

  /**
   * Convert the wrapped parameter value to the specified target class.
   *
   */
  public Object convert(Type type) throws ConversionException;

  public void marshal(OutputStream out) throws IOException;

  public void serialize(OutputStream out) throws IOException;

  /**
   * Returns a score indicating whether this parameter value can be converted
   * to the specified type.  This helper method is used to determine which
   * (possibly overloaded) method of a component can/should be called.
   *
   * 0 - Cannot be converted
   * 1 - Can be converted to this type
   * 2 - Param is this exact type
   *
   */
  public ConversionScore conversionScore(Class cls);
}
