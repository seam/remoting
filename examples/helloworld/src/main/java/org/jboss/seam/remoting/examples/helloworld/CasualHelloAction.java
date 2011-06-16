package org.jboss.seam.remoting.examples.helloworld;

import org.jboss.seam.remoting.annotations.WebRemote;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.ENGLISH;

@Casual
@Localized(ENGLISH)
public class CasualHelloAction extends HelloAction {
    @WebRemote
    public String sayHello(String name) {
        return "Hi, " + name;
    }
}
