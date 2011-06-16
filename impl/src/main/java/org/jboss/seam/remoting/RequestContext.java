package org.jboss.seam.remoting;

import org.dom4j.Element;

/**
 * @author Shane Bryzak
 */
public class RequestContext {
    public RequestContext(Element header) {
        if (header != null) {
            Element context = header.element("context");
            if (context != null) {
                Element convId = context.element("conversationId");
                if (convId != null) {
                    setConversationId(convId.getText());
                }
                Element callId = context.element("callId");
                if (callId != null) {
                    setCallId(Long.valueOf(callId.getText()));
                }
            }
        }
    }

    private String conversationId;
    private Long callId;

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getCallId() {
        return callId;
    }

    public void setCallId(Long callId) {
        this.callId = callId;
    }
}
