/*JSR 303 integration with Seam-Remoting Module 
 *@author Amir Sadri 
 */
Seam.createValidationRequest = function() {
  var beans = Seam.__validationBeans;	
  var b = "<beans>";
  for(var i=0;i<beans.length;i++){
	var metaData = Seam.getBeanType(beans[i]);  
	b += "<bean target=\"" + metaData.__name + "\"";
	var qualifiers = metaData.__qualifiers;
	if(qualifiers != undefined && qualifiers != null){
      var q = qualifiers[0];  
	  for(var j=1;j<qualifiers.length;j++){
		 q += ","+qualifiers[j];	 
	  }
	 b += " qualifiers=\""+q+"\" />";
	}
	else{
	  b += " />"   
	} 
  }
  b += "</beans>";
  if(Seam.__validationMessages != undefined){
	b += "<messages>";
	var keys = Seam.__validationMessages.keySet();
	for(var i=0;i<keys.length;i++)
	  b+= keys[i] + (i == keys.length-1 ? "" : ",,");	
	b += "</messages>"
  } 
  else{
	Seam.__validationMessages = new Seam.Map();  
  }   
 return b; 
};
/*
 * this method parses the response from server and attaches all validation metadata to their corresponding 
 * client-side beans.
 */
Seam.processMetaData = function(doc , props , vgroups){
  var cn = Seam.Xml.childNode;
  var cns = Seam.Xml.childNodes;
  if (typeof(Seam) == "undefined") 
	  return;
  if (!doc.documentElement) 
	  return;
  
  var violations = []; //////for storing any possible ConstraintViolation
  var validationNode = cn(cn(doc.documentElement, "body"),"validation");
  
  var messagesNode = (cn(validationNode, "messages"));
  var messages = cns(messagesNode , "m");
  for(var i=0;i<messages.length;i++){
	Seam.__validationMessages.put(messages[i].getAttribute("c") , messages[i].getAttribute("msg"));	
   }
  
  var beans = cns(validationNode , "b");
  for(var i=0;i<beans.length;i++){
	var beanMetaData = Seam.getBean(beans[i].getAttribute("n"));
	if(beanMetaData == undefined)
	  continue;
	beanMetaData.__constraints	= new Seam.Map();
	var properties = cns(beans[i] , "p");	
	for(var j=0;j<properties.length;j++){
	   var propName = properties[j].getAttribute("n");	
       var constraints = cns(properties[j] , "c");
       if(constraints.length > 0){
         var cc = [];	 
         for(var k=0;k<constraints.length;k++){
           var constraint = function() {};
           constraint.name = constraints[k].getAttribute("n");
           var parent = constraints[k].getAttribute("parent");
           if(parent != undefined)    ////////////if it is an composed constraint
        	 constraint.parent = parent;  
           var params = cns(constraints[k] , "pm");
           if(params.length > 0){
        	 constraint.params = [];
        	 for(y=0;y<params.length;y++){
        		var param = function() {};
        		param.name  = params[y].getAttribute("n");
        		param.value = params[y].getAttribute("v");
        		constraint.params.push(param);
        	  }  
         	}
            var groups = cns(constraints[k] , "g");
            if(groups.length > 0){
              constraint.groups = [];
              for(y=0;y<groups.length;y++){
          		constraint.groups.push(groups[y].getAttribute("id"));
          	  }  
            }
        	cc.push(constraint);           
           }
         beanMetaData.__constraints.put(propName , cc);
        }   
	  }
	 var ghs = cns(beans[i] , "gh");
	 if(ghs.length > 0){
	   beanMetaData.__groupHierarchy = []; 		 
	 for(var k=0;k<ghs.length;k++){	 
	   var groupHierarchy = function() {}; 
	   groupHierarchy.name = ghs[k].getAttribute("n"); 
	   groupHierarchy.id   = ghs[k].getAttribute("id");
	   var parents = cns(ghs[k] , "gp");
	   if(parents.length > 0){
	     groupHierarchy.parents = [];	 
	     for(var y=0;y<parents.length;y++){
	       var pname = parents[y].getAttribute("n");
	       var parent = function() {};
	       if(pname != undefined)
	        parent.name = pname;
	       else
	    	parent.id = parents[y].getAttribute("id");
	      groupHierarchy.parents.push(parent);  
	     }
	   }
	  beanMetaData.__groupHierarchy.push(groupHierarchy);
	 }
	 }
   }
  Seam.executeValidation(props , vgroups);
};

Seam.validateProperty = function(bean , property , callBack , groups){
	var p = [];
	p.push(property);
	Seam.validateProperties(bean ,p ,callBack , groups);
};

