package org.jboss.seam.remoting.examples.helloworld;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Retention(RUNTIME)
@Target(TYPE)
@Qualifier
public @interface Localized
{
   public static enum Language { ENGLISH, RUSSIAN }
   
   Language value() default Language.ENGLISH;
}
