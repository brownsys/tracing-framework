// Subscribes via websocket, aggregates data, and calls callbacks when new data is received
function Subscriber() {

  var websocket_url = "ws://"+window.location.host+"/ws/";
  var websocket = new WebSocket(websocket_url);
  websocket.onmessage = function(event) {
    cbs["report"].call(this, new ResourceReport(JSON.parse(event.data)));
  };
  
  var cbs = {
      "report": function() {}
  }
  
  var subscriber = {};
  
  subscriber.on = function(evt, _) {
    if (arguments.length==1)
      return cbs[evt];
    else if (arguments.length==2)
      cbs[evt]=_;
  }
  
  return subscriber;
}

