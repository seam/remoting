var Seam = {
  beans: {},
  debug: false,
  debugWindow: null,
  PATH_EXECUTE: "/execute",
  PATH_SUBSCRIPTION: "/subscription",
  PATH_MODEL: "/model",
  PATH_VALIDATION: "/validate",
  PATH_POLL: "/poll"
};

Seam.defaultExceptionHandler = function(exception) {
  alert("An exception has occurred while executing a remote request: " + exception.getExceptionClass() + ":" + exception.getMessage());
};

Seam.exceptionHandler = Seam.defaultExceptionHandler;

Seam.messageHandler = null;

Seam.createBean = function(name) {
  if (!Seam.beans[name]) return null;
  var b = new Seam.beans[name];
  if (arguments.length > 1) {
    b.__qualifiers = [];
    for (var i=1; i<arguments.length; i++) {
      b.__qualifiers.push(arguments[i]);
    }
  }
  return b;
};

Seam.getBean = function(name) {
  for (var b in Seam.beans) {
	  if (Seam.beans[b].__name == name) return Seam.beans[b];
  }
  return undefined;
};

Seam.getBeanType = function(obj) {
  for (var b in Seam.beans) {
    if (obj instanceof Seam.beans[b]) return Seam.beans[b];
  }
  return undefined;
};

Seam.getBeanName = function(obj) {
  var t = Seam.getBeanType(obj);
  return t ? t.__name : undefined;
};

Seam.isBeanRegistered = function(name) {
  return Seam.beans[name] != null;
};

Seam.createSetterMethod = function(fieldName) {
  return function(value) { this[fieldName] = value; }; 
};

Seam.createGetterMethod = function(fieldName) {
  return function() { return this[fieldName]; }; 
};

Seam.createBeanFunction = function(methodName, paramCount) {
  return function() {
    var p = [];
    for (var i=0; i<paramCount; i++) {
      p[i] = arguments[i];
    }
    var c = (arguments.length > paramCount) ? arguments[paramCount] : undefined;
    var eh = (arguments.length > (paramCount + 1)) ? arguments[paramCount + 1] : undefined;
    return Seam.execute(this, methodName, p, c, eh); 
  } 
};

Seam.registerBean = function(name, metadata, methods) {
  if (Seam.isBeanRegistered(name)) return;
  var t = function() {};
  t.__name = name;
  if (metadata) {
    var m = [];
    for (var f in metadata) {
      var s = f.substring(0,1).toUpperCase() + f.substring(1);      
      t.prototype["set" + s] = Seam.createSetterMethod(f);
      t.prototype["get" + s] = Seam.createGetterMethod(f);
      m.push({field:f, type:metadata[f]});
    }
    t.__metadata = m;
  } else {
    for (var m in methods) {
      t.prototype[m] = Seam.createBeanFunction(m, methods[m]);
    }
  }
  Seam.beans[name] = t;
};

Seam.importBeans = function() {
  var names = [];
  var cb = null;
  for (var i=0; i<arguments.length; i++) {
    if (typeof arguments[i] != "function") {
      if (arguments[i] instanceof Array) {
        for (var j=0; j<arguments[i].length; j++) {
          names.push(arguments[i][j]);
        } 
      } else {           
        names.push(arguments[i]);
      }
    } else {
        cb = arguments[i];
    }
  }
  
  var c = Seam.createImportBeansCall(names, cb);
  var envelope = Seam.createEnvelope(Seam.createHeader(c.id), c.data);
  Seam.pendingCalls.put(c.id, c);
  Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE, Seam.processResponse, false);
};

Seam.createImportBeansCall = function(names, callback, originalCall) {
  var n = "org.jboss.seam.remoting.BeanMetadata";
  if (!Seam.isBeanRegistered(n)) {
    Seam.registerBean(n, {name: "str", methods: "Seam.Map", properties: "Seam.Map"});
    Seam.registerBean("org.jboss.seam.remoting.MetadataCache", null, {loadBeans:1});
  }

  var c = Seam.createCall(new Seam.beans["org.jboss.seam.remoting.MetadataCache"](), "loadBeans", [names], callback);
  c.handler = Seam.processImportBeansResponse;
  if (originalCall) c.originalCall = originalCall;
  return c;  
};

Seam.getBeanMetadata = function(obj) {
  var b = Seam.getBeanType(obj);
  return b ? b.__metadata : undefined;
};

Seam.Xml = {
  childNode: function(e, tag) {
    for (var i=0; i<e.childNodes.length; i++) {
      if (e.childNodes.item(i).tagName == tag) return e.childNodes.item(i);
    }
  },
  childNodes: function(e, tag) {
    var n = [];
    for (var i=0; i<e.childNodes.length;i++) {
      if (e.childNodes.item(i).tagName == tag) n.push(e.childNodes.item(i));
    }
    return n;
  }
};

Seam.extractEncodedSessionId = function(url) {
  if (url.indexOf(';jsessionid=') >= 0) {
    var qpos = url.indexOf('?');
    return url.substring(url.indexOf(';jsessionid=') + 12, qpos >= 0 ? qpos : url.length);
  }
  return null;
};

Seam.encodedSessionId = Seam.extractEncodedSessionId(window.location.href);

