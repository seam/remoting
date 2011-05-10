package org.jboss.seam.remoting;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.seam.remoting.annotationparser.AnnotationParser;
import org.jboss.seam.remoting.annotationparser.ParseException;
import org.jboss.seam.remoting.annotationparser.syntaxtree.AnnotationsUnit;
import org.jboss.seam.remoting.annotationparser.syntaxtree.BooleanLiteral;
import org.jboss.seam.remoting.annotationparser.syntaxtree.ClassOrInterfaceType;
import org.jboss.seam.remoting.annotationparser.syntaxtree.Literal;
import org.jboss.seam.remoting.annotationparser.syntaxtree.MarkerAnnotation;
import org.jboss.seam.remoting.annotationparser.syntaxtree.MemberValue;
import org.jboss.seam.remoting.annotationparser.syntaxtree.MemberValuePair;
import org.jboss.seam.remoting.annotationparser.syntaxtree.MemberValuePairs;
import org.jboss.seam.remoting.annotationparser.syntaxtree.Name;
import org.jboss.seam.remoting.annotationparser.syntaxtree.Node;
import org.jboss.seam.remoting.annotationparser.syntaxtree.NodeListOptional;
import org.jboss.seam.remoting.annotationparser.syntaxtree.NodeOptional;
import org.jboss.seam.remoting.annotationparser.syntaxtree.NodeSequence;
import org.jboss.seam.remoting.annotationparser.syntaxtree.NodeToken;
import org.jboss.seam.remoting.annotationparser.syntaxtree.NormalAnnotation;
import org.jboss.seam.remoting.annotationparser.syntaxtree.SingleMemberAnnotation;
import org.jboss.seam.remoting.annotationparser.visitor.DepthFirstVisitor;

/**
 * Parses a comma-separated list of annotation expressions and produces an
 * array of Annotation instances.
 *
 * @author Shane Bryzak
 */
public class AnnotationsParser extends DepthFirstVisitor {
    protected class AnnotationMetadata {
        private Class<? extends Annotation> annotationType;
        private Map<String, Object> memberValues = new HashMap<String, Object>();

        public AnnotationMetadata(String name) {
            this.annotationType = determineAnnotationType(name, beanType);
        }

        public void addMemberValue(String name, Object value) {
            memberValues.put(name, value);
        }

        public Map<String, Object> getMemberValues() {
            return memberValues;
        }

        public Class<? extends Annotation> getAnnotationType() {
            return annotationType;
        }
    }

    @SuppressWarnings("all")
    private class AnyQualifier extends AnnotationLiteral<Any> implements Any {
    }

    ;

    private Class<?> beanType;
    private BeanManager beanManager;
    private List<AnnotationMetadata> meta = new ArrayList<AnnotationMetadata>();

    private Annotation[] annotations;

    public AnnotationsParser(Class<?> beanType, String declaration, BeanManager beanManager) {
        this.beanType = beanType;
        this.beanManager = beanManager;

        // TODO cache the results somewhere

        AnnotationParser parser = new AnnotationParser(new StringReader(declaration));

        try {
            Node root = parser.AnnotationsUnit();
            root.accept(this);
        } catch (ParseException e) {
            throw new IllegalArgumentException(
                    "Error while parsing annotation declaration: " + declaration, e);
        }

        annotations = new Annotation[meta.size()];

        for (int i = 0; i < meta.size(); i++) {
            AnnotationMetadata ann = meta.get(i);
            InvocationHandler handler = new AnnotationInvocationHandler(
                    (Class<? extends Annotation>) ann.getAnnotationType(), ann.getMemberValues());
            annotations[i] = (Annotation) Proxy.newProxyInstance(
                    ann.getAnnotationType().getClassLoader(),
                    new Class[]{ann.getAnnotationType()}, handler);
        }

        meta = null;
    }

