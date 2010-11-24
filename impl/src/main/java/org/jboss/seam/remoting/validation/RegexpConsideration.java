package org.jboss.seam.remoting.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.validation.constraints.Pattern.Flag;

/**
 * some basic incompatibility between Java and JavaScript Regular Expression syntax is handled here
 * however since Java regular expression is relatively more powerful than JavaScript counterpart
 * it is developer's responsibility to make sure the provided Regular Expression is a valid
 * JS expression as well. 
 * the main features that are not supported by JavaScript regular expression engines are:
 * <ul>
 *  <li> No Unicode categories or blocks [just unicode characters]</li>
 *  <li> No Character class Union, Intersection or substraction [simply no nesting and no &&] </li>
 *  <li> No Possessive Quantifiers
 * </ul> 
 *  
 * @author Amir Sadri
 */
public class RegexpConsideration implements SpecialConsideration{

	private final static Pattern BACKSLASH_REPLACEMENT         = Pattern.compile("\\\\\\\\");
	private final static Pattern PERMANENT_START_REPLACEMENT   = Pattern.compile("\\\\A");
	private final static Pattern PERMANENT_END_REPLACEMENT     = Pattern.compile("\\\\Z");
	
	
	public HashMap<String, Object> reassessParameters(Map<String, Object> constraintParams) 
	{
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("regexp",convertRegex((String)constraintParams.get("regexp")));
		Flag[] flags = (Flag[])constraintParams.get("flags");
		int[] values = new int[flags.length];
		for(int i=0;i<values.length;i++)
			values[i] = flags[i].getValue();
		
		ArrayList<String> changedFlags = convertRegexFlags(values);
		if(changedFlags.size() > 0)
		    params.put("flags", changedFlags);

	  return params;	
   	}

	public String reassessConstraintName(String constraintName) {
	  return constraintName;
	}
	
	public final static String convertRegex(String regex)
	{
	    regex = BACKSLASH_REPLACEMENT.matcher(regex).replaceAll("\\");	
	    //////// JavaScript doesn't support \A and \Z anchors, so we just replace them with '^' and '$'
	    //////// although this strategy could easily get broken when multi-line flag is on 
	    regex = PERMANENT_START_REPLACEMENT.matcher(regex).replaceAll("^");
	    regex = PERMANENT_END_REPLACEMENT.matcher(regex).replaceAll("$");
	    ///// Matcher.matches()[which is what I am assuming we are all used to]
        ///// treats Regexps a little different than JS test() method does, so here I just try to make sure
	    ///// that we are in the same page in both client and server side
	    
	    if(regex.charAt(0) != '^')  
	        regex = "^"+regex;    
	    if(regex.charAt(regex.length()-1) != '$')
	        regex += "$"; 
                         
	  return regex;  	                
	}
	
	public final static ArrayList<String> convertRegexFlags(int[] flags)
	{
	   ArrayList<String> jsFlags = new ArrayList<String>();
	   for(int flag : flags){
		  switch(flag){
		       
		  case java.util.regex.Pattern.CASE_INSENSITIVE : jsFlags.add("i");
		                                                  break;
		  case java.util.regex.Pattern.MULTILINE        : jsFlags.add("m");
		                                                  break; 
		  
		  }
	   }
	  return jsFlags; 
	}

	
}
