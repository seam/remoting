package org.jboss.seam.remoting.examples.helloworld;

import static org.jboss.seam.remoting.examples.helloworld.Localized.Language.ENGLISH;

import javax.enterprise.inject.Default;

import org.jboss.seam.remoting.annotations.WebRemote;

@Default @Formal @Localized(ENGLISH)
public class HelloAction 
{
  @WebRemote
  public String sayHello(String name) 
  {
    return "Hello, " + name;
  }
}

