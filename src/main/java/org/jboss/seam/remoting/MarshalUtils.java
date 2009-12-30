package org.jboss.seam.remoting;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.jboss.seam.remoting.model.Model;
import org.jboss.seam.remoting.wrapper.BeanWrapper;
import org.jboss.seam.remoting.wrapper.Wrapper;

/**
 * 
 * 
 * @author Shane Bryzak
 */
public class MarshalUtils
{
   private static final byte[] RESULT_TAG_OPEN = "<result>".getBytes();
   private static final byte[] RESULT_TAG_CLOSE = "</result>".getBytes();
   
   private static final byte[] MODEL_TAG_OPEN_START = "<model id=\"".getBytes();
   private static final byte[] MODEL_TAG_OPEN_END = "\">".getBytes();
   private static final byte[] MODEL_TAG_CLOSE = "</model>".getBytes();

   private static final byte[] VALUE_TAG_OPEN = "<value>".getBytes();
   private static final byte[] VALUE_TAG_CLOSE = "</value>".getBytes();
   
   private static final byte[] ALIASED_VALUE_TAG_OPEN_START = "<value alias=\"".getBytes();
   private static final byte[] ALIASED_VALUE_TAG_OPEN_END = "\">".getBytes();
   private static final byte[] ALIASED_VALUE_TAG_CLOSE = "</value>".getBytes();

   private static final byte[] EXCEPTION_TAG_OPEN = "<exception>".getBytes();
   private static final byte[] EXCEPTION_TAG_CLOSE = "</exception>".getBytes();

   private static final byte[] MESSAGE_TAG_OPEN = "<message>".getBytes();
   private static final byte[] MESSAGE_TAG_CLOSE = "</message>".getBytes();

   public static void marshalCallResult(Call call, OutputStream out)
         throws IOException
   {
      out.write(RESULT_TAG_OPEN);

      if (call.getException() != null)
      {
         marshalException(call.getException(), call.getContext(), out);
      }
      else
      {
         out.write(VALUE_TAG_OPEN);
         call.getContext().createWrapperFromObject(call.getResult(), "")
               .marshal(out);
         out.write(VALUE_TAG_CLOSE);

         out.write(RequestHandler.REFS_TAG_OPEN);
         marshalRefs(call.getContext().getOutRefs(), call.getConstraints(), out);
         out.write(RequestHandler.REFS_TAG_CLOSE);
      }

      out.write(RESULT_TAG_CLOSE);
   }
   
   public static void marshalException(Throwable exception, CallContext ctx, OutputStream out)
      throws IOException
   {
      out.write(EXCEPTION_TAG_OPEN);
      out.write(MESSAGE_TAG_OPEN);
      ctx.createWrapperFromObject(exception.getMessage(), "").marshal(out);
      out.write(MESSAGE_TAG_CLOSE);
      out.write(EXCEPTION_TAG_CLOSE);      
   }
   
   public static void marshalModel(Model model, OutputStream out)
      throws IOException
   {
      out.write(MODEL_TAG_OPEN_START);
      out.write(model.getId().getBytes());      
      out.write(MODEL_TAG_OPEN_END);
      
      for (String alias : model.getBeanProperties().keySet())
      {
         Model.BeanProperty property = model.getBeanProperties().get(alias);
         
         out.write(ALIASED_VALUE_TAG_OPEN_START);
         out.write(alias.getBytes());
         out.write(ALIASED_VALUE_TAG_OPEN_END);
         model.getCallContext().createWrapperFromObject(property.getValue(), "")
            .marshal(out);
         out.write(ALIASED_VALUE_TAG_CLOSE);         
      }      

      out.write(RequestHandler.REFS_TAG_OPEN);
      marshalRefs(model.getCallContext().getOutRefs(), null, out);
      out.write(RequestHandler.REFS_TAG_CLOSE);      
      
      out.write(MODEL_TAG_CLOSE);
   }
   
   public static void marshalRefs(List<Wrapper> refs, List<String> constraints, 
         OutputStream out)
      throws IOException   
   {
      for (int i = 0; i < refs.size(); i++)
      {
         Wrapper wrapper = refs.get(i);

         out.write(RequestHandler.REF_TAG_OPEN_START);
         out.write(Integer.toString(i).getBytes());
         out.write(RequestHandler.REF_TAG_OPEN_END);

         if (wrapper instanceof BeanWrapper && constraints != null)
         {
            ((BeanWrapper) wrapper).serialize(out, constraints);
         }
         else
         {
            wrapper.serialize(out);
         }

         out.write(RequestHandler.REF_TAG_CLOSE);
      }      
   }
}
