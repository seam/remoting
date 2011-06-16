package org.jboss.seam.remoting;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

/**
 * @author Shane Bryzak
 */
public class AnnotationInvocationHandler implements InvocationHandler {
    private Class<? extends Annotation> annotationType;

    private Map<String, Object> memberValues;

    public AnnotationInvocationHandler(Class<? extends Annotation> annotationType,
                                       Map<String, Object> memberValues) {
        this.annotationType = annotationType;
        this.memberValues = memberValues;
    }

    public Object invoke(Object proxy, Method method, Object[] args)
            throws Throwable {
        if ("annotationType".equals(method.getName())) {
            return annotationType;
        } else if ("equals".equals(method.getName())) {
            return equals(args[0]);
        } else if ("hashCode".equals(method.getName())) {
            return hashCode();
        } else if ("toString".equals(method.getName())) {
            return toString();
        } else {
            return memberValues.get(method.getName());
        }
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append('@').append(annotationType.getName()).append('(');
        for (int i = 0; i < annotationType.getDeclaredMethods().length; i++) {
            string.append(annotationType.getDeclaredMethods()[i].getName()).append('=');
            Object value = memberValues.get(annotationType.getDeclaredMethods()[i].getName());
            if (value instanceof boolean[]) {
                appendInBraces(string, Arrays.toString((boolean[]) value));
            } else if (value instanceof byte[]) {
                appendInBraces(string, Arrays.toString((byte[]) value));
            } else if (value instanceof short[]) {
                appendInBraces(string, Arrays.toString((short[]) value));
            } else if (value instanceof int[]) {
                appendInBraces(string, Arrays.toString((int[]) value));
            } else if (value instanceof long[]) {
                appendInBraces(string, Arrays.toString((long[]) value));
            } else if (value instanceof float[]) {
                appendInBraces(string, Arrays.toString((float[]) value));
            } else if (value instanceof double[]) {
                appendInBraces(string, Arrays.toString((double[]) value));
            } else if (value instanceof char[]) {
                appendInBraces(string, Arrays.toString((char[]) value));
            } else if (value instanceof String[]) {
                String[] strings = (String[]) value;
                String[] quoted = new String[strings.length];
                for (int j = 0; j < strings.length; j++) {
                    quoted[j] = "\"" + strings[j] + "\"";
                }
                appendInBraces(string, Arrays.toString(quoted));
            } else if (value instanceof Class<?>[]) {
                Class<?>[] classes = (Class<?>[]) value;
                String[] names = new String[classes.length];
                for (int j = 0; j < classes.length; j++) {
                    names[j] = classes[j].getName() + ".class";
                }
                appendInBraces(string, Arrays.toString(names));
            } else if (value instanceof Object[]) {
                appendInBraces(string, Arrays.toString((Object[]) value));
            } else if (value instanceof String) {
                string.append('"').append(value).append('"');
            } else if (value instanceof Class<?>) {
                string.append(((Class<?>) value).getName()).append(".class");
            } else {
                string.append(value);
            }
            if (i < annotationType.getDeclaredMethods().length - 1) {
                string.append(", ");
            }
        }
        return string.append(')').toString();
    }

    private void appendInBraces(StringBuilder buf, String s) {
        buf.append('{').append(s.substring(1, s.length() - 1)).append('}');
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Annotation) {
            Annotation that = (Annotation) other;
            if (this.annotationType.equals(that.annotationType())) {
                for (Method member : annotationType.getDeclaredMethods()) {
                    Object thisValue = memberValues.get(member.getName());
                    Object thatValue = invoke(member, that);
                    if (thisValue instanceof byte[] && thatValue instanceof byte[]) {
                        if (!Arrays.equals((byte[]) thisValue, (byte[]) thatValue))
                            return false;
                    } else if (thisValue instanceof short[]
                            && thatValue instanceof short[]) {
                        if (!Arrays.equals((short[]) thisValue, (short[]) thatValue))
                            return false;
                    } else if (thisValue instanceof int[]
                            && thatValue instanceof int[]) {
                        if (!Arrays.equals((int[]) thisValue, (int[]) thatValue))
                            return false;
                    } else if (thisValue instanceof long[]
                            && thatValue instanceof long[]) {
                        if (!Arrays.equals((long[]) thisValue, (long[]) thatValue))
                            return false;
                    } else if (thisValue instanceof float[]
                            && thatValue instanceof float[]) {
                        if (!Arrays.equals((float[]) thisValue, (float[]) thatValue))
                            return false;
                    } else if (thisValue instanceof double[]
                            && thatValue instanceof double[]) {
                        if (!Arrays
                                .equals((double[]) thisValue, (double[]) thatValue))
                            return false;
                    } else if (thisValue instanceof char[]
                            && thatValue instanceof char[]) {
                        if (!Arrays.equals((char[]) thisValue, (char[]) thatValue))
                            return false;
                    } else if (thisValue instanceof boolean[]
                            && thatValue instanceof boolean[]) {
                        if (!Arrays.equals((boolean[]) thisValue,
                                (boolean[]) thatValue))
                            return false;
                    } else if (thisValue instanceof Object[]
                            && thatValue instanceof Object[]) {
                        if (!Arrays
                                .equals((Object[]) thisValue, (Object[]) thatValue))
                            return false;
                    } else {
                        if (!thisValue.equals(thatValue))
                            return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (Method member : annotationType.getDeclaredMethods()) {
            int memberNameHashCode = 127 * member.getName().hashCode();
            Object value = memberValues.get(member.getName());
            int memberValueHashCode;
            if (value instanceof boolean[]) {
                memberValueHashCode = Arrays.hashCode((boolean[]) value);
            } else if (value instanceof short[]) {
                memberValueHashCode = Arrays.hashCode((short[]) value);
            } else if (value instanceof int[]) {
                memberValueHashCode = Arrays.hashCode((int[]) value);
            } else if (value instanceof long[]) {
                memberValueHashCode = Arrays.hashCode((long[]) value);
            } else if (value instanceof float[]) {
                memberValueHashCode = Arrays.hashCode((float[]) value);
            } else if (value instanceof double[]) {
                memberValueHashCode = Arrays.hashCode((double[]) value);
            } else if (value instanceof byte[]) {
                memberValueHashCode = Arrays.hashCode((byte[]) value);
            } else if (value instanceof char[]) {
                memberValueHashCode = Arrays.hashCode((char[]) value);
            } else if (value instanceof Object[]) {
                memberValueHashCode = Arrays.hashCode((Object[]) value);
            } else {
                memberValueHashCode = value.hashCode();
            }
            hashCode += memberNameHashCode ^ memberValueHashCode;
        }
        return hashCode;
    }

    private static Object invoke(Method method, Object instance) {
        try {
            if (!method.isAccessible())
                method.setAccessible(true);
            return method.invoke(instance);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error checking value of member method "
                    + method.getName() + " on " + method.getDeclaringClass(), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error checking value of member method "
                    + method.getName() + " on " + method.getDeclaringClass(), e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Error checking value of member method "
                    + method.getName() + " on " + method.getDeclaringClass(), e);
        }
    }
}
