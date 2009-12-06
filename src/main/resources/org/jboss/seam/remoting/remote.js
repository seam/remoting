var Seam = new Object();
Seam.Remoting = new Object();
Seam.Remoting.beans = new Array();

Seam.Remoting.instance = function(name) {
  var b = Seam.Remoting.beans;
  for (var i = 0; i < b.length; i++) {
    if (b[i].__name == name) {
      var v = new b[i];
      if (arguments.length > 1) {
        v.__qualifiers = new Array();
        for (var j = 1; j < arguments.length; j++) { 
          v.__qualifiers.push(arguments[j]);
        }
      }
      return v;
    }
  }
  return null;
}

Seam.Remoting.getBeanType = function(obj) {
  var b = Seam.Remoting.beans;
  for (var i = 0; i < b.length; i++) {
    if (obj instanceof b[i]) return b[i];
  }
  return null;
}

Seam.Remoting.getBeanName = function(obj) {
  var t = Seam.Remoting.getBeanType(obj);
  return t ? t.__name : null;
}

Seam.Remoting.registerBean = function(bean) {
  var b = Seam.Remoting.beans;
  for (var i = 0; i < b.length; i++) {
    if (b[i].__name == bean.__name) {
      b[i] = bean;
      return;
    }
  }
  b.push(bean);
}

Seam.Remoting.isBeanRegistered = function(name) {
  var b = Seam.Remoting.beans;
  for (var i = 0; i < b.length; i++) {
    if (b[i].__name == name)
      return true;
  }
  return false;
}

Seam.Remoting.getBeanMetadata = function(obj) {
  var b = Seam.Remoting.beans;
  for (var i = 0; i < b.length; i++) {
    if (obj instanceof b[i]) return b[i].__metadata;
  }
  return null;
}

Seam.Remoting.extractEncodedSessionId = function(url) {
	var sid = null;
  if (url.indexOf(';jsessionid=') >= 0) {
    var qpos = url.indexOf('?');
    sid = url.substring(url.indexOf(';jsessionid=') + 12, qpos >= 0 ? qpos : url.length); 
  }
	return sid;
}

Seam.Remoting.PATH_EXECUTE = "/execute";
Seam.Remoting.PATH_SUBSCRIPTION = "/subscription";
Seam.Remoting.PATH_MODEL = "/model";
Seam.Remoting.PATH_POLL = "/poll";

Seam.Remoting.encodedSessionId = Seam.Remoting.extractEncodedSessionId(window.location.href);

Seam.Remoting.type = new Object(); // namespace
Seam.Remoting.types = new Array(); 

Seam.Remoting.debug = false;
Seam.Remoting.debugWindow = null;

Seam.Remoting.setDebug = function(val) {
  Seam.Remoting.debug = val;
}

