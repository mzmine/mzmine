/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot;

import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.plaf.ComponentUI;

public class ScatterPlotToolTip extends JToolTip {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private ScatterPlotDataSet dataSet;
	private int fold;
	private static ComponentUI toolTipUI = new ScatterPlotToolTipUI();

	public ScatterPlotToolTip() {
		logger.finest("Crea tooltip " + this.hashCode());
		setUI(toolTipUI);
	}

	public ScatterPlotDataSet getDataSet() {
		return dataSet;
	}

	public int getIndex() {
		try {
			return Integer.parseInt(this.getTipText());
		} catch (Exception e) {
			return -1;
		}
	}

	public void setDataFile(ScatterPlotDataSet newSet) {
		this.dataSet = newSet;
	}

	public void setSelectedFold(int fold) {
		this.fold = fold;
	}

	public int getFold() {
		return fold;
	}
	
	public JPanel getToolTipComponent() {
		int index = Integer.parseInt(this.getTipText());
		ScatterPlotToolTipComponent component = new ScatterPlotToolTipComponent(index, dataSet, fold);
	  return component;	
	
	}

}
