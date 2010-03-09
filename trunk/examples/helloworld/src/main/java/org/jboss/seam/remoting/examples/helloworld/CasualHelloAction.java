package org.jboss.seam.remoting.examples.helloworld;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.ENGLISH;

import org.jboss.seam.remoting.annotations.WebRemote;

@Casual @Localized(ENGLISH)
public class CasualHelloAction extends HelloAction
{
   @WebRemote
   public String sayHello(String name) 
   {
     return "Hi, " + name;
   }
}
