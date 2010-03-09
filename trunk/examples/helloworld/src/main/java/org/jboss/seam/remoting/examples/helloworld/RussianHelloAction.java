package org.jboss.seam.remoting.examples.helloworld;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.RUSSIAN;

import org.jboss.seam.remoting.annotations.WebRemote;

@Formal @Localized(RUSSIAN)
public class RussianHelloAction extends HelloAction
{
   @WebRemote
   public String sayHello(String name) 
   {
     return "Zdravstvuite, " + name;
   }
}
