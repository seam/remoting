package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;

import javax.enterprise.inject.spi.BeanManager;

/**
 * Int wrapper class.
 *
 * @author Shane Bryzak
 */
public class NumberWrapper extends BaseWrapper implements Wrapper {
    public NumberWrapper(BeanManager beanManager) {
        super(beanManager);
    }

    private static final byte[] NUMBER_TAG_OPEN = "<number>".getBytes();
    private static final byte[] NUMBER_TAG_CLOSE = "</number>".getBytes();

    public Object convert(Type type) throws ConversionException {
        String val = element.getStringValue().trim();

        if (type.equals(Short.class)) {
            value = !"".equals(val) ? Short.valueOf(val) : null;
        } else if (type.equals(Short.TYPE)) {
            value = Short.parseShort(val);
        } else if (type.equals(Integer.class)) {
            value = !"".equals(val) ? Integer.valueOf(val) : null;
        } else if (type.equals(Integer.TYPE)) {
            value = Integer.parseInt(val);
        } else if (type.equals(Long.class) || type.equals(Object.class)) {
            value = !"".equals(val) ? Long.valueOf(val) : null;
        } else if (type.equals(Long.TYPE)) {
            value = Long.parseLong(val);
        } else if (type.equals(Float.class)) {
            value = !"".equals(val) ? Float.valueOf(val) : null;
        } else if (type.equals(Float.TYPE)) {
            value = Float.parseFloat(val);
        } else if (type.equals(Double.class)) {
            value = !"".equals(val) ? Double.valueOf(val) : null;
        } else if (type.equals(Double.TYPE)) {
            value = Double.parseDouble(val);
        } else if (type.equals(Byte.class)) {
            value = !"".equals(val) ? Byte.valueOf(val) : null;
        } else if (type.equals(Byte.TYPE)) {
            value = Byte.parseByte(val);
        } else if (type.equals(String.class)) {
            value = val;
        } else {
            throw new ConversionException(String.format(
                    "Value [%s] cannot be converted to type [%s].", element
                    .getStringValue(), type));
        }

        return value;
    }

    /**
     * @param out OutputStream
     * @throws IOException
     */
    public void marshal(OutputStream out) throws IOException {
        out.write(NUMBER_TAG_OPEN);
        out.write(value.toString().getBytes());
        out.write(NUMBER_TAG_CLOSE);
    }

    /**
     * Allow conversions to either Integer or String.
     *
     * @param cls Class
     * @return ConversionScore
     */
    public ConversionScore conversionScore(Class<?> cls) {
        if (cls.equals(Integer.class) || cls.equals(Integer.TYPE)
                || cls.equals(Long.class) || cls.equals(Long.TYPE)
                || cls.equals(Short.class) || cls.equals(Short.TYPE)
                || cls.equals(Double.class) || cls.equals(Double.TYPE)
                || cls.equals(Float.class) || cls.equals(Float.TYPE)
                || cls.equals(Byte.class) || cls.equals(Byte.TYPE)) {
            return ConversionScore.exact;
        }

        if (cls.equals(String.class) || cls.equals(Object.class)) {
            return ConversionScore.compatible;
        }

        return ConversionScore.nomatch;
    }
}
