package org.jboss.seam.remoting.examples.helloworld;

import javax.enterprise.inject.Default;

import org.jboss.seam.remoting.annotations.WebRemote;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.ENGLISH;

@Default
@Formal
@Localized(ENGLISH)
public class HelloAction {
    @WebRemote
    public String sayHello(String name) {
        return "Hello, " + name;
    }
}

