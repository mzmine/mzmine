/*
    Copyright 2005 VTT Biotechnology

    This file is part of MZmine.

    MZmine is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    MZmine is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with MZmine; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/
package net.sf.mzmine.util;
import net.sf.mzmine.alignmentresultmethods.*;
import net.sf.mzmine.alignmentresultvisualizers.*;
import net.sf.mzmine.datastructures.*;
import net.sf.mzmine.obsoletedistributionframework.*;
import net.sf.mzmine.peaklistmethods.*;
import net.sf.mzmine.rawdatamethods.*;
import net.sf.mzmine.rawdatavisualizers.*;
import net.sf.mzmine.userinterface.*;
import net.sf.mzmine.util.*;


public class HeatMapColorPicker {

	private int[] heatmap_pal_waypoints;
	private int[][] heatmap_pal_waypointRGBs;


	/**
	 * Constructor: Creates a new heatmap with given parameters
	 * @param	waypointIntensities		Intensity levels where exact color is defined
	 * @param	waypointRGBs			Colors (RGB values) at waypoints
	 */
	public HeatMapColorPicker(int[] intensityLevels, int[][] rgbValues) {
		heatmap_pal_waypoints = intensityLevels;
		heatmap_pal_waypointRGBs = rgbValues;
	}


	/**
	 * This method sets heatmap's waypoint intensity levels
	 * @param	waypointIntensities		Intensity levels where exact color is defined
	 */
	public void setIntensityLevels(int[] intensityLevels) {
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
	public int[] getIntensityLevels() {
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
	 * This function returns color for given intensity level as byte array
	 * @return	Array of RGB values
	 */
	public byte[] getColorB(int intensity) {

		int[] tmp = getColorI(intensity);
		byte[] res = new byte[3];

		res[0] = (byte)tmp[0];
		res[1] = (byte)tmp[1];
		res[2] = (byte)tmp[2];

		return res;

	}


	/**
	 * This function returns color for given intensity level as Color object
	 * @return	java.awt.Color object
	 */
	public java.awt.Color getColorC(int intensity) {

		int[] tmp = getColorI(intensity);

		float r = (float)tmp[0]/(float)255.0;
		float g = (float)tmp[1]/(float)255.0;
		float b = (float)tmp[2]/(float)255.0;

		java.awt.Color c = new java.awt.Color(r,g,b);

		return c;

	}

	/**
	 * This function returns color for given intensity level as Color object
	 * @return	Array of RGB values
	 */
	public int[] getColorI(int intensity) {
		int wpi = 0;
		int before_index;
		int before_intensity;
		double before_r, before_g, before_b;
		int after_index;
		int after_intensity;
		double after_r, after_g, after_b;
		int r,g,b;
		int[] res = new int[3];

		// Check for underflow or overflor
		if (intensity<=heatmap_pal_waypoints[0]) {
			res[0]=heatmap_pal_waypointRGBs[0][0];
			res[1]=heatmap_pal_waypointRGBs[0][1];
			res[2]=heatmap_pal_waypointRGBs[0][2];
			return res;
		}

		if (intensity>=heatmap_pal_waypoints[heatmap_pal_waypoints.length-1]) {
			res[0]=heatmap_pal_waypointRGBs[heatmap_pal_waypoints.length-1][0];
			res[1]=heatmap_pal_waypointRGBs[heatmap_pal_waypoints.length-1][1];
			res[2]=heatmap_pal_waypointRGBs[heatmap_pal_waypoints.length-1][2];
			return res;
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
		res[0]=r; res[1]=g; res[2]=b;
		return res;
	}


}