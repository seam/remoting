package org.jboss.seam.remoting;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.seam.remoting.annotations.WebRemote;
import org.jboss.seam.remoting.util.Strings;
import org.jboss.seam.remoting.wrapper.ConversionException;
import org.jboss.seam.remoting.wrapper.ConversionScore;
import org.jboss.seam.remoting.wrapper.Wrapper;

/**
 * 
 * @author Shane Bryzak
 */
public class Call
{
   public static Annotation[] EMPTY_ANNOTATIONS = new Annotation[]{};
   
   private BeanManager beanManager;

   private String methodName;
   private Throwable exception;

   private List<Wrapper> params = new ArrayList<Wrapper>();

   private Object result;

   private CallContext context;

   private List<String> constraints = null;
   
   private Bean<?> targetBean = null;

   public Call(BeanManager beanManager, String beanName, String qualifiers, String methodName)
   {
      this.beanManager = beanManager;
      this.methodName = methodName;
      this.context = new CallContext(beanManager);
      
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
   }

   /**
    * Return the call context.
    * 
    * @return CallContext
    */
   public CallContext getContext()
   {
      return context;
   }

   /**
    * Returns the exception thrown by the invoked method. If no exception was
    * thrown, will return null.
    */
   public Throwable getException()
   {
      return exception;
   }

   /**
    * Add a parameter to this call.
    * 
    * @param param
    */
   public void addParameter(Wrapper param)
   {
      params.add(param);
   }

   /**
    * Returns the result of this call.
    * 
    * @return Wrapper
    */
   public Object getResult()
   {
      return result;
   }

   /**
    * Required for unit tests
    * 
    * @param result
    */
   public void setResult(Object result)
   {
      this.result = result;
   }

   /**
    * Returns the object graph constraints annotated on the method that is
    * called.
    * 
    * @return List The constraints
    */
   public List<String> getConstraints()
   {
      return constraints;
   }

   /**
    * Required for unit tests
    * 
    * @param constraints
    */
   public void setConstraints(List<String> constraints)
   {
      this.constraints = constraints;
   }

   /**
    * Execute this call
    * 
    * @throws Exception
    */
   public void execute() throws Exception
   {
      CreationalContext<?> ctx = beanManager.createCreationalContext(targetBean);
      
      // Get an instance of the component
      Object instance = beanManager.getReference(targetBean, targetBean.getBeanClass(), ctx);;      

      if (instance == null)
      {
         throw new RuntimeException(String.format(
               "Could not create instance of bean %s", 
               targetBean.getBeanClass().getName()));
      }

      // Find the method according to the method name and the parameter classes
      Method m = findMethod(methodName, targetBean.getBeanClass());
      if (m == null)
      {
         throw new RuntimeException("No compatible method found.");
      }

      if (m.getAnnotation(WebRemote.class).exclude().length > 0)
      {
         constraints = Arrays.asList(m.getAnnotation(WebRemote.class).exclude());
      }

      Object[] params = convertParams(m.getGenericParameterTypes());

      try
      {
         result = m.invoke(instance, params);
      } 
      catch (InvocationTargetException e)
      {
         this.exception = e.getCause();
      }
   }

   /**
    * Convert our parameter values to an Object array of the specified target
    * types.
    * 
    * @param targetTypes
    *           Class[] An array containing the target class types.
    * @return Object[] The converted parameter values.
    */
   private Object[] convertParams(Type[] targetTypes)
         throws ConversionException
   {
      Object[] paramValues = new Object[targetTypes.length];

      for (int i = 0; i < targetTypes.length; i++)
      {
         paramValues[i] = params.get(i).convert(targetTypes[i]);
      }

      return paramValues;
   }

   /**
    * Find the best matching method within the specified class according to the
    * parameter types that were provided to the Call.
    * 
    * @param name
    *           String The name of the method.
    * @param cls
    *           Class The Class to search in.
    * @return Method The best matching method.
    */
   private Method findMethod(String name, Class<?> cls)
   {
      Map<Method, Integer> candidates = new HashMap<Method, Integer>();

      for (Method m : cls.getDeclaredMethods())
      {
         if (m.getAnnotation(WebRemote.class) == null)
            continue;

         if (name.equals(m.getName())
               && m.getParameterTypes().length == params.size())
         {
            int score = 0;

            for (int i = 0; i < m.getParameterTypes().length; i++)
            {
               ConversionScore convScore = params.get(i).conversionScore(
                     m.getParameterTypes()[i]);
               if (convScore == ConversionScore.nomatch)
                  continue;
               score += convScore.getScore();
            }
            candidates.put(m, score);
         }
      }

      Method bestMethod = null;
      int bestScore = 0;

      for (Entry<Method, Integer> entry : candidates.entrySet())
      {
         int thisScore = entry.getValue();
         if (bestMethod == null || thisScore > bestScore)
         {
            bestMethod = entry.getKey();
            bestScore = thisScore;
         }
      }

      return bestMethod;
   }
}
