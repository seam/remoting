package org.jboss.seam.remoting.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Some utility functions to convert Java Data-Structures into JavaScript equivalent
 *
 * @author Amir Sadri
 */
public class JsConverter {

    public final static String convertArray(Object[] array) {
        if (array == null || array.length == 0)
            return "";
        StringBuilder content = new StringBuilder("[");
        for (Object temp : array) {
            content.append(convertObject(temp)).append(",");
        }
        content.deleteCharAt(content.length() - 1);
        content.append("]");

        return content.toString();
    }

    public final static String convertCollection(Collection<?> collection) {
        return convertArray(collection.toArray());
    }

    public final static String convertMap(Map<?, ?> map) {

        if (map == null || map.size() == 0)
            return "";
        StringBuilder content = new StringBuilder("{");
        Iterator<?> keys = map.keySet().iterator();
        while (keys.hasNext()) {
            final Object key = keys.next();
            content.append(key.toString()).append(" : ").append(convertObject(map.get(key))).append(",");
        }
        content.deleteCharAt(content.length() - 1);
        content.append("}");

        return content.toString();
    }


    public final static String convertObject(Object value) {
        if (value instanceof String)
            return "'" + value + "'";
        else if (value instanceof Object[])
            return convertArray((Object[]) value);
        else if (value instanceof Collection<?>)
            return convertCollection((Collection<?>) value);
        else if (value instanceof Map)
            return convertMap((Map<?, ?>) value);
        else
            return value.toString();
    }

}
