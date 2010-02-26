JavaCC grammar file for parsing annotation expressions.

JavaCC can be downloaded from https://javacc.dev.java.net/.

JTB (Java Tree Builder) can be downloaded from http://compilers.cs.ucla.edu/jtb/

Steps:

1. First run JTB on the grammar file:

  java -jar jtb132.jar -np org.jboss.seam.remoting.annotationparser.syntaxtree
-vp org.jboss.seam.remoting.annotationparser.visitor AnnotationParser.jj

This command should be run from the src/main/java/org/jboss/seam/remoting/annotationparser directory.

2. Next run javacc on jtb.out.jj.  This command should also be run in the
annotationparser directory.

Usage: /path/to/javacc jtb.out.jj
