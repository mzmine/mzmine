/*
 * Copyright 2006 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.visualizers.alignmentresult.table;

import javax.swing.JTable;

import net.sf.mzmine.data.AlignmentResult;
import sunutils.TableSorter;

public class AlignmentResultTable extends JTable {

	private AlignmentResult alignmentResult;
	private AlignmentResultTableModel tableModel;
	private TableSorter sorter;
	
	public AlignmentResultTable(AlignmentResultTableVisualizerWindow masterFrame, AlignmentResult alignmentResult) {
		this.alignmentResult = alignmentResult;
		
		AlignmentResultTableColumnSelection columnSelection = new AlignmentResultTableColumnSelection();
		columnSelection.setAllColumns();
		
		initializeTableModel(columnSelection);
	}

	
	private void initializeTableModel(AlignmentResultTableColumnSelection columnSelection) {

		tableModel = new AlignmentResultTableModel(alignmentResult, columnSelection);

		// Initialize sorter
		sorter = new TableSorter(tableModel);
		getTableHeader().setReorderingAllowed(false);
		sorter.addMouseListenerToHeaderInTable(this);
		setModel(sorter);

	}
	
	public void setColumnSelection(AlignmentResultTableColumnSelection columnSelection) {
		initializeTableModel(columnSelection);
	}
	
	public AlignmentResultTableColumnSelection getColumnSelection() {
		return tableModel.getColumnSelection();
	}


	

}