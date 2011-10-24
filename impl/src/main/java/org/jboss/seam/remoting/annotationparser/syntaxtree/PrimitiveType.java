//
// Generated by JTB 1.3.2
//

package org.jboss.seam.remoting.annotationparser.syntaxtree;

/**
 * Grammar production:
 * f0 -> "boolean"
 *       | "char"
 *       | "byte"
 *       | "short"
 *       | "int"
 *       | "long"
 *       | "float"
 *       | "double"
 */
public class PrimitiveType implements Node {
   public NodeChoice f0;

   public PrimitiveType(NodeChoice n0) {
      f0 = n0;
   }

   public void accept(org.jboss.seam.remoting.annotationparser.visitor.Visitor v) {
      v.visit(this);
   }
   public <R,A> R accept(org.jboss.seam.remoting.annotationparser.visitor.GJVisitor<R,A> v, A argu) {
      return v.visit(this,argu);
   }
   public <R> R accept(org.jboss.seam.remoting.annotationparser.visitor.GJNoArguVisitor<R> v) {
      return v.visit(this);
   }
   public <A> void accept(org.jboss.seam.remoting.annotationparser.visitor.GJVoidVisitor<A> v, A argu) {
      v.visit(this,argu);
   }
}

