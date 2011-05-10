package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

/**
 * String wrapper class.
 *
 * @author Shane Bryzak
 */
public class StringWrapper extends BaseWrapper implements Wrapper {
    public StringWrapper(BeanManager beanManager) {
        super(beanManager);
    }

    private interface StringConverter {
        Object convert(String value);
    }

    private static final Map<Class, StringConverter> converters = new HashMap<Class, StringConverter>();

    static {
        converters.put(String.class, new StringConverter() {
            public Object convert(String value) {
                return value;
            }
        });
        converters.put(Object.class, new StringConverter() {
            public Object convert(String value) {
                return value;
            }
        });
        converters.put(StringBuilder.class, new StringConverter() {
            public Object convert(String value) {
                return new StringBuilder(value);
            }
        });
        converters.put(StringBuffer.class, new StringConverter() {
            public Object convert(String value) {
                return new StringBuffer(value);
            }
        });
        converters.put(Integer.class, new StringConverter() {
            public Object convert(String value) {
                return Integer.valueOf(value);
            }
        });
        converters.put(Integer.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Integer.parseInt(value);
            }
        });
        converters.put(Long.class, new StringConverter() {
            public Object convert(String value) {
                return Long.valueOf(value);
            }
        });
        converters.put(Long.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Long.parseLong(value);
            }
        });
        converters.put(Short.class, new StringConverter() {
            public Object convert(String value) {
                return Short.valueOf(value);
            }
        });
        converters.put(Short.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Short.parseShort(value);
            }
        });
        converters.put(Boolean.class, new StringConverter() {
            public Object convert(String value) {
                return Boolean.valueOf(value);
            }
        });
        converters.put(Boolean.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Boolean.parseBoolean(value);
            }
        });
        converters.put(Double.class, new StringConverter() {
            public Object convert(String value) {
                return Double.valueOf(value);
            }
        });
        converters.put(Double.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Double.parseDouble(value);
            }
        });
        converters.put(Float.class, new StringConverter() {
            public Object convert(String value) {
                return Float.valueOf(value);
            }
        });
        converters.put(Float.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Float.parseFloat(value);
            }
        });
        converters.put(Character.class, new StringConverter() {
            public Object convert(String value) {
                return Character.valueOf(value.charAt(0));
            }
        });
        converters.put(Character.TYPE, new StringConverter() {
            public Object convert(String value) {
                return value.charAt(0);
            }
        });
        converters.put(Byte.class, new StringConverter() {
            public Object convert(String value) {
                return Byte.valueOf(value);
            }
        });
        converters.put(Byte.TYPE, new StringConverter() {
            public Object convert(String value) {
                return Byte.parseByte(value);
            }
        });
        converters.put(BigInteger.class, new StringConverter() {
            public Object convert(String value) {
                return new BigInteger(value);
            }
        });
        converters.put(BigDecimal.class, new StringConverter() {
            public Object convert(String value) {
                return new BigDecimal(value);
            }
        });
    }

    public static final String DEFAULT_ENCODING = "UTF-8";

    private static final byte[] STRING_TAG_OPEN = "<str>".getBytes();
    private static final byte[] STRING_TAG_CLOSE = "</str>".getBytes();

    private static final Class[] COMPATIBLE_CLASSES = {Integer.class,
            Integer.TYPE, Long.class, Long.TYPE, Short.class, Short.TYPE,
            Boolean.class, Boolean.TYPE, Double.class, Double.TYPE, Float.class,
            Float.TYPE, Character.class, Character.TYPE, Byte.class, Byte.TYPE,
            BigInteger.class, BigDecimal.class, Object.class};

    public Object convert(Type type) throws ConversionException {
        String elementValue = null;
        try {
            elementValue = URLDecoder.decode(element.getStringValue(),
                    DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException ex) {
            throw new ConversionException(
                    "Error converting value - encoding not supported.");
        }

        try {
            if (converters.containsKey(type))
                value = converters.get(type).convert(elementValue);
            else if (type instanceof Class && ((Class) type).isEnum())
                value = Enum.valueOf((Class) type, elementValue);
            else
                // Should never reach this line - calcConverstionScore should
                // guarantee this.
                throw new ConversionException(String.format(
                        "Value [%s] cannot be converted to type [%s].", elementValue,
                        type));

            return value;
        } catch (Exception ex) {
            if (ex instanceof ConversionException)
                throw (ConversionException) ex;
            else
                throw new ConversionException(String.format(
                        "Could not convert value [%s] to type [%s].", elementValue,
                        type.toString()), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public ConversionScore conversionScore(Class<?> cls) {
        if (cls.equals(String.class) || StringBuffer.class.isAssignableFrom(cls))
            return ConversionScore.exact;

        for (Class<?> c : COMPATIBLE_CLASSES) {
            if (cls.equals(c))
                return ConversionScore.compatible;
        }

        if (cls.isEnum()) {
            try {
                String elementValue = URLDecoder.decode(element.getStringValue(),
                        DEFAULT_ENCODING);
                Enum.valueOf((Class<? extends Enum>) cls, elementValue);
                return ConversionScore.compatible;
            } catch (IllegalArgumentException ex) {
            } catch (UnsupportedEncodingException ex) {
            }
        }

        return ConversionScore.nomatch;
    }

    public void marshal(OutputStream out) throws IOException {
        out.write(STRING_TAG_OPEN);
        out.write(URLEncoder.encode(value.toString(), DEFAULT_ENCODING).replace(
                "+", "%20").getBytes());
        out.write(STRING_TAG_CLOSE);
    }
}
