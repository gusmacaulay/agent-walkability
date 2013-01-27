var map, filter, filterStrategy;
var animationTimer;
var currentDate;
// var startDate = new Date(1272736800000); // lower bound of when values
// var endDate = new Date(1272737100000); // upper value of when values
var startDate = new Date(0); // lower bound of when values
var endDate = new Date(1200000); // upper value of when values
var step = 9; // seccods to advance each interval
var interval = 0.03; // seconds between each step in the animation
var easting,northing;

function startAnimation() {
	// loadPaths();
	if (animationTimer) {
		stopAnimation(true);
	}
	if (!currentDate) {
		currentDate = startDate;
	}
	// var spanEl = document.getElementById("span");
	var next = function() {
		// alert("working! current date " + currentDate + " end date " + endDate
		// );
		var span = 40
		if (currentDate < endDate) {
			filter.lowerBoundary = startDate;// currentDate;
			filter.upperBoundary = new Date(currentDate.getTime()
					+ (span * 1000));
			filterStrategy.setFilter(filter);
			currentDate = new Date(currentDate.getTime() + (step * 1000));
			var date = new Date(currentDate);
			document.getElementById('clock').innerHTML = "Time: " + date.getMinutes() + "m :" + date.getSeconds() + "s";
		} else {
			// stopAnimation(true);
		}
	};

	animationTimer = window.setInterval(next, interval * 1000);
}

function stopAnimation(reset) {
	window.clearInterval(animationTimer);
	animationTimer = null;
	if (reset === true) {
		currentDate = null;
	}
}


function loadPaths() {
	filter = new OpenLayers.Filter.Comparison({
		type : OpenLayers.Filter.Comparison.BETWEEN,
		property : "when",
		lowerBoundary : startDate,
		upperBoundary : new Date(startDate.getTime() + (15 * 1000))
	});

	filterStrategy = new OpenLayers.Strategy.Filter({
		filter : filter
	});
	
	var pathStyle = new OpenLayers.Style();

	var ruleRed = new OpenLayers.Rule({
	  filter: new OpenLayers.Filter.Comparison({
	      type: OpenLayers.Filter.Comparison.LESS_THAN,
	      property: "when",
	      value: 240000,
	  }),
	  symbolizer: {pointRadius: 10, fillColor: "red",
	               fillOpacity: 0.1, strokeColor: "red", strokeOpacity: 0.1}
	});

	var ruleOrange = new OpenLayers.Rule({
	  filter: new OpenLayers.Filter.Comparison({
	      type: OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO,
	      property: "when",
	      value: 240000,
	  }),
	  symbolizer: {pointRadius: 10, fillColor: "orange",
	               fillOpacity: 0.1, strokeColor: "orange", strokeOpacity: 0.1}
	});
	
	var ruleYellow = new OpenLayers.Rule({
		  filter: new OpenLayers.Filter.Comparison({
		      type: OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO,
		      property: "when",
		      value: 480000,
		  }),
		  symbolizer: {pointRadius: 10, fillColor: "yellow",
		               fillOpacity: 0.1, strokeColor: "yellow", strokeOpacity: 0.1}
		});

	pathStyle.addRules([ruleRed, ruleOrange, ruleYellow]);

	var paths = new OpenLayers.Layer.Vector("Paths", {
		projection : geographic,
		strategies : [ new OpenLayers.Strategy.Fixed(), filterStrategy ],

		protocol : new OpenLayers.Protocol.HTTP({
			// url: "paths_wgs84.geojson",
			// url : "/service/agent-paths/285752.0/5824386.0",
			url : "/service/agent-paths/" + easting + "/" + northing,
			format : new OpenLayers.Format.GeoJSON()
		}),
		styleMap : new OpenLayers.StyleMap({
			"default" : pathStyle
// "default" : new OpenLayers.Style({
// graphicName : "circle",
// pointRadius : 10,
// fillOpacity : 0.5,
// fillColor : "#9e69e3",
// strokeColor : "#8e45ed",
// strokeWidth : 1
// })
		}),
		renderers : [ "Canvas", "SVG", "VML" ]
	});

	map.addLayer(paths);
	// alert("Loaded Paths");
// map.setCenter(new OpenLayers.LonLat(144.570412433435773, -37.701804450869475)
// .transform(geographic, mercator), 16);

}
function showValue(newValue, spanId)
{
	document.getElementById(spanId).innerHTML=newValue;
}

