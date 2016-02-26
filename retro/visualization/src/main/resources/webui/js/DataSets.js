/*
 * Defines different ways of displaying pubsub data as graphs
 */

var DataSets = function(/*optional*/ reports){
  if (!reports) reports = [];
  
  // filters for 'accepts'
  var DISK = function(report) { return report.type=="DISK"; };
  var DISKCACHE = function(report) { return report.type=="DISKCACHE"; };
  var NETWORKREAD = function(report) { return report.type=="NETWORK" && report.operation=="READ"; };
  var HDFSREQUEST = function(report) { return report.type=="HDFSREQUEST"; };
  var LOCK = function(report) { return report.type=="LOCKING"; };
  var CPU = function(report) { return report.type=="CPU"; };
  var THREAD = function(report) { return report.type=="THREAD"; };
  var DATANODE = function(report) { return report.processname=="DataNode" };
  var NAMENODE = function(report) { return report.processname=="NameNode" };
  var TENANT1 = function(report) { return report.tenant=="1"; };
  var DISKRW = function(report) { return report.type=="DISK" && (report.operation=="READ" || report.operation=="WRITE"); };
  
  // functions for 'seriesid'
  var PERTENANT = function(report) { return report.tenant; };
  var PERTENANTANDTYPE = function(report) { return report.tenant + "_" + report.operation; };
  var PERLOCK = function(report) { return report.processid + "_"+report.resourceid; };
  var PERLOCKANDTENANT = function(report) { return report.processid + "_"+report.resourceid + "_" + report.tenant; };
  
  // functions for 'seriesname'
  var TENANTNAME = function(report) { return report.tenant==-1 ? "Background" : "Tenant " + report.tenant; };
  var TENANTANDTYPE = function(report) { return TENANTNAME(report) + " " + report.operation; };
  var LOCKNAME = function(report) { 
    return report.resourceid;
  };
  var LOCKANDTENANTNAME = function(report) { return TENANTNAME(report) + " " + LOCKNAME(report); };
  
  // functions for 'y'
  var CONSUMPTIONPERSECOND = function(report) { return (report.consumption / 1000000.0) / (report.end - report.start); };
  var CYCLES = function(report) { return (report.consumption / 1000000.0) / (report.end - report.start); };
  var CYCLESEFFICIENCY = function(report) { return (report.consumption / 1000000.0) / (report.duration / 1000000000.0); };
  var CYCLESEFFICIENCYPERCENT = function(report) { return 100 * (report.consumption / 1000000.0) / (1800 * report.duration / 1000000000.0); };
  var CPUDELAY = function(report) { return (report.duration / 1000000.0) * (100 - 100 * (report.consumption / 1000000.0) / (1800 * report.duration / 1000000000.0)); };
  var LATENCY = function(report) { return (report.duration / 1000000.0) / report.count; };
  var THROUGHPUT = function(report) { return report.count / (report.end-report.start); }
  var UTILIZATION = function(report) { return 100 * (report.consumption / 1000000000.0) / (report.end - report.start); }
  var CONCURRENCY = function(report) { return report.duration / (1000000000.0 * (report.end - report.start)); };
  var ACQUISITIONTIME = function(report) { return (report.duration - report.consumption) / (report.count * 1000000.0)};
  var HOLDTIME = function(report) { return (report.consumption) / (report.count * 1000000.0)};
  
  // functions for report extent
  var DEFAULT = function(report) { return [report.start*1000, report.end*1000]; };
  
  var datasets = {};

  datasets.TENANT_DATANODE_DISK_USAGE = DataSet().name("DataNode Disk Throughput (MB/s)").accepts(DISK, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                 .y(CONSUMPTIONPERSECOND).yname("Disk Throughput (MB/s)").showtotal(true).miny(10);
  datasets.TENANT_DATANODE_DISK_OPS = DataSet().name("DataNode Disk Op Throughput (ops/s)").accepts(DISK, DATANODE).seriesid(PERTENANTANDTYPE).seriesname(TENANTANDTYPE)
                                                .y(THROUGHPUT).yname("Number of disk operations per second").showtotal(true);
  datasets.TENANT_DATANODE_DISK_LATENCY = DataSet().name("DataNode Disk Latency (ms)").accepts(DISK, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                   .y(LATENCY).yname("Total time in disk per second").showtotal(false).miny(10);
  datasets.TENANT1_DATANODE_DISK_LATENCY = DataSet().name("DataNode Disk Latency (Tenant 1)").accepts(DISK, DATANODE, TENANT1).seriesid(PERTENANT).seriesname(TENANTNAME)
  .y(LATENCY).yname("Total time in disk per second").showtotal(false).miny(10);
  datasets.TENANT_DATANODE_DISKCACHE_USAGE = DataSet().name("DataNode Cache Throughput (MB/s)").accepts(DISKCACHE, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                 .y(CONSUMPTIONPERSECOND).yname("Disk Cache Throughput (MB/s)").showtotal(true);
  datasets.TENANT_DATANODE_DISK_OPLATENCY = DataSet().name("DataNode Disk Op Latency (ms/op)").accepts(DISK, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                 .y(LATENCY).yname("Avg. latency per disk request (ms)").showtotal(false);
  datasets.TENANT_DATANODE_DISK_CONCURRENCY = DataSet().name("DataNode Disk IO concurrency").accepts(DISK, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                 .y(CONCURRENCY).yname("Avg. num requests pending at disk").showtotal(false);  
  datasets.TENANT_NETWORK_USAGE = DataSet().name("HDFS Network Utilization (MB/s)").accepts(NETWORKREAD).seriesid(PERTENANT).seriesname(TENANTNAME)
                                           .y(CONSUMPTIONPERSECOND).yname("Network Usage (MB/s)").showtotal(true).miny(10);
  datasets.LOCK_UTILIZATION = DataSet().name("NameNode Lock Utilization (%)").accepts(LOCK, NAMENODE).seriesid(PERLOCK).seriesname(LOCKNAME)
                                       .y(UTILIZATION).yname("Lock Utilization (%)").showtotal(false).miny(10);
  datasets.LOCK_ACQUISITION = DataSet().name("NameNode Lock Acquisition Times (ms/acquire)").accepts(LOCK, NAMENODE).seriesid(PERLOCK).seriesname(LOCKNAME)
                                       .y(ACQUISITIONTIME).yname("Avg. lock acquisition time (ms)").showtotal(false).miny(0.1);
  datasets.LOCK_HOLD = DataSet().name("NameNode Lock Hold Times (ms/hold)").accepts(LOCK, NAMENODE).seriesid(PERLOCK).seriesname(LOCKNAME)
                                       .y(HOLDTIME).yname("Avg. lock hold time (ms)").showtotal(false);
  datasets.TENANT1_LOCK_ACQUISITION = DataSet().name("NameNode Lock Acquisition Times (Tenant 1)").accepts(TENANT1, LOCK, NAMENODE).seriesid(PERLOCK).seriesname(LOCKNAME)
                                       .y(ACQUISITIONTIME).yname("Time acquiring lock(ms)").showtotal(false).miny(0.1);
  datasets.TENANT_REQUEST_LATENCY = DataSet().name("Request Latency (ms/req)").accepts(HDFSREQUEST).seriesid(PERTENANT).seriesname(TENANTNAME)
                                             .y(LATENCY).yname("Avg. Latency (ms)").showtotal(false);
  datasets.TENANT_THROUGHPUT = DataSet().name("Request Throughput (req/s)").accepts(HDFSREQUEST).seriesid(PERTENANT).seriesname(TENANTNAME)
                                        .y(THROUGHPUT).yname("Throughput (req/s)").showtotal(false);
  datasets.TENANT1_REQUEST_LATENCY = DataSet().name("Request Latency (Tenant 1)").accepts(HDFSREQUEST, TENANT1).seriesid(PERTENANT).seriesname(TENANTNAME)
                                             .y(LATENCY).yname("Avg. Latency (ms)").showtotal(false);
  datasets.TENANT1_THROUGHPUT = DataSet().name("Request Throughput (Tenant 1)").accepts(HDFSREQUEST, TENANT1).seriesid(PERTENANT).seriesname(TENANTNAME)
                                        .y(THROUGHPUT).yname("Throughput (req/s)").showtotal(false);
  datasets.TENANT_CONCURRENCY = DataSet().name("Request Concurrency").accepts(HDFSREQUEST).seriesid(PERTENANT).seriesname(TENANTNAME)
                                         .y(CONCURRENCY).yname("Concurrency (req)").showtotal(false);
  datasets.TENANT_DATANODE_CPU_USAGE = DataSet().name("DataNode CPU Usage (MHz)").accepts(CPU, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                .y(CYCLES).yname("CPU Usage (MHz)").showtotal(true).miny(1000);
  datasets.TENANT_DATANODE_CPU_EFFICIENCY = DataSet().name("DataNode CPU Efficiency").accepts(CPU, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                .y(CYCLESEFFICIENCY).yname("CPU Efficiency (MHz)").showtotal(false).showbackground(false);
  datasets.TENANT_NAMENODE_CPU_USAGE = DataSet().name("NameNode CPU Usage (MHz)").accepts(CPU, NAMENODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                .y(CYCLES).yname("CPU Usage (MHz)").showtotal(true).miny(1000);
  datasets.TENANT_NAMENODE_CPU_EFFICIENCY = DataSet().name("NameNode CPU Efficiency").accepts(CPU, NAMENODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                .y(CYCLESEFFICIENCY).yname("CPU Efficiency (MHz)").showtotal(false).showbackground(false);
  datasets.TENANT_DATANODE_CPU_EFFICIENCY_MSRSANDBOX012 = DataSet().name("DataNode CPU Efficiency").accepts(CPU, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                     .y(CYCLESEFFICIENCYPERCENT).yname("CPU Efficiency (%)").showtotal(false).showbackground(true);
  datasets.TENANT_NAMENODE_CPU_EFFICIENCY_MSRSANDBOX012 = DataSet().name("NameNode CPU Efficiency").accepts(CPU, NAMENODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                                                     .y(CYCLESEFFICIENCYPERCENT).yname("CPU Efficiency (%)").showtotal(false).showbackground(true);
  datasets.TENANT_DATANODE_CPU_LATENCY_MSRSANDBOX012 = DataSet().name("DataNode CPU Delay").accepts(CPU, DATANODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                            .y(CPUDELAY).yname("CPU Efficiency (%)").showtotal(false).showbackground(false);
  datasets.TENANT_NAMENODE_CPU_LATENCY_MSRSANDBOX012 = DataSet().name("NameNode CPU Delay").accepts(CPU, NAMENODE).seriesid(PERTENANT).seriesname(TENANTNAME)
                            .y(CPUDELAY).yname("CPU Efficiency (%)").showtotal(false).showbackground(false);
  
  // Defines the grid layout for the multi visualization
  var gridlayout = [
      [datasets.TENANT1_REQUEST_LATENCY,
       datasets.TENANT1_THROUGHPUT,
       datasets.TENANT_NETWORK_USAGE
      ],
      [datasets.LOCK_UTILIZATION,
       datasets.TENANT1_LOCK_ACQUISITION,
       datasets.TENANT_NAMENODE_CPU_USAGE,
       datasets.TENANT_DATANODE_CPU_USAGE,
       datasets.TENANT_NAMENODE_CPU_EFFICIENCY_MSRSANDBOX012,
       datasets.TENANT_DATANODE_CPU_EFFICIENCY_MSRSANDBOX012
      ],
      [datasets.TENANT_DATANODE_DISK_USAGE, 
       datasets.TENANT_DATANODE_DISKCACHE_USAGE,
       datasets.TENANT1_DATANODE_DISK_LATENCY,
       datasets.TENANT_DATANODE_DISK_OPS
      ]
  ];
  
  var collection = {};
  
  collection.datasets = datasets;
  collection.grid = gridlayout;
  
  collection.collect = function(report) {
    for (var i = 0; report.tenantreports && i < report.tenantreports.length; i++) {
      var tenantreport = report.tenantreports[i];
      for (var key in datasets)
        datasets[key].collect(tenantreport);
    }
  }
  collection.keys = function() {
    return Object.keys(datasets);
  }
  collection.names = function() {
    return Object.keys(datasets).map(function(key) { return datasets[key].name(); });
  }
  collection.get = function(key) {
    return datasets[key];
  }
  
  for (var i = 0; i < reports.length; i++) {
    collection.collect(new ResourceReport(reports[i]));
  }
  
  return collection;
};