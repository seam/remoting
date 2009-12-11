var Seam = {
  type: new Object(),
  beans: new Array(),
  debug: false,
  debugWindow: null,
  PATH_EXECUTE: "/execute",
  PATH_SUBSCRIPTION: "/subscription",
  PATH_MODEL: "/model",
  PATH_POLL: "/poll", 
}

Seam.createBean = function(name) {
  var b = Seam.beans;
  for (var i=0; i<b.length; i++) {
    if (b[i].__name == name) {
      var v = new b[i];
      if (arguments.length > 1) {
        v.__qualifiers = new Array();
        for (var j=1; j<arguments.length; j++) {
          v.__qualifiers.push(arguments[j]);
        }
      }
      return v;
    }
  }
  return null;
}

Seam.getBeanType = function(obj) {
  var b = Seam.beans;
  for (var i=0; i<b.length; i++) {
    if (obj instanceof b[i]) return b[i];
  }
  return undefined;
}

Seam.getBeanName = function(obj) {
  var t = Seam.getBeanType(obj);
  return t ? t.__name : undefined;
}

Seam.registerBean = function(bean) {
  var b = Seam.beans;
  for (var i=0; i<b.length; i++) {
    if (b[i].__name == bean.__name) {
      b[i] = bean;
      return;
    }
  }
  b.push(bean);
}

Seam.isBeanRegistered = function(name) {
  var b = Seam.beans;
  for (var i=0; i<b.length; i++) {
    if (b[i].__name == name)
      return true;
  }
  return false;
}

Seam.getBeanMetadata = function(obj) {
  var b = Seam.getBeanType(obj);
  return b ? b.__metadata : undefined;
}

Seam.Xml = {
  childNode: function(e, tag) {
    for (var i=0; i<e.childNodes.length; i++) {
      if (e.childNodes.item(i).tagName == tag) return e.childNodes.item(i);
    }
  },
  childNodes: function(e, tag) {
    var n = new Array();
    for (var i=0; i<e.childNodes.length;i++) {
      if (e.childNodes.item(i).tagName == tag) n.push(e.childNodes.item(i));
    }
    return n;    
  }   
}

Seam.extractEncodedSessionId = function(url) {
  if (url.indexOf(';jsessionid=') >= 0) {
    var qpos = url.indexOf('?');
    return url.substring(url.indexOf(';jsessionid=') + 12, qpos >= 0 ? qpos : url.length);
  }
  return null;
}

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
}

Seam.createNamespace = function(namespace) {
  var p = namespace.split(".");
  var b = Seam.type;
  for(var i=0; i<p.length; i++) {
    if (typeof b[p[i]] == "undefined") b[p[i]] = new Object();
    b = b[p[i]];
  }
}

Seam.Context = function() {
  this.conversationId = null;
  Seam.Context.prototype.setConversationId = function(conversationId) {
    this.conversationId = conversationId;
  }

  Seam.Context.prototype.getConversationId = function() {
    return this.conversationId;
  }
}
Seam.context = new Seam.Context();

Seam.Exception = function(msg) {
  this.message = msg;
  Seam.Exception.prototype.getMessage = function() {
    return this.message;
  }
}

Seam.Map = function() {
  this.elements = new Array();

  Seam.Map.prototype.size = function() {
    return this.elements.length;
  }

  Seam.Map.prototype.isEmpty = function() {
    return this.elements.length == 0;
  }

  Seam.Map.prototype.keySet = function() {
    var keySet = new Array();
    for (var i=0; i<this.elements.length; i++) {
      keySet[keySet.length] = this.elements[i].key;
    }
    return keySet;
  }

  Seam.Map.prototype.values = function() {
    var vals = new Array();
    for (var i=0; i<this.elements.length; i++) {
      vals.push(this.elements[i].value);
    }
    return vals;
  }

  Seam.Map.prototype.get = function(key) {
    for (var i=0; i<this.elements.length; i++) {
      if (this.elements[i].key == key) return this.elements[i].value;
    }
    return null;
  }

  Seam.Map.prototype.put = function(key, value) {
    for (var i=0; i<this.elements.length; i++) {
      if (this.elements[i].key == key) {
        this.elements[i].value = value;
        return;
      }
    }
    this.elements.push({key:key,value:value});
  }

  Seam.Map.prototype.remove = function(key) {
    for (var i=0; i<this.elements.length; i++) {
      if (this.elements[i].key == key)
        this.elements.splice(i, 1);
    }
  }

  Seam.Map.prototype.contains = function(key) {
    for (var i=0; i<this.elements.length; i++) {
      if (this.elements[i].key == key) return true;
    }
    return false;
  }
}

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
  }
  else {
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
}