Seam.log = function(msg) {
  if (!Seam.debug) return;
  if (!Seam.debugWindow || Seam.debugWindow.document == null) {
    var attr = "left=400,top=400,resizable=yes,scrollbars=yes,width=400,height=400";
    Seam.debugWindow = window.open("", "__seamDebugWindow", attr);
    if (Seam.debugWindow) {
      Seam.debugWindow.document.write("<html><head><title>Seam Debug Window</title></head><body></body></html>");
      var t = Seam.debugWindow.document.getElementsByTagName("body").item(0);
      t.style.fontFamily = "arial";
      t.style.fontSize = "8pt";
    }
  }
  if (Seam.debugWindow) {
    Seam.debugWindow.document.write("<pre>" + (new Date()) + ": " + msg.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</pre><br/>");
  }
};

Seam.Context = function() {
  this.conversationId = null;
  this.callId = null;
  Seam.Context.prototype.setConversationId = function(conversationId) { this.conversationId = conversationId; };
  Seam.Context.prototype.getConversationId = function() { return this.conversationId; };
  Seam.Context.prototype.setCallId = function(callId) { this.callId = callId; };
  Seam.Context.prototype.getCallId = function() { return this.callId; };
};
Seam.context = new Seam.Context();

Seam.Exception = function(exceptionClass, msg) {
  this.exceptionClass = exceptionClass;
  this.message = msg;
  Seam.Exception.prototype.getMessage = function() {
    return this.message;    
  };
  Seam.Exception.prototype.getExceptionClass = function() {
    return this.exceptionClass;
  };
};

Seam.Message = function(level, targets, text) {
  this.level = level;
  this.targets = targets;
  this.text = text;
  Seam.Message.prototype.getLevel = function() {
    return this.level;
  };
  Seam.Message.prototype.getTargets = function() {
    return this.targets;
  };
  Seam.Message.prototype.getText = function() {
    return this.text;
  };
};

Seam.equals = function(v1, v2) {
  if (v1 == v2) return true;
  if (v1 instanceof Date && v2 instanceof Date && v1.getTime() == v2.getTime()) 
	 return true;
  return false;
};

Seam.Map = function() {
  this.elements = [];

  Seam.Map.prototype.clear = function() {
    this.elements = [];
  };

  Seam.Map.prototype.size = function() {
    return this.elements.length;
  };
  
  Seam.Map.prototype.isEmpty = function() {
    return this.elements.length == 0;
  };
  
  Seam.Map.prototype.keySet = function() {
    var keySet = [];
    for (var i=0; i<this.elements.length; i++) {
      keySet[keySet.length] = this.elements[i].key;
    }
    return keySet;
  };
  
  Seam.Map.prototype.values = function() {
    var vals = [];
    for (var i=0; i<this.elements.length; i++) {
      vals.push(this.elements[i].value);
    }
    return vals;
  };
  
  Seam.Map.prototype.get = function(key) {
    for (var i=0; i<this.elements.length; i++) {
      var e = this.elements[i];
      if (Seam.equals(e.key, key)) return e.value;
    }
    return null;
  };
  
  Seam.Map.prototype.put = function(key, value) {
    for (var i=0; i<this.elements.length; i++) {
      if (Seam.equals(this.elements[i].key, key)) {
        this.elements[i].value = value;
        return;
      }
    }
    this.elements.push({key:key,value:value});
  };
  
  Seam.Map.prototype.remove = function(key) {
    for (var i=0; i<this.elements.length; i++) {
      if (Seam.equals(this.elements[i].key, key)) {
        this.elements.splice(i, 1);
        break;
      }
    }
  };
  
  Seam.Map.prototype.contains = function(key) {
    for (var i=0; i<this.elements.length; i++) {
      if (Seam.equals(this.elements[i].key, key)) return true;
    }
    return false;
  };
};

Seam.serializeValue = function(v, type, refs) {
  if (v == null) return "<null/>";
  if (type) {
    switch (type) {
      case "bool": return "<bool>" + (v ? "true" : "false") + "</bool>";
      case "number": return "<number>" + v + "</number>";
      case "date": return Seam.serializeDate(v);
      case "bean": return Seam.getTypeRef(v, refs);
      case "bag": return Seam.serializeBag(v, refs);
      case "map": return Seam.serializeMap(v, refs);
      default: return "<str>" + encodeURIComponent(v) + "</str>";
    }
  } else {
    switch (typeof(v)) {
      case "number":
        return "<number>" + v + "</number>";
      case "boolean":
        return "<bool>" + (v ? "true" : "false") + "</bool>";
      case "object":
        if (v instanceof Array)
          return Seam.serializeBag(v, refs);
        else if (v instanceof Date)
          return Seam.serializeDate(v);
        else if (v instanceof Seam.Map)
          return Seam.serializeMap(v, refs);
        else
          return Seam.getTypeRef(v, refs);
      default:
        return "<str>" + encodeURIComponent(v) + "</str>";
    }
  }
};

Seam.serializeBag = function(v, refs) {
  var d = "<bag>";
  for (var i=0; i<v.length; i++) {
    d += "<element>" + Seam.serializeValue(v[i], null, refs) + "</element>";
  }
  d += "</bag>";
  return d;
};

Seam.serializeMap = function(v, refs) {
  var d = "<map>";
  var k = v.keySet();
  for (var i=0; i<k.length; i++) {
    d += "<element><k>" + Seam.serializeValue(k[i], null, refs) + "</k><v>" +
    Seam.serializeValue(v.get(k[i]), null, refs) + "</v></element>";
  }
  d += "</map>";
  return d;
};

Seam.serializeDate = function(v) {
  var zeroPad = function(val, digits) { while (("" + val).length < digits) val = "0" + val; return val; };
  return "<date>" + v.getFullYear() + zeroPad(v.getMonth() + 1, 2) + zeroPad(v.getDate(), 2) +
    zeroPad(v.getHours(), 2) + zeroPad(v.getMinutes(), 2) + zeroPad(v.getSeconds(), 2) +
    zeroPad(v.getMilliseconds(), 3) +"</date>";
};

Seam.getTypeRef = function(obj, refs) {
  var refId = -1;
  for (var i=0; i<refs.length; i++) {
    if (refs[i] == obj) {
      refId = i;
      break;
    }
  }
  if (refId == -1) {
    refId = refs.length;
    refs[refId] = obj;
  }
  return "<ref id=\"" + refId + "\"/>";
};

Seam.serializeType = function(obj, refs) {
  var t = Seam.getBeanType(obj);
  if (!t) {
    alert("Unknown Type error.");
    return null;
  }
  var d = "<bean type=\"" + t.__name + "\">\n";
  var meta = Seam.getBeanMetadata(obj);
  for (var i=0; i<meta.length; i++) {
    d += "<member name=\"" + meta[i].field + "\">" +
      Seam.serializeValue(obj[meta[i].field], meta[i].type, refs) + "</member>";
  }
  d += "</bean>";
  return d;
};

Seam.createCall = function(component, methodName, params, callback, exceptionHandler) {
  var callId = "" + Seam.__callId++;
  if (!callback && component.__callback) callback = component.__callback[methodName];
  var d = "<call><target>" + Seam.getBeanType(component).__name + "</target>";
  if (component.__qualifiers != null) {
    d += "<qualifiers>";
    for (var i=0; i<component.__qualifiers.length; i++) {
      d += component.__qualifiers[i];
      if (i < component.__qualifiers.length - 1) data += ",";
    }
    d += "</qualifiers>";
  }
  d += "<method>" + methodName + "</method><params>";
  var refs = [];
  for (var i=0; i<params.length; i++) {
    d += "<param>" + Seam.serializeValue(params[i], null, refs) + "</param>";
  }
  d += "</params><refs>";
  for (var i=0; i<refs.length; i++) {
    d += "<ref id=\"" + i + "\">" + Seam.serializeType(refs[i], refs) + "</ref>";
  }
  d += "</refs></call>";
  
  var handler = (exceptionHandler !== null && exceptionHandler != undefined) ? exceptionHandler : Seam.exceptionHandler;
  
  return {data: d, id: callId, callback: callback, exceptionHandler: handler, handler: Seam.preProcessCallResponse};
};

Seam.createHeader = function(callId, ctx) {
  var h = "<context><callId>" + callId + "</callId>";
  if (!ctx) ctx = Seam.context;
  if (ctx.getConversationId()) {
    h += "<conversationId>" + ctx.getConversationId() + "</conversationId>";
  }
  h += "</context>";
  return h;
};

Seam.createEnvelope = function(header, body) {
  var d = "<envelope>";
  if (header) d += "<header>" + header + "</header>";
  if (body) d += "<body>" + body + "</body>";
  d += "</envelope>";
  return d;
};

Seam.__callId = 0;
Seam.pendingCalls = new Seam.Map();

Seam.cancelCall = function(callId) {
  var c = Seam.pendingCalls.get(callId);
  Seam.pendingCalls.remove(callId);
  if (c && c.asyncReq) {
    if (Seam.pendingCalls.isEmpty()) Seam.hideLoadingMessage();
    window.setTimeout(function() {
      c.asyncReq.onreadystatechange = function() {};
    }, 0);
    c.asyncReq.abort();
  }
};

Seam.execute = function(component, methodName, params, callback, exceptionHandler) {
  var c = Seam.createCall(component, methodName, params, callback, exceptionHandler);
  var envelope = Seam.createEnvelope(Seam.createHeader(c.id), c.data);
  Seam.pendingCalls.put(c.id, c);
  Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE, Seam.processResponse, false);
  return c;
};

