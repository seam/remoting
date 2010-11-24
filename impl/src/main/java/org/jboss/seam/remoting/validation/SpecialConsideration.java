package org.jboss.seam.remoting.validation;

import java.util.HashMap;
import java.util.Map;


/**
 * just a simple layer of indirection to give the opportunity to make any changes that might
 * be required to make the server-side constraint easier to deal with in the client-side validator.     
 *  
 * @author Amir Sadri
 */
public interface SpecialConsideration {
	public HashMap<String,Object> reassessParameters(Map<String,Object> constraintParams);
	public String reassessConstraintName(String constraintName);
}