Seam.Remoting.log = function(msg) {
  if (!Seam.Remoting.debug) return;

  if (!Seam.Remoting.debugWindow || Seam.Remoting.debugWindow.document == null) {
    var attr = "left=400,top=400,resizable=yes,scrollbars=yes,width=400,height=400";
    Seam.Remoting.debugWindow = window.open("", "__seamDebugWindow", attr);
    if (Seam.Remoting.debugWindow) {
      Seam.Remoting.debugWindow.document.write("<html><head><title>Seam Debug Window</title></head><body></body></html>");
      var t = Seam.Remoting.debugWindow.document.getElementsByTagName("body").item(0);
      t.style.fontFamily = "arial";
      t.style.fontSize = "8pt";
    }
  }

  if (Seam.Remoting.debugWindow) {
    Seam.Remoting.debugWindow.document.write("<pre>" + (new Date()) + ": " + msg.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;") + "</pre><br/>");
  }
}

Seam.Remoting.createNamespace = function(namespace) {
  var p = namespace.split(".");
  var b = Seam.Remoting.type;
  
  for(var i = 0; i < p.length; i++) {
    if (typeof b[p[i]] == "undefined") b[p[i]] = new Object();
    b = b[p[i]];
  }
}

Seam.Remoting.__Context = function() {
  this.conversationId = null;
  Seam.Remoting.__Context.prototype.setConversationId = function(conversationId) {
    this.conversationId = conversationId;
  }

  Seam.Remoting.__Context.prototype.getConversationId = function() {
    return this.conversationId;
  }
}

Seam.Remoting.Exception = function(msg) {
  this.message = msg;
  Seam.Remoting.Exception.prototype.getMessage = function() {
    return this.message;
  }
}

Seam.Remoting.context = new Seam.Remoting.__Context();

Seam.Remoting.getContext = function() {
  return Seam.Remoting.context;
}

Seam.Remoting.Map = function() {
  this.elements = new Array();

  Seam.Remoting.Map.prototype.size = function() {
    return this.elements.length;
  }

  Seam.Remoting.Map.prototype.isEmpty = function() {
    return this.elements.length == 0;
  }

  Seam.Remoting.Map.prototype.keySet = function() {
    var keySet = new Array();
    for (var i = 0; i < this.elements.length; i++) {
      keySet[keySet.length] = this.elements[i].key;
    }
    return keySet;
  }

  Seam.Remoting.Map.prototype.values = function() {
    var values = new Array();
    for (var i = 0; i < this.elements.length; i++) {
      values[values.length] = this.elements[i].value;
    }
    return values;
  }

  Seam.Remoting.Map.prototype.get = function(key) {
    for (var i = 0; i < this.elements.length; i++) {
      if (this.elements[i].key == key) return this.elements[i].value;
    }
    return null;
  }

  Seam.Remoting.Map.prototype.put = function(key, value) {
    for (var i = 0; i < this.elements.length; i++) {
      if (this.elements[i].key == key) {
        this.elements[i].value = value;
        return;
      }
    }
    this.elements.push({key:key,value:value});
  }

  Seam.Remoting.Map.prototype.remove = function(key) {
    for (var i = 0; i < this.elements.length; i++) {
      if (this.elements[i].key == key)
        this.elements.splice(i, 1);
    }
  }

  Seam.Remoting.Map.prototype.contains = function(key) {
    for (var i = 0; i < this.elements.length; i++) {
      if (this.elements[i].key == key) return true;
    }
    return false;
  }
}

Seam.Remoting.registerType = function(type) {
  for (var i = 0; i < Seam.Remoting.types.length; i++) {
    if (Seam.Remoting.types[i].__name == type.__name) {
      Seam.Remoting.types[i] = type;
      return;
    }
  }
  Seam.Remoting.types.push(type);
}

Seam.Remoting.createType = function(name) {
  for (var i = 0; i < Seam.Remoting.types.length; i++) {
    if (Seam.Remoting.types[i].__name == name)
      return new Seam.Remoting.types[i];
  }
}

Seam.Remoting.getType = function(obj) {
  for (var i = 0; i < Seam.Remoting.types.length; i++) {
    if (obj instanceof Seam.Remoting.types[i]) return Seam.Remoting.types[i];
  }
  return null;
}

Seam.Remoting.getTypeName = function(obj) {
  var type = Seam.Remoting.getType(obj);
  return type ? type.__name : null;
}

Seam.Remoting.getMetadata = function(obj) {
  for (var i = 0; i < Seam.Remoting.types.length; i++) {
    if (obj instanceof Seam.Remoting.types[i])
      return Seam.Remoting.types[i].__metadata;
  }
  return null;
}

Seam.Remoting.serializeValue = function(value, type, refs) {
  if (value == null) {
    return "<null/>";
  }
  else if (type) {
    switch (type) {
      case "bool": return "<bool>" + (value ? "true" : "false") + "</bool>";
      case "number": return "<number>" + value + "</number>";
      case "date": return Seam.Remoting.serializeDate(value);
      case "bean": return Seam.Remoting.getTypeRef(value, refs);
      case "bag": return Seam.Remoting.serializeBag(value, refs);
      case "map": return Seam.Remoting.serializeMap(value, refs);
      default: return "<str>" + encodeURIComponent(value) + "</str>";
    }
  }
  else {
    switch (typeof(value)) {
      case "number":
        return "<number>" + value + "</number>";
      case "boolean":
        return "<bool>" + (value ? "true" : "false") + "</bool>";
      case "object":
        if (value instanceof Array)
          return Seam.Remoting.serializeBag(value, refs);
        else if (value instanceof Date)
          return Seam.Remoting.serializeDate(value);
        else if (value instanceof Seam.Remoting.Map)
          return Seam.Remoting.serializeMap(value, refs);
        else
          return Seam.Remoting.getTypeRef(value, refs);
      default:
        return "<str>" + encodeURIComponent(value) + "</str>";
    }
  }
}

Seam.Remoting.serializeBag = function(value, refs) {
  var d = "<bag>";
  for (var i = 0; i < value.length; i++) {
    d += "<element>";
    d += Seam.Remoting.serializeValue(value[i], null, refs);
    d += "</element>";
  }
  d += "</bag>";
  return d;
}

Seam.Remoting.serializeMap = function(value, refs) {
  var d = "<map>";
  var keyset = value.keySet();
  for (var i = 0; i < keyset.length; i++) {
    d += "<element><k>";
    d += Seam.Remoting.serializeValue(keyset[i], null, refs);
    d += "</k><v>";
    d += Seam.Remoting.serializeValue(value.get(keyset[i]), null, refs);
    d += "</v></element>";
  }

  d += "</map>";
  return d;
}

Seam.Remoting.serializeDate = function(value) {
  var zeroPad = function(val, digits) { while (("" + val).length < digits) val = "0" + val; return val; };

  return "<date>" + value.getFullYear() + zeroPad(value.getMonth() + 1, 2) + zeroPad(value.getDate(), 2) +
    zeroPad(value.getHours(), 2) + zeroPad(value.getMinutes(), 2) + zeroPad(value.getSeconds(), 2) +
    zeroPad(value.getMilliseconds(), 3) +"</date>";
}

Seam.Remoting.getTypeRef = function(obj, refs) {
  var refId = -1;
  for (var i = 0; i < refs.length; i++) {
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
}

Seam.Remoting.serializeType = function(obj, refs) {
  var d = "<bean type=\"";
  var t = Seam.Remoting.getBeanType(obj);
  var isComponent = t != null;
  if (!isComponent) t = Seam.Remoting.getType(obj);

  if (!t) {
    alert("Unknown Type error.");
    return null;
  }

  d += t.__name;
  d += "\">\n";

  var meta = isComponent ? Seam.Remoting.getBeanMetadata(obj) : Seam.Remoting.getMetadata(obj);
  for (var i = 0; i < meta.length; i++) {
    d += "<member name=\"";
    d += meta[i].field;
    d += "\">";
    d += Seam.Remoting.serializeValue(obj[meta[i].field], meta[i].type, refs);
    d += "</member>\n";
  }

  d += "</bean>";

  return d;
}

Seam.Remoting.__callId = 0;

Seam.Remoting.createCall = function(component, methodName, params, callback, exceptionHandler) {
  var callId = "" + Seam.Remoting.__callId++;
  if (!callback) callback = component.__callback[methodName];

  var d = "<call id=\"" + callId + "\">\n";   
  d += "<target>" + Seam.Remoting.getBeanType(component).__name + "</target>";
  
  if (component.__qualifiers != null) {
    d += "<qualifiers>";    
    for (var i = 0; i < component.__qualifiers.length; i++) {
      d += component.__qualifiers[i];
      if (i < component.__qualifiers.length - 1) data += ",";
    }
    d += "</qualifiers>";
  }
  
  d += "<method>" + methodName + "</method>";
  d += "<params>";

  var refs = new Array();
  for (var i = 0; i < params.length; i++) {
    d += "<param>";
    d += Seam.Remoting.serializeValue(params[i], null, refs);
    d += "</param>";
  }

  d += "</params>";
  d += "<refs>";
  for (var i = 0; i < refs.length; i++) {
    d += "<ref id=\"" + i + "\">";
    d += Seam.Remoting.serializeType(refs[i], refs);
    d += "</ref>";
  }
  d += "</refs>";
  d += "</call>";

  return {data: d, id: callId, callback: callback, exceptionHandler: exceptionHandler};
}

Seam.Remoting.createHeader = function() {
  var h = "<context>";
  if (Seam.Remoting.getContext().getConversationId()) {
    h += "<conversationId>";
    h += Seam.Remoting.getContext().getConversationId();
    h += "</conversationId>";
  }
  h += "</context>";
  return h;
}

Seam.Remoting.createEnvelope = function(header, body) {
  var d = "<envelope>";
  if (header) d += "<header>" + header + "</header>";
  if (body) d += "<body>" + body + "</body>";
  d += "</envelope>";
  return d;
}

Seam.Remoting.pendingCalls = new Seam.Remoting.Map();
Seam.Remoting.inBatch = false;
Seam.Remoting.batchedCalls = new Array();

Seam.Remoting.startBatch = function() {
  Seam.Remoting.inBatch = true;
  Seam.Remoting.batchedCalls.length = 0;
}

Seam.Remoting.executeBatch = function() {
  if (!Seam.Remoting.inBatch) return;

  var d = "";
  for (var i = 0; i < Seam.Remoting.batchedCalls.length; i++) {
    Seam.Remoting.pendingCalls.put(Seam.Remoting.batchedCalls[i].id, Seam.Remoting.batchedCalls[i]);
    d += Seam.Remoting.batchedCalls[i].data;
  }

  var envelope = Seam.Remoting.createEnvelope(Seam.Remoting.createHeader(), d);
  Seam.Remoting.batchAsyncReq = Seam.Remoting.sendAjaxRequest(envelope, Seam.Remoting.PATH_EXECUTE,
    Seam.Remoting.processResponse, false);
  Seam.Remoting.inBatch = false;
}

Seam.Remoting.cancelBatch = function() {
  Seam.Remoting.inBatch = false;
  for (var i = 0; i < Seam.Remoting.batchedCalls.length; i++) {
    Seam.Remoting.pendingCalls.remove(Seam.Remoting.batchedCalls[i].id);
  }
}

Seam.Remoting.cancelCall = function(callId) {
  var c = Seam.Remoting.pendingCalls.get(callId);
  Seam.Remoting.pendingCalls.remove(callId);
  if (c && c.asyncReq) {
    if (Seam.Remoting.pendingCalls.isEmpty()) Seam.Remoting.hideLoadingMessage();
    window.setTimeout(function() {
      c.asyncReq.onreadystatechange = function() {};
    }, 0);
    c.asyncReq.abort();
  }
}

Seam.Remoting.execute = function(component, methodName, params, callback, exceptionHandler) {
  var c = Seam.Remoting.createCall(component, methodName, params, callback, exceptionHandler);
  if (Seam.Remoting.inBatch) {
    Seam.Remoting.batchedCalls[Seam.Remoting.batchedCalls.length] = c;
  }
  else {
    var envelope = Seam.Remoting.createEnvelope(Seam.Remoting.createHeader(), c.data);
    Seam.Remoting.pendingCalls.put(c.id, c);
    Seam.Remoting.sendAjaxRequest(envelope, Seam.Remoting.PATH_EXECUTE, Seam.Remoting.processResponse, false);
  }
  return c;
}

Seam.Remoting.sendAjaxRequest = function(envelope, path, callback, silent)
{
  Seam.Remoting.log("Request packet:\n" + envelope);
  if (!silent) Seam.Remoting.displayLoadingMessage();    
  var r;
  if (window.XMLHttpRequest) {
    r = new XMLHttpRequest();
    if (r.overrideMimeType) r.overrideMimeType('text/xml');
  }
  else {
    r = new ActiveXObject("Microsoft.XMLHTTP");
  }

  r.onreadystatechange = function() {
    if (r.readyState == 4) {
      var inScope = typeof(Seam) == "undefined" ? false : true;      
      if (inScope) Seam.Remoting.hideLoadingMessage();
        
      if (r.status == 200) {
        // We do this to avoid a memory leak
        window.setTimeout(function() {
          r.onreadystatechange = function() {};
        }, 0);      
      
        if (inScope) Seam.Remoting.log("Response packet:\n" + r.responseText);
  
        if (callback) {
          // The following code deals with a Firefox security issue.  It reparses the XML
          // response if accessing the documentElement throws an exception
          try {         
            r.responseXML.documentElement;
            callback(r.responseXML);
          }
          catch (ex) {
             try {
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
      }
      else {
        Seam.Remoting.displayError(r.status);
      }
    }    
  }

  if (Seam.Remoting.encodedSessionId) {
    path += ';jsessionid=' + Seam.Remoting.encodedSessionId;
  }
    
  r.open("POST", Seam.Remoting.resourcePath + path, true);
  r.send(envelope);
}

Seam.Remoting.displayError = function(code) {
  alert("There was an error processing your request.  Error code: " + code);  
}

Seam.Remoting.setCallback = function(component, methodName, callback) {
  component.__callback[methodName] = callback;
}

Seam.Remoting.processResponse = function(doc) {
  var headerNode;
  var bodyNode;
  var inScope = typeof(Seam) == "undefined" ? false : true;
  if (!inScope) return;    
  
  var context = new Seam.Remoting.__Context;

  if (doc.documentElement) {
    for (var i = 0; i < doc.documentElement.childNodes.length; i++) {
      var node = doc.documentElement.childNodes.item(i);
      if (node.tagName == "header")
        headerNode = node;
      else if (node.tagName == "body")
        bodyNode = node;
    }
  }

  if (headerNode) {
    var contextNode;
    for (var i = 0; i < headerNode.childNodes.length; i++) {
      var node = headerNode.childNodes.item(i);
      if (node.tagName == "context") {
        contextNode = node;
        break;
      }
    }
    if (contextNode && context) {
      Seam.Remoting.unmarshalContext(contextNode, context);
      if (context.getConversationId() && Seam.Remoting.getContext().getConversationId() == null)
        Seam.Remoting.getContext().setConversationId(context.getConversationId());
    }
  }

  if (bodyNode) {
    for (var i = 0; i < bodyNode.childNodes.length; i++) {
      var n = bodyNode.childNodes.item(i);
      if (n.tagName == "result") Seam.Remoting.processResult(n, context);
    }
  }
}

Seam.Remoting.processResult = function(result, context) {
  var callId = result.getAttribute("id");
  var call = Seam.Remoting.pendingCalls.get(callId);
  Seam.Remoting.pendingCalls.remove(callId);

  if (call && (call.callback || call.exceptionHandler)) {
    var valueNode = null;
    var refsNode = null;
    var exceptionNode = null;

    var c = result.childNodes;
    for (var i = 0; i < c.length; i++) {
      var tag = c.item(i).tagName;
      if (tag == "value")
        valueNode = c.item(i);
      else if (tag == "refs")
        refsNode = c.item(i);
      else if (tag == "exception")
        exceptionNode = c.item(i);
    }

    if (exceptionNode != null) {
      var msgNode = null;
      var c = exceptionNode.childNodes;
      for (var i = 0; i < c.length; i++) {
        var tag = c.item(i).tagName;
        if (tag == "message")
          msgNode = c.item(i); 
      }
      
      var msg = Seam.Remoting.unmarshalValue(msgNode.firstChild);
      var ex = new Seam.Remoting.Exception(msg);
      call.exceptionHandler(ex); 
    }
    else {
      var refs = new Array();
      if (refsNode) Seam.Remoting.unmarshalRefs(refsNode, refs);
      var value = Seam.Remoting.unmarshalValue(valueNode.firstChild, refs);
      call.callback(value, context, callId);
    }
  }
}

Seam.Remoting.unmarshalContext = function(ctxNode, context) {
  for (var i = 0; i < ctxNode.childNodes.length; i++) {
    var tag = ctxNode.childNodes.item(i).tagName;
    if (tag == "conversationId") context.setConversationId(ctxNode.childNodes.item(i).firstChild.nodeValue);
  }
}

Seam.Remoting.unmarshalRefs = function(refsNode, refs) {
  var objs = new Array();

  for (var i = 0; i < refsNode.childNodes.length; i++) {
    if (refsNode.childNodes.item(i).tagName == "ref") {
      var refNode = refsNode.childNodes.item(i);
      var refId = parseInt(refNode.getAttribute("id"));

      var valueNode = refNode.firstChild;
      if (valueNode.tagName == "bean") {
        var obj = null;
        var typeName = valueNode.getAttribute("type");
        if (Seam.Remoting.isBeanRegistered(typeName))
          obj = Seam.Remoting.instance(typeName);
        else
          obj = Seam.Remoting.createType(typeName);
        if (obj) {
          refs[refId] = obj;
          objs[objs.length] = {obj: obj, node: valueNode};
        }
      }
    }
  }

  for (var i = 0; i < objs.length; i++) {
    for (var j = 0; j < objs[i].node.childNodes.length; j++) {
      var child = objs[i].node.childNodes.item(j);
      if (child.tagName == "member") {
        var name = child.getAttribute("name");
        objs[i].obj[name] = Seam.Remoting.unmarshalValue(child.firstChild, refs);
      }
    }
  }
}

Seam.Remoting.unmarshalValue = function(element, refs) {
  var tag = element.tagName;

  switch (tag) {
    case "bool": return element.firstChild.nodeValue == "true";
    case "number":
      if (element.firstChild.nodeValue.indexOf(".") == -1)
        return parseInt(element.firstChild.nodeValue);
      else
        return parseFloat(element.firstChild.nodeValue);
    case "str":
      var data = "";
      for (var i = 0; i < element.childNodes.length; i++) {
        if (element.childNodes[i].nodeType == 3)
          data += element.childNodes[i].nodeValue;
      }
      return decodeURIComponent(data);
    case "ref": return refs[parseInt(element.getAttribute("id"))];
    case "bag":
      var value = new Array();
      for (var i = 0; i < element.childNodes.length; i++) {
        if (element.childNodes.item(i).tagName == "element")
          value[value.length] = Seam.Remoting.unmarshalValue(element.childNodes.item(i).firstChild, refs);
      }
      return value;
    case "map":
      var map = new Seam.Remoting.Map();
      for (var i = 0; i < element.childNodes.length; i++) {
        var childNode = element.childNodes.item(i);
        if (childNode.tagName == "element") {
          var key = null
          var value = null;

          for (var j = 0; j < childNode.childNodes.length; j++) {
            if (key == null && childNode.childNodes.item(j).tagName == "k")
              key = Seam.Remoting.unmarshalValue(childNode.childNodes.item(j).firstChild, refs);
            else if (value == null && childNode.childNodes.item(j).tagName == "v")
              value = Seam.Remoting.unmarshalValue(childNode.childNodes.item(j).firstChild, refs);
          }

          if (key != null) map.put(key, value);
        }
      }
      return map;
    case "date": return Seam.Remoting.deserializeDate(element.firstChild.nodeValue);
    default: return null;
  }
}

Seam.Remoting.deserializeDate = function(val) {
  var dte = new Date();
  dte.setFullYear(parseInt(val.substring(0,4), 10),
                  parseInt(val.substring(4,6), 10) - 1,
                  parseInt(val.substring(6,8), 10));
  dte.setHours(parseInt(val.substring(8,10), 10));
  dte.setMinutes(parseInt(val.substring(10,12), 10));
  dte.setSeconds(parseInt(val.substring(12,14), 10));
  dte.setMilliseconds(parseInt(val.substring(14,17), 10));
  return dte;
}

Seam.Remoting.loadingMsgDiv = null;
Seam.Remoting.loadingMessage = "Please wait...";
Seam.Remoting.displayLoadingMessage = function() {
  if (!Seam.Remoting.loadingMsgDiv) {
    Seam.Remoting.loadingMsgDiv = document.createElement('div');
    var d = Seam.Remoting.loadingMsgDiv;
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

    var text = document.createTextNode(Seam.Remoting.loadingMessage);
    d.appendChild(text);
  }
  else {
    Seam.Remoting.loadingMsgDiv.innerHTML = Seam.Remoting.loadingMessage;
    Seam.Remoting.loadingMsgDiv.style.visibility = 'visible';
  }
}

Seam.Remoting.hideLoadingMessage = function() {
  if (Seam.Remoting.loadingMsgDiv)
    Seam.Remoting.loadingMsgDiv.style.visibility = 'hidden';
}

/* Remote Model API */

Seam.Remoting.Action = function() {
	this.beanType = null;
	this.qualifiers = null;
	this.method = null;
	this.params = new Array();
	this.expression = null;
		
	Seam.Remoting.Action.prototype.setBeanType = function(beanType) {
		this.beanType = beanType;
		return this;
  }
  
  Seam.Remoting.Action.prototype.setQualifiers = function(qualifiers) {
  	this.qualifiers = qualifiers;
  	return this;
  }
  
  Seam.Remoting.Action.prototype.setMethod = function(method) { 
  	this.method = method;
  	return this;
  }
  
  Seam.Remoting.Action.prototype.addParam = function(param) {
  	this.params.push(param);
  	return this;
  }
  
  Seam.Remoting.Action.prototype.setExpression = function(expr) {
  	this.expression = expr;
    return this;
  }
}

Seam.Remoting.Model = function() {
  this.expressions = new Array();
  this.beans = new Array();
	
	Seam.Remoting.Model.prototype.addExpression = function(alias, expr) {
		this.expressions.push({alias: alias, expr: expr});
  }
  
  Seam.Remoting.Model.prototype.addBean = function(alias, bean, property) {
    var q = null;
    if (arguments.length > 3) {
	    q = new Array();
	    for (var i = 3; i < arguments.length; i++) { 
	      q.push(arguments[i]);
	    }
	  }
	  this.beans.push({alias: alias, bean: bean, property: property, qualifiers: q});
  }
  
  Seam.Remoting.Model.prototype.fetch = function(action) {
  	var r = this.createFetchRequest(action);
    var env = Seam.Remoting.createEnvelope(Seam.Remoting.createHeader(), r.data);
    Seam.Remoting.pendingCalls.put(r.id, r);
    Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_MODEL, this.processFetchResponse, false);  	
  }
  
  Seam.Remoting.Model.prototype.createFetchRequest = function(a) { // a = action
    var callId = "" + Seam.Remoting.__callId++;
    var d = "<model operation=\"fetch\" callId=\"" + callId + ">";
    var refs = new Array();
    
    if (a) {
      d += "<action>";
      if (a.beanType) {
        d += "<target>" + a.beanType + "</target>";
        if (a.qualifiers) d += "<qualifiers>" + a.qualifiers + "</qualifiers>";
        if (a.method) d += "<method>" + a.method + "</method>";
        if (a.params.length > 0) {
          d += "<params>";
          for (var i = 0; i < a.params.length; i++) {
            d += "<param>" + Seam.Remoting.serializeValue(a.params[i], null, refs) + "</param>";
          }
          d += "</params>";
        }       
      }
      else if (a.expression) {
        d += "<target>" + a.expression + "</target>";
      }
      d += "</action>";
    }    
    if (this.beans.length > 0) {
      for (var i = 0; i < this.beans.length; i++) {
        var b = this.beans[i];
        d += "<bean alias=\"" + b.alias + "\"><name>" + b.name + "</name>";
        if (b.qualifiers && b.qualifiers.length > 0) {
          d += "<qualifiers>";
          for (var j = 0; j < b.qualifiers.length; j++) {
             d += (j > 0 ? "," : "") + b.qualifiers[j];
          } 
          d += "</qualifiers>";
        }
        d += "<property>" + b.property + "</property></bean>";        
      }
    }    
    if (this.expressions.length > 0) {
      for (var i = 0; i < this.expressions.length; i++) {
        var e = this.expressions[i];
        d += "<expression alias=\"" + e.alias + "\">" + e.expr + "</expression>";
      } 
    }
    if (refs.length > 0) {
      d += "<refs>";
      for (var i = 0; i < refs.length; i++) {
        d += "<ref id=\"" + i + "\">";
        d += Seam.Remoting.serializeType(refs[i], refs);
        d += "</ref>";
      }
      d += "</refs>";
    }
    d += "</model>";    
    
    return {data: d, id: callId};
  }
    
  Seam.Remoting.Model.prototype.processFetchResponse = function(doc) {
    
  }  
  
  Seam.Remoting.Model.prototype.applyUpdates = function(action) {
  	
  } 
  
   
}

/* Messaging API */

Seam.Remoting.pollInterval = 10; // Default poll interval of 10 seconds
Seam.Remoting.pollTimeout = 0; // Default timeout of 0 seconds
Seam.Remoting.polling = false;

Seam.Remoting.setPollInterval = function(interval) {
  Seam.Remoting.pollInterval = interval;
}

Seam.Remoting.setPollTimeout = function(timeout) {
  Seam.Remoting.pollTimeout = timeout;
}

Seam.Remoting.subscriptionRegistry = new Array();

Seam.Remoting.subscribe = function(topicName, callback) {
  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++) {
    if (Seam.Remoting.subscriptionRegistry[i].topic == topicName)
      return;
  }

  var body = "<subscribe topic=\"" + topicName + "\"/>";
  var env = Seam.Remoting.createEnvelope(null, body);
  Seam.Remoting.subscriptionRegistry.push({topic:topicName, callback:callback});
  Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_SUBSCRIPTION, Seam.Remoting.subscriptionCallback, false);
}

Seam.Remoting.unsubscribe = function(topicName) {
  var token = null;

  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++) {
    if (Seam.Remoting.subscriptionRegistry[i].topic == topicName) {
      token = Seam.Remoting.subscriptionRegistry[i].token;
      Seam.Remoting.subscriptionRegistry.splice(i, 1);
    }
  }

  if (token) {
    var body = "<unsubscribe token=\"" + token + "\"/>";
    var env = Seam.Remoting.createEnvelope(null, body);
    Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_SUBSCRIPTION, null, false);
  }
}