Seam.serializeBag = function(v, refs) {
  var d = "<bag>";
  for (var i=0; i<v.length; i++) {
    d += "<element>" + Seam.serializeValue(v[i], null, refs) + "</element>";
  }
  d += "</bag>";
  return d;
}

Seam.serializeMap = function(v, refs) {
  var d = "<map>";
  var keyset = v.keySet();
  for (var i=0; i<keyset.length; i++) {
    d += "<element><k>" + Seam.serializeValue(keyset[i], null, refs) + "</k><v>" +
    Seam.serializeValue(v.get(keyset[i]), null, refs) + "</v></element>";
  }
  d += "</map>";
  return d;
}

Seam.serializeDate = function(v) {
  var zeroPad = function(val, digits) { while (("" + val).length < digits) val = "0" + val; return val; };
  return "<date>" + v.getFullYear() + zeroPad(v.getMonth() + 1, 2) + zeroPad(v.getDate(), 2) +
    zeroPad(v.getHours(), 2) + zeroPad(v.getMinutes(), 2) + zeroPad(v.getSeconds(), 2) +
    zeroPad(v.getMilliseconds(), 3) +"</date>";
}

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
}

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
      Seam.serializeValue(obj[meta[i].field], meta[i].type, refs) + "</member>\n";
  }
  d += "</bean>";
  return d;
}

Seam.createCall = function(component, methodName, params, callback, exceptionHandler) {
  var callId = "" + Seam.__callId++;
  if (!callback) callback = component.__callback[methodName];
  var d = "<call id=\"" + callId + "\">\n" +
    "<target>" + Seam.getBeanType(component).__name + "</target>";
  if (component.__qualifiers != null) {
    d += "<qualifiers>";
    for (var i=0; i<component.__qualifiers.length; i++) {
      d += component.__qualifiers[i];
      if (i < component.__qualifiers.length - 1) data += ",";
    }
    d += "</qualifiers>";
  }
  d += "<method>" + methodName + "</method>" + "<params>";
  var refs = new Array();
  for (var i = 0; i < params.length; i++) {
    d += "<param>" + Seam.serializeValue(params[i], null, refs) + "</param>";
  }
  d += "</params><refs>";
  for (var i = 0; i < refs.length; i++) {
    d += "<ref id=\"" + i + "\">" + Seam.serializeType(refs[i], refs) + "</ref>";
  }
  d += "</refs></call>";
  return {data: d, id: callId, callback: callback, exceptionHandler: exceptionHandler};
}

Seam.createHeader = function() {
  var h = "<context>";
  if (Seam.context.getConversationId()) {
    h += "<conversationId>" + Seam.context.getConversationId() + "</conversationId>";
  }
  h += "</context>";
  return h;
}

Seam.createEnvelope = function(header, body) {
  var d = "<envelope>";
  if (header) d += "<header>" + header + "</header>";
  if (body) d += "<body>" + body + "</body>";
  d += "</envelope>";
  return d;
}

Seam.__callId = 0;
Seam.pendingCalls = new Seam.Map();
Seam.inBatch = false;
Seam.batchedCalls = new Array();

Seam.startBatch = function() {
  Seam.inBatch = true;
  Seam.batchedCalls.length = 0;
}

Seam.executeBatch = function() {
  if (!Seam.inBatch) return;
  var d = "";
  for (var i=0; i<Seam.batchedCalls.length; i++) {
    Seam.pendingCalls.put(Seam.batchedCalls[i].id, Seam.batchedCalls[i]);
    d += Seam.batchedCalls[i].data;
  }
  var envelope = Seam.createEnvelope(Seam.createHeader(), d);
  Seam.batchAsyncReq = Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE,
    Seam.processResponse, false);
  Seam.inBatch = false;
}

Seam.cancelBatch = function() {
  Seam.inBatch = false;
  for (var i=0; i<Seam.batchedCalls.length; i++) {
    Seam.pendingCalls.remove(Seam.batchedCalls[i].id);
  }
}

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
}

Seam.execute = function(component, methodName, params, callback, exceptionHandler) {
  var c = Seam.createCall(component, methodName, params, callback, exceptionHandler);
  if (Seam.inBatch) {
    Seam.batchedCalls[Seam.batchedCalls.length] = c;
  }
  else {
    var envelope = Seam.createEnvelope(Seam.createHeader(), c.data);
    Seam.pendingCalls.put(c.id, c);
    Seam.sendAjaxRequest(envelope, Seam.PATH_EXECUTE, Seam.processResponse, false);
  }
  return c;
}

