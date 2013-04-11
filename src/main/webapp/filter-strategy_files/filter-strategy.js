var map, filter, filterStrategy;
var animationTimer;
var currentDate;
var startDate = new Date(0); // lower bound of when values

var step = 9; // seccods to advance each interval
var interval = 0.03; // seconds between each step in the animation
var easting, northing;
var draw, modify, snap, split, vectors, wfs;

function startAnimation() {
	// loadPaths();
	maxTime = document.getElementById('walkingTimeSlider').value * 60 * 1000;
	var endDate = new Date(maxTime); // upper value of when values
	if (animationTimer) {
		stopAnimation(true);
	}
	if (!currentDate) {
		currentDate = startDate;
	}
	var next = function() {
		var span = 40
		if (currentDate < endDate) {
			filter.lowerBoundary = startDate;// currentDate;
			filter.upperBoundary = new Date(currentDate.getTime()
					+ (span * 1000));
			filterStrategy.setFilter(filter);
			currentDate = new Date(currentDate.getTime() + (step * 1000));
			var date = new Date(currentDate);
			document.getElementById('clock').innerHTML = "Time: "
					+ date.getMinutes() + "m :" + date.getSeconds() + "s";
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

function downloadOutput() {
	window
			.open("/agent-walkability/service/agent-paths/downloadGeneratedOutputAzShpZip");

}
function loadPaths() {
	if (!easting || !northing) {
		$("#dialog").dialog({
			modal : true,
			buttons : {
				Ok : function() {
					$(this).dialog("close");
				}
			}
		});
		return;

	}
	document.getElementById('simulation_status').innerHTML = "Simulation is running ...";
	document.getElementById('play').disabled = "disabled";
	document.getElementById('pause').disabled = "disabled";
	document.getElementById('download').disabled = "disabled";

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
		filter : new OpenLayers.Filter.Comparison({
			type : OpenLayers.Filter.Comparison.LESS_THAN,
			property : "when",
			value : 240000,
		}),
		symbolizer : {
			pointRadius : 10,
			fillColor : "red",
			fillOpacity : 0.1,
			strokeColor : "red",
			strokeOpacity : 0.1
		}
	});

	var ruleOrange = new OpenLayers.Rule({
		filter : new OpenLayers.Filter.Comparison({
			type : OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO,
			property : "when",
			value : 240000,
		}),
		symbolizer : {
			pointRadius : 10,
			fillColor : "orange",
			fillOpacity : 0.1,
			strokeColor : "orange",
			strokeOpacity : 0.1
		}
	});

	var ruleYellow = new OpenLayers.Rule({
		filter : new OpenLayers.Filter.Comparison({
			type : OpenLayers.Filter.Comparison.GREATER_THAN_OR_EQUAL_TO,
			property : "when",
			value : 480000,
		}),
		symbolizer : {
			pointRadius : 10,
			fillColor : "yellow",
			fillOpacity : 0.1,
			strokeColor : "yellow",
			strokeOpacity : 0.1
		}
	});

	pathStyle.addRules([ ruleRed, ruleOrange, ruleYellow ]);
	maxTime = document.getElementById('walkingTimeSlider').value * 60 * 1000;
	intersectionTime = document.getElementById('waitingTimeSlider').value * 1000;
	walkSpeed = document.getElementById('walkingSpeedSlider').value;
	var paths = new OpenLayers.Layer.Vector("Paths", {
		projection : geographic,
		strategies : [ new OpenLayers.Strategy.Fixed(), filterStrategy ],

		protocol : new OpenLayers.Protocol.HTTP({
			url : "/agent-walkability/service/agent-paths/" + easting + "/"
					+ northing + "/" + maxTime + "/" + intersectionTime + "/"
					+ walkSpeed,
			format : new OpenLayers.Format.GeoJSON(),
		}),
		styleMap : new OpenLayers.StyleMap({
			"default" : pathStyle
		}),
		renderers : [ "Canvas", "SVG", "VML" ]
	});

	paths.events
			.register(
					"loadend",
					paths,
					function(a) {
						if (!a.response.features
								|| a.response.features.length == 0) {
							document.getElementById('simulation_status').innerHTML = "Generated output is empty";

						} else {
							document.getElementById('simulation_status').innerHTML = "Simulation is completed";
							document.getElementById('play').disabled = "";
							document.getElementById('pause').disabled = "";
							document.getElementById('download').disabled = "";
						}
						//
					});

	map.addLayer(paths);
}
function showValue(newValue, spanId) {
	document.getElementById(spanId).innerHTML = newValue;
}

// add behavior to elements
document.getElementById("simulate").onclick = loadPaths;
document.getElementById("play").onclick = startAnimation;
document.getElementById("pause").onclick = stopAnimation;
document.getElementById("download").onclick = downloadOutput;
// document.getElementById("slider").onchange = showValue;

var mercator = new OpenLayers.Projection("EPSG:900913");
var geographic = new OpenLayers.Projection("EPSG:4326");
var vicgrid = new OpenLayers.Projection("EPSG:28355");
var controls = [ new OpenLayers.Control.LayerSwitcher(),
		new OpenLayers.Control.Zoom() ];
map = new OpenLayers.Map("map", {
	controls : controls
});

var osm = new OpenLayers.Layer.OSM();
var saveStrategy = new OpenLayers.Strategy.Save();

OpenLayers.Control.Click = OpenLayers.Class(OpenLayers.Control, {
	defaultHandlerOptions : {
		'single' : true,
		'double' : false,
		'pixelTolerance' : 0,
		'stopSingle' : false,
		'stopDouble' : false
	},

	initialize : function(options) {
		this.handlerOptions = OpenLayers.Util.extend({},
				this.defaultHandlerOptions);
		OpenLayers.Control.prototype.initialize.apply(this, arguments);
		this.handler = new OpenLayers.Handler.Click(this, {
			'click' : this.trigger
		}, this.handlerOptions);
	},

	trigger : function(e) {
		map.g
		var lonlat = (map.getLonLatFromPixel(e.xy));
		easting = lonlat.lon;
		northing = lonlat.lat;
	}

});
vectors = new OpenLayers.Layer.Vector("Vector Layer", {
	renderers : [ "Canvas", "SVG", "VML" ]
});
pointControl = new OpenLayers.Control.DrawFeature(vectors,
		OpenLayers.Handler.Point);

var DeleteFeature = OpenLayers.Class(OpenLayers.Control, {
	initialize : function(layer, options) {
		OpenLayers.Control.prototype.initialize.apply(this, [ options ]);
		this.layer = layer;
		this.handler = new OpenLayers.Handler.Feature(this, layer, {
			click : this.clickFeature
		});
	},
	clickFeature : function(feature) {
		// if feature doesn't have a fid, destroy it
		if (feature.fid == undefined) {
			this.layer.destroyFeatures([ feature ]);
		} else {
			feature.state = OpenLayers.State.DELETE;
			this.layer.events.triggerEvent("afterfeaturemodified", {
				feature : feature
			});
			feature.renderIntent = "select";
			this.layer.drawFeature(feature);
		}
	},
	setMap : function(map) {
		this.handler.setMap(map);
		OpenLayers.Control.prototype.setMap.apply(this, arguments);
	},
	CLASS_NAME : "OpenLayers.Control.DeleteFeature"
});

function initWFSTools() {

	var styles = new OpenLayers.StyleMap({
		"default" : new OpenLayers.Style(null, {
			rules : [ new OpenLayers.Rule({
				symbolizer : {
					"Point" : {
						pointRadius : 5,
						graphicName : "square",
						fillColor : "white",
						fillOpacity : 0.25,
						strokeWidth : 1,
						strokeOpacity : 1,
						strokeColor : "#333333"
					},
					"Line" : {
						strokeWidth : 3,
						strokeOpacity : 1,
						strokeColor : "#666666"
					}
				}
			}) ]
		}),
		"select" : new OpenLayers.Style({
			strokeColor : "#00ccff",
			strokeWidth : 4
		}),
		"temporary" : new OpenLayers.Style(null, {
			rules : [ new OpenLayers.Rule({
				symbolizer : {
					"Point" : {
						pointRadius : 5,
						graphicName : "square",
						fillColor : "white",
						fillOpacity : 0.25,
						strokeWidth : 1,
						strokeOpacity : 1,
						strokeColor : "#333333"
					},
					"Line" : {
						strokeWidth : 3,
						strokeOpacity : 1,
						strokeColor : "#00ccff"
					}
				}
			}) ]
		})
	});

	wfs = new OpenLayers.Layer.Vector(
			"Editable Roads",
			{
				strategies : [ new OpenLayers.Strategy.BBOX(), saveStrategy ],
				projection : mercator,
				styleMap : styles,
				protocol : new OpenLayers.Protocol.WFS(
						{
							version : "1.1.0",
							srsName : "EPSG:900913",
							url : "/agent-walkability/geoserver/wfs",
							featureType : "melton_roads_sample",
							geometryName : "geom",
							schema : "/agent-walkability/geoserver/wfs/DescribeFeatureType?version=1.1.0&typename=CSDILA_local:melton_roads_sample"
						})
			});
}

function setControls() {
	// configure the snapping agent
	var snap = new OpenLayers.Control.Snapping({
		layer : wfs
	});
	map.addControl(snap);
	snap.activate();

	// configure split agent
	var split = new OpenLayers.Control.Split({
		layer : wfs,
		source : wfs,
		tolerance : 0.0001,
		deferDelete : true,
		eventListeners : {
			aftersplit : function(event) {
				var msg = "Split resulted in " + event.features.length
						+ " features.";
				flashFeatures(event.features);
			}
		}
	});
	map.addControl(split);
	split.activate();

	// add some editing tools to a panel
	var panel = new OpenLayers.Control.Panel({
		displayClass : 'customEditingToolbar',
		allowDepress : true
	});
	var draw = new OpenLayers.Control.DrawFeature(wfs, OpenLayers.Handler.Path,
			{
				title : "Draw Feature",
				displayClass : "olControlDrawFeaturePoint",
				handlerOptions : {
					multi : true
				}
			});
	modify = new OpenLayers.Control.ModifyFeature(wfs, {
		displayClass : "olControlModifyFeature"
	});
	var del = new DeleteFeature(wfs, {
		title : "Delete Feature"
	});

	var save = new OpenLayers.Control.Button({
		title : "Save Changes",
		trigger : function() {
			if (modify.feature) {
				modify.selectControl.unselectAll();
			}
			saveStrategy.save();
		},
		displayClass : "olControlSaveFeatures"
	});

	panel.addControls([ save, del, modify, draw ]);

	map.addControl(panel);
}

function flashFeatures(features, index) {
	if (!index) {
		index = 0;
	}
	var current = features[index];
	if (current && current.layer === wfs) {
		wfs.drawFeature(features[index], "select");
	}
	var prev = features[index - 1];
	if (prev && prev.layer === wfs) {
		wfs.drawFeature(prev, "default");
	}
	++index;
	if (index <= features.length) {
		window.setTimeout(function() {
			flashFeatures(features, index)
		}, 100);
	}
}

function init() {

	map.addLayers([ osm, vectors ])
	initWFSTools();
	destinations = new OpenLayers.Layer.WMS("Destinations",
			"/agent-walkability/geoserver/wms", {
				LAYERS : 'CSDILA_local:random_destinations',
				srsName : "EPSG:900913",
				STYLES : '',
				format : 'image/png',
				tiled : true,
				transparent : true,
				tilesOrigin : map.maxExtent.left + ',' + map.maxExtent.bottom
			}, {
				buffer : 0,
				displayOutsideMaxExtent : true,
				reproject : true
			});
	destinations.setIsBaseLayer(false);
	destinations.setVisibility(false)
	map.addLayers([ wfs, destinations ])
	setControls();
	map.setCenter(new OpenLayers.LonLat(16093371, -4537265), 15);
	var click = new OpenLayers.Control.Click();
	map.addControl(click);
	map.addControl(pointControl);
	pointControl.activate();
	click.activate();

}

init();
