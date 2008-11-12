/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.peaklist.table;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.impl.SimpleCompoundIdentity;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableParameters;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTablePopupMenu;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableVisualizer;
import net.sf.mzmine.modules.visualization.peaklist.PeakListTableWindow;
import net.sf.mzmine.util.components.GroupableTableHeader;
import net.sf.mzmine.util.components.PopupListener;
import net.sf.mzmine.util.dialogs.CompoundIdentitySetupDialog;

import com.sun.java.TableSorter;

public class PeakListTable extends JTable {

	static final String UNKNOWN_IDENTITY = "Unknown";
	static final String REMOVE_IDENTITY = "Remove";
	static final String NEW_IDENTITY = "Add new...";

	private static final Font comboFont = new Font("SansSerif", Font.PLAIN, 10);

	private TableSorter sorter;
	private PeakListTableModel pkTableModel;
	private PeakList peakList;
	private PeakListRow peakListRow;
	private PeakListTableColumnModel cm;

	public PeakListTable(PeakListTableVisualizer visualizer,
			PeakListTableWindow window, PeakListTableParameters parameters,
			PeakList peakList) {

		this.peakList = peakList;

		this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		this.setAutoCreateColumnsFromModel(false);

		this.pkTableModel = new PeakListTableModel(peakList);

		GroupableTableHeader header = new GroupableTableHeader();
		setTableHeader(header);

		cm = new PeakListTableColumnModel(visualizer, header, pkTableModel,
				parameters, peakList);
		cm.setColumnMargin(0);
		setColumnModel(cm);

		// create default columns
		cm.createColumns();

		// Initialize sorter
		sorter = new TableSorter(pkTableModel, header);
		setModel(sorter);

		PeakListTablePopupMenu popupMenu = new PeakListTablePopupMenu(window,
				this, cm, peakList);
		addMouseListener(new PopupListener(popupMenu));

		setRowHeight(parameters.getRowHeight());

	}

	public PeakList getPeakList() {
		return peakList;
	}

	public TableCellEditor getCellEditor(int row, int column) {

		CommonColumnType commonColumn = pkTableModel.getCommonColumn(column);
		if (commonColumn == CommonColumnType.IDENTITY) {
			int peakListRowIndex = sorter.modelIndex(row);
			peakListRow = peakList.getRow(peakListRowIndex);

			PeakIdentity identities[] = peakListRow.getCompoundIdentities();
			PeakIdentity preferredIdentity = peakListRow
					.getPreferredCompoundIdentity();
			JComboBox combo;

			if ((identities != null) && (identities.length > 0)) {
				combo = new JComboBox(identities);
				combo.addItem("-------------------------");
				combo.addItem(REMOVE_IDENTITY);
			} else {
				combo = new JComboBox();
			}

			combo.setFont(comboFont);

			combo.addItem(UNKNOWN_IDENTITY);
			combo.addItem(NEW_IDENTITY);
			if (preferredIdentity == null) {
				combo.setSelectedItem(UNKNOWN_IDENTITY);
			} else
				combo.setSelectedItem(preferredIdentity);

			combo.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					JComboBox combo = (JComboBox) e.getSource();
					Object item = combo.getSelectedItem();
					if (item != null) {
						if (item.toString() == NEW_IDENTITY) {
							CompoundIdentitySetupDialog dialog = new CompoundIdentitySetupDialog(
									peakListRow);
							dialog.setVisible(true);
							return;
						}
						if (item.toString() == UNKNOWN_IDENTITY) {
							peakListRow
									.addCompoundIdentity(new SimpleCompoundIdentity(
											null, UNKNOWN_IDENTITY, null, null,
											null, "User defined", null), true);
							return;
						}
						if (item.toString() == REMOVE_IDENTITY) {
							PeakIdentity identity = peakListRow
									.getPreferredCompoundIdentity();
							if (identity != PeakIdentity.UNKNOWN_IDENTITY) {
								peakListRow.removeCompoundIdentity(identity);
								DefaultComboBoxModel comboModel = (DefaultComboBoxModel) combo
										.getModel();
								comboModel.removeElement(identity);
							}
							return;
						}
						if (item instanceof PeakIdentity) {
							peakListRow
									.setPreferredCompoundIdentity((PeakIdentity) item);
						}
					}
				}
			});

			DefaultCellEditor cellEd = new DefaultCellEditor(combo);
			return cellEd;
		}

		return super.getCellEditor(row, column);

	}

}