var PrototypeBuilder = function() {
  var getter = null;
  var accessors = [];
  var mappers = [];
  var id = true;
  var times = true;
  
  var build = function(cls) {
    // First, add the ID and getID methods if requested
    if (id) {
      cls.prototype.ID = function() { return this.fqid; };
      cls.prototype.globalID = function() { return this.id; };
      cls.getID = function(obj) { return obj.ID(); };
      cls.getGlobalID = function(obj) { return obj.globalID(); };
    }
    
    // The getter is just an accessor
    if (getter!=null) {
      cls.prototype.getter = getter;
      accessors.push(getter);
    }
    
    // Accessors access data fields
    for (var i = 0; i < accessors.length; i++) {
      var accessor = accessors[i];
      var accessor_data_field = accessor.toLowerCase();
      cls.prototype[accessor] = function(field) { return function() { return this[field]; }; }(accessor_data_field);
    }

    // At this point, remaining methods need a getter.  If no getter, return
    if (getter==null)
      return;
    
    // Mappers simply call the mapper method on each of the elements returned by the getter
    for (var i = 0; i < mappers.length; i++) {
      cls.prototype[mappers[i]] = function(mapper) {
        return function(/*optional*/dontcache) {
          if (this[mapper.toLowerCase()])
            return this[mapper.toLowerCase()];
          var ret = [].concat.apply([], this[this.getter]().map(function(elem) { return elem[mapper](true); }));
          if (!dontcache)
            this[mapper.toLowerCase()] = ret;
          return ret;
        };
      }(mappers[i]);
    };
    
    // Times call the max/min of the times of the getter
    if (times) {
      cls.prototype.Start = function() {
        if (this.start==null)
          this.start = Math.min.apply(this, this[this.getter]().map(function(elem) { return elem.Start(); }));
        return this.start;
      };
      cls.prototype.End = function() {
        if (this.end==null)
          this.end = Math.max.apply(this, this[this.getter]().map(function(elem) { return elem.End(); })); 
        return this.end; 
      };
      cls.prototype.Duration = function() {
        return this.End() - this.Start();
      };
    }
  };

  build.getter = function(_) { if (!arguments.length) return getter; getter = _; return build; };
  build.accessors = function(_) { if (!arguments.length) return accessors; accessors = _; return build; };
  build.mappers = function(_) { if (!arguments.length) return mappers; mappers = _; return build; };
  build.id = function(_) { if (!arguments.length) return id; id = _; return build; };
  build.times = function(_) { if (!arguments.length) return times; times = _; return build; };
  
  return build;
};



var XEvent = function(span, report) {
	this.report = report;
	this.span = span;
  this.id = report.id;
	this.fqid = this.id;
	this.timestamp = report.Timestamp
	this.type = "event";
	if (report["Operation"])
		this.type = "operation " + report.Operation;
	this.start = this.timestamp;
	this.end = this.timestamp;
	this.duration = 0;
	if (report["Duration"]) {
		this.duration = Number(report["Duration"]) / 1000000.0;
		this.start = this.timestamp - this.duration;
		this.end = this.timestamp;
	}
	if (report["Operation"] && report.Operation.substr(0, 4)=="file" && report.Agent.indexOf("ScheduledFileIO")!=-1) {
	  var keys = ["PreWait", "PreDuration", "IOWait", "IODuration", "PostWait", "PostDuration"];
	  this.duration = 0;
	  for (var i = 0; i < keys.length; i++) {
	    if (this.report[keys[i]])
	      this.duration += Number(this.report[keys[i]]);
	  }
	  this.duration = this.duration / 1000000.0;
	  this.start = this.timestamp - this.duration;
	  this.end = this.timestamp;
	}

	this.span.thread.process.machine.task.reports_by_id[this.id] = this;
};
XEvent.prototype.Edges = function() {
  if (this.edges==null) {
    this.edges = [];
    var parents = this.report.ParentEventID;
    for (var i = 0; parents!=null && i < parents.length; i++) {
      var edge = {
          id: this.id+parents[i],
          parent: this.span.thread.process.machine.task.reports_by_id[parents[i]],
          child: this
      };
      if (edge.parent && edge.child) 
        this.edges.push(edge);
    }
  }
  return this.edges;    
};
PrototypeBuilder().accessors(["Timestamp"])(XEvent);



