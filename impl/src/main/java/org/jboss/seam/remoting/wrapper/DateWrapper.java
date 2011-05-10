package org.jboss.seam.remoting.wrapper;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.enterprise.inject.spi.BeanManager;

/**
 * Handles date conversions
 *
 * @author Shane Bryzak
 */
public class DateWrapper extends BaseWrapper implements Wrapper {
    public DateWrapper(BeanManager beanManager) {
        super(beanManager);
        // TODO Auto-generated constructor stub
    }

    private static final byte[] DATE_TAG_OPEN = "<date>".getBytes();
    private static final byte[] DATE_TAG_CLOSE = "</date>".getBytes();

    private static final String DATE_FORMAT = "yyyyMMddHHmmssSSS";

    private DateFormat getDateFormat() {
        return new SimpleDateFormat(DATE_FORMAT);
    }

    public void marshal(OutputStream out) throws IOException {
        out.write(DATE_TAG_OPEN);
        if (Date.class.isAssignableFrom(value.getClass())) {
            out.write(getDateFormat().format(value).getBytes());
        } else if (Calendar.class.isAssignableFrom(value.getClass())) {
            out.write(getDateFormat().format(((Calendar) value).getTime())
                    .getBytes());
        }
        out.write(DATE_TAG_CLOSE);
    }

    public Object convert(Type type) throws ConversionException {
        if ((type instanceof Class && Date.class.isAssignableFrom((Class) type))
                || type.equals(Object.class)) {
            try {
                value = getDateFormat().parse(element.getStringValue());
            } catch (ParseException ex) {
                throw new ConversionException(String.format(
                        "Date value [%s] is not in a valid format.", element
                        .getStringValue()));
            }
        } else if ((type instanceof Class && Calendar.class
                .isAssignableFrom((Class) type))) {
            try {
                Calendar cal = Calendar.getInstance();
                cal.setTime(getDateFormat().parse(element.getStringValue()));
                value = cal;
            } catch (ParseException ex) {
                throw new ConversionException(String.format(
                        "Date value [%s] is not in a valid format.", element
                        .getStringValue()));
            }
        } else {
            throw new ConversionException(String.format(
                    "Value [%s] cannot be converted to type [%s].", element
                    .getStringValue(), type));
        }

        return value;
    }

    public ConversionScore conversionScore(Class<?> cls) {
        if (Date.class.isAssignableFrom(cls)
                || Calendar.class.isAssignableFrom(cls)) {
            return ConversionScore.exact;
        } else if (cls.equals(Object.class)) {
            return ConversionScore.compatible;
        } else {
            return ConversionScore.nomatch;
        }
    }
}
