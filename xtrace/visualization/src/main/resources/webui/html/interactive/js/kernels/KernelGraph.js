function KernelNode(id, label, data) {
    this.id = id;
    this.label = label;
    this.data = data;
}

KernelNode.prototype.field_equals = function(name, val) {
    if (this.data.hasOwnProperty(name)) {
        for (var i = 0; i < this.data[name].length; i++) {
            if (this.data[name][i]==val) return true;
        }
    }
    return false;
}
    
KernelNode.prototype.field_one_of = function(name, values) {
    for (var i = 0; i < values.length; i++) {
        if (this.field_equals(name, values[i])) return true;
    }
    return false;
}

KernelNode.prototype.clone = function() {
    return new KernelNode(this.id, this.label, this.data);
}

KernelNode.fromJSON = function(json) {
    var id = json["X-Trace"][0].substr(18);
    var label = hash_report(json);
    return new KernelNode(id, label, json);
}


function KernelGraph(id, nodelist) {    
    // Private, internal variables
    var nodes = {};
    var parents = {};
    var children = {};
    var labels = {};
    
    var node;
    for (var i = 0; i < nodelist.length; i++) {
        node = nodelist[i];
        
        // Create empty sets to store the parents and children of each node.
        nodes[node.id] = node;
        parents[node.id] = {};
        children[node.id] = {};
        if (!labels.hasOwnProperty(node.label)) labels[node.label] = {};

        // Also remember each node's label    
        labels[node.label][node.id] = true;        
    }        

    // This function links together two nodes
    this.link = function(pid, cid) {
        if (nodes.hasOwnProperty(pid) && nodes.hasOwnProperty(cid)) {
            parents[cid][pid] = true;
            children[pid][cid] = true;
        }
    }
    
    // This function completely removes a node from the trace.
    // Each of the node's children get re-linked to the node's parents and vice versa
    this.remove = function(node) {
        if (typeof node == "string") node = nodes[node];
        
        // Do nothing if the node doesn't exist
        if (!nodes.hasOwnProperty(node.id)) return;
        
        // Get the children and the parents of the node
        var pid, cid, ps = parents[node.id], cs = children[node.id];
        
        // For each parent, remove the node as a child
        // For each parent, add the node's children as the parent's children
        for (pid in ps) {
            if (children.hasOwnProperty(pid)) {
                delete children[pid][node.id];
                for (cid in cs) {
                    children[pid][cid] = true;                    
                }
            }
        }
        
        // For each child, remove the node as a parent
        // For each child, add the node's parents as the child's parents
        for (cid in cs) {
            if (parents.hasOwnProperty(cid)) {
                delete parents[cid][node.id];
                for (pid in ps) {
                    parents[cid][pid] = true;
                }
            }
        }
        
        // Finally, remove all the evidence that node ever existed
        delete nodes[node.id];
        delete children[node.id];
        delete parents[node.id];
        delete labels[node.label][node.id];
        
        // If there are no other nodes sharing this node's label, remove the label too
        if (Object.keys(labels[node.label]).length==0) {
            delete labels[node.label];
        }
    }
    
    this.get_node_ids = function() {
        return Object.keys(nodes);
    }
    
    this.get_nodes = function() {
        return this.get_node_ids().map(function(id) { return nodes[id]; });
    }
    
    this.get_node_data = function() {
        return this.get_nodes().map(function(node) { return node.data; });
    }

    this.get_parent_ids = function(nodeid) {
        if (!parents.hasOwnProperty(nodeid)) {
            return [];
        }
        return Object.keys(parents[nodeid]);
    }
    
    this.get_parents = function(node) {
        return this.get_parent_ids(node.id).map(function(id) { return nodes[id]; });
    }
    
    this.get_parent_labels = function(node) {
        return this.get_parents(node).map(function(node) { return node.label; });
    }
    
    this.get_child_ids = function(nodeid) {
        if (!children.hasOwnProperty(nodeid)) {
            return [];
        }
        return Object.keys(children[nodeid]);
    }
    
    this.get_children = function(node) {
        return this.get_child_ids(node.id).map(function(id) { return nodes[id]; });
    }
    
    this.get_child_labels = function(node) {
        return this.get_children(node).map(function(node) { return node.label; });
    }
    
    this.get_neighbour_ids = function(nodeid) {
        return this.get_parent_ids(nodeid).concat(this.get_child_ids(nodeid));
    }
    
    this.get_neighbours = function(node) {
        return this.get_parents(node).concat(this.get_children(node));
    }
    
    this.get_neighbour_labels = function(node) {
        return this.get_child_labels(node).concat(this.get_parent_labels(node));
    }
    
    this.get_labels = function() {
        return Object.keys(labels);
    }
    
    this.get_node_ids_for_label = function(label) {
        if (labels.hasOwnProperty(label)) {
            return Object.keys(labels[label]);
        }
        return [];
    }
    
    this.get_label_count = function(label) {
        return this.get_node_ids_for_label(label).length;
    }
    
    this.clone = function() {
        var cloned_nodes = this.get_nodes().map(function(node) {
            return node.clone();
        });
        var clone = new KernelGraph(id, cloned_nodes);
        for (var cid in parents) {
            for (var pid in parents[cid]) {
                clone.link(pid, cid);
            }
        }
        return clone;
    }
    
    this.get_id = function() {
        return id;
    }
    
    this.relabel = function(nodeid, label) {
        var node = nodes[nodeid];
        
        delete labels[node.label][node.id];
        
        // If there are no other nodes sharing this node's label, remove the label too
        if (Object.keys(labels[node.label]).length==0) {
            delete labels[node.label];
        }
        
        node.label = label;
        
        if (!labels.hasOwnProperty(label)) {
            labels[label] = {};
        }
        labels[label][node.id] = true;
    }
    
    this.print = function() {
        console.log(nodes, labels, parents, children);
    }
}

KernelGraph.fromJSON = function(json) {
    var nodes = json["reports"].map(function(report) { return KernelNode.fromJSON(report); });
    var trace = new KernelGraph(json["id"], nodes);
    nodes.forEach(function(node) {
        var edges = node.data["Edge"];
        if (edges) {
            for (var i = 0; i < edges.length; i++) {
                trace.link(edges[i], node.id);
            }
        }
    });
    nodes.forEach(function(node) {
        if (node.data["Operation"] && node.data["Operation"][0]=="merge") {
            trace.remove(node);
        }
    });
    return trace;
}

