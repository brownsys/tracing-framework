function SwimLane() {
	/* Default values for placement of the swimlane.  User should pass these */
	var x = 0;
	var y = 0;
	var width = 500;
	var height = 100;
	var margin = 120;
	
	/* turns on or off key components of the viz */
	var properties = {
	    "showevents": true,
	    "showedges": true,
	    "showgc": true,
	    "showspans": true,
	    "showcpath": false
	};

	/* event callbacks */
	var callbacks = {
		"refresh": function(){}
	};

	// For internal use
	var sx = d3.scale.linear(); // scale between global / zoomed
	var brush = d3.svg.brush(); // specifies the active draw area
	var axis = d3.svg.axis().orient("bottom").ticks(10).tickSize(6, 0, 0); // time axis at bottom of viz

	// Tooltips
	var ttGravity = $.fn.tipsy.autoBounds(Math.min(window.width(), window.height()) / 3, "s");
	var EventTooltip = makeEventTooltip(ttGravity);
	var GCTooltip = makeGCTooltip(ttGravity);
  var HDDTooltip = makeHDDTooltip(ttGravity);
  var NetworkTooltip = makeNetworkTooltip(ttGravity);
	var ThreadTooltip = makeThreadTooltip($.fn.tipsy.autoWE);

	/* Main rendering function */
	function swimlane(selection) {
		selection.each(function(layout) {  
			// For spacing out the threads
			sx.range([0, width]);
			var sy = d3.scale.linear().domain([0, layout.Height()]).range([0, height]);

			// Create the clip def
			var defs = d3.select(this).selectAll(".clipdef").data([layout]);
			defs.enter().append("defs").attr("class", "clipdef").append("clipPath").attr("id", "clip").append("rect");
			defs.select("rect").attr("width", width).attr("height", height);
			defs.exit().remove();

      // Add all of the containers for the viz
      var main = d3.select(this).selectAll(".main").data([layout]);
      var newmain = main.enter().append('g').attr("class", "main");
      var newlanes = newmain.append('g').attr("class", "lanes");
      newlanes.append("g").attr("class", "lane-background");
      newlanes.append("g").attr("class", "axis");
      newlanes.append("g").attr("class", "spans");
      newlanes.append("g").attr("class", "timeindicator").append("line");
      newlanes.append("g").attr("class", "edges");
      newlanes.append("g").attr("class", "gc");
      newlanes.append("g").attr("class", "hdd");
      newlanes.append("g").attr("class", "network");
      newlanes.append("g").attr("class", "events");
      var newmargin = newmain.append('g').attr("class", "margin");
      newmargin.append("g").attr("class", "lane-labels");
      newmargin.append("g").attr("class", "lane-controls");
      main.exit().remove();

      // Position the containers
      main.attr("transform", "translate("+x+","+y+")").attr("width", width).attr("height", height);
      main.selectAll(".lanes").attr("transform", "translate("+margin+",0)");

			// Draw the thread backgrounds
			var lanes = main.select(".lane-background").selectAll("rect").data(layout.Lanes());
			lanes.enter().append('rect').attr('fill', Lane.Fill);
			lanes.attr('x', 0).attr('y', Lane.Scale(sy).Offset).attr('width', width).attr('height', Lane.Scale(sy).Height);
			lanes.exit().remove();

			// Draw the lane labels
			var lanelabels = main.select(".lane-labels").selectAll("text").data(layout.Lanes());
			lanelabels.enter().append("text").attr('text-anchor', 'end').attr('fill', function(d) { return d.Fill().darker(1); })
			.text(Lane.Label).call(ThreadTooltip);
			lanelabels.attr('x', margin-12).attr('y', function(d) { return sy(d.Offset()+d.Height()*0.5); }).attr("dominant-baseline", "middle");
			lanelabels.exit().remove();
			
			// Add the hit area
			var lanecontrols = main.select(".lane-controls").selectAll("rect.groupcontrols").data(layout.Groups());
			lanecontrols.enter().append("rect").attr("class", "groupcontrols");
			lanecontrols.attr("x", margin-6).attr("y", Group.Scale(sy).Offset).attr("width", 4).attr("height", Group.Scale(sy).Height)
			.attr("fill", function(d) { return d.Fill().darker(1); });
			lanecontrols.exit().remove();

			// Place the time axis
			main.select(".axis").attr("transform", "translate(0,"+height+")");

			// Update the clip paths of the visualization elements
			main.select(".spans").attr("clip-path", "url(#clip)");
			main.select(".timeindicator").attr("clip-path", "url(#clip)");
			main.select(".edges").attr("clip-path", "url(#clip)");
			main.select(".gc").attr("clip-path", "url(#clip)");
      main.select(".hdd").attr("clip-path", "url(#clip)");
      main.select(".network").attr("clip-path", "url(#clip)");
			main.select(".events").attr("clip-path", "url(#clip)");

			// Add a mouse marker
			main.select(".timeindicator line").attr('y1', 0).attr('y2', height);
			main.on("mousemove", function(e) {
				var mousex = d3.mouse(this)[0]-margin;
				d3.select(this).select(".timeindicator line").attr('x1', mousex).attr('x2', mousex);				
			});

			// Attach the zoom behaviour.  A little bit hairy for now
			var moving = false,
			lastx = null;
			main.on("mousedown", function() { moving = true; lastx = null; });
			main.on("mouseup", function() { moving = false; lastx = null; });

			var zoom = d3.behavior.zoom();
			zoom.on("zoom", function() {
			  var data = layout.workload;
				var datalen = data.max - data.min;
				var rangemin = data.min - datalen / 10.0;
				var rangemax = data.max + datalen / 10.0;

				var mousex = sx.invert(d3.mouse(this)[0]-margin);

				// do the zoom in or out, clamping if necessary
				var newx0 = mousex +  ((brush.extent()[0] - mousex) / d3.event.scale);
				var newx1 = mousex + ((brush.extent()[1] - mousex) / d3.event.scale);
				newx0 = Math.max(newx0, rangemin);
				newx1 = Math.min(newx1, rangemax);

				// Apply any translate
				if (moving) {
					if (lastx!=null) {
						var deltax = sx.invert(lastx) - sx.invert(d3.event.translate[0]);
						if ((newx0 > rangemin || deltax > 0) && (newx1 < rangemax || deltax < 0)) {
							newx0 = newx0 + deltax;
							newx1 = newx1 + deltax;
						}
					}
					lastx = d3.event.translate[0];
				}

				// apply the extent and refresh
				brush.extent([newx0, newx1]);
				callbacks["refresh"].call(this);
				zoom.scale(1);
			});
			zoom.call(main);

			// Remove any of the actual viz.  Done here because y co-ords only update on a redraw, so optimization to put here rather than
			// update y co-ords unnecessarily on each refresh
			main.select(".spans").selectAll("rect").remove();
			main.select(".events").selectAll("circle").remove();
			main.select(".edges").selectAll("line").remove();
			main.select(".gc").selectAll("rect").remove();
      main.select(".hdd").selectAll("rect").remove();
      main.select(".network").selectAll("rect").remove();
		});

	};

	swimlane.refresh = function(selection) {
		selection.each(function(layout) {
			var main = d3.select(this).select(".main");

			// Update the x scale from the brush, create a y scale
			sx.domain(brush.extent());
      var sy = d3.scale.linear().domain([0, layout.Height()]).range([0, height]);

			// Hide open tooltips
			$(".tipsy").remove();

			var minExtent = sx.domain()[0];
			var maxExtent = sx.domain()[1];
			
			var start = new Date().getTime();

			// Figure out which data should be drawn
			var spandata = layout.Spans().filter(function (d) { return d.Start() < maxExtent && d.End() > minExtent; });
			var gcdata = layout.GC().filter(function(d) { return d.start < maxExtent && d.end > minExtent; });
      var hdddata = layout.HDD().filter(function(d) { return d.start < maxExtent && d.end > minExtent; });
      var networkdata = layout.Network().filter(function(d) { return d.start < maxExtent && d.end > minExtent; });
			
			var eventdata = layout.Events();
			if (properties.showcpath==true)
			  eventdata = filter_criticalpath_events(eventdata);
			eventdata = eventdata.filter(function(d) { return d.Timestamp() > minExtent && d.Timestamp() < maxExtent; });
			
			var edgedata = layout.Edges();
			if (properties.showcpath==true)
			  edgedata = edgedata.filter(function(edge) { return $.inArray(edge.parent, eventdata)!=-1 && $.inArray(edge.child, eventdata)!=-1;});
			edgedata = edgedata.filter(function(d) { return d.parent.Timestamp() < maxExtent && d.child.Timestamp() > minExtent; });
			

			start = new Date().getTime();
			// Update the span rects
			if (properties.showspans==true) {
  			var spans = main.select(".spans").selectAll("rect").data(spandata, XSpan.getID);
  			spans.enter().append("rect").classed("waiting", function(d){return d.waiting;})
  			.attr('y', function(d) { return sy(d.lane.Offset() + 0.1 * d.lane.Height()); })
  			.attr('height', function(d) { return sy(0.8 * d.lane.Height()); });
  			spans.attr('x', function(d) { return sx(d.Start()); })
  			.attr('width', function(d) { return sx(d.End()) - sx(d.Start()); });
  			spans.exit().remove();
			} else {
			  main.select(".spans").selectAll("rect").remove();
			}

			// Update the event dots
			if (properties.showevents==true) {
    		var events = main.select(".events").selectAll("circle").data(eventdata, XEvent.getID);
    		events.enter().append('circle').attr("class", function(d) { return d.type; })
    		.attr('cy', function(d) { return sy(d.lane.Offset() + 0.5 * d.lane.Height()); })
    		.attr('r', function(d) { return d.type=="event" ? 5 : 2; })
    		.attr('id', function(d) { return d.ID(); })
    		.call(EventTooltip);
    		events.attr('cx', function(d) { return sx(d.Timestamp()); });
    		events.exit().remove();
			} else {
			  main.select(".events").selectAll("circle").remove();
			}

			// Update the causality edges
      if (properties.showedges==true) {
  			var edges = main.select(".edges").selectAll("line").data(edgedata, function(d) { return d.id; });
  			edges.enter().append("line")
  			.attr('y1', function(d) { return sy(d.parent.lane.Offset() + 0.5 * d.parent.lane.Height()); })
  			.attr('y2', function(d) { return sy(d.child.lane.Offset() + 0.5 * d.child.lane.Height()); });
        edges.exit().remove();
  			edges.attr('x1', function(d) { return sx(d.parent.Timestamp()); })
  			.attr('x2', function(d) { return sx(d.child.Timestamp()); })
  			.attr('class', function(d) { return d.type; });
			} else {
			  main.select(".edges").selectAll("line").remove();
			}

			// Update the GC blocks
      if (properties.showgc==true) {
  			var gc = main.select(".gc").selectAll("rect").data(gcdata, GCEvent.getID);
  			gc.enter().append("rect").attr('y', function(d) { return sy(d.group.Offset()); })
  			.attr('height', function(d) { return sy(d.group.Height()); }).call(GCTooltip);
  			gc.attr('x', function(d) { return sx(d.start); }).attr('width', function(d) { return sx(d.end) - sx(d.start); });
  			gc.exit().remove();
      } else {
        main.select(".gc").selectAll("rect").remove();        
      }

      // Update the HDD blocks
      var hdd = main.select(".hdd").selectAll("rect").data(hdddata, XEvent.getID);
      hdd.enter().append('rect').attr('class', function(d) { return d.type; })
      .attr('y', function(d) { return sy(d.lane.Offset() + d.lane.Height() * 0.25); })
      .attr('height', function(d) { return sy(d.lane.Height() * 0.5); })
      .call(HDDTooltip);
      hdd.attr('x', function(d) { return sx(d.start); })
      .attr('width', function(d) { return sx(d.end) - sx(d.start); });
      hdd.exit().remove();

      // Update the network blocks
      var network = main.select(".network").selectAll("rect").data(networkdata, XEvent.getID);
      network.enter().append('rect').attr('class', function(d) { return d.type; })
      .attr('y', function(d) { return sy(d.lane.Offset() + d.lane.Height() * 0.25); })
      .attr('height', function(d) { return sy(d.lane.Height() * 0.5); })
      .call(NetworkTooltip);
      network.attr('x', function(d) { return sx(d.start); })
      .attr('width', function(d) { return sx(d.end) - sx(d.start); });
      network.exit().remove();

			// Update the axis
			var norm = d3.scale.linear().range([0, width]).domain([brush.extent()[0] - layout.workload.min, brush.extent()[1] - layout.workload.min]);
			main.select(".axis").call(axis.scale(norm));
		});
	};

	swimlane.on = function(evt, cb) {
		if (cb==null)
			return callbacks[evt];
		callbacks[evt] = cb;
		return swimlane;
	};

	swimlane.brush = function(_) { if (!arguments.length) return brush; brush = _; return swimlane; };
	swimlane.x = function(_) { if (!arguments.length) return x; x = _; return swimlane; };
	swimlane.y = function(_) { if (!arguments.length) return y; y = _; return swimlane; };
	swimlane.width = function(_) { if (!arguments.length) return width; width = _; return swimlane; };
	swimlane.height = function(_) { if (!arguments.length) return height; height = _; return swimlane; };
  swimlane.margin = function(_) { if (!arguments.length) return margin; margin = _; return swimlane; };
  swimlane.property = function(key, value) { properties[key] = value; swimlane.on("refresh").call(this); };

	return swimlane;    
}