Seam.sendAjaxRequest = function(env, path, callback, silent) {
  Seam.log("Request packet:\n" + env);
  if (!silent) Seam.displayLoadingMessage();
  var r;
  if (window.XMLHttpRequest) {
    r = new XMLHttpRequest();
    if (r.overrideMimeType) r.overrideMimeType('text/xml');
  } else {
    r = new ActiveXObject("Microsoft.XMLHTTP");
  }
  r.onreadystatechange = function() {
    if (r.readyState == 4) {
      var inScope = typeof(Seam) == "undefined" ? false : true;
      if (inScope) Seam.hideLoadingMessage();

      if (r.status == 200) {
        // We do this to avoid a memory leak
        window.setTimeout(function() {
          r.onreadystatechange = function() {};
        }, 0);
        if (inScope) Seam.log("Response packet:\n" + r.responseText);
        if (callback) {
          // The following code deals with a Firefox security issue.  It reparses the XML
          // response if accessing the documentElement throws an exception
          try {
            r.responseXML.documentElement;
            callback(r.responseXML);
          }
          catch (ex) {
             try {
               Seam.log(ex.message + " : " + ex.stack);
               // Try it the IE way first...
               var doc = new ActiveXObject("Microsoft.XMLDOM");
               doc.async = "false";
               doc.loadXML(r.responseText);
               callback(doc);
             }
             catch (e) {
               // If that fails, use standards
               var parser = new DOMParser();
               callback(parser.parseFromString(r.responseText, "text/xml"));
             }
          }
        }
      } else {
        Seam.displayError(r.status);
      }
    }
  };
  if (Seam.encodedSessionId) {
    path += ';jsessionid=' + Seam.encodedSessionId;
  }
  r.open("POST", Seam.resourcePath + path, true);
  r.send(env);
};

Seam.processImportBeansResponse = function(call) {   
  var cn = Seam.Xml.childNode;
  Seam.pendingCalls.remove(call.callId);
  var n = cn(cn(call.response.documentElement, "body"), "result");
  var valueNode = cn(n, "value");
  var exceptionNode = cn(n, "exception");
  if (exceptionNode != null && call.exceptionHandler != null) {
    var clsNode = cn(exceptionNode, "class");
    var exCls = Seam.unmarshalValue(clsNode.firstChild);
    var msgNode = cn(exceptionNode, "message");
    var msg = Seam.unmarshalValue(msgNode.firstChild);    
    call.exceptionHandler(new Seam.Exception(exCls, msg));
  } else {
    var refsNode = cn(n, "refs");
    var refs = Seam.unmarshalRefs(refsNode);
    var v = Seam.unmarshalValue(valueNode.firstChild, refs);
    for (var i=0; i<v.length; i++) {
      var meta = v[i]; 
      if (meta.beanType == "state") {
        var props = {};
        for (var j=0;j<meta.properties.elements.length; j++) {
          props[meta.properties.elements[j].key] = meta.properties.elements[j].value;
        }
        Seam.registerBean(meta.name, props);
      } else if (meta.beanType == "action") {
        var methods = {};
        for (var j=0; j<meta.methods.elements.length; j++) {
          methods[meta.methods.elements[j].key] = meta.methods.elements[j].value;
        }
        Seam.registerBean(meta.name, null, methods);
      }
    }
    if (call.callback) {
      call.callback(v, call.context);
    } else if (call.originalCall) {
      call.originalCall.handler(call.originalCall);
    }
  }
};

Seam.preProcessCallResponse = function(call) {
  var cn = Seam.Xml.childNode;
  var bodyNode = cn(call.response.documentElement, "body");
  if (bodyNode) {
    var n = cn(bodyNode, "result");
    if (n) {
      var refsNode = cn(n, "refs");
      var u = Seam.validateRefs(refsNode);
      if (u.length > 0) {
        call.handler = Seam.processCallResponse;
        var c = Seam.createImportBeansCall(u, null, call);
        var envelope = Seam.createEnvelope(Seam.createHeader(c.id), c.data);
        Seam.pendingCalls.put(c.id, c);
        Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE, Seam.processResponse, false);                        
      } else {
        Seam.processCallResponse(call);
      }
    }
  }
};

Seam.processCallResponse = function(call) {
  var cn = Seam.Xml.childNode;
  Seam.pendingCalls.remove(call.callId);
  var n = cn(cn(call.response.documentElement, "body"), "result");
  var valueNode = cn(n, "value");
  var exceptionNode = cn(n, "exception");
  if (exceptionNode != null && call.exceptionHandler != null) {
    var clsNode = cn(exceptionNode, "class");
    var exCls = Seam.unmarshalValue(clsNode.firstChild);
    var msgNode = cn(exceptionNode, "message");
    var msg = Seam.unmarshalValue(msgNode.firstChild);
    call.exceptionHandler(new Seam.Exception(exCls, msg));
  } else {
    var m = cn(cn(call.response.documentElement, "header"), "msgs");
    if (m) {
      var me = Seam.Xml.childNodes(m, "m");
      var msgs = [];
      for (var i = 0; i < me.length; i++) {
        var enc = "";
        for (var j=0; j<me[i].childNodes.length; j++) {
        if (me[i].childNodes[j].nodeType == 3)
          enc += me[i].childNodes[j].nodeValue;
        }
        var text = decodeURIComponent(enc);      
        msgs.push(new Seam.Message(me[i].getAttribute("lvl"), me[i].getAttribute("tgt"), text));      
      };
      if (typeof(Seam.messageHandler) == "function") {
        Seam.messageHandler(msgs);
      }
    }  
  
    if (call.callback != null) {
      var refsNode = cn(n, "refs");
      var refs = Seam.unmarshalRefs(refsNode);
      var v = Seam.unmarshalValue(valueNode.firstChild, refs);
      call.callback(v, call.context);
    }
  }
};

Seam.preProcessModelResponse = function(call) {
  var cn = Seam.Xml.childNode;
  var b = cn(call.response.documentElement, "body");
  if (b) {
    var m = cn(b, "model");
    if (m) {
      var refsNode = cn(m, "refs");
      var u = Seam.validateRefs(refsNode);
      if (u.length > 0) {
        call.handler = Seam.processModelResponse;
        var c = Seam.createImportBeansCall(u, null, call);
        var envelope = Seam.createEnvelope(Seam.createHeader(c.id), c.data);
        Seam.pendingCalls.put(c.id, c);
        Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE, Seam.processResponse, false);                        
      } else {
        Seam.processModelResponse(call);
      }
    }
  }  
};

Seam.preProcessModelExpandResponse = function(call) {
  var cn = Seam.Xml.childNode; 
  var b = cn(call.response.documentElement, "body");
  if (b) {
    var m = cn(b, "model");
    if (m) {  
      var refsNode = cn(m, "refs");
      var u = Seam.validateRefs(refsNode);
      if (u.length > 0) {
        call.handler = Seam.processModelExpandResponse;
        var c = Seam.createImportBeansCall(u, null, call);
        var envelope = Seam.createEnvelope(Seam.createHeader(c.id), c.data);
        Seam.pendingCalls.put(c.id, c);
        Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE, Seam.processResponse, false);                        
      } else {
        Seam.processModelExpandResponse(call);
      }
    }
  } 
};

Seam.processModelExpandResponse = function(call) {
  Seam.pendingCalls.remove(call.callId);
  var cn = Seam.Xml.childNode;
  var b = cn(call.response.documentElement, "body");
  if (b) {
    var n = cn(b, "model");
    if (call.model) {
      call.model.context.setConversationId(call.context.conversationId);
      call.model.processExpandResponse(n, call.refId, call.property, call.callback);
    }
  }
};

