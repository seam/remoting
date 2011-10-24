package org.jboss.seam.remoting.examples.helloworld;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

import org.jboss.seam.international.status.Messages;
import org.jboss.seam.remoting.annotations.WebRemote;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.ENGLISH;

@Default
@Formal
@Localized(ENGLISH)
public class HelloAction {
    
    @Inject Messages messages;
    
    @WebRemote
    public String sayHello(String name) {
        messages.info("Invoked HelloAction.sayHello()");
        
        return "Hello, " + name;
    }
}

