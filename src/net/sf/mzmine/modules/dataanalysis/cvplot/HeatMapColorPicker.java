package net.sf.mzmine.modules.dataanalysis.cvplot;

import java.awt.Color;

public class HeatMapColorPicker {

private double[] heatmap_pal_waypoints = {	0.00, 0.15, 0.30, 0.45 };
		/*
		{00, 20, 40, 60},
		{00, 25, 50, 75},
		{00, 30, 60, 90}
		*/

private int[][] heatmap_pal_waypointRGBs = { {0,0,0}, {102,255,102}, {51,102,255}, {255,0,0} };


/**
 * This method sets heatmap's waypoint intensity levels
 * @param	waypointIntensities		Intensity levels where exact color is defined
 */
public void setIntensityLevels(double[] intensityLevels) {
	heatmap_pal_waypoints = intensityLevels;
}

/**
 * This method returns RGB values at heatmap's waypoint intensity levels.
 * @param	waypointRGBs			Colors (RGB values) at waypoints
 */
public void setRGBValues(int[][] rgbValues) {
	heatmap_pal_waypointRGBs = rgbValues;
}



/**
 * This method returns heatmap's waypoint intensity levels
 * @return	array of intensity levels
 */
public double[] getIntensityLevels() {
	return heatmap_pal_waypoints;
}

/**
 * This method returns RGB values at heatmap's waypoint intensity levels.
 * @return	array of RGB value arrays
 */
public int[][] getRGBValues() {
	return heatmap_pal_waypointRGBs;
}


/**
 * This function returns color for given intensity level as Color object
 * @return	Array of RGB values
 */
public Color getColor(double intensity) {
	int wpi = 0;
	int before_index;
	double before_intensity;
	double before_r, before_g, before_b;
	int after_index;
	double after_intensity;
	double after_r, after_g, after_b;
	int r,g,b;
	int[] res = new int[3];

	// Check for underflow or overflor
	if (intensity<=heatmap_pal_waypoints[0]) {
		return new Color(	heatmap_pal_waypointRGBs[0][0],
							heatmap_pal_waypointRGBs[0][1],
							heatmap_pal_waypointRGBs[0][2]);
	}

	if (intensity>=heatmap_pal_waypoints[heatmap_pal_waypoints.length-1]) {
		return new Color(	heatmap_pal_waypointRGBs[heatmap_pal_waypoints.length-1][0],
							heatmap_pal_waypointRGBs[heatmap_pal_waypoints.length-1][1],
							heatmap_pal_waypointRGBs[heatmap_pal_waypoints.length-1][2]);
	}

	// Search for a waypoint before and after the intensity
	before_index=0;
	while (heatmap_pal_waypoints[wpi]<intensity) {
		before_index = wpi;
		wpi++;
	}
	after_index = wpi;

	// Interpolate
	before_intensity = heatmap_pal_waypoints[before_index];
	after_intensity = heatmap_pal_waypoints[after_index];

	before_r = heatmap_pal_waypointRGBs[before_index][0];
	before_g = heatmap_pal_waypointRGBs[before_index][1];
	before_b = heatmap_pal_waypointRGBs[before_index][2];

	after_r = heatmap_pal_waypointRGBs[after_index][0];
	after_g = heatmap_pal_waypointRGBs[after_index][1];
	after_b = heatmap_pal_waypointRGBs[after_index][2];

	r = (int)java.lang.Math.round((double)(after_r - before_r) / (double)(after_intensity - before_intensity) * (double)(intensity - before_intensity) + (double)before_r);
	g = (int)java.lang.Math.round((double)(after_g - before_g) / (double)(after_intensity - before_intensity) * (double)(intensity - before_intensity) + (double)before_g);
	b = (int)java.lang.Math.round((double)(after_b - before_b) / (double)(after_intensity - before_intensity) * (double)(intensity - before_intensity) + (double)before_b);

	// Return result
	return new Color(r,g,b);
}


}