Seam.processModelResponse = function(call) {
  Seam.pendingCalls.remove(call.callId);
  var cn = Seam.Xml.childNode;  
  var b = cn(call.response.documentElement, "body");
  if (b) {
    var n = cn(b, "model");
    if (call.model) {
      call.model.context.setConversationId(call.context.conversationId);
      call.model.processResponse(n, call.callback);
    }
  }
};

Seam.displayError = function(code) {
  alert("There was an error processing your request.  Error code: " + code);
};

Seam.setCallback = function(component, methodName, callback) {
  component.__callback[methodName] = callback;
};

Seam.processResponse = function(doc) {
  var cn = Seam.Xml.childNode;
  if (typeof(Seam) == "undefined") return;
  if (!doc.documentElement) return;
  var ctx = new Seam.Context;
  var headerNode = cn(doc.documentElement, "header");
  if (headerNode) {
    var contextNode = cn(headerNode, "context");
    if (contextNode) {
      Seam.unmarshalContext(contextNode, ctx);
      var call = Seam.pendingCalls.get(ctx.getCallId());
      Seam.context.setConversationId(ctx.getConversationId() ? ctx.getConversationId() : null);
      if (call) {
        call.context = ctx;
        call.response = doc;
        if (call.handler) call.handler(call); 
      }
    }
  }
};

Seam.unmarshalContext = function(ctxNode, context) {
  var c = Seam.Xml.childNode(ctxNode, "conversationId");
  if (c) context.setConversationId(c.firstChild.nodeValue);
  c = Seam.Xml.childNode(ctxNode, "callId");
  if (c) context.setCallId(c.firstChild.nodeValue);
};

Seam.validateRefs = function(refsNode) {
  var unknowns = [];
  if (refsNode) {
    var cn = Seam.Xml.childNodes(refsNode, "ref");
    for (var i=0; i<cn.length; i++) {
      var n = cn[i].firstChild;
      if (n.tagName == "bean") {
        var name = n.getAttribute("type");
        if (!Seam.isBeanRegistered(name)) unknowns.push(name);
      }
    }
  }
  return unknowns;
};

Seam.unmarshalRefs = function(refsNode, refs) {
  if (!refsNode) return;
  if (!refs) refs = [];
  var objs = [];
  var cn = Seam.Xml.childNodes(refsNode, "ref");
  for (var i=0; i<cn.length; i++) {
    var refId = parseInt(cn[i].getAttribute("id"));
    var valueNode = cn[i].firstChild;
    if (valueNode.tagName == "bean") {
      var name = valueNode.getAttribute("type");
      var obj = Seam.isBeanRegistered(name) ? Seam.createBean(name) : null;
      if (obj) {
        refs[refId] = obj;
        objs.push({obj:obj, node:valueNode});
      }
    }
  }
  for (var i=0; i<objs.length; i++) {
    var cn = Seam.Xml.childNodes(objs[i].node, "member");
    for (var j=0; j<cn.length; j++) {
      if (cn[j].tagName == "member") {
        var name = cn[j].getAttribute("name");
        objs[i].obj[name] = Seam.unmarshalValue(cn[j].firstChild, refs);
      }
    }
  }
  return refs;
};

Seam.unmarshalValue = function(element, refs) {
  switch (element.tagName) {
    case "bool": return element.firstChild.nodeValue == "true";
    case "number":
      if (element.firstChild.nodeValue.indexOf(".") == -1)
        return parseInt(element.firstChild.nodeValue);
      else
        return parseFloat(element.firstChild.nodeValue);
    case "str":
      var data = "";
      for (var i=0; i<element.childNodes.length; i++) {
        if (element.childNodes[i].nodeType == 3)
          data += element.childNodes[i].nodeValue;
      }
      return decodeURIComponent(data);
    case "ref": return refs[parseInt(element.getAttribute("id"))];
    case "bag":
      var value = [];
      var cn = Seam.Xml.childNodes(element, "element");
      for (var i=0; i<cn.length; i++) {
        value.push(Seam.unmarshalValue(cn[i].firstChild, refs));
      }
      return value;
    case "map":
      var m = new Seam.Map();
      var cn = Seam.Xml.childNodes(element, "element");
      for (var i=0; i<cn.length; i++) {
        var k = Seam.unmarshalValue(Seam.Xml.childNode(cn[i], "k").firstChild, refs);
        var v = Seam.unmarshalValue(Seam.Xml.childNode(cn[i], "v").firstChild, refs);
        if (k != null) m.put(k, v);
      }
      return m;
    case "date": return Seam.deserializeDate(element.firstChild.nodeValue);
    case "undefined": return undefined;
    default: return null;
  }
};

Seam.cloneObject = function(obj, refMap) {
  if (refMap && refMap.contains(obj)) return refMap.get(obj);
  if (typeof obj == "object") {
    if (obj == null) return null;
    var m = (refMap == null) ? new Seam.Map() : refMap;
    if (obj instanceof Array) {
      var c = [];
      m.put(obj, c);
      for (var i=0; i<obj.length; i++) {
        c[i] = Seam.cloneObject(obj[i], m);
      }
      return c;
    } else if (obj instanceof Date) {
      return new Date(obj.getTime())
    }
    var t = Seam.getBeanType(obj);
    var c = (t == undefined) ? new Object() : new t();
    m.put(obj, c);
    for (var p in obj) c[p] = Seam.cloneObject(obj[p], m);
    return c;
  }
  return obj;
};

Seam.deserializeDate = function(val) {
  var d = new Date();
  d.setFullYear(parseInt(val.substring(0,4), 10),
    parseInt(val.substring(4,6), 10) - 1,
    parseInt(val.substring(6,8), 10));
  d.setHours(parseInt(val.substring(8,10), 10));
  d.setMinutes(parseInt(val.substring(10,12), 10));
  d.setSeconds(parseInt(val.substring(12,14), 10));
  d.setMilliseconds(parseInt(val.substring(14,17), 10));
  return d;
};

Seam.loadingMsgDiv = null;
Seam.loadingMessage = "Please wait...";

Seam.displayLoadingMessage = function() {
  if (!Seam.loadingMsgDiv) {
    Seam.loadingMsgDiv = document.createElement('div');
    var d = Seam.loadingMsgDiv;
    d.setAttribute('id', 'loadingMsg');
    d.style.position = "absolute";
    d.style.top = "0px";
    d.style.right = "0px";
    d.style.background = "red";
    d.style.color = "white";
    d.style.fontFamily = "Verdana,Helvetica,Arial";
    d.style.fontSize = "small";
    d.style.padding = "2px";
    d.style.border = "1px solid black";
    document.body.appendChild(d);
    var text = document.createTextNode(Seam.loadingMessage);
    d.appendChild(text);
  } else {
    Seam.loadingMsgDiv.innerHTML = Seam.loadingMessage;
    Seam.loadingMsgDiv.style.visibility = 'visible';
  }
};

Seam.hideLoadingMessage = function() {
  if (Seam.loadingMsgDiv) Seam.loadingMsgDiv.style.visibility = 'hidden';
};

Seam.Action = function() {
  this.beanType = null;
  this.qualifiers = null;
  this.method = null;
  this.params = [];
  this.expression = null;
  
  Seam.Action.prototype.setBeanType = function(beanType) {
    this.beanType = beanType;
    return this;
  };
  
  Seam.Action.prototype.setQualifiers = function(qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  };
  
  Seam.Action.prototype.setMethod = function(method) {
    this.method = method;
    return this;
  };
  
  Seam.Action.prototype.addParam = function(param) {
    this.params.push(param);
    return this;
  };
  
  Seam.Action.prototype.setExpression = function(expr) {
    this.expression = expr;
    return this;
  };
};