Seam.validateProperties = function(bean , properties ,callBack , groups){
	Seam.validateBean(bean, callBack ,groups, properties);
};

Seam.validateBean = function(bean , callBack , groups, properties){
	var beans = [];
	beans.push(bean);
	Seam.validateBeans(beans , callBack , groups , properties );
};

Seam.validateBeans = function(beans , callBack , groups, properties){
  var beanList = [];	
  var flag = false;
  for(var i=0;i<beans.length;i++){	
    var beanMetaData = Seam.getBeanType(beans[i]);
    if(beanMetaData == undefined)
      continue;
    if(beanMetaData.__constraints == undefined) 
      flag = true;
       
    beanList.push(beans[i]);
  }	                        
   if(beanList.length > 0) {
	Seam.__validationCallback = callBack;   
	Seam.__validationBeans    = beanList;
    ///// There is at least one bean for which we need to fetch its metadata from server and then
    ///// we will do it for all other beans as well, even if they have got it already...
    if(flag) { 
      var data = Seam.createValidationRequest();
      var envelope = Seam.createEnvelope("", data);
      var processorProcedure = function(doc) { Seam.processMetaData(doc , properties , groups); };  
      Seam.sendAjaxRequest(envelope, Seam.PATH_VALIDATION, processorProcedure , false);
    }
    else{	
      Seam.executeValidation(properties , groups);	   
    }
  }
  else{
	 Seam.__validationCallback = undefined;   
     Seam.__validationBeans    = undefined;   
  }
};

/* it's quite common to validate an object, show validation errors and then 
 * do the same validation again after rectifying errors, this function makes things a little bit easier and faster
 */
Seam.repeatValidation = function(properties){
  if(Seam.__validationBeans == undefined || Seam.__validationCallback == undefined)
	Seam.log("There is no previous validation state available!! [use validateBean()]");
  else
    Seam.executeValidation(properties);		
};

/* this method walks through all constraints and call validateAgainstConstraint method for each one of them.
 */
Seam.executeValidation = function(properties , groups) {            
   var violations = [];	
   for(var i=0;i<Seam.__validationBeans.length;i++){ 
	  var metadata = Seam.getBeanType(Seam.__validationBeans[i]); 
	  var constraints = metadata.__constraints;         ////see if we have to validate the whole bean or not...
	  var keys = !properties ? constraints.keySet() : properties;
	   for(var j=0;j<keys.length;j++){
		 var cc = constraints.get(keys[j]);
		  for(var k=0;k<cc.length;k++){
			var flag = false;  
			if(!groups)
			  groups = Seam.ValidationGroups;
			if(groups == undefined)
			  groups = ["Default"]; ////if no groups provided, will be validated against Default Group
			else if(typeof groups == "string")
			   groups = [groups];
			var cgroups = cc[k].groups;
			if(cc[k].parent != undefined){
			  cgroups = Seam.findElement(cc[k].parent , cc).groups;  
			}
			if(cgroups == undefined){  /////Default Group
			  for(var y=0;y<groups.length;y++){
				 if(Seam.groupExistInHierarchy(metadata , groups[y] , "Default")){
				   flag = true;
				   break;
				 }	  
			   }
			}
			else{
			  for(var y=0;y<groups.length;y++){
			   for(var x=0;x<cgroups.length;x++){
				 var groupName = Seam.getGroupName(metadata , cgroups[x]);
				 if(Seam.groupExistInHierarchy(metadata , groups[y] , groupName)){
				   flag = true;
				   break;
				 }	  
				}
			    if(flag) break;
			   }	   
			 }
			if(flag){
			  var result = Seam.validateAgainstConstraint(Seam.__validationBeans[i] , keys[j] , cc[k]);  
			  if(result != -1)
			    violations.push(result);
		    } 
		  } 
	   }
    }
  Seam.__validationCallback(violations);  
};

Seam.findElement = function(element , array){
	for(var i=0;i<array.length;i++)
	  if(array[i].name == element)
		return array[i];		
  return null; 
};

Seam.getGroupName = function(bean , id){
   if(bean.__groupHierarchy != undefined){
	 for(var i=0;i<bean.__groupHierarchy.length;i++)
		if(bean.__groupHierarchy[i].id == id)
	      return bean.__groupHierarchy[i].name;		
   }
  return null; 
};

