//lightweight is an optional argument that will try to draw the graph as fast as possible
function static_graph(attachPoint) {	
  var attach = d3.select(attachPoint);
  
  // Get a handle to the datasets
  var datasets = DataSets();
  
  // Get the active dataset
  var active = datasets.keys()[0];
  
  var extent = [0, 1];
  
  // Attach a JSON file picker
  var selectfile = attach.append("input").attr("type", "file");
  selectfile.node().addEventListener("change", function(e) {
    var reader = new FileReader();
    reader.onload = function() {
      var json = JSON.parse(this.result);
      console.log("Loaded JSON from file: ", json);
      extent = [Number.MAX_VALUE, Number.MIN_VALUE];
      for (var i = 0; i < json.length; i++) {
        if (json[i].start==0) {
          console.log("Bad report timings:", json[i]);
          continue;
        }
        if (json[i].start < extent[0])
          extent[0] = json[i].start;
        if (json[i].end > extent[1])
          extent[1] = json[i].end;
      }
      extent = [extent[0], extent[1]];
      datasets = DataSets(json);
      draw();
    };
    reader.readAsText(e.target.files[0]);
  }, false);
  
  // First create a dropdown
  var select = attach.append("select").attr("class", "graphselector")
                                      .on("change", function() { active = select.node().value; draw(); });
  var options = select.selectAll("option").data(datasets.keys());
  options.enter().append("option");
  options.exit().remove();
  options.attr("value", function(d) { return d; }).text(function(d) { return datasets.get(d).name(); });
  

	// Create the root SVG element and set its width and height
	var root = attach.append('svg:svg').attr('class', 'graphs');
	
	// Create the visualization components
	var graph = StaticGraph();
  
	/* Redraws the whole viz, for example when the parameters change or screen is resized.
	 * For now we redraw at a fixed interval */
	var nextdraw = null;
	function draw() {
		// Determine the new widths and heights
		var width = window.innerWidth;
		var height = window.innerHeight - select.node().offsetHeight;
		
		console.log(width, height);
		
		// Resize the chart
		root.attr('width', width);
		root.attr('height', height);
		
		// Modify the viz parameters
		graph.width(width).height(height).extent(extent);

		// Redraw the viz
		root.datum(datasets.get(active)).call(graph);
	}
	
	// Finally, draw it explicitly the first time
  draw();
  
}