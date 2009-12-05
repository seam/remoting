package org.jboss.seam.remoting.examples.helloworld;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.RUSSIAN;

import org.jboss.seam.remoting.annotations.WebRemote;

@Casual @Localized(RUSSIAN)
public class CasualRussianHelloAction extends HelloAction
{
   @WebRemote
   public String sayHello(String name) 
   {
     return "Privyet, " + name;
   }
}
