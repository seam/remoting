//
// Generated by JTB 1.3.2
//

package org.jboss.seam.remoting.annotationparser.visitor;
import org.jboss.seam.remoting.annotationparser.syntaxtree.*;
import java.util.*;

/**
 * All void visitors must implement this interface.
 */

public interface Visitor {

   //
   // void Auto class visitors
   //

   public void visit(NodeList n);
   public void visit(NodeListOptional n);
   public void visit(NodeOptional n);
   public void visit(NodeSequence n);
   public void visit(NodeToken n);

   //
   // User-generated visitor methods below
   //

   /**
    * f0 -> [ Annotation() ( "," Annotation() )* ]
    */
   public void visit(AnnotationsUnit n);

   /**
    * f0 -> "boolean"
    *       | "char"
    *       | "byte"
    *       | "short"
    *       | "int"
    *       | "long"
    *       | "float"
    *       | "double"
    */
   public void visit(PrimitiveType n);

   /**
    * f0 -> <IDENTIFIER>
    * f1 -> ( "." <IDENTIFIER> )*
    */
   public void visit(Name n);

   /**
    * f0 -> <INTEGER_LITERAL>
    *       | <FLOATING_POINT_LITERAL>
    *       | <CHARACTER_LITERAL>
    *       | <STRING_LITERAL>
    *       | BooleanLiteral()
    *       | NullLiteral()
    */
   public void visit(Literal n);

   /**
    * f0 -> "true"
    *       | "false"
    */
   public void visit(BooleanLiteral n);

   /**
    * f0 -> "null"
    */
   public void visit(NullLiteral n);

   /**
    * f0 -> NormalAnnotation()
    *       | SingleMemberAnnotation()
    *       | MarkerAnnotation()
    */
   public void visit(Annotation n);

   /**
    * f0 -> "@"
    * f1 -> Name()
    * f2 -> "("
    * f3 -> [ MemberValuePairs() ]
    * f4 -> ")"
    */
   public void visit(NormalAnnotation n);

   /**
    * f0 -> "@"
    * f1 -> Name()
    */
   public void visit(MarkerAnnotation n);

   /**
    * f0 -> "@"
    * f1 -> Name()
    * f2 -> "("
    * f3 -> MemberValue()
    * f4 -> ")"
    */
   public void visit(SingleMemberAnnotation n);

   /**
    * f0 -> MemberValuePair()
    * f1 -> ( "," MemberValuePair() )*
    */
   public void visit(MemberValuePairs n);

   /**
    * f0 -> <IDENTIFIER>
    * f1 -> "="
    * f2 -> MemberValue()
    */
   public void visit(MemberValuePair n);

   /**
    * f0 -> Annotation()
    *       | MemberValueArrayInitializer()
    *       | Literal()
    *       | ClassOrInterfaceType()
    */
   public void visit(MemberValue n);

   /**
    * f0 -> <IDENTIFIER>
    * f1 -> ( "." <IDENTIFIER> )*
    * f2 -> [ "." "class" ]
    */
   public void visit(ClassOrInterfaceType n);

   /**
    * f0 -> "{"
    * f1 -> MemberValue()
    * f2 -> ( "," MemberValue() )*
    * f3 -> [ "," ]
    * f4 -> "}"
    */
   public void visit(MemberValueArrayInitializer n);

}