// add behavior to elements
document.getElementById("simulate").onclick = loadPaths;
document.getElementById("play").onclick = startAnimation;
document.getElementById("pause").onclick = stopAnimation;
// document.getElementById("slider").onchange = showValue;


var mercator = new OpenLayers.Projection("EPSG:900913");
var geographic = new OpenLayers.Projection("EPSG:4326");
var vicgrid = new OpenLayers.Projection("EPSG:28355");
// var victorian = new OpenLayers.Projection("EPSG:28355");
var controls = [new OpenLayers.Control.LayerSwitcher(), new OpenLayers.Control.Zoom()];
map = new OpenLayers.Map("map", {controls : controls});

var osm = new OpenLayers.Layer.OSM();

// var paths_static = new OpenLayers.Layer.Vector("Paths Buffer", {
// projection : geographic,
// strategies: [new OpenLayers.Strategy.BBOX()],
// //strategies: [new OpenLayers.Strategy.Fixed(),new
// OpenLayers.Strategy.BBox()],
// protocol : new OpenLayers.Protocol.HTTP({
// url : "/paths_buffer_wgs84.geojson",
// format : new OpenLayers.Format.GeoJSON()
// }),
// styleMap : new OpenLayers.StyleMap({
// "default" : new OpenLayers.Style({
// graphicName : "circle",
// pointRadius : 10,
// fillOpacity : 0.25,
// fillColor : "#4de800",
// strokeColor : "#02aa21",
// strokeWidth : 1
// })
// }),
// renderers : [ "Canvas", "SVG", "VML" ]
// });

var roads = new OpenLayers.Layer.Vector("Roads", {
	projection : geographic,
	strategies: [new OpenLayers.Strategy.BBOX()],
	// strategies: [new OpenLayers.Strategy.Fixed(),new
	// OpenLayers.Strategy.BBox()],
	protocol : new OpenLayers.Protocol.HTTP({
		url : "/graph_wgs84.geojson",
		format : new OpenLayers.Format.GeoJSON()
	}),
	styleMap : new OpenLayers.StyleMap({
		"default" : new OpenLayers.Style({
			graphicName : "circle",
			pointRadius : 10,
			fillOpacity : 0.25,
			fillColor : "#428beb",
			strokeColor : "#428beb",
			strokeWidth : 3
		})
	}),
	renderers : [ "Canvas", "SVG", "VML" ]
});

OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {                
    defaultHandlerOptions: {
        'single': true,
        'double': false,
        'pixelTolerance': 0,
        'stopSingle': false,
        'stopDouble': false
    },

    initialize: function(options) {
        this.handlerOptions = OpenLayers.Util.extend(
            {}, this.defaultHandlerOptions
        );
        OpenLayers.Control.prototype.initialize.apply(
            this, arguments
        ); 
        this.handler = new OpenLayers.Handler.Click(
            this, {
                'click': this.trigger
            }, this.handlerOptions
        );
    }, 

    trigger: function(e) {
    	map.g
        var lonlat = (map.getLonLatFromPixel(e.xy)); // .transform(mercator,geographic);
        easting = lonlat.lon;
        northing = lonlat.lat;
//        alert("You clicked near " + lonlat.lat + " N, " +
//                                  + lonlat.lon + " E");
    }

});
vectors = new OpenLayers.Layer.Vector("Vector Layer", {
    renderers: [ "Canvas", "SVG", "VML" ]
});
    pointControl = new OpenLayers.Control.DrawFeature(vectors,OpenLayers.Handler.Point);
    
//    for(var key in controls) {
//        map.addControl(controls[key]);
//    }
 
map.addLayers([roads, osm, vectors])
map.setCenter(new OpenLayers.LonLat(144.570412433435773, -37.701804450869475)
		.transform(geographic, mercator), 16);
var click = new OpenLayers.Control.Click();
map.addControl(click);
map.addControl(pointControl);
pointControl.activate();
click.activate();
