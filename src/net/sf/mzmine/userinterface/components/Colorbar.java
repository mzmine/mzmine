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
package net.sf.mzmine.userinterface.components;
import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import net.sf.mzmine.util.HeatMapColorPicker;


public class Colorbar extends JPanel {

	private static int marginX = 2;
	private static int marginY = 10;

	private HeatMapColorPicker colorMap;
	private int numOfSlots;
	private String[] intensityLabels;
	private int[] intensityLevels;


	public Colorbar(HeatMapColorPicker _colorMap, String[] _intensityLabels, int _numOfSlots) {
		colorMap = _colorMap;
		intensityLevels = colorMap.getIntensityLevels();

		intensityLabels = _intensityLabels;

		numOfSlots = _numOfSlots;

	}

	public void setColormap(HeatMapColorPicker _colorMap) {
		colorMap = _colorMap;
	}

	public void setIntensityLabels(String[] _intensityLabels) {
		intensityLabels = _intensityLabels;
	}


	public void paint(Graphics g) {

		intensityLevels = colorMap.getIntensityLevels();

		super.paint(g);

		double w = getWidth();
		double h = getHeight();

		double diff_y_dat = intensityLevels[intensityLevels.length-1]-intensityLevels[0];
		double diff_y_scr = h-marginY*2;

		double scrStep = diff_y_scr / numOfSlots;
		double currentY = h-marginY;;
		double dataStep = diff_y_dat / numOfSlots;
		double currentInt = intensityLevels[0];
		double pixelsPerData = diff_y_scr / diff_y_dat;

		Color c;

		// Draw colorbar
		for (int i=0; i<numOfSlots; i++) {

			c = colorMap.getColorC((int)java.lang.Math.round(currentInt));
			g.setColor(c);

			g.fillRect(	0,
						(int)java.lang.Math.round(currentY),
						(int)java.lang.Math.round(0.4*w),
						(int)java.lang.Math.round(currentY+scrStep)-(int)java.lang.Math.round(currentY)
						);

			currentInt += dataStep;
			currentY -= scrStep;
		}

		// Draw labels
		g.setColor(Color.black);
		for (int i=0; i<intensityLabels.length; i++) {
			currentY = h-marginY-(intensityLevels[i]-intensityLevels[0]) * pixelsPerData;
			String s = intensityLabels[i];
			g.drawBytes(s.getBytes(), 0, s.length(), (int)java.lang.Math.round(0.4*w), (int)(int)java.lang.Math.round(currentY)+5);

		}

	}

}