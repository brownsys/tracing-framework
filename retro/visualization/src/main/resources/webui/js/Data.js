var series_color_cache = {
    "total": d3.rgb(0,0,0),
    "-1": d3.rgb(128,128,128),
    "1": d3.rgb(255,0,0),
    "2": d3.rgb(0,255,0),
    "3": d3.rgb(0,0,255),
    "4": d3.rgb(255,255,0),
    "5": d3.rgb(0,255,255),
    "6": d3.rgb(255,0,255)
};

/*
 * Data representations for graphs and series
 */
var Series = function(id) {

  var name = "";
  if (!(id in series_color_cache))
    series_color_cache[id] = d3.rgb(Math.random()*255, Math.random()*255, Math.random()*255);
  
  /* Internally, elements are a linked list with references to prev and next,
   * plus we keep an actual list ordered by time, plus we keep a map indexed by time. */
  var elements = [];
  var elementsmap = {};
  
  var series = {};
  
  series.extent = function(getter) {
    return d3.extent(elements, getter);    
  };
  
  series.prune = function(minvalue) {
    if (elements.length > 0 && elements[elements.length-1].time < minvalue)
      return true;
    var cutoff = 0;
    while (cutoff < elements.length-2 && elements[cutoff+2].time < minvalue) {
      delete elements[cutoff].next["prev"];
      delete elementsmap[elements[cutoff].time];
      cutoff++;
    }
    elements.splice(0, cutoff);    
    return elements.length==0;
  };

  series.addData = function(from, to, consumption) {
    addPoint(from);
    addPoint(to);
    var elem = elementsmap[from];
    while (elem.next!=null && elem.time<to) {
      elem.usage += consumption;
      elem = elem.next;
    }
  };
  
  series.data = function() {
    var points = [];
    for (var i = 0; i < elements.length-1; i++) {
      var cur = elements[i];
      var next = elements[i+1];
      if (cur.usage==0) {
        points.push({time: cur.time, usage: 0});
        points.push({time: next.time, usage: 0});
      } else {
        points.push({time: (3*cur.time/4 + next.time/4), usage: cur.usage});
        points.push({time: (cur.time/4 + 3*next.time/4), usage: cur.usage});
      }
    }
    if (elements.length > 1)
      points.push({time: elements[elements.length-1].time, usage: elements[elements.length-2].usage});
    return points;
  };
  
  series.last = function() {
    if (elements.length < 3)
      return elements[0];
    return elements[elements.length-3];
  }
  
  series.length = function() {
    return elements.length;
  };
  
  series.id = function(_) { if (!arguments.length) return id; id = _; return series; }
  series.name = function(_) { if (!arguments.length) return name; name = _; return series; }
  series.color = function(_) { if (!arguments.length) return series_color_cache[id]; series_color_cache[id] = _; return series; }
  
  var addPoint = function(time) {
    // Only add points if they don't already exist
    if (elementsmap[time]) return;
    
    // Create an empty element; we will fill in the appropriate values once it's inserted
    var elem = {time: time, usage: 0};
    elementsmap[time] = elem;
    
    // For efficiency, start at the end of the elements and work in reverse
    var i = elements.length;
    while (i > 0 && elements[i-1].time > time)
      i--;
    
    // Preserve the linked list
    if (i > 0) {
      var prev = elements[i-1];
      elem.prev = prev;
      prev.next = elem;
      elem.usage = elem.prev.usage;
    }
    if (i < elements.length) {
      var next = elements[i];
      elem.next = next;
      next.prev = elem;
    }
    
    // Insert the element
    elements.splice(i, 0, elem);
  }
  
  return series;
};


/* Wrapper around json representation of report received over websocket */
function ResourceReport(json) {
  this.json = json;
  this.tenantreports = [];
  for (var i = 0; i < json.tenantReports.length; i++) {
    this.tenantreports.push(new TenantReport(this, json.tenantReports[i]));
  }
}