Seam.Changeset = function() {
  this.propertyChange = new Seam.Map();
  Seam.Changeset.prototype.addProperty = function(name, val) {
    this.propertyChange.put(name, val);
  };
};

Seam.Delta = function(model) {
  this.model = model;
  this.refs = new Seam.Map();

  Seam.Delta.prototype.testEqual = function(v1, v2) {
    if (v1 == null) return v2 == null;
    switch (typeof(v1)) {
      case "number":
        return typeof(v2) == "number" && v1 == v2;
      case "boolean":
        return typeof(v2) == "boolean" && v1 == v2;
      case "string":
        return typeof(v2) == "string" && v1 == v2;
      case "object":
        if (v1 instanceof Date) {
          return (v2 instanceof Date) && v1.getTime() == v2.getTime();
        } else if (Seam.getBeanType(v1)) {
          return this.getSourceObject(v1) == v2;
        } else if (v1 instanceof Array) {
          if (!(v2 instanceof Array)) return false;
          if (v1.length != v2.length) return false;
          for (var i=0; i<v1.length; i++) {
            if (!this.testEqual(v1[i], v2[i])) return false;
          }
          return true;
        } else if (v1 instanceof Seam.Map) {
          if (!(v2 instanceof Seam.Map)) return false;
          if (v1.size() != v2.size()) return false;
          for (var i=0; i<v1.size(); i++) {
            var e = v1.elements[i];
            if (Seam.getBeanType(e.key) && this.testEqual(e.value, v2.get(this.getSourceObject(e.key)))) break;
            if (this.testEqual(e.value, v2.get(e.key)) && (e.value != null || v2.contains(e.key))) break;
            return false;
          }
          return true;
        }
    }
    return false;
  };

  Seam.Delta.prototype.registerPropertyChange = function(obj, prop) {
    var cs = this.refs.get(obj);
    if (cs == null) {
      cs = new Seam.Changeset();
      this.refs.put(obj, cs);
    }
    cs.addProperty(prop, obj[prop]);
  };

  Seam.Delta.prototype.scanForChanges = function(obj) {
    if (obj == null || this.refs.contains(obj)) return;
    this.refs.put(obj, null);
    var src = this.getSourceObject(obj);
    if (Seam.getBeanType(obj)) {
      var m = Seam.getBeanMetadata(obj);
      for (var i=0; i<m.length; i++) {
        var f=m[i].field;
        var t=m[i].type;
        if (src && !this.testEqual(obj[f], src[f])) this.registerPropertyChange(obj, f);
        if (t == "bag" || t == "map" || t == "bean") this.scanForChanges(obj[f]);
      }
    } else if (obj instanceof Array) {
      if (src && !this.testEqual(obj, src)) this.refs.put(obj, true);
      for (var i=0; i<obj.length; i++) {
        if (Seam.getBeanType(obj[i]) || obj[i] instanceof Array || obj[i] instanceof Seam.Map) {
          this.scanForChanges(obj[i]);
        }
      }
    } else if (obj instanceof Seam.Map) {
      if (src && !this.testEqual(obj, src)) this.refs.put(obj, true);
      for (var i=0; i<obj.elements.length; i++) {
        var k = obj.elements[i].key;
        var v = obj.elements[i].value;
        if (Seam.getBeanType(k) || k instanceof Array || k instanceof Seam.Map) {
          this.scanForChanges(k);
        }
        if (Seam.getBeanType(v) || v instanceof Array || v instanceof Seam.Map) {
          this.scanForChanges(v);
        }
      }
    }
  };

  Seam.Delta.prototype.getSourceObject = function(obj) {
    for (var i=0;i<this.model.workingRefs.length; i++) {
      if (obj == this.model.workingRefs[i]) return this.model.sourceRefs[i];
    }
    return null;
  };

  Seam.Delta.prototype.getNewRefs = function() {
    var nr = [];
    for (var i=0; i<this.refs.elements.length; i++) {
      var key = this.refs.elements[i].key;
      if (Seam.getBeanType(key) && this.model.getRefId(key) == -1) nr.push(key);
    }
    return nr;
  };
};