    public Annotation[] getAnnotations() {
        return annotations;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends Annotation> determineAnnotationType(String name, Class<?> beanType) {
        try {
            return (Class<? extends Annotation>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            // Iterate through the annotations on the bean type and look for a simple name match
            for (Annotation beanAnnotation : beanType.getAnnotations()) {
                if (name.equals(beanAnnotation.annotationType().getSimpleName())) {
                    return beanAnnotation.annotationType();
                }
            }

            // Couldn't find the annotation on the bean type itself - let's look at all beans
            // with the same type
            Set<Bean<?>> beans = beanManager.getBeans(beanType, new AnyQualifier());
            for (Bean<?> bean : beans) {
                for (Annotation beanAnnotation : bean.getBeanClass().getAnnotations()) {
                    if (name.equals(beanAnnotation.annotationType().getSimpleName())) {
                        return beanAnnotation.annotationType();
                    }
                }
            }

            if ("Default".equals(name)) {
                return Default.class;
            } else if ("Any".equals(name)) {
                return Any.class;
            }

            return null;
        }
    }

    @Override
    public void visit(AnnotationsUnit node) {
        List<org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation> annotations =
                new ArrayList<org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation>();

        // TODO messy! turn this into a recursive function

        NodeOptional n = (NodeOptional) node.f0;
        if (n.present()) {
            if (n.node instanceof NodeSequence) {
                NodeSequence ns = (NodeSequence) n.node;
                {
                    for (Node nsNode : ns.nodes) {
                        if (nsNode instanceof org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation) {
                            annotations.add((org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation) nsNode);
                        } else if (nsNode instanceof NodeListOptional) {
                            NodeListOptional nlo = (NodeListOptional) nsNode;
                            if (nlo.present()) {
                                for (Node nloNode : nlo.nodes) {
                                    if (nloNode instanceof NodeSequence) {
                                        for (Node cn : ((NodeSequence) nloNode).nodes) {
                                            if (cn instanceof org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation) {
                                                annotations.add((org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation) cn);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        for (org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation a : annotations) {
            processAnnotation(a);
        }

        super.visit(node);
    }

    private void processAnnotation(org.jboss.seam.remoting.annotationparser.syntaxtree.Annotation node) {
        if (node.f0.choice instanceof MarkerAnnotation) {
            meta.add(new AnnotationMetadata(extractName(((MarkerAnnotation) node.f0.choice).f1)));
        } else if (node.f0.choice instanceof NormalAnnotation) {
            NormalAnnotation ann = (NormalAnnotation) node.f0.choice;
            AnnotationMetadata metadata = new AnnotationMetadata(extractName(ann.f1));

            if (ann.f3.present() && ann.f3.node instanceof MemberValuePairs) {
                MemberValuePairs mvp = (MemberValuePairs) ann.f3.node;
                extractMemberValue(metadata, mvp.f0.f0.tokenImage, mvp.f0.f2);

                if (mvp.f1.present()) {
                    for (Node n : mvp.f1.nodes) {
                        if (n instanceof NodeSequence) {
                            for (Node nsn : ((NodeSequence) n).nodes) {
                                if (nsn instanceof MemberValuePair) {
                                    MemberValuePair p = (MemberValuePair) nsn;
                                    extractMemberValue(metadata, p.f0.tokenImage, p.f2);
                                }
                            }
                        }
                    }
                }
            }
            meta.add(metadata);
        } else if (node.f0.choice instanceof SingleMemberAnnotation) {
            AnnotationMetadata metadata = new AnnotationMetadata(
                    extractName(((SingleMemberAnnotation) node.f0.choice).f1));
            extractMemberValue(metadata, "value", ((SingleMemberAnnotation) node.f0.choice).f3);
            meta.add(metadata);
        }

    }

    private void extractMemberValue(AnnotationMetadata metadata, String memberName,
                                    MemberValue memberValue) {
        Class<?> memberType = null;

        for (Method m : metadata.getAnnotationType().getMethods()) {
            if (memberName.equals(m.getName())) {
                memberType = m.getReturnType();
                break;
            }
        }

        if (memberType == null) {
            throw new RuntimeException("Annotation member " + memberName +
                    " not found on annotation type " + metadata.getAnnotationType().getName());
        }

        Object value = null;

        switch (memberValue.f0.which) {
            // TODO add the missing conversions
            case 0: // Annotation
                break;
            case 1: // MemberValueArray
                // not supported - array member values are non-binding
                break;
            case 2: // Literal
                value = convertLiteral((Literal) memberValue.f0.choice);
                break;
            case 3: // ClassOrInterfaceType
                value = convertClassOrInterfaceType(
                        (ClassOrInterfaceType) memberValue.f0.choice, memberType);
                break;
        }

        metadata.addMemberValue(memberName, value);
    }

    private Object convertLiteral(Literal literal) {
        switch (literal.f0.which) {
            case 0: // <INTEGER_LITERAL>
                return Integer.parseInt(((NodeToken) literal.f0.choice).tokenImage);
            case 1: // <FLOATING_POINT_LITERAL>
                return Float.parseFloat(((NodeToken) literal.f0.choice).tokenImage);
            case 2: // <CHARACTER_LITERAL>
                return ((NodeToken) literal.f0.choice).tokenImage.charAt(1); // ignore the single quotes
            case 3: // <STRING_LITERAL>
                String stringVal = ((NodeToken) literal.f0.choice).tokenImage;
                return stringVal.substring(1, stringVal.length() - 1); // strip the double quotes
            case 4: // BooleanLiteral()
                return "true".equals(((NodeToken) ((BooleanLiteral) literal.f0.choice).f0.choice).tokenImage);
            case 5: // NullLiteral()
                return null;
        }
        return null;
    }

    private Object convertClassOrInterfaceType(ClassOrInterfaceType node, Class<?> memberType) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.f0.tokenImage);

        if (node.f1.present()) {
            for (Node n : node.f1.nodes) {
                if (n instanceof NodeSequence) {
                    for (Node nsn : ((NodeSequence) n).nodes) {
                        if (nsn instanceof NodeToken) {
                            sb.append(((NodeToken) nsn).tokenImage);
                        }
                    }
                }
            }
        }

        String className = sb.toString();

        if (memberType.isEnum()) {
            for (Object e : memberType.getEnumConstants()) {
                if (className.equals(((Enum<?>) e).name())) return e;
            }

            throw new IllegalArgumentException(
                    "Invalid enum specified for annotation member value: " + className);
        } else {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException e) {
                if (!className.startsWith("java.lang.")) {
                    // try finding the class in the java.lang package
                    try {
                        return Class.forName("java.lang." + className);
                    } catch (ClassNotFoundException e1) {
                    }
                }

                throw new IllegalArgumentException(
                        "Invalid class name specified for annotation member value: " + className);
            }
        }
    }

    private String extractName(Name name) {
        StringBuilder sb = new StringBuilder();

        sb.append(name.f0.tokenImage);

        NodeListOptional nodeList = ((NodeListOptional) name.f1);
        if (nodeList.present()) {
            for (Node node : nodeList.nodes) {
                if (node instanceof NodeSequence) {
                    for (Node n : ((NodeSequence) node).nodes) {
                        if (n instanceof NodeToken) {
                            sb.append(((NodeToken) n).tokenImage);
                        }
                    }
                }
            }
        }

        return sb.toString();
    }
}