Seam.sendAjaxRequest = function(env, path, callback, silent) {
  Seam.log("Request packet:\n" + env);
  if (!silent) Seam.displayLoadingMessage();
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
        Seam.displayError(r.status);
      }
    }
  }
  if (Seam.encodedSessionId) {
    path += ';jsessionid=' + Seam.encodedSessionId;
  }
  r.open("POST", Seam.resourcePath + path, true);
  r.send(env);
}

Seam.displayError = function(code) {
  alert("There was an error processing your request.  Error code: " + code);
}

Seam.setCallback = function(component, methodName, callback) {
  component.__callback[methodName] = callback;
}

Seam.processResponse = function(doc) {  
  if (typeof(Seam) == "undefined") return;
  if (!doc.documentElement) return;
  var context = new Seam.Context;
  var headerNode = Seam.Xml.childNode(doc.documentElement, "header");
  var bodyNode = Seam.Xml.childNode(doc.documentElement, "body");
  if (headerNode) {
    var contextNode = Seam.Xml.childNode(headerNode, "context");
    if (contextNode && context) {
      Seam.unmarshalContext(contextNode, context);
      if (context.getConversationId() && Seam.context.getConversationId() == null)
        Seam.context.setConversationId(context.getConversationId());
    }
  }
  if (bodyNode) {
    var n = Seam.Xml.childNode(bodyNode, "result");
    if (n) Seam.processResult(n, context);
  }
}

Seam.processResult = function(result, context) {
  var callId = result.getAttribute("id");
  var call = Seam.pendingCalls.get(callId);
  Seam.pendingCalls.remove(callId);
  if (call && (call.callback || call.exceptionHandler)) {
    var valueNode = Seam.Xml.childNode(result, "value");
    var refsNode = Seam.Xml.childNode(result, "refs");
    var exceptionNode = Seam.Xml.childNode(result, "exception");
    if (exceptionNode != null) {
      var msgNode = Seam.Xml.childNode(exceptionNode, "message");
      var msg = Seam.unmarshalValue(msgNode.firstChild);
      call.exceptionHandler(new Seam.Exception(msg));
    }
    else {
      var refs = new Array();
      if (refsNode) Seam.unmarshalRefs(refsNode, refs);
      var value = Seam.unmarshalValue(valueNode.firstChild, refs);
      call.callback(value, context, callId);
    }
  }
}

Seam.unmarshalContext = function(ctxNode, context) {
  var c = Seam.Xml.childNode(ctxNode, "conversationId");
  if (c) context.setConversationId(c.firstChild.nodeValue);
}

Seam.unmarshalRefs = function(refsNode, refs) {
  var objs = new Array();
  var cn = Seam.Xml.childNodes(refsNode, "ref");
  for (var i=0; i<cn.length; i++) {
    var refId = parseInt(cn[i].getAttribute("id"));
    var valueNode = refNode.firstChild;
    if (valueNode.tagName == "bean") {
      var name = valueNode.getAttribute("type");
      var obj = Seam.isBeanRegistered(name) ? Seam.createBean(name) : null;
      if (obj) {
        refs[refId] = obj;
        objs.push({obj: obj, node: valueNode});
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
}

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
      var value = new Array();
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
    default: return null;
  }
}

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
}

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
  }
  else {
    Seam.loadingMsgDiv.innerHTML = Seam.loadingMessage;
    Seam.loadingMsgDiv.style.visibility = 'visible';
  }
}

Seam.hideLoadingMessage = function() {
  if (Seam.loadingMsgDiv)
    Seam.loadingMsgDiv.style.visibility = 'hidden';
}

/* Remote Model API */

Seam.Action = function() {
  this.beanType = null;
  this.qualifiers = null;
  this.method = null;
  this.params = new Array();
  this.expression = null;

  Seam.Action.prototype.setBeanType = function(beanType) {
    this.beanType = beanType;
    return this;
  }

  Seam.Action.prototype.setQualifiers = function(qualifiers) {
    this.qualifiers = qualifiers;
    return this;
  }

  Seam.Action.prototype.setMethod = function(method) {
    this.method = method;
    return this;
  }

  Seam.Action.prototype.addParam = function(param) {
    this.params.push(param);
    return this;
  }

  Seam.Action.prototype.setExpression = function(expr) {
    this.expression = expr;
    return this;
  }
}

