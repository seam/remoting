package org.jboss.seam.remoting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.BeanManager;

import org.dom4j.Element;
import org.jboss.seam.remoting.wrapper.Wrapper;
import org.jboss.seam.remoting.wrapper.WrapperFactory;

/**
 * Represents the context of an individual call.
 *
 * @author Shane Bryzak
 */
public class CallContext {
    private BeanManager beanManager;

    public CallContext(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    /**
     * Contains references to inbound bean objects identified by their ref.
     */
    private Map<String, Wrapper> inRefs = new HashMap<String, Wrapper>();

    /**
     * Contains references to outbound bean objects identified by their object.
     */
    private List<Wrapper> outRefs = new ArrayList<Wrapper>();

    /**
     * @param element Element
     * @return Wrapper
     */
    public Wrapper createWrapperFromElement(Element element) {
        if ("ref".equals(element.getQualifiedName())) {
            if (inRefs.containsKey(element.attributeValue("id"))) {
                return inRefs.get(element.attributeValue("id"));
            } else {
                Element value = (Element) element.elements().get(0);

                Wrapper w = WrapperFactory.getInstance().createWrapper(
                        value.getQualifiedName(), beanManager);
                w.setElement(value);
                w.setCallContext(this);
                inRefs.put(element.attributeValue("id"), w);
                return w;
            }
        } else {
            Wrapper w = WrapperFactory.getInstance().createWrapper(
                    element.getQualifiedName(), beanManager);
            w.setElement(element);
            w.setCallContext(this);
            return w;
        }
    }

    /**
     * @return Wrapper
     */
    public Wrapper createWrapperFromObject(Object value, String path) {
        // Not very efficient but has to be done - may implement something better
        // later
        for (Wrapper ref : outRefs) {
            if (ref.getValue().equals(value))
                return ref;
        }

        Wrapper w = WrapperFactory.getInstance().getWrapperForObject(value,
                beanManager);
        w.setCallContext(this);
        w.setPath(path);
        return w;
    }

    /**
     * Returns the inbound object references
     *
     * @return Map
     */
    public Map<String, Wrapper> getInRefs() {
        return inRefs;
    }

    /**
     * Returns the outbound object references
     *
     * @return List
     */
    public List<Wrapper> getOutRefs() {
        return outRefs;
    }

    /**
     * Add an outbound object reference
     *
     * @param w Wrapper
     */
    public void addOutRef(Wrapper w) {
        if (!outRefs.contains(w))
            outRefs.add(w);
    }
}