function TenantReport(resource, json) {
  this.resource = resource;
  this.json = json;
  this.start = resource.json.start / 1000.0;
  this.end = resource.json.end / 1000.0;
  this.type = resource.json.resource;
  this.host = resource.json.machine;
  if (resource.json.resourceID)
    this.resourcetype = resource.json.resource+" ("+resource.json.resourceID+")";
  else
    this.resourcetype = resource.json.resource;
  this.resourceid = resource.json.resourceID;
  this.process = resource.json.processName+" ("+resource.json.processID+")";
  this.processid = resource.json.processID;
  this.processname = resource.json.processName;
  this.tenant = json.tenantClass;
  this.operation = json.operation;
  this.count = json.numFinished;
  this.duration = json.totalLatency;
  this.consumption = json.totalWork;
}


function DataSet() {
  
  var accepts = [];
  var seriesid = function(report) { return report.tenant; };
  var seriesname = function(report) { return report.tenant==-1 ? "Background" : "Tenant " + report.tenant; }
  var y = function(report) { return report.consumption / 1000000.0; };
  var yname = "Disk Usage (MB)";
  var showtotal = true;
  var showbackground = true;
  var name = "";
  var reportextent = function(report) { return [report.start*1000, report.end*1000]; };
  var miny = 0;
  
  var total = Series("total").name("Total");
  var serieslist = [];
  var seriesmap = {};
  
  var dataset = {};

  dataset.accepts = function() { if (arguments.length==1 && arguments[0] instanceof Array) accepts = arguments[0]; else accepts = arguments; return this; };
  dataset.seriesid = function(_) { if (!arguments.length) return seriesid; seriesid = _; return dataset; };
  dataset.seriesname = function(_) { if (!arguments.length) return seriesname; seriesname = _; return dataset; };
  dataset.y = function(_) { if (!arguments.length) return y; y = _; return dataset; };
  dataset.yname = function(_) { if (!arguments.length) return yname; yname = _; return dataset; };
  dataset.reportextent = function(_) { if (!arguments.length) return reportextent; reportextent = _; return dataset; };
  dataset.showtotal = function(_) { if (!arguments.length) return showtotal; showtotal = _; return dataset; };
  dataset.showbackground = function(_) { if (!arguments.length) return showbackground; showbackground = _; return dataset; };
  dataset.name = function(_) { if (!arguments.length) return name; name = _; return dataset; };
  dataset.miny = function(_) { if (!arguments.length) return miny; miny = _; return dataset; };
  
  dataset.extent = function(getter) {
    if (showtotal)
      return total.extent(getter);
    
    if (serieslist.length==0) return [0,0];
    
    var min = Number.MAX_VALUE;
    var max = -min;
    
    for (var i = 0; i < serieslist.length; i++) {
      var extent = serieslist[i].extent(getter);
      min = Math.min(min, extent[0]);
      max = Math.max(max, extent[1]);
    }
    
    return [min, max];
  };
  
  dataset.prune = function(minvalue) {
    for (var i = 0; i < serieslist.length; i++) {
      var series = serieslist[i];
      if (series.prune(minvalue)) {
        serieslist.splice(i--, 1);
        delete seriesmap[series.id()];
      }
    }
    total.prune(minvalue);
  };
  
  dataset.series = function() {
    if (showtotal)
      return [total].concat(serieslist);
    return serieslist;
  };
  
  // Adds the report if it's valid for this dataset, and returns true if successfully added
  dataset.collect = function(report) {
    for (var i = 0; i < accepts.length; i++)
      if (!accepts[i](report))
        return false;
    
    // Get the y value; if it's zero, no need to add anything
    var yvalue = y(report);
    if (yvalue==0)
      return;
    
    // Get the ID of the series that this report belongs to
    var id = seriesid(report);
    
    // Get the series and create if it doesn't exist
    var series = seriesmap[id];
    if (series==null) {
      series = Series(id).name(seriesname(report));
      seriesmap[id] = series;
      if (seriesname(report)=="Background") {
        if (!showbackground)
          return false;
        serieslist.push(series);
      } else
        serieslist.splice(0, 0, series);
      serieslist.sort(function(a, b) {
        return a.id() - b.id();
      });
    }
    
    // Add the data to the series
    var extent = reportextent(report);
    series.addData(extent[0], extent[1], yvalue);
    total.addData(extent[0], extent[1], yvalue);
    
    return true;
  };
  
  
  
  return dataset;
}

