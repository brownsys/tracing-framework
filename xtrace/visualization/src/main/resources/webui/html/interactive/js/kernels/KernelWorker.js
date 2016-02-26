importScripts("../xtrace_utils.js", "KernelGraph.js", "Kernel.js", "NodeCountKernel.js", "WeisfeilerLehmanKernel.js");

var kernel = new WeisfeilerLehmanKernel();
var graphs = [];

onmessage = function(event) {
    if (event.data.type=="data") ondata(event);
    else if (event.data.type=="calculate") oncalculate(event);
};

ondata = function(event) {
    graphs = event.data.data.map(function(report) { return yarnchild_kernelgraph_for_trace(report); });
}

oncalculate = function(event) {
    var a = graphs[event.data.a], b = graphs[event.data.b];
    var score = kernel.calculate(a, b);
    postMessage({"source": a.get_id(), "target": b.get_id(), "score": score});
}