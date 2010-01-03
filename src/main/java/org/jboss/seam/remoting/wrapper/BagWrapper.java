package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import javax.enterprise.inject.spi.BeanManager;

import org.dom4j.Element;
import org.hibernate.collection.PersistentCollection;

/**
 * Wrapper for collections, arrays, etc.
 * 
 * @author Shane Bryzak
 */
public class BagWrapper extends BaseWrapper implements Wrapper
{
   public BagWrapper(BeanManager beanManager)
   {
      super(beanManager);
   }

   private static final byte[] BAG_TAG_OPEN = "<bag>".getBytes();
   private static final byte[] BAG_TAG_CLOSE = "</bag>".getBytes();

   private static final byte[] ELEMENT_TAG_OPEN = "<element>".getBytes();
   private static final byte[] ELEMENT_TAG_CLOSE = "</element>".getBytes();
   
   private static final byte[] UNDEFINED_TAG = "<undefined/>".getBytes();
   
   private boolean loadLazy = false;
   
   public void setLoadLazy(boolean loadLazy)
   {
      this.loadLazy = loadLazy;
   }

   @SuppressWarnings("unchecked")
   public void marshal(OutputStream out) throws IOException
   {
      // Fix to prevent uninitialized lazy loading in Hibernate
      if (value instanceof PersistentCollection && !loadLazy)
      {
         if (!((PersistentCollection) value).wasInitialized())
         {
            out.write(UNDEFINED_TAG);
            return;
         }
      }

      out.write(BAG_TAG_OPEN);      
      
      Collection<Object> vals = null;

      // If the value is an array, convert it to a Collection
      if (value.getClass().isArray())
      {
         vals = new ArrayList<Object>();
         for (int i = 0; i < Array.getLength(value); i++)
         {
            vals.add(Array.get(value, i));
         }
      } 
      else if (Collection.class.isAssignableFrom(value.getClass()))
      {
         vals = (Collection) value;
      }
      else
      {
         throw new RuntimeException(String.format(
               "Can not marshal object as bag: [%s]", value));
      }

      for (Object val : vals)
      {
         out.write(ELEMENT_TAG_OPEN);
         context.createWrapperFromObject(val, path).marshal(out);
         out.write(ELEMENT_TAG_CLOSE);
      }

      out.write(BAG_TAG_CLOSE);
   }

   @SuppressWarnings("unchecked")
   public Object convert(Type type) throws ConversionException
   {
      // First convert the elements in the bag to a List of Wrappers
      List<Wrapper> vals = new ArrayList<Wrapper>();

      for (Element e : (List<Element>) element.elements("element"))
      {
         vals.add(context.createWrapperFromElement((Element) e.elements().get(0)));
      }

      if (type instanceof Class && ((Class) type).isArray())
      {
         Class arrayType = ((Class) type).getComponentType();
         value = Array.newInstance(arrayType, vals.size()); // Fix this
         for (int i = 0; i < vals.size(); i++)
         {
            Array.set(value, i, vals.get(i).convert(arrayType));
         }
      } 
      else if (type instanceof Class && 
            Collection.class.isAssignableFrom((Class) type))
      {
         try
         {
            value = getConcreteClass((Class) type).newInstance();
         } 
         catch (Exception ex)
         {
            throw new ConversionException(String.format(
                  "Could not create instance of target type [%s].", type));
         }
         
         for (Wrapper w : vals)
         {
            ((Collection) value).add(w.convert(Object.class));
         }
      } 
      else if (type instanceof ParameterizedType && 
            Collection.class.isAssignableFrom((Class) ((ParameterizedType) type).getRawType()))
      {
         Class rawType = (Class) ((ParameterizedType) type).getRawType();
         Type genType = Object.class;

         for (Type t : ((ParameterizedType) type).getActualTypeArguments())
         {
            genType = t;
            break;
         }

         try
         {
            value = getConcreteClass(rawType).newInstance();
         } 
         catch (Exception ex)
         {
            throw new ConversionException(String.format(
                  "Could not create instance of target type [%s].", rawType));
         }

         for (Wrapper w : vals)
         {
            ((Collection) value).add(w.convert(genType));
         }
      }

      return value;
   }

   private Class<?> getConcreteClass(Class<?> c)
   {
      if (c.isInterface())
      {
         // Support Set, Queue and (by default, and as a last resort) List
         if (Set.class.isAssignableFrom(c))
         {
            return HashSet.class;
         }
         else if (Queue.class.isAssignableFrom(c))
         {
            return LinkedList.class;
         }
         else
         {
            return ArrayList.class;
         }
      } 
      else
      {
         return c;
      }
   }

   /**
    * 
    * @param cls
    *           Class
    * @return ConversionScore
    */
   public ConversionScore conversionScore(Class<?> cls)
   {
      // There's no such thing as an exact match for a bag, so we'll just look
      // for a compatible match
      if (cls.isArray() || cls.equals(Object.class) || Collection.class.isAssignableFrom(cls))
      {
         return ConversionScore.compatible;
      }
      else
      {
         return ConversionScore.nomatch;
      }
   }
}
