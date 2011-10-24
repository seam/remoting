package org.jboss.seam.remoting;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.seam.international.status.Message;
import org.jboss.seam.international.status.Messages;
import org.jboss.solder.core.Requires;

/**
 * This event observer writes any queued messages to the response header.  It requires the Seam International
 * module to be present.
 * 
 * @author Shane Bryzak
 *
 */
@ApplicationScoped
@Requires("org.jboss.seam.international.status.Messages")
public class MessageMarshaller {
    
    private static final byte[] MESSAGES_TAG_OPEN = "<msgs>".getBytes();
    private static final byte[] MESSAGES_TAG_CLOSE = "</msgs>".getBytes();
    private static final byte[] MESSAGE_TAG_OPEN = "<m".getBytes();
    private static final byte[] ATTRIB_LEVEL = " lvl=\"".getBytes();
    private static final byte[] ATTRIB_TARGETS = " tgt=\"".getBytes();
    private static final byte TAG_CLOSE = '>';
    private static final byte[] MESSAGE_TAG_CLOSE = "</m>".getBytes();
    private static final byte DOUBLE_QUOTE = '"';
    
    public static void marshalMessages(@Observes WriteHeaderEvent event, Messages msgs) throws IOException {
        Set<Message> messages = msgs.getAll();
        
        if (!messages.isEmpty()) {
            OutputStream out = event.getOutputStream();
            out.write(MESSAGES_TAG_OPEN);
            
            for (Message m : messages) {
                out.write(MESSAGE_TAG_OPEN);
                
                out.write(ATTRIB_LEVEL);
                out.write(m.getLevel().name().getBytes());
                out.write(DOUBLE_QUOTE);
                
                if (m.getTargets() != null) {
                    out.write(ATTRIB_TARGETS);
                    out.write(m.getTargets().getBytes());
                    out.write(DOUBLE_QUOTE);
                }
                
                out.write(TAG_CLOSE);

                out.write(URLEncoder.encode(m.getText(), "UTF-8").replace(
                        "+", "%20").getBytes());
                
                out.write(MESSAGE_TAG_CLOSE);
            }            
            
            out.write(MESSAGES_TAG_CLOSE);
        }
    }
}
