function SwimLaneOverview() {

	/* Default values for placement of the swimlane.  User should pass these */
	var x = 0;
	var y = 0;
	var width = 500;
	var height = 100;
	var margin = 120;

	/* event callbacks */
	var callbacks = {
		"refresh": function(){}
	};

	// The time axis along the bottom of the viz
	var axis = d3.svg.axis().orient('bottom').ticks(10).tickSize(6, 0, 0);

	// The brush.  Ought to be overridden for useful behavior
	var brush = d3.svg.brush();

	/* Main rendering function */
	function overview(selection) {
		selection.each(function(layout) {   
			// Add or remove the swimlane viz
			var mini = d3.select(this).selectAll(".mini").data([layout]);
			var newmini = mini.enter().append("g").attr("class", "mini");
			var newlanes = newmini.append("g").attr("class", "lanes");
      newlanes.append("g").attr("class", "lane-lines");
      newlanes.append("g").attr("class", "spans");
      newlanes.append("g").attr("class", "axis");
      newlanes.append("rect").attr("class", "hitarea");
      newlanes.append("g").attr("class", "brush").attr('clip-path', 'url(#clip)');
			var newmargin = newmini.append("g").attr("class", "margin");
      newmargin.append("g").attr("class", "lane-labels");
			mini.exit().remove();
			
			// Update the size of the swimlane
			mini.attr("transform", "translate("+x+","+y+")").attr("width", width).attr("height", height);      
			mini.selectAll(".lanes").attr("transform", "translate("+margin+","+0+")");

			// Used to translate lane positions to co-ordinates
			var data = layout.workload;
			var datalen = data.max - data.min;
			var rangemin = data.min - datalen / 10.0;
			var rangemax = data.max + datalen / 10.0;
			var norm = d3.scale.linear().domain([rangemin - data.min, rangemax - data.min]).range([0, width]);
			var sx = d3.scale.linear().domain([rangemin, rangemax]).range([0, width]);
      var sy = d3.scale.linear().domain([0, layout.Height()]).range([0, height]);

			// Add and remove new and old lanes
			var lanes = mini.select(".lane-lines").selectAll("line").data(layout.Lanes());
			lanes.enter().append("line");
			lanes.attr('x1', 0).attr('x2', width)
			.attr('y1', function(d) { return d3.round(sy(d.Offset())) + 0.5; })
			.attr('y2', function(d) { return d3.round(sy(d.Offset())) + 0.5; });
			lanes.exit().remove();

			// Add and remove lane text
			var lanetext = mini.select(".lane-labels").selectAll("text").data(layout.Lanes());
			lanetext.enter().append("text").text(Lane.Label).attr('dy', '0.5ex').attr('text-anchor', 'end');
			lanetext.attr('x', margin-10).attr('y', function(d) { return sy(d.Offset()+d.Height()*0.5); });
			lanetext.exit().remove();

			// Add and remove the spans
			var spans = mini.select(".spans").selectAll("path").data(layout.Lanes());
			spans.enter().append('path');
			spans.attr('d', function(lane) { 
				return lane.Spans().map(function(span) {
				  return ['M',sx(span.Start()),(sy(lane.Offset()+lane.Height()*0.5)+0.5),'H',sx(span.End())];
				}).reduce(function(a, b) { return a.concat(b); }).join(" ");
			});
			spans.exit().remove();

			// Add the the time axis
			mini.select(".axis").attr('transform','translate(0,'+height+')').call(axis.scale(norm));

			// Update the size of the brush
			mini.select(".brush").call(brush).selectAll('rect').attr('y', 1).attr('height', height - 1);
			mini.select(".brush rect.background").remove();

			// If there is no hitarea, now we create it
			mini.select(".hitarea").attr('width', width).attr('height', height)
			.attr('pointer-events', 'painted')
			.attr('visibility', 'hidden')
			.on('mouseup', function() {
				var point = sx.invert(d3.mouse(this)[0]);
				var halfExtent = (brush.extent()[1] - brush.extent()[0]) / 2;
				var start = point - halfExtent;
				var end = point + halfExtent;
				brush.extent([start,end]);
				callbacks["refresh"].call(this);
			});;
		});

	};

	overview.refresh = function(selection) {
		selection.each(function(layout) {
			d3.select(this).selectAll(".mini").data([layout]).select(".brush").call(brush);
		});
	};

	overview.on = function(evt, cb) {
		if (cb==null)
			return callbacks[evt];
		callbacks[evt] = cb;
		return overview;
	};

	overview.brush = function(_) { if (!arguments.length) return brush; brush = _; return overview; };
	overview.x = function(_) { if (!arguments.length) return x; x = _; return overview; };
	overview.y = function(_) { if (!arguments.length) return y; y = _; return overview; };
	overview.width = function(_) { if (!arguments.length) return width; width = _; return overview; };
  overview.height = function(_) { if (!arguments.length) return height; height = _; return overview; };
  overview.margin = function(_) { if (!arguments.length) return margin; margin = _; return overview; };

	return overview;    
}