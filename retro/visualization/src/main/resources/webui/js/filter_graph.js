//lightweight is an optional argument that will try to draw the graph as fast as possible
function filter_graph(attachPoint) {	
  // Set up the attach points
  var attach = d3.select(attachPoint);
  var heading = attach.append('div').attr('class', 'dropdowns');
  
  var history_size_seconds = 300;
  
  // Set up the headings
  var dropdowns = heading.append('div').attr("style", "float: left; display: inline-block;").attr('class', 'filters');
  var groupby = heading.append('div').attr("style", "float: right; display: inline-block;").attr('class', 'groups').append("select").on("change", function() {
    dataset.groupby(d3.select(this.options[this.selectedIndex]).datum());
  });
  heading.append('div').attr("style", "clear: both;");
  
  // Function to refresh the values in the dropdowns
  var refresh_dropdowns = function() {
    // Callback when an element is selected
    var selects_onchange = function() {
      var property = d3.select(this).datum();
      var value = d3.select(this.options[this.selectedIndex]).datum();
      dataset.filter(property, value);
      dataset.refresh();
    };
    
    // Set up each of the dropdowns
    var selects = dropdowns.selectAll("select").data(dataset.properties().sort());
    selects.enter().append("select").attr("class", "graphselector").on("change", selects_onchange);
    selects.exit().remove();
    
    // Set up the values for the dropdowns
    selects.each(function(d, i) {
      var options = d3.select(this).selectAll("option").data(["<Filter "+d+">"].concat(dataset.values(d).sort()));
      options.enter().append("option");
      options.exit().remove();
      options.text(function(d) { return d; });
    });
    
    // Set up the groupby
    var groupby_options = groupby.selectAll("option").data(["<GroupBy>"].concat(dataset.properties().sort()));
    groupby_options.enter().append("option");
    groupby_options.exit().remove();
    groupby_options.text(function(d) { return d; });
  }

  /* Redraws the whole viz, for example when the parameters change or screen is resized.
   * For now we redraw at a fixed interval */
  var nextdraw = null;
  function draw() {
    // Make sure only one draw loop at a time
    window.clearTimeout(nextdraw);
    
    var datasets = dataset.datasets;
    
    // Determine the new widths and heights
    var width = window.innerWidth / Math.ceil(Math.sqrt(datasets.length))-2;
    var height = (window.innerHeight - dropdowns.node().offsetHeight) / Math.ceil(Math.sqrt(datasets.length))-2;
    
    // Modify the viz parameters
    for (var i = 0; i < datasets.length; i++) {
      attachs[i].attr('width', width);
      attachs[i].attr('height', height);
      graphs[i].width(width).height(height);
      attachs[i].datum(datasets[i]).call(graphs[i]);
    }

    nextdraw = window.setTimeout(draw, 500);
  }

  // This function animates the graph by calling itself at a regular interval
  var nextanimate;
  function animate() {
    window.clearTimeout(nextanimate);
    var datasets = dataset.datasets;
    for (var i = 0; i < datasets.length; i++) {
      attachs[i].datum(datasets[i]).call(graphs[i].transition);
    }
    nextanimate = window.setTimeout(animate, graphs[0].refreshinterval());
  }
  
  var dataset = FilterDataSet().on("refresh", draw).on("property", refresh_dropdowns);
  var subscriber = new Subscriber().on("report", dataset.collect);
  var attachs = dataset.datasets.map(function() { return attach.append("svg:svg").attr("class", "graphs"); });
  var graphs = dataset.datasets.map(function() { return ResourceGraph().window(history_size_seconds*1000).refreshinterval(1000); });
  
	// Finally, draw it explicitly the first time
  dataset.refresh();
  animate();
  draw();
  
  window.jon = dataset;
}