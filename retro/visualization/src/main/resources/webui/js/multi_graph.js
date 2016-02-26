//lightweight is an optional argument that will try to draw the graph as fast as possible
function multi_graph(attachPoint) {	
  var attach = d3.select(attachPoint);
  
  var history_size_seconds = 300;
  
  // Get a handle to the datasets
  var d = DataSets();
  var grid = d.grid;

  var graphs = [];
  for (var i = 0; i < grid.length; i++) {
    var row = [];
    for (var j = 0; j < grid[i].length; j++) {
      row.push({"dataset": grid[i][j],
        "graph": ResourceGraph().window(history_size_seconds*1000).refreshinterval(1000),
        "attach": attach.append("svg:svg").attr("class", "graphs")
      });
    }
    graphs.push(row);
  }
	
	// Create the subscriber
	var subscriber = new Subscriber().on("report", function(report) { d.collect(report); });
  
	/* Redraws the whole viz, for example when the parameters change or screen is resized.
	 * For now we redraw at a fixed interval */
	var nextdraw = null;
	function draw() {
	  // Make sure only one draw loop at a time
	  window.clearTimeout(nextdraw);
	  
		// Determine the new widths and heights
		var width = window.innerWidth;
		var height = window.innerHeight;
		var rowheight = (height / graphs.length)-2;
		
		for (var i = 0; i < graphs.length; i++) {
		  var row = graphs[i];
		  var rowwidth = (width / row.length)-2;
		  for (var j = 0; j < row.length; j++) {
		    var info = row[j];
		    draw_graph(info.dataset, info.attach, info.graph, rowwidth, rowheight);
		  }
		}

		nextdraw = window.setTimeout(draw, 500);
	}
	
	function draw_graph(dataset, attachpoint, graph, width, height) {
    // Resize the chart
	  attachpoint.attr('width', width);
	  attachpoint.attr('height', height);
    
    // Modify the viz parameters
    graph.width(width).height(height);
  
    // Redraw the viz
    attachpoint.datum(dataset).call(graph);
	}

	// This function animates the graph by calling itself at a regular interval
	var nextanimate;
  function animate(first) {
    window.clearTimeout(nextanimate);

    var next_refresh = Number.MAX_VALUE;
    for (var i = 0; i < graphs.length; i++) {
      var row = graphs[i];
      for (var j = 0; j < graphs[i].length; j++) {
        var info = graphs[i][j];
        var trans = info.graph.refreshinterval();
        if (first)
          info.graph.refreshinterval(2);
        info.attach.datum(info.dataset).call(info.graph.transition);
        if (first)
          info.graph.refreshinterval(trans);
        next_refresh = Math.min(next_refresh, info.graph.refreshinterval());
      }
    }
    
    nextanimate = window.setTimeout(animate, next_refresh);
  }
	
	// Finally, draw it explicitly the first time
  draw();
  animate(true);  
}