Seam.Remoting.subscriptionCallback = function(doc) {
  var body = doc.documentElement.firstChild;
  for (var i = 0; i < body.childNodes.length; i++) {
    var node = body.childNodes.item(i);
    if (node.tagName == "subscription") {
      var topic = node.getAttribute("topic");
      var token = node.getAttribute("token");
      for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++) {
        if (Seam.Remoting.subscriptionRegistry[i].topic == topic) {
          Seam.Remoting.subscriptionRegistry[i].token = token;
          Seam.Remoting.poll();
          break;
        }
      }
    }
  }
}

Seam.Remoting.pollTimeoutFunction = null;

Seam.Remoting.poll = function() {
  if (Seam.Remoting.polling) return;

  Seam.Remoting.polling = true;
  clearTimeout(Seam.Remoting.pollTimeoutFunction);

  var body = "";

  if (Seam.Remoting.subscriptionRegistry.length == 0) {
    Seam.Remoting.polling = false;
    return;
  }

  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++) {
    body += "<poll token=\"" + Seam.Remoting.subscriptionRegistry[i].token + "\" ";
    body += "timeout=\"" + Seam.Remoting.pollTimeout + "\"/>";
  }

  var env = Seam.Remoting.createEnvelope(null, body);
  Seam.Remoting.sendAjaxRequest(env, Seam.Remoting.PATH_POLL, Seam.Remoting.pollCallback, true);
}