Seam.groupExistInHierarchy = function(bean , childNode , name){
	if(name == childNode)
	  return true;	
	if(bean.__groupHierarchy != undefined){
	  for(var i=0;i<bean.__groupHierarchy.length;i++){
		   if(bean.__groupHierarchy[i].name == childNode){
		     var parents = bean.__groupHierarchy[i].parents;
		     if(parents != undefined){
		       for(var i=0;i<parents.length;i++){
		    	  var parent = parents[i];
		    	  var pname = !parent.name ? Seam.getGroupName(bean,parent.id) : parent.name;
		    	  if(Seam.groupExistInHierarchy(bean , pname , name))
		    		return true;  
		       } 
		     }
		     
		   }
		}
	 }
  return false; 	
};
/*
 * this method allows users to complete the group hierarchy when information that has been
 * extracted from bean will not be enough to validate the bean against a particular group.
 */
Seam.addSubGroup = function(bean , subGroupName , superGroupName){
	metaData = Seam.getBeanType(bean);
	if(metadata.__groupHierarchy != undefined){
	  var id = -1;	
	  for(var group in metadata.__groupHierarchy)
		if(group.name == superGroupName){
		 id = group.id;
		 break;
		}
	  if(id == -1)
		return false;  
	  hierarchyNode = function() {};
	  hierarchyNode.name = subGroupName;
	  hierarchyNode.id   = 50 + id;  /////I can't imagine a situation where there are more than 50 groups...
	                                ///// and well, if it occurs we are in trouble ;) ,since the whole algorithm sucks for anything more than a dozen groups
	  hierarchyNode.parents = [function(){}.id = id];
	  metadata.__groupHierarchy.push(hierarchyNode);
	 return true;	
	}
  return false;	
};

/* this method finds the appropriate validator and pass the current property's value
 * if validation fails, an instance of ConstraintViolation is created which contains
 * all required information about validation failure along with the validation message
 */
Seam.validateAgainstConstraint = function(bean , property , constraint) {	
	var validator = Validation.getValidator(constraint.name);
	if(validator == null)
	  Seam.log("Validator for the requested constraint is not available. ["+constraint.name+"]");
	else {	                    //////////////////////get all properties if there is any	
	  if(validator.properties != null){
		if(constraint.params != undefined){	
		  var properties = [];	
		  for(var i=0;i<constraint.params.length;i++){
			for(var j=0;j<validator.properties.length;j++){
			   if(constraint.params[i].name.toLowerCase() == validator.properties[j].toLowerCase()){
				   validator[validator.properties[j]] = constraint.params[i].value; ////////injecting all attributes so they will be available inside validator
			       properties.push(constraint.params[i]);
			       break;
			    }   
			  }	   
			}   
		  }
		  else{ ////make sure old values will not be used
			for(var i=0;i<validator.properties.length;i++)
		     validator[validator.properties[i]] = undefined;  
		  } 
		}
      
	    validator.currentValue = bean[property];
	    if(validator.currentValue == "")
	    	validator.currentValue = null;	 
	    var valid = validator.isValid();
	    if(!valid){
	      var message = Seam.getValidationMessage(constraint , properties);
	      if(message.indexOf("?value?") != -1)
	    	message = message.replace("?value?" , validator.currentValue);   
		  return new ConstraintViolation(bean , 
				                         property , 
				                         validator.currentValue ,
				                         message);
	    }	
	  }
  return -1;  ////no Violation	
};

Seam.getValidationMessage = function(constraint , attrs){
	var name = constraint.name;
	var keys = Seam.__validationMessages.keySet();
	if(constraint.parent != undefined)
	  name = constraint.parent; ////if it is an composed constraint, its parent's message will be used instead	
	else if(attrs != null && attrs.length > 0){
      name = name + "_["+attrs[0].name+":"+attrs[0].value;
	  for(var i=1;i<attrs.length;i++)
		name += ","+attrs[i].name+":"+attrs[i].value; 
	  name += "]"; 		
	}	
	for(var i=0;i<keys.length;i++){
	  if(name == keys[i])
	    return Seam.__validationMessages.get(keys[i]); 
	}
  return "Validation has faild. [@"+name+"]";
};
////////ConstraintViolation
function ConstraintViolation(bean , propertyName , currentValue , message)
{
  this.bean     = bean;
  this.property = propertyName;
  this.value    = currentValue;
  this.message  = message;
};
///////////////Validator
function Validator(constName , props , validateLogic)
{
  this.constraintName = constName.toLowerCase();
  this.isValid        = validateLogic; 
  this.properties     = props;         
};

var Validation = {
  validators : []		
};

Validation.getValidator = function(constraintName) {
	for(var i=0;i<this.validators.length;i++)
	  if(this.validators[i].constraintName == constraintName.toLowerCase())
		  return this.validators[i];
  return null;	
};

