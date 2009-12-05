package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.BeanManager;

/**
 * @author Shane Bryzak
 */
public class BooleanWrapper extends BaseWrapper implements Wrapper
{
   public BooleanWrapper(BeanManager beanManager)
   {
      super(beanManager);
      // TODO Auto-generated constructor stub
   }

   private static final byte[] BOOL_TAG_OPEN = "<bool>".getBytes();
   private static final byte[] BOOL_TAG_CLOSE = "</bool>".getBytes();

   public void marshal(OutputStream out) throws IOException
   {
      out.write(BOOL_TAG_OPEN);
      out.write(((Boolean) value).toString().getBytes());
      out.write(BOOL_TAG_CLOSE);
   }

   public Object convert(Type type) throws ConversionException
   {
      if (type.equals(Boolean.class) || type.equals(Object.class))
         value = Boolean.valueOf(element.getStringValue());
      else if (type.equals(Boolean.TYPE))
         value = Boolean.parseBoolean(element.getStringValue());
      else
         throw new ConversionException(String.format(
               "Parameter [%s] cannot be converted to type [%s].", element
                     .getStringValue(), type));

      return value;
   }

   public ConversionScore conversionScore(Class cls)
   {
      if (cls.equals(Boolean.class) || cls.equals(Boolean.TYPE))
         return ConversionScore.exact;
      else if (cls.equals(Object.class))
         return ConversionScore.compatible;
      else
         return ConversionScore.nomatch;
   }
}