Seam.Model = function() {
  this.expressions = new Array();
  this.beans = new Array();
  this.values = new Array();
  this.callback = null;

  Seam.Model.prototype.addExpression = function(alias, expr) {
    this.expressions.push({alias: alias, expr: expr});
  }
  
  Seam.Model.prototype.getValue = function(alias) {
    for (var i = 0; i < this.values.length; i++) {
      if (this.values[i].alias == alias) {
        return this.values[i].value; 
      } 
    } 
    return null;
  }

  Seam.Model.prototype.addBean = function(alias, bean, property) {
    var q = null;
    if (arguments.length > 3) {
      q = new Array();
      for (var i = 3; i < arguments.length; i++) {
        q.push(arguments[i]);
      }
    }
    this.beans.push({alias: alias, bean: bean, property: property, qualifiers: q});
  }

  Seam.Model.prototype.fetch = function(action, callback) {
    this.callback = callback;
    var r = this.createFetchRequest(action);
    var env = Seam.createEnvelope(Seam.createHeader(), r.data);
    Seam.pendingCalls.put(r.id, r);
    Seam.sendAjaxRequest(env, Seam.PATH_MODEL, this.processFetchResponse, false);
  }

  Seam.Model.prototype.createFetchRequest = function(a) { // a = action
    var callId = "" + Seam.__callId++;
    var d = "<model operation=\"fetch\" callId=\"" + callId + "\">";
    var refs = new Array();
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
      }
      else if (a.expression) {
        d += "<target>" + a.expression + "</target>";
      }
      d += "</action>";
    }
    if (this.beans.length > 0) {
      for (var i = 0; i < this.beans.length; i++) {
        var b = this.beans[i];
        d += "<bean alias=\"" + b.alias + "\"><name>" + b.bean + "</name>";
        if (b.qualifiers && b.qualifiers.length > 0) {
          d += "<qualifiers>";
          for (var j=0; j<b.qualifiers.length; j++) {
             d += (j > 0 ? "," : "") + b.qualifiers[j];
          }
          d += "</qualifiers>";
        }
        d += "<property>" + b.property + "</property></bean>";
      }
    }
    if (this.expressions.length > 0) {
      for (var i=0; i<this.expressions.length; i++) {
        var e = this.expressions[i];
        d += "<expression alias=\"" + e.alias + "\">" + e.expr + "</expression>";
      }
    }
    d += "</model>";
    return {data: d, id: callId};
  }

  Seam.Model.prototype.processFetchResponse = function(doc) {
    var headerNode;
    var bodyNode;
    var inScope = typeof(Seam) == "undefined" ? false : true;
    if (!inScope) return;
    var context = new Seam.Context; 
    if (doc.documentElement) {
      for (var i=0; i<doc.documentElement.childNodes.length; i++) {
        var node = doc.documentElement.childNodes.item(i);
        if (node.tagName == "header")
          headerNode = node;
        else if (node.tagName == "body")
          bodyNode = node;
      }
    }
    if (headerNode) {
      var contextNode;
      for (var i=0; i<headerNode.childNodes.length; i++) {
        var node = headerNode.childNodes.item(i);
        if (node.tagName == "context") {
          contextNode = node;
          break;
        }
      }
      if (contextNode && context) {
        Seam.unmarshalContext(contextNode, context);
        if (context.getConversationId() && Seam.context.getConversationId() == null)
          Seam.context.setConversationId(context.getConversationId());
      }
    }
    if (bodyNode) {
      for (var i=0; i<bodyNode.childNodes.length; i++) {
        var n = bodyNode.childNodes.item(i);
        if (n.tagName == "model") {          
          var valueNodes = new Array();
          var refsNode = null;
          for (var j=0; j<n.childNodes.length; j++) {
            var cn = n.childNodes.item(j);
            if (cn.tagName == "value")
              valueNodes.push({node:cn.firstChild,alias:cn.getAttribute("alias")});
            else if (cn.tagName == "refs")
              refsNode = n.childNodes.item(j);
          }
          var refs = new Array();
          if (refsNode) Seam.unmarshalRefs(refsNode, refs);
          for (var j=0; j<valueNodes.length; j++) {
            var value = Seam.unmarshalValue(valueNodes[j].node, refs);
            this.values.push({alias:valueNodes[j].alias, value:value}); 
          }
        }
      }
    }
    if (this.callback) callback(this);
  }

  Seam.Model.prototype.applyUpdates = function(action) {

  }
}