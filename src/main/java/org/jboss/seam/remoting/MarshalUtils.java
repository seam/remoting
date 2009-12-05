package org.jboss.seam.remoting;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.seam.remoting.wrapper.BeanWrapper;
import org.jboss.seam.remoting.wrapper.Wrapper;

/**
 *
 *
 * @author Shane Bryzak
 */
public class MarshalUtils
{
  private static final byte[] RESULT_TAG_OPEN_START = "<result id=\"".getBytes();
  private static final byte[] RESULT_TAG_OPEN_END = "\">".getBytes();
  private static final byte[] RESULT_TAG_OPEN = "<result>".getBytes();
  private static final byte[] RESULT_TAG_CLOSE = "</result>".getBytes();

  private static final byte[] VALUE_TAG_OPEN = "<value>".getBytes();
  private static final byte[] VALUE_TAG_CLOSE = "</value>".getBytes();
  
  private static final byte[] EXCEPTION_TAG_OPEN = "<exception>".getBytes();
  private static final byte[] EXCEPTION_TAG_CLOSE = "</exception>".getBytes();
  
  private static final byte[] MESSAGE_TAG_OPEN = "<message>".getBytes();
  private static final byte[] MESSAGE_TAG_CLOSE = "</message>".getBytes();

  public static void marshalResult(Call call, OutputStream out)
      throws IOException
  {
    if (call.getId() != null)
    {
      out.write(RESULT_TAG_OPEN_START);
      out.write(call.getId().getBytes());
      out.write(RESULT_TAG_OPEN_END);
    }
    else
      out.write(RESULT_TAG_OPEN);

    if (call.getException() != null)
    {
       out.write(EXCEPTION_TAG_OPEN);
       out.write(MESSAGE_TAG_OPEN);
       call.getContext().createWrapperFromObject(call.getException().getMessage(), "").marshal(out);
       out.write(MESSAGE_TAG_CLOSE);
       out.write(EXCEPTION_TAG_CLOSE);
    }
    else
    {
       out.write(VALUE_TAG_OPEN);
   
       call.getContext().createWrapperFromObject(call.getResult(), "").marshal(out);
   
       out.write(VALUE_TAG_CLOSE);
   
       out.write(RequestHandler.REFS_TAG_OPEN);
   
       // Using a for-loop, because stuff can get added to outRefs as we recurse the object graph
       for (int i = 0; i < call.getContext().getOutRefs().size(); i++)
       {
         Wrapper wrapper = call.getContext().getOutRefs().get(i);
   
         out.write(RequestHandler.REF_TAG_OPEN_START);
         out.write(Integer.toString(i).getBytes());
         out.write(RequestHandler.REF_TAG_OPEN_END);
   
         if (wrapper instanceof BeanWrapper && call.getConstraints() != null)
           ((BeanWrapper) wrapper).serialize(out, call.getConstraints());
         else
           wrapper.serialize(out);
   
         out.write(RequestHandler.REF_TAG_CLOSE);
       }
   
       out.write(RequestHandler.REFS_TAG_CLOSE);
    }
    
    out.write(RESULT_TAG_CLOSE);
  }
}
