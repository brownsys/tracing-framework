// JSON reports as argument, produce IDs on critical path
function critical_path(reports, finalreport) {
	if (finalreport==null)
		finalreport = reports[reports.length-1];
	
	var reportmap = {};
	for (var i = 0; i < reports.length; i++) {
		reportmap[report_id(reports[i])] = reports[i];
	}
	
	var cpath = [];
	var next = finalreport;
	while (next && next["Edge"]) {
		cpath.push(next);
		var parents = next["Edge"];
		next = reportmap[parents[0]];
		for (var i = 1; next==null && i < parents.length; i++) {
			next = reportmap[parents[i]];
		}
		for (var i = 1; i < parents.length; i++) {
			var candidate = reportmap[parents[i]];
			if (reportmap[parents[i]] && Number(candidate["Timestamp"][0]) > Number(next["Timestamp"][0]))
				next = candidate;
		}
	}
	return cpath;
};

function filter_criticalpath_events(events) {
  var reports = events.map(function(event) { return event.report; });
  
  var finalevent = events[0];
  events.forEach(function(event) {
    if (event.Timestamp() > finalevent.Timestamp())
      finalevent = event;
  });  
  
  var cpath = critical_path(reports, finalevent.report);

  var oncpath = {};
  for (var i = 0; i < cpath.length; i++) {
    oncpath[report_id(cpath[i])] = true;
  }
  
  return events.filter(function(event) { return oncpath[event.id]; });  
}