Seam.Model = function() {
  this.id = null;
  this.expressions = [];
  this.beans = [];
  this.values = [];
  this.sourceRefs = [];
  this.workingRefs = [];
  this.context = new Seam.Context();

  Seam.Model.prototype.addExpression = function(alias, expr) {
    this.expressions.push({alias: alias, expr: expr});
  };

  Seam.Model.prototype.getValue = function(alias) {
    for (var i=0; i<this.values.length; i++) {
      if (this.values[i].alias == alias) {
        return this.values[i].value;
      }
    }
    return null;
  };

  Seam.Model.prototype.addBean = function(alias, bean) {
    var q = null;
    if (arguments.length > 2) {
      q = [];
      for (var i=2; i<arguments.length; i++) {
        q.push(arguments[i]);
      }
    }
    this.beans.push({alias: alias, bean: bean, qualifiers: q});
  };

  Seam.Model.prototype.addBeanProperty = function(alias, bean, property) {
    var q = null;
    if (arguments.length > 3) {
      q = [];
      for (var i=3; i<arguments.length; i++) {
        q.push(arguments[i]);
      }
    }
    this.beans.push({alias: alias, bean: bean, property: property, qualifiers: q});
  };

  Seam.Model.prototype.fetch = function(action, cb) {
    var r = this.createFetchRequest(action, cb);
    var env = Seam.createEnvelope(Seam.createHeader(r.id, this.context), r.data);
    Seam.pendingCalls.put(r.id, r);
    Seam.sendAjaxRequest(env, Seam.PATH_MODEL, Seam.processResponse, false);
  };

  Seam.Model.prototype.createFetchRequest = function(a, cb) {
    var callId = "" + Seam.__callId++;
    var d = "<model operation=\"fetch\">";
    var refs = [];
    if (a) {
      d += "<action>";
      if (a.beanType) {
        d += "<target>" + a.beanType + "</target>";
        if (a.qualifiers) d += "<qualifiers>" + a.qualifiers + "</qualifiers>";
        if (a.method) d += "<method>" + a.method + "</method>";
        if (a.params.length > 0) {
          d += "<params>";
          for (var i=0; i<a.params.length; i++) {
            d += "<param>" + Seam.serializeValue(a.params[i], null, refs) + "</param>";
          }
          d += "</params>";
        }
        if (refs.length > 0) {
          d += "<refs>";
          for (var i=0; i<refs.length; i++) {
            d += "<ref id=\"" + i + "\">" + Seam.serializeType(refs[i], refs) + "</ref>";
          }
          d += "</refs>";
        }
      } else if (a.expression) {
        d += "<target>" + a.expression + "</target>";
      }
      d += "</action>";
    }
    if (this.beans.length > 0) {
      for (var i=0; i<this.beans.length; i++) {
        var b = this.beans[i];
        d += "<bean alias=\"" + b.alias + "\"><name>" + b.bean + "</name>";
        if (b.qualifiers && b.qualifiers.length > 0) {
          d += "<qualifiers>";
          for (var j=0; j<b.qualifiers.length; j++) {
             d += (j>0 ? "," : "") + b.qualifiers[j];
          }
          d += "</qualifiers>";
        }
        if (b.property) d += "<property>" + b.property + "</property>";
        d += "</bean>";
      }
    }
    if (this.expressions.length > 0) {
      for (var i=0; i<this.expressions.length; i++) {
        var e = this.expressions[i];
        d += "<expression alias=\"" + e.alias + "\">" + e.expr + "</expression>";
      }
    }
    d += "</model>";
    return {data:d, id:callId, model:this, handler: Seam.preProcessModelResponse, callback: cb};
  };

  Seam.Model.prototype.processResponse = function(modelNode, cb) {
    var refsNode = Seam.Xml.childNode(modelNode, "refs");
    this.id = modelNode.getAttribute("id");
    var valueNodes = Seam.Xml.childNodes(modelNode, "value");         
    this.sourceRefs = Seam.unmarshalRefs(refsNode);
    this.workingRefs = Seam.cloneObject(this.sourceRefs);
    for (var i=0; i<valueNodes.length; i++) {
      var value = Seam.unmarshalValue(valueNodes[i].firstChild,this.workingRefs);
      this.values.push({alias:valueNodes[i].getAttribute("alias"),value:value, refIndex:i});
    }
    if (cb) cb(this);
  };

  Seam.Model.prototype.applyUpdates = function(a, cb) {
    var d = new Seam.Delta(this);
    for (var i=0; i<this.values.length; i++) {
      d.scanForChanges(this.values[i].value);
    }
    var r = this.createApplyRequest(a, d, cb);
    var env = Seam.createEnvelope(Seam.createHeader(r.id, this.context), r.data);
    Seam.pendingCalls.put(r.id, r);
    Seam.sendAjaxRequest(env, Seam.PATH_MODEL, Seam.processResponse, false);
  };

  Seam.Model.prototype.createApplyRequest = function(a, delta, cb) {
    var callId = "" + Seam.__callId++;
    var refs = [];
    for (var i=0; i<this.workingRefs.length; i++) {
      refs[i] = this.workingRefs[i];
    }
    var d = "<model id=\"" + this.id + "\" operation=\"apply\">";
    if (a) {
      d += "<action>";
      if (a.beanType) {
        d += "<target>" + a.beanType + "</target>";
        if (a.qualifiers) d += "<qualifiers>" + a.qualifiers + "</qualifiers>";
        if (a.method) d += "<method>" + a.method + "</method>";
        if (a.params.length > 0) {
          d += "<params>";
          for (var i=0; i<a.params.length; i++) {
            d += "<param>" + Seam.serializeValue(a.params[i], null, refs) + "</param>";
          }
          d += "</params>";
        }
      } else if (a.expression) {
        d += "<target>" + a.expression + "</target>";
      }
      d += "</action>";
    }
    d += "<delta>";
    for (var i=0; i<delta.refs.elements.length; i++) {
      var k = delta.refs.elements[i].key;
      var v = delta.refs.elements[i].value;
      if (v) {
        var refId = this.getRefId(k);        
        d += "<changeset refid=\"" + refId + "\">";
        if (v instanceof Seam.Changeset) {
          for (var j=0; j<v.propertyChange.elements.length; j++) {
            d += "<member name=\"" + v.propertyChange.elements[j].key + "\">";
            d += Seam.serializeValue(v.propertyChange.elements[j].value, null, refs);
            d += "</member>";
          }
        } else {
          d += Seam.serializeValue(k, null, refs);
        }
        d += "</changeset>";
      }
    }
    d += "</delta>";
    var newRefs = delta.getNewRefs();    
    if (newRefs.length > 0) {
      d += "<refs>";
      for (var i=0; i<newRefs.length; i++) {
        var idx = refs.length;
        for (var j=0; j<refs.length; j++) {
          if (refs[j] == newRefs[i]) {
            idx = j;
            break;
          }
        }
        d += "<ref id=\"" + idx + "\">" + Seam.serializeType(newRefs[i], refs) + "</ref>";
      }
      d += "</refs>";
    }
    d += "</model>";
    return {data:d, id:callId, model:this, handler: Seam.preProcessModelResponse, callback: cb};
  };

  Seam.Model.prototype.getRefId = function(v) {
    for (var i=0; i<this.workingRefs.length; i++) {
      if (this.workingRefs[i] == v) return i;
    }
    return -1;
  };
  
  Seam.Model.prototype.expand = function(v, p, cb) {
    if (v[p] != undefined) return;
    var refId = this.getRefId(v);    
    var r = this.createExpandRequest(refId, p, cb);
    var env = Seam.createEnvelope(Seam.createHeader(r.id, this.context), r.data);
    Seam.pendingCalls.put(r.id, r);
    Seam.sendAjaxRequest(env, Seam.PATH_MODEL, Seam.processResponse, false);
  };

  Seam.Model.prototype.createExpandRequest = function(refId, propName, cb) {
    var callId = "" + Seam.__callId++;
    var d = "<model id=\"" + this.id + "\" operation=\"expand\">" +
      "<ref id=\"" + refId + "\"><member name=\"" + propName + "\"/></ref></model>";
    return {data: d, id: callId, model: this, refId: refId, property: propName, handler: Seam.preProcessModelExpandResponse, callback: cb};
  };

  Seam.Model.prototype.processExpandResponse = function(modelNode, refId, propName, cb) {
    var refsNode = Seam.Xml.childNode(modelNode, "refs");
    var resultNode = Seam.Xml.childNode(modelNode, "result");
    Seam.unmarshalRefs(refsNode, this.sourceRefs);
    Seam.unmarshalRefs(refsNode, this.workingRefs);
    this.sourceRefs[refId][propName] = Seam.unmarshalValue(resultNode.firstChild,this.sourceRefs);
    this.workingRefs[refId][propName] = Seam.unmarshalValue(resultNode.firstChild,this.workingRefs);
    if (cb) cb(this);
  };
};

/*JSR 303 integration with Seam-Remoting Module
 *@author Amir Sadri
 */

Seam.validation_bean     = "bean";
Seam.validation_props    = "properties";
Seam.validation_groups   = "groups";
Seam.validation_traverse = "traverse";

Seam.createValidationRequest = function() {
  var beans =  Seam.__validationBeans;	
  var b = "<beans>";
  for(var i=0;i<beans.length;i++) {
	var bean = beans[i][Seam.validation_bean];  
    var metaData = Seam.getBeanType(bean);
     if(metaData == undefined){
    	 Seam.log("Error: Invalid Validation Request [Undefined Bean was provided].");
    	 continue;
     }
    b += "<bean target=\"" + metaData.__name + "\"";
    var qualifiers = metaData.__qualifiers;
    if(qualifiers != undefined && qualifiers != null) {
      var q = qualifiers[0];
      for(var j=1;j<qualifiers.length;j++) {
        q += ","+qualifiers[j];
      }
      b += " qualifiers=\""+q+"\" />";
    }
    else {
      b += " />"
    }
  }
  b += "</beans>";
  if(Seam.__validationMessages != undefined) {
    b += "<messages>";
    var keys = Seam.__validationMessages.keySet();
    for(var i=0;i<keys.length;i++) {
      b+= keys[i] + (i == keys.length-1 ? "" : ",,");
    }
    b += "</messages>"
  }
  else {
    Seam.__validationMessages = new Seam.Map();
  }
  return b;
};

/*
 * This method parses the response from server and attaches all validation metadata to their corresponding
 * client-side beans.
 */
