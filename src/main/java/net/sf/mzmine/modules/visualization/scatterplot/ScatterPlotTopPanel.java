/*
 * Copyright 2006-2015 The MZmine 2 Development Team
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

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotDataSet;

public class ScatterPlotTopPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JLabel itemNameLabel, numOfDisplayedItems;

    public ScatterPlotTopPanel() {

	itemNameLabel = new JLabel();
	itemNameLabel.setForeground(Color.BLUE);
	itemNameLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

	numOfDisplayedItems = new JLabel();
	numOfDisplayedItems.setFont(new Font("SansSerif", Font.PLAIN, 10));

	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	add(numOfDisplayedItems);
	add(Box.createHorizontalGlue());
	add(itemNameLabel);

    }

    public void updateNumOfItemsText(PeakList peakList,
	    ScatterPlotDataSet dataSet, ScatterPlotAxisSelection axisX,
	    ScatterPlotAxisSelection axisY, int fold) {

	int percentage = 100;

	int totalItems = dataSet.getItemCount(0);

	int itemsWithinFold = 0;
	double x, y, ratio;
	final double thresholdMax = fold, thresholdMin = (1.0 / fold);

	for (int i = 0; i < totalItems; i++) {
	    x = dataSet.getXValue(0, i);
	    y = dataSet.getYValue(0, i);
	    ratio = x / y;
	    if ((ratio >= thresholdMin) && (ratio <= thresholdMax))
		itemsWithinFold++;
	}

	ratio = ((double) itemsWithinFold / totalItems);

	percentage = (int) Math.round(ratio * 100);

	String display = "<html><b>" + dataSet.getItemCount(0)
		+ "</b> peaks displayed, <b>" + percentage + "%</b> within <b>"
		+ fold + "-fold</b> margin";

	// If we have a selection, show it
	if (dataSet.getSeriesCount() > 1) {

	    itemsWithinFold = 0;
	    totalItems = dataSet.getItemCount(1);

	    for (int i = 0; i < totalItems; i++) {
		x = dataSet.getXValue(1, i);
		y = dataSet.getYValue(1, i);
		ratio = x / y;
		if ((ratio >= thresholdMin) && (ratio <= thresholdMax))
		    itemsWithinFold++;
	    }

	    ratio = ((double) itemsWithinFold / totalItems);

	    percentage = (int) Math.round(ratio * 100);

	    display += " (" + percentage + "% of selected)";
	}

	display += "</html>";

	numOfDisplayedItems.setText(display);
    }

    public void updateItemNameText(PeakListRow selectedRow) {

	if (selectedRow == null) {
	    itemNameLabel.setText("");
	    return;
	}

	PeakIdentity identity = selectedRow.getPreferredPeakIdentity();
	String itemName;
	if (identity != null) {
	    itemName = identity.getName();
	} else {
	    itemName = selectedRow.toString();
	}

	itemNameLabel.setText(itemName);
    }

}
