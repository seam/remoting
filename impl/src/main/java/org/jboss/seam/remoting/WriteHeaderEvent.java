package org.jboss.seam.remoting;

import java.io.OutputStream;

/**
 * An event that is fired when the response header is being written. 
 * 
 * @author Shane Bryzak
 *
 */
public class WriteHeaderEvent {
    private OutputStream out;
    
    public WriteHeaderEvent(OutputStream out) {
        this.out = out;
    }
    
    public OutputStream getOutputStream() {
        return out;
    }
}