Seam.processMetaData = function(doc) {
  var cn = Seam.Xml.childNode;
  var cns = Seam.Xml.childNodes;
  if (typeof(Seam) == "undefined") return;
  if (!doc.documentElement) return;

  var violations = []; // for storing any possible ConstraintViolation
  var validationNode = cn(cn(doc.documentElement, "body"),"validation");

  var messagesNode = (cn(validationNode, "messages"));
  var messages = cns(messagesNode , "m");

  for(var i=0;i<messages.length;i++) {
    Seam.__validationMessages.put(messages[i].getAttribute("c") , messages[i].getAttribute("msg"));
  }

  var beans = cns(validationNode , "b");

  for (var i=0;i<beans.length;i++) {
    var beanMetaData = Seam.getBean(beans[i].getAttribute("n"));
    if (beanMetaData == undefined) continue;
    beanMetaData.__constraints  = new Seam.Map();
    var properties = cns(beans[i] , "p");
    for(var j=0;j<properties.length;j++) {
      var propName = properties[j].getAttribute("n");
      var constraints = cns(properties[j] , "c");
      if(constraints.length > 0) {
        var cc = [];
        for(var k=0;k<constraints.length;k++) {
          var constraint = function() {};
          constraint.cname = constraints[k].getAttribute("n");
          var parent = constraints[k].getAttribute("parent");
          if(parent != undefined) // if it is an composed constraint
            constraint.parent = parent;
            var params = cns(constraints[k] , "pm");
            if(params.length > 0) {
              constraint.params = [];
              for(y=0;y<params.length;y++) {
                var param = function() {};
                param.pname  = params[y].getAttribute("n");
                param.value = params[y].getAttribute("v");
                constraint.params.push(param);
              }
            }
            var groups = cns(constraints[k] , "g");
            if(groups.length > 0) {
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
    if(ghs.length > 0) {
      beanMetaData.__groupHierarchy = [];
      for(var k=0;k<ghs.length;k++) {
        var groupHierarchy = function() {};
        groupHierarchy.gname = ghs[k].getAttribute("n");
        groupHierarchy.id   = ghs[k].getAttribute("id");
        var parents = cns(ghs[k] , "gp");
        if(parents.length > 0) {
          groupHierarchy.parents = [];
          for(var y=0;y<parents.length;y++) {
            var pname = parents[y].getAttribute("n");
            var parent = function() {};
            if(pname != undefined) {
              parent.gname = pname;
            } else {
              parent.id = parents[y].getAttribute("id");
            }
            groupHierarchy.parents.push(parent);
          }
        }
        beanMetaData.__groupHierarchy.push(groupHierarchy);
      }
    }
  }
  Seam.executeValidation();
};

Seam.validateProperty = function(bean , property , callBack , groups) {
  var p = [];
  p.push(property);
  Seam.validateBean(bean, callBack, groups , p);
};

Seam.validateBean = function(object , callBack , groups, props){
	Seam.validate([{bean: object,
		            properties: props ,
		            traverse: true}] , callBack , groups);
};

Seam.validate = function(beansList , callBack, groups){
   var flag = false;	
   var tempList = []; 
   if(!(beansList instanceof Array))
	 beansList = [beansList];  
   for(var i=0;i<beansList.length;i++){
	 var listItem   = beansList[i];  
	 var bean       = listItem[Seam.validation_bean];  
	 if(bean == undefined){
		Seam.log("Error: Invalid Argument [bean must be provided].");
		continue;
	 }
	 if(!listItem[Seam.validation_traverse]) ///// default is true
	   listItem[Seam.validation_traverse] = true;	
	 if(!listItem[Seam.validation_groups] && groups)
	   listItem[Seam.validation_groups] = groups;
	 if(listItem[Seam.validation_traverse] == true){  
	   var properties = listItem[Seam.validation_props];
	   var beans = Seam.getObjectGraph(bean, true, [] ,properties);		
	    for(var j=0;j<beans.length;j++){	
	      if(!groups && !listItem[Seam.validation_groups])	
		    tempList.push({bean: beans[j]});
	      else{
	    	var gr = !listItem[Seam.validation_groups] ? groups : listItem[Seam.validation_groups];   
	    	tempList.push({bean: beans[j] , groups: gr});
	      }
	      if(Seam.getBeanType(beans[j]).__constraints == undefined) 
	    	flag = true;
	    }
	 }
	 if(Seam.getBeanType(bean).__constraints == undefined) 
	   flag = true;
   }////End of outer loop
   
   beansList = beansList.concat(tempList);
   if(beansList.length > 0) {
	 Seam.__validationCallback = callBack;
	 Seam.__validationBeans    = beansList;
	   
     // There is at least one bean for which we need to fetch its metadata from server and then
     // we will do it for all other beans as well, even if they have got it already...
     if(flag) {
       var data = Seam.createValidationRequest();
       var envelope = Seam.createEnvelope("", data);
       var processorProcedure = function(doc) { Seam.processMetaData(doc); };
       Seam.sendAjaxRequest(envelope, Seam.PATH_VALIDATION, processorProcedure , false);
     } 
     else {
       Seam.executeValidation();
     }
   } 
   else{
	Seam.__validationCallback = undefined;
	Seam.__validationBeans    = undefined;   
   }   
};

Seam.getObjectGraph = function(bean, recursive, graph, properties){
	if(graph.length > 0){
	  for(var i=0;i<graph.length;i++)
		if(graph[i] == bean)
		  return [];	
	}
	graph.push(bean);
	var beans = [];
	var meta = [];
	var metaData = Seam.getBeanMetadata(bean);
	if(recursive){                   
		if(properties != undefined){
		  for(var i=0;i<properties.length;i++){
			for(var j=0;j<metaData.length;j++){
			  if(metaData[j].field == properties[i]){
				meta.push(metaData[j]);
				break;
			   }	
			 }
		  }
		}
		else{
		 meta = metaData;	
		}
		for(var i=0;i<meta.length;i++){
		  if(bean[meta[i].field] != undefined){	
		    if(meta[i].type == "bean"){
		      beans.push(bean[meta[i].field]);	
		      beans = beans.concat(Seam.getObjectGraph(bean[meta[i].field] , recursive, graph));
		    }
		    else{
		      var values = [];
		      if(meta[i].type == "bag" && bean[meta[i].field].length > 0)
		    	values = bean[meta[i].field];
		      else if(meta[i].type == "map" && bean[meta[i].field].size() > 0)
		    	values = bean[meta[i].field].values();  
		      	
		      for(var k=0;k<values.length;k++){
		    	beans.push(values[k]);	  
		    	beans = beans.concat(Seam.getObjectGraph(values[k], recursive, graph));
		      }
		    } 
		  }	  
		}   
	} ///////End Of Outer IF
  return beans;	
};

/* this method walks through all constraints and call validateAgainstConstraint method for each one of them.
 */
Seam.executeValidation = function() {
  var beansList =  Seam.__validationBeans;	
  var violations = [];
  for (var i=0;i<beansList.length;i++) {
	var listItem = beansList[i];  
	var bean     = listItem[Seam.validation_bean];
	var props    = listItem[Seam.validation_props];
	var groups   = listItem[Seam.validation_groups];
    var metadata = Seam.getBeanType(bean);
    var constraints = metadata.__constraints; 
    var keys = [];
    if(props != undefined){ // see if we have to validate the whole bean or not...
      if(typeof props == "string")
    	keys.push(props);
      else
        keys = props;
    } 
    else{
      keys = constraints.keySet();
    }
    for(var j=0;j<keys.length;j++) {
      var cc = constraints.get(keys[j]);
      if(cc == undefined)
    	 continue; 
      for(var k=0;k<cc.length;k++) {
        var flag = false;
        if(groups == undefined) groups = Seam.ValidationGroups;
        if(groups == undefined) {
          groups = ["Default"]; // if no groups provided, will be validated against Default Group
        } else if (typeof groups == "string") {
          groups = [groups];
        }
        var cgroups = cc[k].groups;
        if(cc[k].parent != undefined) {
          cgroups = Seam.findElement(cc[k].parent , cc).groups;
        }
        if(cgroups == undefined) { //Default Group
          for(var y=0;y<groups.length;y++) {
            if(Seam.groupExistInHierarchy(metadata , groups[y] , "Default")) {
              flag = true;
              break;
            }
          }
        } else {
          for(var y=0;y<groups.length;y++) {
            for(var x=0;x<cgroups.length;x++) {
              var groupName = Seam.getGroupName(metadata , cgroups[x]);
              if(Seam.groupExistInHierarchy(metadata , groups[y] , groupName)) {
                flag = true;
                break;
              }
            }
            if(flag) break;
          }
        }
        if(flag) {
          var result = Seam.validateAgainstConstraint(bean , keys[j] , cc[k]);
          if(result != -1) violations.push(result);
        }
      }
    }
  }
  Seam.__validationCallback(violations);
};

Seam.findElement = function(element , array) {
  for(var i=0;i<array.length;i++) {
    if(array[i].cname == element) return array[i];
  }
  return null;
};

Seam.getGroupName = function(bean , id) {
  if (bean.__groupHierarchy != undefined) {
    for(var i=0;i<bean.__groupHierarchy.length;i++) {
      if(bean.__groupHierarchy[i].id == id) return bean.__groupHierarchy[i].gname;
    }
  }
  return null;
};

Seam.groupExistInHierarchy = function(bean , childNode , name) {
  if(name == childNode) return true;
  if(bean.__groupHierarchy != undefined) {
    for(var i=0;i<bean.__groupHierarchy.length;i++) {
      if(bean.__groupHierarchy[i].gname == childNode) {
        var parents = bean.__groupHierarchy[i].parents;
        if(parents != undefined) {
          for(var j=0;j<parents.length;j++) {
            var parent = parents[j];
            var pname = !parent.gname ? Seam.getGroupName(bean,parent.id) : parent.gname;
            if (Seam.groupExistInHierarchy(bean , pname , name)) return true;
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
Seam.addSubGroup = function(bean , subGroupName , superGroupName) {
  metaData = Seam.getBeanType(bean);
  if (metadata.__groupHierarchy != undefined) {
    var id = -1;
    for(var group in metadata.__groupHierarchy) {
      if(group.gname == superGroupName) {
        id = group.id;
        break;
      }
    }
    if(id == -1) return false;
    hierarchyNode = function() {};
    hierarchyNode.gname = subGroupName;
    hierarchyNode.id   = 50 + id;  // I can't imagine a situation where there are more than 50 groups...
                                  // and well, if it occurs we are in trouble ;) ,since the whole algorithm sucks for anything more than a dozen groups
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
  var validator = Validation.getValidator(constraint.cname);
  if(validator == null) {
    Seam.log("Validator for the requested constraint is not available. ["+constraint.cname+"]");
  } else { // get all properties if there is any
    if(validator.properties != null) {
      if(constraint.params != undefined) {
        var properties = [];
        for(var i=0;i<constraint.params.length;i++) {
          for(var j=0;j<validator.properties.length;j++) {
            if(constraint.params[i].pname.toLowerCase() == validator.properties[j].toLowerCase()) {
              // injecting all attributes so they will be available inside validator
              validator[validator.properties[j]] = constraint.params[i].value;
              properties.push(constraint.params[i]);
              break;
            }
          }
        }
      } else { //make sure old values will not be used
        for(var i=0;i<validator.properties.length;i++) {
          validator[validator.properties[i]] = undefined;
        }
      }
    }

    validator.currentValue = bean[property];
    if (validator.currentValue == "")
      validator.currentValue = null;
      var valid = validator.isValid();
      if(!valid) {
        var message = Seam.getValidationMessage(constraint , properties);
        if (message.indexOf("?value?") != -1) message = message.replace("?value?" , validator.currentValue);
        return new ConstraintViolation(bean, property, validator.currentValue, message);
      }
    }
  return -1; // no Violation
};

Seam.getValidationMessage = function(constraint , attrs) {
  var name = constraint.cname;
  var keys = Seam.__validationMessages.keySet();
  if (constraint.parent != undefined) {
    name = constraint.parent; ////if it is an composed constraint, its parent's message will be used instead
  } else if(attrs != null && attrs.length > 0) {
    name = name + "_["+attrs[0].pname+":"+attrs[0].value;
    for(var i=1;i<attrs.length;i++) {
      name += ","+attrs[i].pname+":"+attrs[i].value;
    }
    name += "]";
  }
  for(var i=0;i<keys.length;i++) {
    if(name == keys[i]) return Seam.__validationMessages.get(keys[i]);
  }
  return "Validation has failed. [@"+name+"]";
};

// ConstraintViolation
function ConstraintViolation(bean , propertyName , currentValue , message) {
  this.bean = bean;
  this.property = propertyName;
  this.value = currentValue;
  this.message = message;
};

// Validator
function Validator(constName , props , validateLogic) {
  this.constraintName = constName.toLowerCase();
  this.isValid = validateLogic;
  this.properties = props;
};

var Validation = {
  validators : []
};

Validation.getValidator = function(constraintName) {
  for(var i=0;i<this.validators.length;i++) {
    if(this.validators[i].constraintName == constraintName.toLowerCase()) return this.validators[i];
  }
  return null;
};

Validation.registerValidator = function(validator) {
  if (this.getValidator(validator.constraintName) == null) this.validators.push(validator);
};

// Validators for JSR-303 Built-in Constraints
Validation.registerValidator(new Validator("Null" , null , function() {
  return this.currentValue == null; // currentValue will be injected to each Validator object during validation
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
	if(this.currentValue == null) 
	  return true;
	if(this.currentValue instanceof Seam.Map)
	  return (this.min <= this.currentValue.size() && this.currentValue.size() <= this.max);	
	
   return (this.min <= this.currentValue.length && this.currentValue.length <= this.max);
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
  if(this.currentValue == null) return true;
  var regex = null;
  if(this.flags == undefined) {
    regex = new RegExp(this.regexp);
  } else {
    regex = new RegExp(this.regexp , eval(this.flags));
  }
  return regex.test(this.currentValue);
}) );

// Validators for Hibernate Validator built-in constraints
Validation.registerValidator(new Validator("CreditCardNumber" , null , function() {
  cc_array = this.currentValue.split( " " ); // Luhn Algorithm
  cc_array.reverse();
  digit_string = " ";
  for (var counter=0; counter < cc_array.length; counter++ ) {
    current_digit = parseInt( cc_array[counter] );
    if (counter %2 != 0) cc_array[counter] *= 2;
    digit_string += cc_array[counter];
  }
  digit_sum = 0;
  for (var counter=0; counter<digit_string.length; counter++ ) {
    current_digit = parseInt( digit_string.charAt(counter) );
    digit_sum += current_digit;
  }
  return ( digit_sum % 10 == 0 );
}) );

Validation.registerValidator(new Validator("Email" , null , function() {
  var pattern = /^[a-zA-Z0-9]+[a-zA-Z0-9_.-]+[a-zA-Z0-9_-]+@[a-zA-Z0-9]+[a-zA-Z0-9.-]+[a-zA-Z0-9]+.[a-z]{2,4}$/;
  return pattern.test(this.currentValue);
}) );

Validation.registerValidator(new Validator("Url" , null , function() {
  var pattern = /(ftp|http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/;
  return pattern.test(this.currentValue);
}) );
