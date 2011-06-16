package org.jboss.seam.remoting.examples.helloworld;

import org.jboss.seam.remoting.annotations.WebRemote;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.RUSSIAN;

@Casual
@Localized(RUSSIAN)
public class CasualRussianHelloAction extends HelloAction {
    @WebRemote
    public String sayHello(String name) {
        return "Privyet, " + name;
    }
}
