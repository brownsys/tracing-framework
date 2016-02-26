function Kernel() {
    this.attributes = {};
}

Kernel.prototype.getAttributeNames = function() {
    return Object.keys(this.attributes);
}

Kernel.prototype.getAttributeType = function(name) {
    return typeof this.attributes[name]
}

Kernel.prototype.setAttribute = function(name, value) {
    this.attributes[name] = value;
}

Kernel.prototype.getAttribute = function(name) {
    return this.attributes[name];
}

Kernel.prototype.calculate = function(a, b) {
    return 0;
}

Kernel.prototype.calculateAll = function(graphs) {
    var scores = {};
    for (var i = 0; i < graphs.length; i++) {
        scores[graph.id] = {};
    }
    for (var i = 0; i < graphs.length; i++) {
        for (var j = 0; j < i; j++) {
            var score = this.calculate(graphs[i], graphs[j]);
            scores[graphs[i].id][graph[j].id] = score;
            scores[graphs[j].id][graph[i].id] = score;
        }
    }
    return scores;
}