Validation.registerValidator = function(validator) {
	if(this.getValidator(validator.constraintName) == null)
	  this.validators.push(validator);
};

/////////////Validators for JSR-303 Built-in Constraints
Validation.registerValidator(new Validator("Null" , null , function() {
	return this.currentValue == null;   /////////currentValue will be injected to each Validator object during validation
}) ); 

Validation.registerValidator(new Validator("NotNull" , null , function() {
	return this.currentValue != null;
}) );

Validation.registerValidator(new Validator("AssertTrue" , null , function() {
	return this.currentValue == null || this.currentValue == true; 
}) );

Validation.registerValidator(new Validator("AssertFalse" , null , function() {
	return this.currentValue == null || this.currentValue == false; 
}) );

Validation.registerValidator(new Validator("Min" , ["value"] , function() {
	if(this.currentValue == null) return true;
	this.currentValue = typeof(this.currentValue) == "string" ? parseFloat(this.currentValue) : this.currentValue;
	return !isNaN(this.currentValue) && this.currentValue >= this.value; 
}) );

Validation.registerValidator(new Validator("Max" , ["value"] , function() {
	if(this.currentValue == null) return true;
	this.currentValue = typeof(this.currentValue) == "string" ? parseFloat(this.currentValue) : this.currentValue;
	return !isNaN(this.currentValue) && this.currentValue <= this.value; 
}) );

Validation.registerValidator(new Validator("DecimalMin" , ["value"] , function() {
	return this.currentValue == null || this.currentValue >= this.value; 
}) );

Validation.registerValidator(new Validator("DecimalMax" , ["value"] , function() {
	return this.currentValue == null || (this.currentValue == null || this.currentValue <= this.value); 
}) );

Validation.registerValidator(new Validator("Size" , ["min" , "max"] , function() {
	return  this.currentValue == null || (this.min <= this.currentValue.length && this.currentValue.length <= this.max); 
}) );

Validation.registerValidator(new Validator("Digits" , ["integer" , "fraction"] , function() {
	if(this.currentValue == null) return true;
	this.currentValue = String(this.currentValue);
	var parts = this.currentValue.split(".");
	var left  = parts[0];
	var right = parts[1];
	if(parts.length > 2) return false;
	return !isNaN(parseFloat(this.currentValue)) && (left.length <= this.integer) && (right.length <= this.fraction); 
}) );

Validation.registerValidator(new Validator("Past" , null , function() {
	if(this.currentValue == null) return true;
	if(this.currentValue["getTime"] == undefined) this.currentValue = new Date(this.currentValue); 
	return this.currentValue < new Date(); 
}) );

Validation.registerValidator(new Validator("Future" , null , function() {
	if(this.currentValue == null) return true;
	if(this.currentValue["getTime"] == undefined) this.currentValue = new Date(this.currentValue); 
	return this.currentValue > new Date(); 
}) );

Validation.registerValidator(new Validator("Pattern" , ["regexp" , "flags"] , function() {
	  if(this.currentValue == null)
		return true;  
	  var regex = null;
	  if(this.flags == undefined)
	    regex = new RegExp(this.regexp);
	  else
		regex = new RegExp(this.regexp , eval(this.flags));
	  
	return regex.test(this.currentValue); 
}) );


//////////////////////////Validators for Hibernate Validator built-in constraints
Validation.registerValidator(new Validator("CreditCardNumber" , null , function() {
	cc_array = this.currentValue.split( " " );   /////Luhn Algorithm
	cc_array.reverse();
	digit_string = " ";
	for (var counter=0; counter < cc_array.length; counter++ ){
	   current_digit = parseInt( cc_array[counter] ); 
	   if (counter %2 != 0)
		 cc_array[counter] *= 2;
	     digit_string += cc_array[counter];	
	}
	digit_sum = 0;
	for (var counter=0; counter<digit_string.length; counter++ ){
	   current_digit = parseInt( digit_string.charAt(counter) );
	   digit_sum += current_digit;
	}
	if ( digit_sum % 10 == 0 )
	   return true;
	
	return false;
}) );

Validation.registerValidator(new Validator("Email" , null , function() {
	var pattern = /^[a-zA-Z0-9]+[a-zA-Z0-9_.-]+[a-zA-Z0-9_-]+@[a-zA-Z0-9]+[a-zA-Z0-9.-]+[a-zA-Z0-9]+.[a-z]{2,4}$/;
	return pattern.test(this.currentValue);
}) );

Validation.registerValidator(new Validator("Url" , null , function() {
	var pattern = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
	return pattern.test(this.currentValue);
}) );
