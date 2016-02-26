var Group = function(){};
Group.prototype.Lanes = function() { return []; };

Group.prototype.Events = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Events(); })); };
Group.prototype.Spans = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Spans(); })); };
Group.prototype.Threads = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Threads(); })); };
Group.prototype.Processes = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Processes(); })); };
Group.prototype.Tasks = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Tasks(); })); };
Group.prototype.Edges = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Edges(); })); };
Group.prototype.GC = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.GC(); }));};
Group.prototype.HDD = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.HDD(); }));};
Group.prototype.Network = function() { return [].concat.apply([], this.Lanes().map(function(lane) { return lane.Network(); }));};

Group.prototype.Fill = function(_) { if (!arguments.length) return this.fill ? this.fill : 0; this.fill = _; return this; };
Group.prototype.Height = function(_) { var s=this.Spacing(); if (this.Lanes().length==0) return 0; return this.Lanes().map(function(l) { return l.Height(); }).reduce(function(a,b) { return a+b+s; }); };
Group.prototype.Offset = function(_) { 
  if (!arguments.length) 
    return this.offset ? this.offset : 0; 
  this.offset = _;
  var spacing = this.Spacing();
  this.Lanes().forEach(function(lane) { lane.Offset(_); _+=lane.Height()+spacing; });
  return this; 
};
Group.prototype.Spacing = function(_) {
  if (!arguments.length)
    return this.spacing ? this.spacing : 0;
  this.spacing = _;
  this.Offset(this.Offset());
  return this;
};

Group.Scale = function(scale) {
  return {
    Height: function(group) { return scale(group.Height()); },
    Offset: function(group) { return scale(group.Offset()); }
  };
};

var ProcessGroup = function(layout, process) {
  // Save the arguments
  this.layout = layout;
  this.process = process;
  
  // Create the lanes
  var group = this;
  this.lanes = process.Threads().map(function(thread) { return new ThreadLane(group, thread); });

  // Generate a background colour for this group
  this.Fill(d3.rgb(200+Math.random()*20, 200+Math.random()*20, 200+Math.random()*20));
  this.lanes.forEach(function(lane) { lane.Fill(group.Fill()); });
  
  // Set initial spacing and offset for lanes
  this.Spacing(1).Offset(0);
  
  // Save the group on the GC events
  var group = this;
  this.Events().forEach(function(evt) { evt.group = group; });
  this.Spans().forEach(function(spn) { spn.group = group; });
  this.GC().forEach(function(gc) { gc.group = group; });
  this.Edges().filter(function(edge) { return edge.parent.lane!=edge.child.lane && edge.parent.group==group; }).forEach(function(edge) { edge.type = "group"; });
};
ProcessGroup.prototype = new Group();
ProcessGroup.prototype.Lanes = function() { return this.lanes; };
ProcessGroup.prototype.GC = function() { return this.process.GCEvents(); };

var TenantGroup = function(layout, tasks) {
  this.layout = layout;
  this.tasks = tasks;
  
  // Create the lanes
  var group = this;
  this.lanes = tasks.map(function(task) { return new TaskLane(group, task); });

  // Generate a background colour for this group
  this.Fill(d3.rgb(200+Math.random()*20, 200+Math.random()*20, 200+Math.random()*20));
  this.lanes.forEach(function(lane) { lane.Fill(group.Fill()); });
  
  // Set initial spacing and offset for lanes
  this.Spacing(1).Offset(0);
  
  // Save the group on things
  tasks.forEach(function(task) { task.group = group; });
  this.HDD().forEach(function(evt) { evt.group = group; });
  this.Network().forEach(function(evt) { evt.group = group; });
};
TenantGroup.prototype = new Group();
TenantGroup.prototype.Lanes = function() { return this.lanes; };

//var CompactProcessGroup = function(layout, process) {
//  
//};
//
//function SpanGroup(threads) {
//  var groups = [];
//  
//  var makegroup = function() {
//    var group = [];
//    var id = groupid+"_group"+groups.length;
//    group.ID = function() { return id; };
//    group.End = function() { return group.length==0 ? 0 : group[group.length-1].End(); }
//    group.Events = function() { return [].concat.apply([], group.map(function(span) { return span.Events(); })); };
//    group.Spans = function() { return group; };
//    return group;
//  };
//  
//  for (var i = 0; i < numgroups; i++)
//    groups.push(makegroup(i));
//
//  var affinity = {};
//  for (var i = 0; i < threaddata.length; i++)
//    affinity[threaddata[i].ID()] = i % numgroups;
//  
//  var spans = [].concat.apply([], threaddata.map(function(thread) { return thread.Spans(); }));
//  spans.sort(function(a, b) { return a.Start() - b.Start(); });
//  
//  for (var i = 0; i < spans.length; i++) {
//    var span = spans[i], spanaffinity = affinity[span.thread.ID()];
//    for (var j = 0; j < numgroups; j++) {
//      var k = (j + spanaffinity) % numgroups;
//      if (groups[k].End() <= span.Start()) {
//        groups[k].push(span);
//        break;
//      }
//    }
//  }
//  
//  return groups;
//};