var XSpan = function(thread, id, reports) {
	this.thread = thread;
	this.id = this.thread.fqid + "_Span-" + id;
	this.fqid = this.id;
	this.events = [];
	this.waiting = false; // is this a span where a thread is waiting?
	for (var i = 0; i < reports.length; i++) {
		if (reports[i].Operation && reports[i].Operation.substring(0, 4)=="file") {
			this.events.push(new XEvent(this, reports[i]));
		} else {
			this.events.push(new XEvent(this, reports[i]));
		}
	}
	this.events.sort(function(a, b) { return a.timestamp - b.timestamp; });
	this.start = this.events[0].Timestamp();
	this.end = this.events[this.events.length-1].Timestamp();
  this.hddevents = this.Events().filter(function(event) { return event.report.Operation && event.report.Operation.substring(0, 4)=="file"; });
  this.networkevents = this.Events().filter(function(event) { return event.report.Operation && event.report.Operation.substring(0, 3)=="net"; }); 
};
PrototypeBuilder().getter("Events").accessors(["HDDEvents","NetworkEvents"]).mappers(["Edges"])(XSpan);



var XThread = function(process, id, reports) {
	reports.sort(function(a, b) { return a.Timestamp - b.Timestamp; });
	this.process = process;
  this.id = this.process.id + "_Thread-"+ id;
  this.fqid = this.process.fqid + "_Thread-"+ id;

	this.spans = [];
	var span = [];
	for (var i = 0; i < reports.length; i++) {
		if (reports[i].Operation && reports[i].Operation=="waited") {
			/* Special case: a 'wait' report.  A 'wait' report translates into two events; a start and end.
			 * A 'wait' report is generated at the end of the wait, and contains a field specifying the duration
			 * of the wait.  So we must manually reconstruct the begin event of the wait */

			// The duration of the wait event
			var duration = Number(reports[i].Duration) / 1000000.0;

			// Add an event to the end of the prior span and modify the timestamp
			span.push(reports[i]);
			var preWait = new XSpan(this, this.spans.length, span);
			var preWaitEndEvent = preWait.events[preWait.events.length-1];
			this.spans.push(preWait);

			// Create a span just for the event
			var Wait = new XSpan(this, this.spans.length, [reports[i], reports[i]]);
			Wait.waiting = true;
			Wait.events[0].timestamp = Wait.events[0].timestamp - duration;
			preWaitEndEvent.timestamp = Wait.events[0].timestamp; // modify the timestamp of the end event of the prior span
			this.spans.push(Wait);

			// Fix start/end ts (a hack, whatever)
			preWait.end = preWaitEndEvent.timestamp;
			Wait.start = Wait.events[0].timestamp;

			// Create the start of the next span;
			span = [reports[i]];
		} else if (reports[i].Operation && reports[i].Operation=="unset") {
			span.push(reports[i]);
			this.spans.push(new XSpan(this, this.spans.length, span));
			span = [];
		} else {
			span.push(reports[i]);            
		}
	}
	if (span.length > 0)
		this.spans.push(new XSpan(this, this.spans.length, span));
	this.spans.sort(function(a, b) { return a.Start() - b.Start(); });
	
	// Now set the short name of this thread
  this.shortname = "Thread-"+this.id;
  var names = {};
  names[this.shortname] = true;
  var events = this.Events();
  for (var i = 0; i < events.length; i++) {
    if (events[i].report.ThreadName)
      names[events[i].report.ThreadName] = true;
  }
  delete names[this.shortname];
  var othernames = Object.keys(names);
  if (othernames.length > 0) {
    var selected = othernames[0];
    if (selected.length > 20)
      this.shortname = selected.substring(0, 20)+"...";
    else
      this.shortname = selected;
  }
};
PrototypeBuilder().getter("Spans").accessors(["ShortName"]).mappers(["Events", "Edges", "HDDEvents", "NetworkEvents"])(XThread);



var XProcess = function(machine, id, reports) {
	this.machine = machine;
	this.processid = id;
	this.id = this.machine.id + "_Process-" + id.replace("@","");
	this.fqid = this.machine.fqid + "_Process-"+id.replace("@","");
	this.gcevents = [];

	// We want high resolution timestamps, so perform some averaging
	if (reports[0]["HRT"]) {
		var totalTS = 0.0;
		var totalHRT = 0.0;
		var count = 0.0;
		for (var i = 0; i < reports.length; i++) {
			totalTS += Number(reports[i].Timestamp);
			totalHRT += Number(reports[i].HRT);
			count += 1.0;
		}

		var avgHRT = totalHRT / count;
		var avgTS = totalTS / count;
		for (var i = 0; i < reports.length; i++) {
			var reportHRT = Number(reports[i].HRT);
			var reportTS = avgTS + (reportHRT - avgHRT) / 1000000.0;
			reports[i].Timestamp = reportTS;
		}
	}

	var reports_by_thread = group_reports_by_field(reports, "ThreadID");

	this.threads = [];
	for (var thread_id in reports_by_thread)
		this.threads.push(new XThread(this, thread_id, reports_by_thread[thread_id]));
	this.threads.sort(function(a, b) { return a.Start() - b.Start(); });
};
XProcess.prototype.addGCData = function(gcdata) {
  var gcreports = gcdata[this.processid];
  if (gcreports) {
    var process = this;
    this.gcevents = gcreports.map(function(report) { return new GCEvent(process, report); });
    this.gcevents = this.gcevents.filter(function(gcevent) { 
      return gcevent.start <= process.End() && gcevent.end >= process.Start() && gcevent.duration > 0; 
    });
  };
}
PrototypeBuilder().getter("Threads").accessors(["GCEvents"]).mappers(["Spans", "Events", "Edges", "HDDEvents", "NetworkEvents"])(XProcess);



