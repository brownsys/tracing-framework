/* This is used for javascripts threading approximation, 'web workers', to process task data in a separate thread, so as not to impact the viz */
importScripts("Workload.js", "../../xtrace_utils.js", "VizLayout.js", "VizGroup.js", "VizLane.js");

var workload = new Workload([]);
var layout;

onmessage = function(evt) {
  var data = JSON.parse(evt.data);
  data.forEach(function(task) { 
    sanitizeReports(task.reports);
    workload.addTask(task);
  });
  layout = new PerTaskLayout(workload);
  postMessage(layout);
};