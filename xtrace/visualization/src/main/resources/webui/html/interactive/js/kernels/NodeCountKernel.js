function NodeCountKernel() {}

NodeCountKernel.prototype = new Kernel();

NodeCountKernel.prototype.calculate = function(a, b) {
    // Get the labels from the graphs
    var la = a.get_labels(), lb = b.get_labels();
    
    // Merge the labels from each graph
    var labels = {};
    la.forEach(function(label) { labels[label] = true; });
    lb.forEach(function(label) { labels[label] = true; });
    
    // Create the feature vectors; in this case each feature is the count of nodes in the graph with that label
    var va=[], vb=[];
    for (var label in labels) {
        va.push(a.get_label_count(label));
        vb.push(b.get_label_count(label));
    }
    var total = a.get_nodes().length * b.get_nodes().length;
    return dotProduct(va, vb)/total;
}

function dotProduct(a, b) {
    var score = 0;
    for (var i = 0; i < a.length; i++) {
        score += a[i]*b[i];
    }
    return score;
}
