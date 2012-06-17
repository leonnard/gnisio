/*! Socket.IO.js build:0.9.6, development. Copyright(c) 2011 LearnBoost <dev@learnboost.com> MIT Licensed */

(function(g,e){g.version="0.9.6";g.protocol=1;g.transports=[];g.j=[];g.sockets={};g.connect=function(c,a){var f=g.util.parseUri(c),d,b;e&&e.location&&(f.protocol=f.protocol||e.location.protocol.slice(0,-1),f.host=f.host||(e.document?e.document.domain:e.location.hostname),f.port=f.port||e.location.port);d=g.util.uniqueUri(f);var h={host:f.host,secure:"https"==f.protocol,port:f.port||("https"==f.protocol?443:80),query:f.query||""};g.util.merge(h,a);if(h["force new connection"]||!g.sockets[d])b=new g.Socket(h);
!h["force new connection"]&&b&&(g.sockets[d]=b);b=b||g.sockets[d];return b.of(1<f.path.length?f.path:"")}})("object"===typeof module?module.exports:this.io={},this);
(function(g,e){var c=g.util={},a=/^(?:(?![^:@]+:[^:@\/]*@)([^:\/?#.]+):)?(?:\/\/)?((?:(([^:@]*)(?::([^:@]*))?)?@)?([^:\/?#]*)(?::(\d*))?)(((\/(?:[^?#](?![^?#\/]*\.[^?#\/.]+(?:[?#]|$)))*\/?)?([^?#\/]*))(?:\?([^#]*))?(?:#(.*))?)/,f="source protocol authority userInfo user password host port relative path directory file query anchor".split(" ");c.parseUri=function(b){for(var b=a.exec(b||""),h={},d=14;d--;)h[f[d]]=b[d]||"";return h};c.uniqueUri=function(b){var h=b.protocol,d=b.host,b=b.port;"document"in
e?(d=d||document.domain,b=b||("https"==h&&"https:"!==document.location.protocol?443:document.location.port)):(d=d||"localhost",!b&&"https"==h&&(b=443));return(h||"http")+"://"+d+":"+(b||80)};c.query=function(b,h){var d=c.chunkQuery(b||""),a=[];c.merge(d,c.chunkQuery(h||""));for(var f in d)d.hasOwnProperty(f)&&a.push(f+"="+d[f]);return a.length?"?"+a.join("&"):""};c.chunkQuery=function(b){for(var d={},b=b.split("&"),a=0,c=b.length,f;a<c;++a)f=b[a].split("="),f[0]&&(d[f[0]]=f[1]);return d};var d=!1;
c.load=function(b){if("document"in e&&"complete"===document.readyState||d)return b();c.on(e,"load",b,!1)};c.on=function(b,d,a,f){b.attachEvent?b.attachEvent("on"+d,a):b.addEventListener&&b.addEventListener(d,a,f)};c.request=function(b){if(b&&"undefined"!=typeof XDomainRequest)return new XDomainRequest;if("undefined"!=typeof XMLHttpRequest&&(!b||c.ua.hasCORS))return new XMLHttpRequest;if(!b)try{return new (window[["Active"].concat("Object").join("X")])("Microsoft.XMLHTTP")}catch(d){}return null};"undefined"!=
typeof window&&c.load(function(){d=!0});c.defer=function(b){if(!c.ua.webkit||"undefined"!=typeof importScripts)return b();c.load(function(){setTimeout(b,100)})};c.merge=function(b,d,a,f){var f=f||[],a="undefined"==typeof a?2:a,e;for(e in d)d.hasOwnProperty(e)&&0>c.indexOf(f,e)&&("object"!==typeof b[e]||!a?(b[e]=d[e],f.push(d[e])):c.merge(b[e],d[e],a-1,f));return b};c.mixin=function(b,d){c.merge(b.prototype,d.prototype)};c.inherit=function(b,d){function a(){}a.prototype=d.prototype;b.prototype=new a};
c.isArray=Array.isArray||function(b){return"[object Array]"===Object.prototype.toString.call(b)};c.intersect=function(b,d){for(var a=[],f=b.length>d.length?b:d,e=b.length>d.length?d:b,i=0,r=e.length;i<r;i++)~c.indexOf(f,e[i])&&a.push(e[i]);return a};c.indexOf=function(b,d,a){for(var f=b.length,a=0>a?0>a+f?0:a+f:a||0;a<f&&b[a]!==d;a++);return f<=a?-1:a};c.toArray=function(b){for(var d=[],a=0,f=b.length;a<f;a++)d.push(b[a]);return d};c.ua={};c.ua.hasCORS="undefined"!=typeof XMLHttpRequest&&function(){try{var b=
new XMLHttpRequest}catch(d){return!1}return void 0!=b.withCredentials}();c.ua.webkit="undefined"!=typeof navigator&&/webkit/i.test(navigator.userAgent)})("undefined"!=typeof io?io:module.exports,this);
(function(g,e){function c(){}g.EventEmitter=c;c.prototype.on=function(a,f){this.$events||(this.$events={});this.$events[a]?e.util.isArray(this.$events[a])?this.$events[a].push(f):this.$events[a]=[this.$events[a],f]:this.$events[a]=f;return this};c.prototype.addListener=c.prototype.on;c.prototype.once=function(a,f){function d(){b.removeListener(a,d);f.apply(this,arguments)}var b=this;d.listener=f;this.on(a,d);return this};c.prototype.removeListener=function(a,f){if(this.$events&&this.$events[a]){var d=
this.$events[a];if(e.util.isArray(d)){for(var b=-1,h=0,c=d.length;h<c;h++)if(d[h]===f||d[h].listener&&d[h].listener===f){b=h;break}if(0>b)return this;d.splice(b,1);d.length||delete this.$events[a]}else(d===f||d.listener&&d.listener===f)&&delete this.$events[a]}return this};c.prototype.removeAllListeners=function(a){this.$events&&this.$events[a]&&(this.$events[a]=null);return this};c.prototype.listeners=function(a){this.$events||(this.$events={});this.$events[a]||(this.$events[a]=[]);e.util.isArray(this.$events[a])||
(this.$events[a]=[this.$events[a]]);return this.$events[a]};c.prototype.emit=function(a){if(!this.$events)return!1;var f=this.$events[a];if(!f)return!1;var d=Array.prototype.slice.call(arguments,1);if("function"==typeof f)f.apply(this,d);else if(e.util.isArray(f))for(var f=f.slice(),b=0,h=f.length;b<h;b++)f[b].apply(this,d);else return!1;return!0}})("undefined"!=typeof io?io:module.exports,"undefined"!=typeof io?io:module.parent.exports);
(function(g,e){function c(b){return 10>b?"0"+b:b}function a(b){h.lastIndex=0;return h.test(b)?'"'+b.replace(h,function(b){var d=j[b];return"string"===typeof d?d:"\\u"+("0000"+b.charCodeAt(0).toString(16)).slice(-4)})+'"':'"'+b+'"'}function f(b,d){var h,e,j,g,q=k,o,l=d[b];l instanceof Date&&(l=isFinite(b.valueOf())?b.getUTCFullYear()+"-"+c(b.getUTCMonth()+1)+"-"+c(b.getUTCDate())+"T"+c(b.getUTCHours())+":"+c(b.getUTCMinutes())+":"+c(b.getUTCSeconds())+"Z":null);"function"===typeof i&&(l=i.call(d,b,
l));switch(typeof l){case "string":return a(l);case "number":return isFinite(l)?""+l:"null";case "boolean":case "null":return""+l;case "object":if(!l)return"null";k+=p;o=[];if("[object Array]"===Object.prototype.toString.apply(l)){g=l.length;for(h=0;h<g;h+=1)o[h]=f(h,l)||"null";j=0===o.length?"[]":k?"[\n"+k+o.join(",\n"+k)+"\n"+q+"]":"["+o.join(",")+"]";k=q;return j}if(i&&"object"===typeof i){g=i.length;for(h=0;h<g;h+=1)"string"===typeof i[h]&&(e=i[h],(j=f(e,l))&&o.push(a(e)+(k?": ":":")+j))}else for(e in l)Object.prototype.hasOwnProperty.call(l,
e)&&(j=f(e,l))&&o.push(a(e)+(k?": ":":")+j);j=0===o.length?"{}":k?"{\n"+k+o.join(",\n"+k)+"\n"+q+"}":"{"+o.join(",")+"}";k=q;return j}}if(e&&e.parse)return g.JSON={parse:e.parse,stringify:e.stringify};var d=g.JSON={},b=/[\u0000\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,h=/[\\\"\x00-\x1f\x7f-\x9f\u00ad\u0600-\u0604\u070f\u17b4\u17b5\u200c-\u200f\u2028-\u202f\u2060-\u206f\ufeff\ufff0-\uffff]/g,k,p,j={"\u0008":"\\b","\t":"\\t","\n":"\\n","\u000c":"\\f",
"\r":"\\r",'"':'\\"',"\\":"\\\\"},i;d.stringify=function(b,d,a){var h;p=k="";if("number"===typeof a)for(h=0;h<a;h+=1)p+=" ";else"string"===typeof a&&(p=a);if((i=d)&&"function"!==typeof d&&("object"!==typeof d||"number"!==typeof d.length))throw Error("JSON.stringify");return f("",{"":b})};d.parse=function(d,a){function h(b,d){var f,e,c=b[d];if(c&&"object"===typeof c)for(f in c)Object.prototype.hasOwnProperty.call(c,f)&&(e=h(c,f),void 0!==e?c[f]=e:delete c[f]);return a.call(b,d,c)}var f,d=""+d;b.lastIndex=
0;b.test(d)&&(d=d.replace(b,function(b){return"\\u"+("0000"+b.charCodeAt(0).toString(16)).slice(-4)}));if(/^[\],:{}\s]*$/.test(d.replace(/\\(?:["\\\/bfnrt]|u[0-9a-fA-F]{4})/g,"@").replace(/"[^"\\\n\r]*"|true|false|null|-?\d+(?:\.\d*)?(?:[eE][+\-]?\d+)?/g,"]").replace(/(?:^|:|,)(?:\s*\[)+/g,"")))return f=eval("("+d+")"),"function"===typeof a?h({"":f},""):f;throw new SyntaxError("JSON.parse");}})("undefined"!=typeof io?io:module.exports,"undefined"!==typeof JSON?JSON:void 0);
(function(g,e){var c=g.parser={},a=c.packets="disconnect connect heartbeat message json event ack error noop".split(" "),f=c.reasons=["transport not supported","client not handshaken","unauthorized"],d=c.advice=["reconnect"],b=e.JSON,h=e.util.indexOf;c.encodePacket=function(c){var e=h(a,c.type),k=c.id||"",g=c.endpoint||"",n=c.ack,m=null;switch(c.type){case "error":var s=c.reason?h(f,c.reason):"",c=c.advice?h(d,c.advice):"";if(""!==s||""!==c)m=s+(""!==c?"+"+c:"");break;case "message":""!==c.data&&
(m=c.data);break;case "event":m={name:c.name};c.args&&c.args.length&&(m.args=c.args);m=b.stringify(m);break;case "json":m=b.stringify(c.data);break;case "connect":c.qs&&(m=c.qs);break;case "ack":m=c.ackId+(c.args&&c.args.length?"+"+b.stringify(c.args):"")}e=[e,k+("data"==n?"+":""),g];null!==m&&void 0!==m&&e.push(m);return e.join(":")};c.encodePayload=function(b){var d="";if(1==b.length)return b[0];for(var a=0,c=b.length;a<c;a++)d+="\ufffd"+b[a].length+"\ufffd"+b[a];return d};var k=/([^:]+):([0-9]+)?(\+)?:([^:]+)?:?([\s\S]*)?/;
c.decodePacket=function(c){var h=c.match(k);if(!h)return{};var e=h[2]||"",c=h[5]||"",g={type:a[h[1]],endpoint:h[4]||""};e&&(g.id=e,g.ack=h[3]?"data":!0);switch(g.type){case "error":h=c.split("+");g.reason=f[h[0]]||"";g.advice=d[h[1]]||"";break;case "message":g.data=c||"";break;case "event":try{var n=b.parse(c);g.name=n.name;g.args=n.args}catch(m){}g.args=g.args||[];break;case "json":try{g.data=b.parse(c)}catch(s){}break;case "connect":g.qs=c||"";break;case "ack":if(h=c.match(/^([0-9]+)(\+)?(.*)/))if(g.ackId=
h[1],g.args=[],h[3])try{g.args=h[3]?b.parse(h[3]):[]}catch(t){}}return g};c.decodePayload=function(b){if("\ufffd"==b.charAt(0)){for(var d=[],a=1,h="";a<b.length;a++)"\ufffd"==b.charAt(a)?(d.push(c.decodePacket(b.substr(a+1).substr(0,h))),a+=Number(h)+1,h=""):h+=b.charAt(a);return d}return[c.decodePacket(b)]}})("undefined"!=typeof io?io:module.exports,"undefined"!=typeof io?io:module.parent.exports);
(function(g,e){function c(a,c){this.socket=a;this.sessid=c}g.Transport=c;e.util.mixin(c,e.EventEmitter);c.prototype.onData=function(a){this.clearCloseTimeout();(this.socket.connected||this.socket.connecting||this.socket.reconnecting)&&this.setCloseTimeout();if(""!==a&&(a=e.parser.decodePayload(a))&&a.length)for(var c=0,d=a.length;c<d;c++)this.onPacket(a[c]);return this};c.prototype.onPacket=function(a){this.socket.setHeartbeatTimeout();if("heartbeat"==a.type)return this.onHeartbeat();if("connect"==
a.type&&""==a.endpoint)this.onConnect();"error"==a.type&&"reconnect"==a.advice&&(this.open=!1);this.socket.onPacket(a);return this};c.prototype.setCloseTimeout=function(){if(!this.closeTimeout){var a=this;this.closeTimeout=setTimeout(function(){a.onDisconnect()},this.socket.closeTimeout)}};c.prototype.onDisconnect=function(){this.close&&this.open&&this.close();this.clearTimeouts();this.socket.onDisconnect();return this};c.prototype.onConnect=function(){this.socket.onConnect();return this};c.prototype.clearCloseTimeout=
function(){this.closeTimeout&&(clearTimeout(this.closeTimeout),this.closeTimeout=null)};c.prototype.clearTimeouts=function(){this.clearCloseTimeout();this.reopenTimeout&&clearTimeout(this.reopenTimeout)};c.prototype.packet=function(a){this.send(e.parser.encodePacket(a))};c.prototype.onHeartbeat=function(){this.packet({type:"heartbeat"})};c.prototype.onOpen=function(){this.open=!0;this.clearCloseTimeout();this.socket.onOpen()};c.prototype.onClose=function(){this.open=!1;this.socket.onClose();this.onDisconnect()};
c.prototype.prepareUrl=function(){var a=this.socket.options;return this.scheme()+"://"+a.host+":"+a.port+"/"+a.resource+"/"+e.protocol+"/"+this.name+"/"+this.sessid};c.prototype.ready=function(a,c){c.call(this)}})("undefined"!=typeof io?io:module.exports,"undefined"!=typeof io?io:module.parent.exports);
(function(g,e,c){function a(d){this.options={port:80,secure:!1,document:"document"in c?document:!1,resource:"socket.io",transports:e.transports,"connect timeout":1E4,"try multiple transports":!0,reconnect:!0,"reconnection delay":500,"reconnection limit":Infinity,"reopen delay":3E3,"max reconnection attempts":10,"sync disconnect on unload":!0,"auto connect":!0,"flash policy port":10843};e.util.merge(this.options,d);this.reconnecting=this.connecting=this.open=this.connected=!1;this.namespaces={};this.buffer=
[];this.doBuffer=!1;if(this.options["sync disconnect on unload"]&&(!this.isXDomain()||e.util.ua.hasCORS)){var b=this;e.util.on(c,"unload",function(){b.disconnectSync()},!1)}this.options["auto connect"]&&this.connect()}function f(){}g.Socket=a;e.util.mixin(a,e.EventEmitter);a.prototype.of=function(d){this.namespaces[d]||(this.namespaces[d]=new e.SocketNamespace(this,d),""!==d&&this.namespaces[d].packet({type:"connect"}));return this.namespaces[d]};a.prototype.publish=function(){this.emit.apply(this,
arguments);var d,b;for(b in this.namespaces)this.namespaces.hasOwnProperty(b)&&(d=this.of(b),d.$emit.apply(d,arguments))};a.prototype.handshake=function(d){function b(b){if(b instanceof Error)a.onError(b.message);else d.apply(null,b.split(":"))}var a=this,c=this.options,c=["http"+(c.secure?"s":"")+":/",c.host+":"+c.port,c.resource,e.protocol,e.util.query(this.options.query,"t="+ +new Date)].join("/");if(this.isXDomain()&&!e.util.ua.hasCORS){var g=document.getElementsByTagName("script")[0],j=document.createElement("script");
j.src=c+"&jsonp="+e.j.length;g.parentNode.insertBefore(j,g);e.j.push(function(d){b(d);j.parentNode.removeChild(j)})}else{var i=e.util.request();i.open("GET",c,!0);i.withCredentials=!0;i.onreadystatechange=function(){4==i.readyState&&(i.onreadystatechange=f,200==i.status?b(i.responseText):!a.reconnecting&&a.onError(i.responseText))};i.send(null)}};a.prototype.getTransport=function(d){for(var d=d||this.transports,b=0,a;a=d[b];b++)if(e.Transport[a]&&e.Transport[a].check(this)&&(!this.isXDomain()||e.Transport[a].xdomainCheck()))return new e.Transport[a](this,
this.sessionid);return null};a.prototype.connect=function(d){if(this.connecting)return this;var b=this;b.connecting=!0;this.handshake(function(a,c,f,g){function i(d){b.transport&&b.transport.clearTimeouts();b.transport=b.getTransport(d);if(!b.transport)return b.publish("connect_failed");b.transport.ready(b,function(){b.connecting=!0;b.publish("connecting",b.transport.name);b.transport.open();b.options["connect timeout"]&&(b.connectTimeoutTimer=setTimeout(function(){if(!b.connected&&(b.connecting=
!1,b.options["try multiple transports"])){b.remainingTransports||(b.remainingTransports=b.transports.slice(0));for(var d=b.remainingTransports;0<d.length&&d.splice(0,1)[0]!=b.transport.name;);d.length?i(d):b.publish("connect_failed")}},b.options["connect timeout"]))})}b.sessionid=a;b.closeTimeout=1E3*f;b.heartbeatTimeout=1E3*c;b.transports=g?e.util.intersect(g.split(","),b.options.transports):b.options.transports;b.setHeartbeatTimeout();i(b.transports);b.once("connect",function(){clearTimeout(b.connectTimeoutTimer);
d&&"function"==typeof d&&d()})});return this};a.prototype.setHeartbeatTimeout=function(){clearTimeout(this.heartbeatTimeoutTimer);var d=this;this.heartbeatTimeoutTimer=setTimeout(function(){d.transport.onClose()},this.heartbeatTimeout)};a.prototype.packet=function(d){this.connected&&!this.doBuffer?this.transport.packet(d):this.buffer.push(d);return this};a.prototype.setBuffer=function(d){this.doBuffer=d;!d&&(this.connected&&this.buffer.length)&&(this.transport.payload(this.buffer),this.buffer=[])};
a.prototype.disconnect=function(){if(this.connected||this.connecting)this.open&&this.of("").packet({type:"disconnect"}),this.onDisconnect("booted");return this};a.prototype.disconnectSync=function(){e.util.request().open("GET",this.resource+"/"+e.protocol+"/"+this.sessionid,!0);this.onDisconnect("booted")};a.prototype.isXDomain=function(){var d=c.location.port||("https:"==c.location.protocol?443:80);return this.options.host!==c.location.hostname||this.options.port!=d};a.prototype.onConnect=function(){this.connected||
(this.connected=!0,this.connecting=!1,this.doBuffer||this.setBuffer(!1),this.emit("connect"))};a.prototype.onOpen=function(){this.open=!0};a.prototype.onClose=function(){this.open=!1;clearTimeout(this.heartbeatTimeoutTimer)};a.prototype.onPacket=function(d){this.of(d.endpoint).onPacket(d)};a.prototype.onError=function(d){if(d&&d.advice&&"reconnect"===d.advice&&(this.connected||this.connecting))this.disconnect(),this.options.reconnect&&this.reconnect();this.publish("error",d&&d.reason?d.reason:d)};
a.prototype.onDisconnect=function(d){var b=this.connected,a=this.connecting;this.open=this.connecting=this.connected=!1;if(b||a)this.transport.close(),this.transport.clearTimeouts(),b&&(this.publish("disconnect",d),"booted"!=d&&(this.options.reconnect&&!this.reconnecting)&&this.reconnect())};a.prototype.reconnect=function(){function d(){if(a.connected){for(var d in a.namespaces)a.namespaces.hasOwnProperty(d)&&""!==d&&a.namespaces[d].packet({type:"connect"});a.publish("reconnect",a.transport.name,
a.reconnectionAttempts)}clearTimeout(a.reconnectionTimer);a.removeListener("connect_failed",b);a.removeListener("connect",b);a.reconnecting=!1;delete a.reconnectionAttempts;delete a.reconnectionDelay;delete a.reconnectionTimer;delete a.redoTransports;a.options["try multiple transports"]=f}function b(){if(a.reconnecting){if(a.connected)return d();if(a.connecting&&a.reconnecting)return a.reconnectionTimer=setTimeout(b,1E3);a.reconnectionAttempts++>=c?a.redoTransports?(a.publish("reconnect_failed"),
d()):(a.on("connect_failed",b),a.options["try multiple transports"]=!0,a.transport=a.getTransport(),a.redoTransports=!0,a.connect()):(a.reconnectionDelay<e&&(a.reconnectionDelay*=2),a.connect(),a.publish("reconnecting",a.reconnectionDelay,a.reconnectionAttempts),a.reconnectionTimer=setTimeout(b,a.reconnectionDelay))}}this.reconnecting=!0;this.reconnectionAttempts=0;this.reconnectionDelay=this.options["reconnection delay"];var a=this,c=this.options["max reconnection attempts"],f=this.options["try multiple transports"],
e=this.options["reconnection limit"];this.options["try multiple transports"]=!1;this.reconnectionTimer=setTimeout(b,this.reconnectionDelay);this.on("connect",b)}})("undefined"!=typeof io?io:module.exports,"undefined"!=typeof io?io:module.parent.exports,this);
(function(g,e){function c(c,d){this.socket=c;this.name=d||"";this.flags={};this.json=new a(this,"json");this.ackPackets=0;this.acks={}}function a(a,d){this.namespace=a;this.name=d}g.SocketNamespace=c;e.util.mixin(c,e.EventEmitter);c.prototype.$emit=e.EventEmitter.prototype.emit;c.prototype.of=function(){return this.socket.of.apply(this.socket,arguments)};c.prototype.packet=function(a){a.endpoint=this.name;this.socket.packet(a);this.flags={};return this};c.prototype.send=function(a,d){var b={type:this.flags.json?
"json":"message",data:a};"function"==typeof d&&(b.id=++this.ackPackets,b.ack=!0,this.acks[b.id]=d);return this.packet(b)};c.prototype.emit=function(a){var d=Array.prototype.slice.call(arguments,1),b=d[d.length-1],c={type:"event",name:a};"function"==typeof b&&(c.id=++this.ackPackets,c.ack="data",this.acks[c.id]=b,d=d.slice(0,d.length-1));c.args=d;return this.packet(c)};c.prototype.disconnect=function(){""===this.name?this.socket.disconnect():(this.packet({type:"disconnect"}),this.$emit("disconnect"));
return this};c.prototype.onPacket=function(a){function d(){b.packet({type:"ack",args:e.util.toArray(arguments),ackId:a.id})}var b=this;switch(a.type){case "connect":this.$emit("connect");break;case "disconnect":if(""===this.name)this.socket.onDisconnect(a.reason||"booted");else this.$emit("disconnect",a.reason);break;case "message":case "json":var c=["message",a.data];"data"==a.ack?c.push(d):a.ack&&this.packet({type:"ack",ackId:a.id});this.$emit.apply(this,c);break;case "event":c=[a.name].concat(a.args);
"data"==a.ack&&c.push(d);this.$emit.apply(this,c);break;case "ack":this.acks[a.ackId]&&(this.acks[a.ackId].apply(this,a.args),delete this.acks[a.ackId]);break;case "error":if(a.advice)this.socket.onError(a);else"unauthorized"==a.reason?this.$emit("connect_failed",a.reason):this.$emit("error",a.reason)}};a.prototype.send=function(){this.namespace.flags[this.name]=!0;this.namespace.send.apply(this.namespace,arguments)};a.prototype.emit=function(){this.namespace.flags[this.name]=!0;this.namespace.emit.apply(this.namespace,
arguments)}})("undefined"!=typeof io?io:module.exports,"undefined"!=typeof io?io:module.parent.exports);
(function(g,e,c){function a(a){e.Transport.apply(this,arguments)}g.websocket=a;e.util.inherit(a,e.Transport);a.prototype.name="websocket";a.prototype.open=function(){var a=e.util.query(this.socket.options.query),d=this,b;b||(b=c.MozWebSocket||c.WebSocket);this.websocket=new b(this.prepareUrl()+a);this.websocket.onopen=function(){d.onOpen();d.socket.setBuffer(!1)};this.websocket.onmessage=function(a){d.onData(a.data)};this.websocket.onclose=function(){d.onClose();d.socket.setBuffer(!0)};this.websocket.onerror=
function(a){d.onError(a)};return this};a.prototype.send=function(a){this.websocket.send(a);return this};a.prototype.payload=function(a){for(var d=0,b=a.length;d<b;d++)this.packet(a[d]);return this};a.prototype.close=function(){this.websocket.close();return this};a.prototype.onError=function(a){this.socket.onError(a)};a.prototype.scheme=function(){return this.socket.options.secure?"wss":"ws"};a.check=function(){return"WebSocket"in c&&!("__addTask"in WebSocket)||"MozWebSocket"in c};a.xdomainCheck=function(){return!0};
e.transports.push("websocket")})("undefined"!=typeof io?io.Transport:module.exports,"undefined"!=typeof io?io:module.parent.exports,this);
(function(g,e,c){function a(a){a&&(e.Transport.apply(this,arguments),this.sendBuffer=[])}function f(){}g.XHR=a;e.util.inherit(a,e.Transport);a.prototype.open=function(){this.socket.setBuffer(!1);this.onOpen();this.get();this.setCloseTimeout();return this};a.prototype.payload=function(a){for(var b=[],c=0,f=a.length;c<f;c++)b.push(e.parser.encodePacket(a[c]));this.send(e.parser.encodePayload(b))};a.prototype.send=function(a){this.post(a);return this};a.prototype.post=function(a){function b(){if(4==
this.readyState)if(this.onreadystatechange=f,g.posting=!1,200==this.status)g.socket.setBuffer(!1);else g.onClose()}function e(){this.onload=f;g.socket.setBuffer(!1)}var g=this;this.socket.setBuffer(!0);this.sendXHR=this.request("POST");c.XDomainRequest&&this.sendXHR instanceof XDomainRequest?this.sendXHR.onload=this.sendXHR.onerror=e:this.sendXHR.onreadystatechange=b;this.sendXHR.send(a)};a.prototype.close=function(){this.onClose();return this};a.prototype.request=function(a){var b=e.util.request(this.socket.isXDomain()),
c=e.util.query(this.socket.options.query,"t="+ +new Date);b.open(a||"GET",this.prepareUrl()+c,!0);if("POST"==a)try{b.setRequestHeader?b.setRequestHeader("Content-type","text/plain;charset=UTF-8"):b.contentType="text/plain"}catch(f){}return b};a.prototype.scheme=function(){return this.socket.options.secure?"https":"http"};a.check=function(a,b){try{var f=e.util.request(b),g=c.XDomainRequest&&f instanceof XDomainRequest,p=(a&&a.options&&a.options.secure?"https:":"http:")!=c.location.protocol;if(f&&(!g||
!p))return!0}catch(j){}return!1};a.xdomainCheck=function(){return a.check(null,!0)}})("undefined"!=typeof io?io.Transport:module.exports,"undefined"!=typeof io?io:module.parent.exports,this);
(function(g,e){function c(a){e.Transport.XHR.apply(this,arguments)}g.htmlfile=c;e.util.inherit(c,e.Transport.XHR);c.prototype.name="htmlfile";c.prototype.get=function(){this.doc=new (window[["Active"].concat("Object").join("X")])("htmlfile");this.doc.open();this.doc.write("<html></html>");this.doc.close();this.doc.parentWindow.s=this;var a=this.doc.createElement("div");a.className="socketio";this.doc.body.appendChild(a);this.iframe=this.doc.createElement("iframe");a.appendChild(this.iframe);var c=
this,a=e.util.query(this.socket.options.query,"t="+ +new Date);this.iframe.src=this.prepareUrl()+a;e.util.on(window,"unload",function(){c.destroy()})};c.prototype._=function(a,c){this.onData(a);try{var d=c.getElementsByTagName("script")[0];d.parentNode.removeChild(d)}catch(b){}};c.prototype.destroy=function(){if(this.iframe){try{this.iframe.src="about:blank"}catch(a){}this.doc=null;this.iframe.parentNode.removeChild(this.iframe);this.iframe=null;CollectGarbage()}};c.prototype.close=function(){this.destroy();
return e.Transport.XHR.prototype.close.call(this)};c.check=function(){if("undefined"!=typeof window&&["Active"].concat("Object").join("X")in window)try{return new (window[["Active"].concat("Object").join("X")])("htmlfile")&&e.Transport.XHR.check()}catch(a){}return!1};c.xdomainCheck=function(){return!1};e.transports.push("htmlfile")})("undefined"!=typeof io?io.Transport:module.exports,"undefined"!=typeof io?io:module.parent.exports);
(function(g,e,c){function a(){e.Transport.XHR.apply(this,arguments)}function f(){}g["xhr-polling"]=a;e.util.inherit(a,e.Transport.XHR);e.util.merge(a,e.Transport.XHR);a.prototype.name="xhr-polling";a.prototype.open=function(){e.Transport.XHR.prototype.open.call(this);return!1};a.prototype.get=function(){function a(){if(4==this.readyState)if(this.onreadystatechange=f,200==this.status)g.onData(this.responseText),g.get();else g.onClose()}function b(){this.onerror=this.onload=f;g.onData(this.responseText);
g.get()}function e(){g.onClose()}if(this.open){var g=this;this.xhr=this.request();c.XDomainRequest&&this.xhr instanceof XDomainRequest?(this.xhr.onload=b,this.xhr.onerror=e):this.xhr.onreadystatechange=a;this.xhr.send(null)}};a.prototype.onClose=function(){e.Transport.XHR.prototype.onClose.call(this);if(this.xhr){this.xhr.onreadystatechange=this.xhr.onload=this.xhr.onerror=f;try{this.xhr.abort()}catch(a){}this.xhr=null}};a.prototype.ready=function(a,b){var c=this;e.util.defer(function(){b.call(c)})};
e.transports.push("xhr-polling")})("undefined"!=typeof io?io.Transport:module.exports,"undefined"!=typeof io?io:module.parent.exports,this);
(function(g,e,c){function a(a){e.Transport["xhr-polling"].apply(this,arguments);this.index=e.j.length;var b=this;e.j.push(function(a){b._(a)})}var f=c.document&&"MozAppearance"in c.document.documentElement.style;g["jsonp-polling"]=a;e.util.inherit(a,e.Transport["xhr-polling"]);a.prototype.name="jsonp-polling";a.prototype.post=function(a){function b(){c();f.socket.setBuffer(!1)}function c(){f.iframe&&f.form.removeChild(f.iframe);try{n=document.createElement('<iframe name="'+f.iframeId+'">')}catch(a){n=
document.createElement("iframe"),n.name=f.iframeId}n.id=f.iframeId;f.form.appendChild(n);f.iframe=n}var f=this,g=e.util.query(this.socket.options.query,"t="+ +new Date+"&i="+this.index);if(!this.form){var j=document.createElement("form"),i=document.createElement("textarea"),r=this.iframeId="socketio_iframe_"+this.index,n;j.className="socketio";j.style.position="absolute";j.style.top="0px";j.style.left="0px";j.style.display="none";j.target=r;j.method="POST";j.setAttribute("accept-charset","utf-8");
i.name="d";j.appendChild(i);document.body.appendChild(j);this.form=j;this.area=i}this.form.action=this.prepareUrl()+g;c();this.area.value=e.JSON.stringify(a);try{this.form.submit()}catch(m){}this.iframe.attachEvent?n.onreadystatechange=function(){"complete"==f.iframe.readyState&&b()}:this.iframe.onload=b;this.socket.setBuffer(!0)};a.prototype.get=function(){var a=this,b=document.createElement("script"),c=e.util.query(this.socket.options.query,"t="+ +new Date+"&i="+this.index);this.script&&(this.script.parentNode.removeChild(this.script),
this.script=null);b.async=!0;b.src=this.prepareUrl()+c;b.onerror=function(){a.onClose()};c=document.getElementsByTagName("script")[0];c.parentNode.insertBefore(b,c);this.script=b;f&&setTimeout(function(){var a=document.createElement("iframe");document.body.appendChild(a);document.body.removeChild(a)},100)};a.prototype._=function(a){this.onData(a);this.open&&this.get();return this};a.prototype.ready=function(a,b){var c=this;if(!f)return b.call(this);e.util.load(function(){b.call(c)})};a.check=function(){return"document"in
c};a.xdomainCheck=function(){return!0};e.transports.push("jsonp-polling")})("undefined"!=typeof io?io.Transport:module.exports,"undefined"!=typeof io?io:module.parent.exports,this);