Seam.Remoting.pollCallback = function(doc) {
  Seam.Remoting.polling = false;

  var body = doc.documentElement.firstChild;
  for (var i = 0; i < body.childNodes.length; i++) {
    var node = body.childNodes.item(i);
    if (node.tagName == "messages")
      Seam.Remoting.processMessages(node);
    else if (node.tagName == "errors")
      Seam.Remoting.processPollErrors(node);
  }

  Seam.Remoting.pollTimeoutFunction = setTimeout("Seam.Remoting.poll()", Math.max(Seam.Remoting.pollInterval * 1000, 1000));
}

Seam.Remoting.processMessages = function(messages) {
  var token = messages.getAttribute("token");

  var callback = null;
  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++) {
    if (Seam.Remoting.subscriptionRegistry[i].token == token)
    {
      callback = Seam.Remoting.subscriptionRegistry[i].callback;
      break;
    }
  }

  if (callback != null) {
    var m = null;
    var c = messages.childNodes;
    for (var i = 0; i < c.length; i++) {
      if (c.item(i).tagName == "message")
      {
        m = c.item(i);
        var messageType = m.getAttribute("type");

        var valueNode = null;
        var refsNode = null;
        for (var j = 0; j < m.childNodes.length; j++) {
          var node = m.childNodes.item(j);
          if (node.tagName == "value")
            valueNode = node;
          else if (node.tagName == "refs")
            refsNode = node;
        }

        var refs = new Array();
        if (refsNode) Seam.Remoting.unmarshalRefs(refsNode, refs);
        var v = Seam.Remoting.unmarshalValue(valueNode.firstChild, refs);
        callback(Seam.Remoting.createMessage(messageType, v));
      }
    }
  }
}

