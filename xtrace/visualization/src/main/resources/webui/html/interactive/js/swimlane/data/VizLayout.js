var Layout = function() {};
Layout.prototype.Get = function(func) { return [].concat.apply([], this.Groups().map(function(group) { return group[func].call(group); })); };

Layout.prototype.Workload = function() { return this.workload; };
Layout.prototype.Groups = function() { return []; };
Layout.prototype.Lanes = function() { return this.Get("Lanes"); };

Layout.prototype.Events = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Events(); })); };
Layout.prototype.Spans = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Spans(); })); };
Layout.prototype.Threads = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Threads(); })); };
Layout.prototype.Processes = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Processes(); })); };
Layout.prototype.Tasks = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Tasks(); })); };
Layout.prototype.Edges = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Edges(); })); };
Layout.prototype.GC = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.GC(); }));};
Layout.prototype.HDD = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.HDD(); }));};
Layout.prototype.Network = function() { return [].concat.apply([], this.Groups().map(function(group) { return group.Network(); }));};

Layout.prototype.Height = function() { var spacing = this.Spacing(); if (this.Groups().length==0) return 0; return this.Groups().map(function(g) { return g.Height(); }).reduce(function(a,b) { return a+b+spacing; }); };
Layout.prototype.Spacing = function(_) {
  if (!arguments.length)
    return this.spacing ? this.spacing : 0;
  this.spacing = _;
  var offset = 0;
  this.Groups().forEach(function(group) { group.Offset(offset); offset+=group.Height()+_; });
  return this;
};

var PerTaskLayout = function(workload) {
//  new PerTenantLayout(workload);
  // Save the arguments
  this.workload = workload;
  
  // Create the groups
  var layout = this;
  this.groups = workload.Processes().map(function(process) { return new ProcessGroup(layout, process); });
  this.Edges().filter(function(edge) { return edge.parent.group!=edge.child.group; }).forEach(function(edge) { edge.type = "layout"; });
  
  // Set the process spacing initially to, say, 5
  this.Spacing(5);
};
PerTaskLayout.prototype = new Layout();
PerTaskLayout.prototype.Groups = function() { return this.groups; };
PerTaskLayout.prototype.Height = function() { var s=this.Spacing(); if (this.Groups().length==0) return 0; return this.Groups().map(function(g) { return g.Height(); }).reduce(function(a,b) { return a+b+s; }); };

var PerTenantLayout = function(workload) {
  this.workload = workload;
  
  // First, we determine the tenants of the workload
  var tenant_id_tag_prefix = "Worker_";
  var groupings = {};
  var tasks = workload.Tasks();
  for (var i=0; i<tasks.length; i++) {
    var task = tasks[i];
    var tags = task.Tags();
    var workerid = "(No worker ID set)";
    for (var j = 0; j < tags.length; j++) {
      var tag = tags[j];
      if (tag.substr(0, tenant_id_tag_prefix.length)==tenant_id_tag_prefix) {
        workerid = tag;
        break;
      }
    }
    if (!groupings[workerid])
      groupings[workerid] = [];
    groupings[workerid].push(task);
  }
  
  // Create one group per tenant
  var layout = this;
  this.groups = Object.keys(groupings).map(function(tenantid) { return new TenantGroup(layout, groupings[tenantid]); });

  // Set the process spacing initially to, say, 5
  this.Spacing(5);
};
PerTenantLayout.prototype = new Layout();
PerTenantLayout.prototype.Groups = function() { return this.groups; };