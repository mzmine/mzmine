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

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.clustering;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;

public class ClusteringReportWindow extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private JTable table;

    public ClusteringReportWindow(String[] samplesOrVariables,
	    Integer[] clusteringData, String title) {
	super(title);
	String[] columnNames = { "Variables", "Cluster number" };
	Object[][] data = new Object[samplesOrVariables.length][2];
	for (int i = 0; i < samplesOrVariables.length; i++) {
	    data[i][0] = samplesOrVariables[i];
	    data[i][1] = clusteringData[i];
	}

	table = new JTable(data, columnNames);

	JScrollPane scrollPane = new JScrollPane(table);
	table.setFillsViewportHeight(true);
	this.add(scrollPane);

	pack();
    }
}
