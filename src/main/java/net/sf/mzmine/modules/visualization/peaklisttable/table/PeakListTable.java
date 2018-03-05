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

package net.sf.mzmine.modules.visualization.peaklisttable.table;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.event.RowSorterEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableParameters;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTablePopupMenu;
import net.sf.mzmine.modules.visualization.peaklisttable.PeakListTableWindow;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.util.components.ComponentToolTipManager;
import net.sf.mzmine.util.components.ComponentToolTipProvider;
import net.sf.mzmine.util.components.GroupableTableHeader;
import net.sf.mzmine.util.components.PeakSummaryComponent;
import net.sf.mzmine.util.components.PopupListener;
import net.sf.mzmine.util.dialogs.PeakIdentitySetupDialog;

public class PeakListTable extends JTable implements ComponentToolTipProvider {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    static final String EDIT_IDENTITY = "Edit";
    static final String REMOVE_IDENTITY = "Remove";
    static final String NEW_IDENTITY = "Add new...";

    private static final Font comboFont = new Font("SansSerif", Font.PLAIN, 10);

    private PeakListTableWindow window;
    private PeakListTableModel pkTableModel;
    private PeakList peakList;
    private PeakListRow peakListRow;
    private TableRowSorter<PeakListTableModel> sorter;
    private PeakListTableColumnModel cm;
    private ComponentToolTipManager ttm;
    private DefaultCellEditor currentEditor = null;

    public PeakListTable(PeakListTableWindow window, ParameterSet parameters,
	    PeakList peakList) {

	this.window = window;
	this.peakList = peakList;

	this.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	this.setAutoCreateColumnsFromModel(false);

	this.pkTableModel = new PeakListTableModel(peakList);
	setModel(pkTableModel);

	GroupableTableHeader header = new GroupableTableHeader();
	setTableHeader(header);

	cm = new PeakListTableColumnModel(header, pkTableModel, parameters,
		peakList);
	cm.setColumnMargin(0);
	setColumnModel(cm);

	// create default columns
	cm.createColumns();

	// Initialize sorter
	sorter = new TableRowSorter<PeakListTableModel>(pkTableModel);
	setRowSorter(sorter);

	PeakListTablePopupMenu popupMenu = new PeakListTablePopupMenu(window,
		this, cm, peakList);
	addMouseListener(new PopupListener(popupMenu));
	header.addMouseListener(new PopupListener(popupMenu));

	int rowHeight = parameters.getParameter(
		PeakListTableParameters.rowHeight).getValue();
	setRowHeight(rowHeight);

	ttm = new ComponentToolTipManager();
	ttm.registerComponent(this);

    }

    public JComponent getCustomToolTipComponent(MouseEvent event) {

	JComponent component = null;
	String text = this.getToolTipText(event);
	if (text == null) {
	    return null;
	}

	if (text.contains(ComponentToolTipManager.CUSTOM)) {
	    String values[] = text.split("-");
	    int myID = Integer.parseInt(values[1].trim());
	    for (PeakListRow row : peakList.getRows()) {
		if (row.getID() == myID) {
		    component = new PeakSummaryComponent(row,
			    peakList.getRawDataFiles(), true, false, false,
			    true, false, ComponentToolTipManager.bg);
		    break;
		}
	    }

	} else {
	    text = "<html>" + text.replace("\n", "<br>") + "</html>";
	    JLabel label = new JLabel(text);
	    label.setFont(UIManager.getFont("ToolTip.font"));
	    JPanel panel = new JPanel();
	    panel.setBackground(ComponentToolTipManager.bg);
	    panel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
	    panel.add(label);
	    component = panel;
	}

	return component;

    }

    public PeakList getPeakList() {
	return peakList;
    }

    public TableCellEditor getCellEditor(int row, int column) {

	CommonColumnType commonColumn = pkTableModel.getCommonColumn(column);
	if (commonColumn == CommonColumnType.IDENTITY) {

	    row = this.convertRowIndexToModel(row);
	    peakListRow = peakList.getRow(row);

	    PeakIdentity identities[] = peakListRow.getPeakIdentities();
	    PeakIdentity preferredIdentity = peakListRow
		    .getPreferredPeakIdentity();
	    JComboBox<Object> combo;

	    if ((identities != null) && (identities.length > 0)) {
		combo = new JComboBox<Object>(identities);
		combo.addItem("-------------------------");
		combo.addItem(REMOVE_IDENTITY);
		combo.addItem(EDIT_IDENTITY);
	    } else {
		combo = new JComboBox<Object>();
	    }

	    combo.setFont(comboFont);
	    combo.addItem(NEW_IDENTITY);
	    if (preferredIdentity != null) {
		combo.setSelectedItem(preferredIdentity);
	    }

	    combo.addActionListener(new ActionListener() {

		public void actionPerformed(ActionEvent e) {
		    JComboBox<?> combo = (JComboBox<?>) e.getSource();
		    Object item = combo.getSelectedItem();
		    if (item != null) {
			if (item.toString() == NEW_IDENTITY) {
			    PeakIdentitySetupDialog dialog = new PeakIdentitySetupDialog(
				    window, peakListRow);
			    dialog.setVisible(true);
			    return;
			}
			if (item.toString() == EDIT_IDENTITY) {
			    PeakIdentitySetupDialog dialog = new PeakIdentitySetupDialog(
				    window, peakListRow, peakListRow
					    .getPreferredPeakIdentity());
			    dialog.setVisible(true);
			    return;
			}
			if (item.toString() == REMOVE_IDENTITY) {
			    PeakIdentity identity = peakListRow
				    .getPreferredPeakIdentity();
			    if (identity != null) {
				peakListRow.removePeakIdentity(identity);
				DefaultComboBoxModel<?> comboModel = (DefaultComboBoxModel<?>) combo
					.getModel();
				comboModel.removeElement(identity);
			    }
			    return;
			}
			if (item instanceof PeakIdentity) {
			    peakListRow
				    .setPreferredPeakIdentity((PeakIdentity) item);
			    return;
			}
		    }

		}
	    });

	    // Keep the reference to the editor
	    currentEditor = new DefaultCellEditor(combo);

	    return currentEditor;
	}

	return super.getCellEditor(row, column);

    }

    /**
     * When user sorts the table, we have to cancel current combobox for
     * identity selection. Unfortunately, this doesn't happen automatically.
     */
    public void sorterChanged(RowSorterEvent e) {
	if (currentEditor != null) {
	    currentEditor.stopCellEditing();
	}
	super.sorterChanged(e);
    }

}