Seam.Remoting.processErrors = function(errors) {
  var t = errors.getAttribute("token");

  for (var i = 0; i < Seam.Remoting.subscriptionRegistry.length; i++) {
    if (Seam.Remoting.subscriptionRegistry[i].token == t) {
      Seam.Remoting.subscriptionRegistry.splice(i, 1);
      break;
    }
  }

  for (var i = 0; i < errors.childNodes.length; i++) {
    if (errors.childNodes.item(i).tagName == "error") {
      var errorNode = errors.childNodes.item(i);
      var code = errorNode.getAttribute("code");
      var message = errorNode.firstChild.nodeValue;

      if (Seam.Remoting.onPollError)
        Seam.Remoting.onPollError(code, message);
      else
        alert("A polling error occurred: " + code + " " + message);
    }
  }
}

Seam.Remoting.ObjectMessage = function() {
  this.value = null;

  Seam.Remoting.ObjectMessage.prototype.getValue = function() {
    return this.value;
  }

  Seam.Remoting.ObjectMessage.prototype.setValue = function(value) {
    this.value = value;
  }
}

Seam.Remoting.TextMessage = function() {
  this.text = null;

  Seam.Remoting.TextMessage.prototype.getText = function() {
    return this.text;
  }

  Seam.Remoting.TextMessage.prototype.setText = function(text) {
    this.text = text;
  }
}

Seam.Remoting.createMessage = function(messageType, value) {
  switch (messageType) {
    case "object":
      var msg = new Seam.Remoting.ObjectMessage();
      msg.setValue(value);
      return msg;
    case "text":
      var msg = new Seam.Remoting.TextMessage();
      msg.setText(value);
      return msg;
  }
  return null;
}
