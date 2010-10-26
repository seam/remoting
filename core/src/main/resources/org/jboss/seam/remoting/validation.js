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
Seam.processMetaData = function(doc , props){
  var cn = Seam.Xml.childNode;
  var cns = Seam.Xml.childNodes;
  if (typeof(Seam) == "undefined") 
	  return;
  if (!doc.documentElement) 
	  return;
  
  var violations = []; //////for storing any possible ConstraintViolation
  var validationNode = cn(cn(doc.documentElement, "body"),"validation");
  
  var messagesNode = (cn(validationNode, "messages"));
  var messages = cns(messagesNode , "message");
  for(var i=0;i<messages.length;i++){
	Seam.__validationMessages.put(messages[i].getAttribute("constraint") , messages[i].getAttribute("msg"));	
   }
  
  var beans = cns(validationNode , "bean");
  for(var i=0;i<beans.length;i++){
	var beanMetaData = Seam.getBean(beans[i].getAttribute("name"));
	if(beanMetaData == undefined)
	  continue;
	beanMetaData.__constraints	= new Seam.Map();
	var properties = cns(beans[i] , "property");	
	for(var j=0;j<properties.length;j++){
	   var propName = properties[j].getAttribute("name");	
       var constraints = cns(properties[j] , "constraint");
       if(constraints.length > 0){
         var cc = [];	 
         for(var k=0;k<constraints.length;k++){
           var constraint = function() {};
           constraint.name = constraints[k].getAttribute("name");
           var parent = constraints[k].getAttribute("parent");
           if(parent != undefined)    ////////////if it is an composed constraint
        	 constraint.parent = parent;  
           var params = cns(constraints[k] , "param");
           if(params.length > 0){
        	 constraint.params = [];
        	 for(y=0;y<params.length;y++){
        		var param = function() {};
        		param.name  = params[y].getAttribute("name");
        		param.value = params[y].getAttribute("value");
        		constraint.params.push(param);
        	  }  
         	}
        	cc.push(constraint);           
           }
         beanMetaData.__constraints.put(propName , cc);
        }   
	  }	
   }
  Seam.executeValidation(props);
};

Seam.validateProperty = function(bean , property , callBack){
	var p = [];
	p.push(property);
	Seam.validateProperties(bean ,p ,callBack);
};

Seam.validateProperties = function(bean , properties ,callBack){
	Seam.validateBean(bean, callBack ,properties);
};

Seam.validateBean = function(bean , callBack , properties){
	var beans = [];
	beans.push(bean);
	Seam.validateBeans(beans , callBack , properties);
};

Seam.validateBeans = function(beans , callBack , properties){
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
      var processorProcedure = function(doc) { Seam.processMetaData(doc , properties); };  
      Seam.sendAjaxRequest(envelope, Seam.PATH_VALIDATION, processorProcedure , false);
    }
    else{	
      Seam.executeValidation(properties);	   
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
Seam.executeValidation = function(properties) {            
   var violations = [];	
   for(var i=0;i<Seam.__validationBeans.length;i++){ 
	  var metadata = Seam.getBeanType(Seam.__validationBeans[i]); 
	  var constraints = metadata.__constraints;         ////see if we have to validate the whole bean or not...
	  var keys = !properties ? constraints.keySet() : properties;
	   for(var j=0;j<keys.length;j++){
		 var cc = constraints.get(keys[j]);
		  for(var k=0;k<cc.length;k++){
			var result = Seam.validateAgainstConstraint(Seam.__validationBeans[i] , keys[j] , cc[k]);  
			if(result != -1)
			  violations.push(result);	
		  }
	   }
   }
  Seam.__validationCallback(violations);  
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
}

Validation.registerValidator = function(validator) {
	if(this.getValidator(validator.constraintName) == null)
	  this.validators.push(validator);
}

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
	return !isNaN(parseFloat(this.currentValue) && parts[0].length <= this.integer && parts[1].length <= this.fraction); 
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
	cc_array = this.currentValue.split( " " )   /////Luhn Algorithm
	cc_array.reverse()
	digit_string = " "
	for ( counter=0; counter < cc_array.length; counter++ ){
	   current_digit = parseInt( cc_array[counter] )  
	   if (counter %2 != 0)
		 cc_array[counter] *= 2
	   digit_string += cc_array[counter]	
	}
	digit_sum = 0
	for ( counter=0; counter<digit_string.length; counter++ ){
	   current_digit = parseInt( digit_string.charAt(counter) )
	   digit_sum += current_digit
	}
	if ( digit_sum % 10 == 0 )
	   return true
	
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