var XMachine = function(task, id, reports) {
	this.task = task;
	this.id = "Machine-"+id;
	this.fqid = this.task.ID() + "_" + this.id;
	this.llid = "Machine-"+this.id;

	var reports_by_process = group_reports_by_field(reports, "ProcessID");

	this.processes = [];
	for (var process_id in reports_by_process) {
		this.processes.push(new XProcess(this, process_id, reports_by_process[process_id]));
	}
	this.processes.sort(function(a, b) { return a.Start() - b.Start(); });
};
PrototypeBuilder().getter("Processes").mappers(["Threads", "Spans", "Events", "Edges", "HDDEvents", "NetworkEvents", "GCEvents"])(XMachine);



var XTask = function(data) {
  // Copy the params
  this.id = data.id;
  this.fqid = "Task-"+this.id;
  this.reports = data.reports;
  this.reports_by_id = {};

  for (var i = 0; i < this.reports.length; i++) {
    var report = this.reports[i];
    if (report.EventID==null)
      report.id = ""+(Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000);
    else
      report.id = report.EventID;
    this.reports_by_id[report.id] = report;
  }

  // Create the data structures
  this.machines = [];
  var reports_by_machine = group_reports_by_field(data.reports, "Host");
  for (var machine_id in reports_by_machine)
    this.machines.push(new XMachine(this, machine_id, reports_by_machine[machine_id]));
  this.machines.sort(function(a, b) { return a.Start() - b.Start(); });
  
  // Extract the tags
  var tags = {};
  for (var i = 0; i < this.reports.length; i++) {
    if (this.reports[i]["Tag"])
      for (var j = 0; j < this.reports[i]["Tag"].length; j++)
        tags[this.reports[i]["Tag"][j]] = true;
  }
  this.tags = Object.keys(tags);
};
PrototypeBuilder().getter("Machines").accessors(["Tags"]).mappers(["Processes", "Threads", "Spans", "Events", "Edges", "GCEvents", "HDDEvents", "NetworkEvents"])(XTask);



var Workload = function(data, gcdata) {
  window.workload = this;
  this.data = [];
  this.gcdata = gcdata;
  this.id = unique_id();
  this.fqid = this.id;

  // Create the data structures
  this.tasks = [];
  for (var i = 0; i < data.length; i++) {
    this.addTask(data[i]);
  }
};
Workload.prototype.addTask = function(data) {
  this.data.push(data);
  var task = new XTask(data);
  this.tasks.push(task);
  this.tasks.sort(function(a, b) { return a.Start() - b.Start(); });
  this.start=null; this.end=null;
  this.min = this.Start();
  this.max = this.End();
  var gcdata = this.gcdata;
  if (gcdata)
    task.Processes().forEach(function(process) { process.addGCData(gcdata); });
};
Workload.prototype.addGC = function(gcdata) {
  this.gcdata = gcdata;
  if (this.gcdata)
    this.Processes().forEach(function(process) { process.addGCData(gcdata); });
};
PrototypeBuilder().getter("Tasks").mappers(["Machines", "Processes", "Threads", "Spans", "Events", "Edges", "GCEvents", "HDDEvents", "NetworkEvents"])(Workload);



var GCEvent = function(process, report) {
  this.report = report;
  this.process = process;
  if (report.EventID==null)
    report.id = ""+(Math.floor(Math.random() * (9999999 - 1000000 + 1)) + 1000000);
  else
    report.id = report.EventID;
  this.xtraceid = report.id;
  this.id = this.process.fqid + "_GC-" + this.xtraceid;
  this.fqid = this.id;

  this.start = Number(this.report["GcStart"])+1;
  this.duration = Number(this.report["GcDuration"])-1;
  this.end = this.start + this.duration;
  this.name = this.report["GcName"];
};
PrototypeBuilder().accessors(["Start", "Duration", "Name"])(GCEvent);