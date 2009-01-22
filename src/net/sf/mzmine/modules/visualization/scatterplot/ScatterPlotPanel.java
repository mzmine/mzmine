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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel.ScatterPlotDataSet;
import net.sf.mzmine.modules.visualization.tic.TICVisualizer;
import net.sf.mzmine.modules.visualization.tic.TICVisualizerParameters;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import org.jfree.data.general.DatasetChangeEvent;

public class ScatterPlotPanel extends JPanel implements ActionListener {
	
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private JComboBox comboX, comboY, comboFold;
	private JTextField txtSearchField;
	private JButton btnSrch;
	private JCheckBox labeledItems;
	private JLabel itemName, numOfDisplayedItems;
	private static String[] foldXvalues = { "2", "4", "5", "8", "10", "15",
			"20", "50", "100", "200", "1000" };
	private ScatterPlotToolBar toolbar;
	private ScatterPlot plot;
	private ActionListener scatterPlotWindow;
	private ScatterPlotDataSet dataSet;
	private PeakList peakList;

	public ScatterPlotPanel(ActionListener scatterPlotWindow) {

		this.scatterPlotWindow = scatterPlotWindow;

		// Axis X
		comboX = new JComboBox();
		comboX.addActionListener(this);
		comboX.setActionCommand("DOMAIN");
		comboX.setEnabled(false);

		JPanel pnlX1 = new JPanel(new BorderLayout());
		pnlX1.add(new JLabel("Axis X", SwingConstants.CENTER),
				BorderLayout.CENTER);
		pnlX1.add(comboX, BorderLayout.SOUTH);
		JPanel pnlX = new JPanel(new FlowLayout());
		pnlX.add(pnlX1);

		// Axis Y
		comboY = new JComboBox();
		comboY.addActionListener(this);
		comboY.setActionCommand("DOMAIN");
		comboY.setEnabled(false);

		JPanel pnlY1 = new JPanel(new BorderLayout());
		pnlY1.add(new JLabel("Axis Y", SwingConstants.CENTER),
				BorderLayout.CENTER);
		pnlY1.add(comboY, BorderLayout.SOUTH);
		JPanel pnlY = new JPanel(new FlowLayout());
		pnlY.add(pnlY1);

		// Fold
		comboFold = new JComboBox(foldXvalues);
		comboFold.addActionListener(this);
		comboFold.setActionCommand("FOLD");
		comboFold.setEnabled(false);


		DefaultListCellRenderer centerRenderer = new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList jList,
					Object o, int i, boolean b, boolean b1) {
				JLabel rendrlbl = (JLabel) super.getListCellRendererComponent(
						jList, o, i, b, b1);
				rendrlbl.setHorizontalAlignment(SwingConstants.CENTER);
				return rendrlbl;
			}
		};

		comboFold.setRenderer(centerRenderer);

		JPanel pnlFold1 = new JPanel(new FlowLayout());
		pnlFold1.add(new JLabel("Fold (nX)", SwingConstants.CENTER));
		pnlFold1.add(comboFold);

		txtSearchField = new JTextField();
		txtSearchField.selectAll();
		txtSearchField.setEnabled(true);
		
		btnSrch = new JButton("Search");
		btnSrch.addActionListener(this);
		btnSrch.setActionCommand("SEARCH");
		btnSrch.setEnabled(true);
		
		JPanel pnlSearch = new JPanel();
		pnlSearch.setLayout(new BoxLayout(pnlSearch, BoxLayout.X_AXIS));
		pnlSearch.add(txtSearchField);
		pnlSearch.add(Box.createRigidArea(new Dimension(10, 1)));
		pnlSearch.add(btnSrch);
		
		labeledItems = new JCheckBox(" Show item's labels ");
		labeledItems.addActionListener(this);
		labeledItems.setHorizontalAlignment(SwingConstants.CENTER);
		labeledItems.setActionCommand("LABEL_ITEMS");


		JPanel pnlGrid = new JPanel();
		pnlGrid.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();

		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(5, 5, 5, 5);
		c.ipadx = 50;
		c.gridwidth = 1;

		c.gridx = 0;
		c.gridy = 0;
		pnlGrid.add(Box.createRigidArea(new Dimension(0, 10)), c);
		c.ipadx = 10;
		c.gridx = 0;
		c.gridy = 1;
		pnlGrid.add(new JLabel("Axis X"), c);
		c.gridx = 1;
		c.gridy = 1;
		pnlGrid.add(comboX, c);
		c.gridx = 2;
		c.gridy = 1;
		pnlGrid.add(new JLabel("Axis Y"), c);
		c.gridx = 3;
		c.gridy = 1;
		pnlGrid.add(comboY, c);
		c.gridx = 4;
		c.gridy = 1;
		pnlGrid.add(labeledItems, c);

		
		c.gridx = 0;
		c.gridy = 2;
		pnlGrid.add(new JLabel("Search"), c);
		c.gridwidth = 3;
		c.gridx = 1;
		c.gridy = 2;
		pnlGrid.add(pnlSearch, c);

		c.gridx = 4;
		c.gridy = 2;
		pnlGrid.add(pnlFold1, c);

		// Creates plot and toolbar
		plot = new ScatterPlot(this);
        toolbar = new ScatterPlotToolBar(((ActionListener) plot));

		itemName = new JLabel("NO SELECTED POINT");
		itemName.setForeground(Color.BLUE);
		itemName.setFont(new Font("SansSerif", Font.BOLD, 15));
		numOfDisplayedItems = new JLabel("");
		numOfDisplayedItems.setFont(new Font("SansSerif", Font.PLAIN, 10));
		JPanel pnlName = new JPanel();
		pnlName.setLayout(new BoxLayout(pnlName, BoxLayout.X_AXIS));
		pnlName.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		pnlName.add(numOfDisplayedItems);
		pnlName.add(Box.createHorizontalGlue());
		pnlName.add(itemName);

		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

		JPanel pnlPlot = new JPanel(new BorderLayout());
		pnlPlot.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlot.setBackground(Color.white);

		pnlPlot.add(toolbar, BorderLayout.EAST);
		pnlPlot.add(plot, BorderLayout.CENTER);

		JPanel panelPlotAndName = new JPanel(new BorderLayout());
		panelPlotAndName.add(pnlName, BorderLayout.NORTH);
		panelPlotAndName.add(pnlPlot, BorderLayout.CENTER);

		JPanel pnlWorkspace = new JPanel(new BorderLayout());
		pnlWorkspace.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlWorkspace.add(panelPlotAndName, BorderLayout.CENTER);
		pnlWorkspace.add(pnlGrid, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(pnlWorkspace, BorderLayout.CENTER);

	}

	public ScatterPlot getPlot() {
		return plot;
	}

	public ActionListener getMaster() {
		return scatterPlotWindow;
	}

	public void actionPerformed(ActionEvent event) {

		String command = event.getActionCommand();

		if (command.equals("DOMAIN")) {
			int x = comboX.getSelectedIndex();
			int y = comboY.getSelectedIndex();
			dataSet.setDomainsIndexes(x, y);
			dataSet.updateListofAppliedSelection(txtSearchField.getText());
			numOfDisplayedItems.setText(dataSet.getDisplayedCount());
			plot.setAxisNames(comboX.getSelectedItem().toString(), comboY.getSelectedItem().toString());
			plot.getXYPlot().datasetChanged(
					new DatasetChangeEvent(plot, dataSet));
			return;
		}

		if (command.equals("FOLD")) {
			plot.startDatasetCounter();
			plot.drawDiagonalLines(dataSet);
			return;
		}

		if ((command.equals("ADD")) || (command.equals("REMOVE"))
				|| (command.equals("LABEL_ITEMS"))
				|| (command.equals("SEARCH"))) {
			setLabelItems(labeledItems.isSelected());
			dataSet.updateListofAppliedSelection(txtSearchField.getText());
			plot.setSeriesColor(dataSet);
			return;
		}

		if (command.equals("SETUP_AXES")) {
			AxesSetupDialog dialog = new AxesSetupDialog(plot.getXYPlot());
			dialog.setVisible(true);
		}

		if (command.equals("TIC")) {
			
				int index = getCursorPosition();

				if (index == -1) {
					JOptionPane
							.showMessageDialog(
									this,
									"No point is selected, if you require to make a generic search use the main menu \"Search\"",
									"Searching online error",
									JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				ChromatographicPeak[] peaks = dataSet.getPeaksAt(index);
				Range rtRange = dataSet.getRawDataFilesRTRangeAt(index);
				Range MZRange = null;
				
				for (ChromatographicPeak p: peaks){
					if (MZRange == null){
						MZRange = p.getRawDataPointsMZRange();
					}
					else {
						MZRange.extendRange(p.getRawDataPointsMZRange());
					}
				}
				
				
				TICVisualizer.showNewTICVisualizerWindow(
						dataSet.getRawDataFilesDisplayed(),
						peaks, 1,
						TICVisualizerParameters.plotTypeBP, rtRange, MZRange);

		}

		if (command.equals("SHOW_ITEM_NAME")) {
			
			int index = getCursorPosition();
			if (index > -1){
			String name = dataSet.getDataPointName(index);
			if (name == "")
				name = "Unknown";
			itemName.setText(name);
			}
		}

		if (command.equals("DATASET_UPDATED")) {
			plot.getXYPlot().datasetChanged(
					new DatasetChangeEvent(plot, dataSet));
		}
	
		if (command.equals("DATASET_CREATED")) {
			setDomainsValues(dataSet.getDomainsNames());
			enableButtons();
			plot.startDatasetCounter();
			plot.addDataset(dataSet);
		}

	}

	public void setPeakList(PeakList peakList) {

		dataSet = new ScatterPlotDataSet(peakList, 0, 1, this);
		this.peakList = peakList;
		setDomainsValues(dataSet.getDomainsNames());
		enableButtons();
		plot.startDatasetCounter();
		plot.addDataset(dataSet);
		numOfDisplayedItems.setText(dataSet.getDisplayedCount());

	}

	public void setDomainsValues(String[] domains) {
		comboY.setModel(new DefaultComboBoxModel(domains));
		comboX.setModel(new DefaultComboBoxModel(domains));
		comboY.setSelectedIndex(1);
		plot.getXYPlot().getDomainAxis().setLabel(domains[0]);
		plot.getXYPlot().getRangeAxis().setLabel(domains[1]);
	}

	public void enableButtons() {
		comboX.setEnabled(true);
		comboY.setEnabled(true);
		comboFold.setEnabled(true);
	}

	public int selectedFold() {

		String foldText = foldXvalues[comboFold.getSelectedIndex()];
		int foldValue = Integer.parseInt(foldText);
		if (foldValue >= 0) {
			return foldValue;
		} else
			return 2;

	}

	/**
	 * @return current cursor position
	 */
	public int getCursorPosition() {
		double valueX = plot.getXYPlot().getDomainCrosshairValue();
		double valueY = plot.getXYPlot().getRangeCrosshairValue();
		return dataSet.getIndex(valueX, valueY);
	}

	public void setLabelItems(boolean status) {
		int index = plot.getXYPlot().indexOf(dataSet);
		plot.getXYPlot().getRenderer(index).setBaseItemLabelsVisible(status);
	}

	public ScatterPlotDataSet getDataSet(){
		return dataSet;
	}

	public PeakList getPeakList(){
		return peakList;
	}
}

