/*
 * Collects reports and provides filter dropdowns for filtering reports.
 * Displays a number of graphs of the filtered reports
 */

var FilterDataSet = function(){
  
  // Filterable values for each property based on values observed in reports received
  var properties = {
      "host": {},
      "process": {},
      "type": {},
      "resourcetype": {},
      "operation": {},
      "tenant": {}
  };
  
  // List of n most recent reports, used for constructing new graphs
  var n = 1000;
  var reports = [];

  // For displayable reports, group reports by this function
  var groupby = function(report) { return "all"; };
  
  // Filter reports to not be displayed
  var filters = {
      "default": function(report) { return true; }
  };
  
  // Callbacks
  var on = {
      "refresh": function() {},
      "property": function() {}
  };

  var dataset = {};
  
  // Gets and sets callbacks
  dataset.on = function(evt, f) { if (arguments.length==1) return on[evt]; on[evt] = f; return dataset; };
  
  // Sets the property name that should be used to group reports into datasets
  dataset.groupby = function(property) {
    if (!arguments.length)
      return groupby;
    
    if (properties[property])
      groupby = function(report) { return report[property]; };
    else
      groupby = function(report) { return "all"; };

    dataset.refresh.call(this);
  };
  
  // Sets a filter on any property
  dataset.filter = function(property, value) {
    // Clear the existing filter for this property if it exists
    if (filters[property])
      delete filters[property];
    
    // Apply the filter if permissible
    if (properties[property] && properties[property][value])
      filters[property] = function(report) { return report[property]==value; };
    
    // Refresh
    dataset.refresh.call(this);
  };
  
  dataset.datasets = [null, null, null, null];
  
  var OPS = function(report) { return report.count / (report.end-report.start); }
  var WPS = function(report) { return report.consumption / (report.end - report.start); };
  var LPO = function(report) { return report.duration / report.count / 1000000.0 };
  var WPO = function(report) { return report.consumption / report.count };
  
  // Reconstructs the dataset from scratch, applies the reports we've seen thus far, then calls the 'refresh' callback
  dataset.refresh = function() {
    // Construct the new dataset
    dataset.datasets[0] = DataSet().name("Ops/second").accepts(Object.values(filters)).seriesid(groupby).seriesname(groupby).y(OPS).yname("Operations per second").showtotal(false);
    dataset.datasets[1] = DataSet().name("Work/second").accepts(Object.values(filters)).seriesid(groupby).seriesname(groupby).y(WPS).yname("Work per second").showtotal(false);
    dataset.datasets[2] = DataSet().name("Latency/op (ms)").accepts(Object.values(filters)).seriesid(groupby).seriesname(groupby).y(LPO).yname("Latency per operation").showtotal(false);
    dataset.datasets[3] = DataSet().name("Work/op").accepts(Object.values(filters)).seriesid(groupby).seriesname(groupby).y(WPO).yname("Work per operation").showtotal(false);
    
    // Add reports to the dataset
    for (var i = 0; i < reports.length; i++) {
      for (var j = 0; j < reports[i].tenantreports.length; j++) {
        for (var k = 0; k < dataset.datasets.length; k++) {
          dataset.datasets[k].collect(reports[i].tenantreports[j]);          
        }
      }
    }
    
    // Call the refresh callback
    dataset.on("refresh").call(this, dataset);
  };
  
  // Collects a new report
  dataset.collect = function(report) { 
    for (var i = 0; report.tenantreports && i < report.tenantreports.length; i++) {
      // Save all unique properties
      var tenantreport = report.tenantreports[i];
      for (var property in properties) {
        if (!properties[property][tenantreport[property]]) {
          properties[property][tenantreport[property]] = true;
          dataset.on("property").call(this, property);
        }
      }
    
      // Pass the report to the datasets
      for (var j = 0; j < dataset.datasets.length; j++) {
        dataset.datasets[j].collect(tenantreport);
      }
    }
    
    // Save the report
    reports.push(report);
    if (reports.length > n)
      reports = reports.splice(reports.length-n, n);
  };
  
  // Get a list of filterable properties
  dataset.properties = function() { return Object.keys(properties); };
  
  // Get a list of observed property values
  dataset.values = function(property) { return Object.keys(properties[property]); };
  
  return dataset;
};