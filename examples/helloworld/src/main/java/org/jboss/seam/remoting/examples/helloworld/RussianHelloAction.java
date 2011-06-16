package org.jboss.seam.remoting.examples.helloworld;

import org.jboss.seam.remoting.annotations.WebRemote;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.RUSSIAN;

@Formal
@Localized(RUSSIAN)
public class RussianHelloAction extends HelloAction {
    @WebRemote
    public String sayHello(String name) {
        return "Zdravstvuite, " + name;
    }
}
