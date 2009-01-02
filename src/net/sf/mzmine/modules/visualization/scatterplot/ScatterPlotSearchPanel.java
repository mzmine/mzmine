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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

public class ScatterPlotSearchPanel extends JPanel implements ActionListener {

	private JTextField txtSearchField;
	private JComboBox comboSelection;
	private JList listOfAppliedSelections;
	private JButton btnAdd, btnRemove, btnSrch;
	private JCheckBox labeledItems, searchVisible;
	private JPanel pnlWorkspace;
	private ActionListener master;
	private Vector<ListSelectionItem> listSelectionGroup;

	public ScatterPlotSearchPanel(ActionListener masterFrame) {

		this.master = masterFrame;

		txtSearchField = new JTextField();
		txtSearchField.selectAll();
		txtSearchField.setMaximumSize(new Dimension(250, 30));
		txtSearchField.setEnabled(false);
		
		// Load selection combo
		listSelectionGroup = getListSelectionValues();
		comboSelection = new JComboBox(listSelectionGroup);
		comboSelection.setMaximumSize(new Dimension(200, 30));
		comboSelection.setEnabled(false);
		
		// Elements of buttons
		btnAdd = new JButton("Add");
		btnAdd.addActionListener(this);
		btnAdd.setActionCommand("ADD");
		btnAdd.setEnabled(false);

		btnRemove = new JButton("Remove");
		btnRemove.addActionListener(this);
		btnRemove.setActionCommand("REMOVE");
		btnRemove.setEnabled(false);

		btnSrch = new JButton("Search");
		btnSrch.addActionListener(this);
		btnSrch.setActionCommand("SEARCH");
		btnSrch.setEnabled(false);

		labeledItems = new JCheckBox(" Show item's labels ");
		labeledItems.addActionListener(this);
		labeledItems.setHorizontalAlignment(SwingConstants.CENTER);
		labeledItems.setActionCommand("LABEL_ITEMS");

		searchVisible = new JCheckBox(" Show search panel ");
		searchVisible.addActionListener(this);
		searchVisible.setHorizontalAlignment(SwingConstants.CENTER);
		searchVisible.setActionCommand("SEARCH_PANEL_VISIBLE");
		searchVisible.setSelected(false);

		JPanel pnllabeledItems = new JPanel(new BorderLayout());
		pnllabeledItems.add(new JSeparator(), BorderLayout.NORTH);
		pnllabeledItems.add(labeledItems, BorderLayout.WEST);
		pnllabeledItems.add(searchVisible, BorderLayout.CENTER);
		pnllabeledItems.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

		DefaultListModel listModel = new DefaultListModel();
		listOfAppliedSelections = new SelectionList(listModel);
		((SelectionList) listOfAppliedSelections).setMaster(this);
		/*
		 * listSelections = new JList(listModel){ public String
		 * getToolTipText(MouseEvent evt) {
		 * ToolTipManager.sharedInstance().setInitialDelay(1000);
		 * ToolTipManager.sharedInstance().setDismissDelay(5000); int index =
		 * locationToIndex(evt.getPoint()); if (this.getModel().getSize()>0){
		 * Object item = getModel().getElementAt(index); return
		 * ((ListSelectionItem) item).getTiptext(); } return null; } };
		 */
		
		listOfAppliedSelections.setCellRenderer(new ColorCellRenderer());
		listOfAppliedSelections.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listOfAppliedSelections.setLayoutOrientation(JList.VERTICAL);
		JScrollPane listScroller = new JScrollPane(listOfAppliedSelections);
		listScroller.setPreferredSize(new Dimension(250, 50));
		listScroller.setAlignmentX(LEFT_ALIGNMENT);

		JPanel listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));
		JLabel label = new JLabel("List of selections");
		label.setLabelFor(listOfAppliedSelections);
		listPanel.add(label);
		listPanel.add(listScroller);
		listPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

		pnlWorkspace = new JPanel();
		pnlWorkspace.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		c.ipadx = 50;
		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 0;
		pnlWorkspace.add(new JLabel("Search"), c);
		c.ipadx = 350;
		c.gridx = 1;
		c.gridy = 0;
		pnlWorkspace.add(txtSearchField, c);
		c.ipadx = 50;
		c.gridx = 2;
		c.gridy = 0;
		pnlWorkspace.add(btnSrch, c);

		c.gridx = 0;
		c.gridy = 1;
		pnlWorkspace.add(new JLabel("Load Selection"), c);
		c.ipadx = 350;
		c.gridx = 1;
		c.gridy = 1;
		pnlWorkspace.add(comboSelection, c);
		c.ipadx = 50;
		c.gridx = 2;
		c.gridy = 1;
		pnlWorkspace.add(btnAdd, c);

		c.anchor = GridBagConstraints.CENTER;
		c.ipady = 40; // make this component tall
		c.ipadx = 350;
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		c.gridheight = 3;
		pnlWorkspace.add(listPanel, c);
		c.ipadx = 50;
		c.ipady = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 2;
		c.gridy = 2;
		pnlWorkspace.add(Box.createVerticalStrut(20), c);
		c.gridx = 2;
		c.gridy = 3;
		pnlWorkspace.add(btnRemove, c);
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 3;
		pnlWorkspace.setVisible(false);

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(pnllabeledItems);
		add(pnlWorkspace);
	}

	public int getSelectionTypeIndex() {
		return 0;
	}

	public JList getSelectionList() {
		return listOfAppliedSelections;
	}

	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		if (command.equals("ADD")) {
			Object a = comboSelection.getSelectedItem();
			if (!((ListSelectionItem) a).isSelected()) {

				if (((ListSelectionItem) a).isCustomized()) {
					a = (Object) ((ListSelectionItem) a).clone();
					((ListSelectionItem) a)
							.setCustomizedSearchValues((JFrame) ((ScatterPlotPanel) master)
									.getMaster());
					String[][] values = ((ListSelectionItem) a)
							.getSearchValues();
					if (values.length == 0) {
						return;
					}
				}

				((DefaultListModel) listOfAppliedSelections.getModel()).addElement(a);
				((ListSelectionItem) a).setSelected(true);
				master.actionPerformed(event);
				btnRemove.setEnabled(true);
			}
			return;
		}

		if (command.equals("SEARCH")) {
			ListSelectionItem a = new ListSelectionItem();
			a.setManuallySearchValues(txtSearchField.getText());
			((DefaultListModel) listOfAppliedSelections.getModel())
					.addElement((Object) a);
			master.actionPerformed(event);
			btnRemove.setEnabled(true);
			return;
		}

		if (command.equals("REMOVE")) {
			int index = listOfAppliedSelections.getSelectedIndex();
			if (index == -1)
				index = 0;

			Object a = ((DefaultListModel) listOfAppliedSelections.getModel())
					.get(index);
			((ListSelectionItem) a).setSelected(false);

			if (((ListSelectionItem) a).isCustomized()) {
				((ListSelectionItem) a).removeCustomizedSearch();
			}

			((DefaultListModel) listOfAppliedSelections.getModel()).remove(index);
			int size = ((DefaultListModel) listOfAppliedSelections.getModel()).getSize();

			if (size == 0)
				btnRemove.setEnabled(false);

			master.actionPerformed(event);
			return;
		}

		if (command.equals("LABEL_ITEMS")) {
			ScatterPlotPanel panel= ((ScatterPlot) master).getMaster();
			panel.setLabelItems(labeledItems.isSelected());
			return;
		}

		if (command.equals("SEARCH_PANEL_VISIBLE")) {
			if (searchVisible.isSelected())
				pnlWorkspace.setVisible(true);
			else
				pnlWorkspace.setVisible(false);
			return;
		}


	}

	public void addCustomListSelection(ListSelectionItem selectionItem) {
		((DefaultListModel) listOfAppliedSelections.getModel())
				.addElement(selectionItem);
		selectionItem.setSelected(true);
		master.actionPerformed(new ActionEvent(this, 0, "ADD"));
		btnRemove.setEnabled(true);
	}

	public Vector<ListSelectionItem> getListSelectionValues() {
		Vector<ListSelectionItem> list = new Vector<ListSelectionItem>();
		list.add(new ListSelectionItem());
		return list;
	}

	public String getTextSearch() {
		return txtSearchField.getText();
	}

	public ActionListener getMaster() {
		return master;
	}

	public void activeButtons() {
		comboSelection.setEnabled(true);
		btnAdd.setEnabled(true);
		btnSrch.setEnabled(true);
		txtSearchField.setEnabled(true);
	}

}

