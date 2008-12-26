/*
 * Copyright 2006-2008 The MZmine Development Team
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

package net.sf.mzmine.modules.visualization.scatterplot;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.util.dialogs.AxesSetupDialog;

import org.jfree.data.general.DatasetChangeEvent;

public class ScatterPlotPanel extends JPanel implements ActionListener {
	
    private Logger logger = Logger.getLogger(this.getClass().getName());

	private JComboBox comboX, comboY, comboFold;
	private JLabel itemName;
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
		comboX.setMaximumSize(new Dimension(50, 30));
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
		comboY.setMaximumSize(new Dimension(50, 30));
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
		comboFold.setMaximumSize(new Dimension(50, 30));
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

		itemName = new JLabel("NO SELECTED POINT");
		itemName.setForeground(Color.BLUE);
		itemName.setFont(new Font("SansSerif", Font.BOLD, 15));
		JPanel pnlName = new JPanel();
		pnlName.setLayout(new BoxLayout(pnlName, BoxLayout.X_AXIS));
		pnlName.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		pnlName.add(Box.createRigidArea(new Dimension(50, 0)));
		pnlName.add(Box.createHorizontalGlue());
		pnlName.add(itemName);

		JPanel pnlFold1 = new JPanel(new BorderLayout());
		pnlFold1.add(new JLabel("Fold (nX)", SwingConstants.CENTER),
				BorderLayout.CENTER);
		pnlFold1.add(comboFold, BorderLayout.SOUTH);
		JPanel pnlFold = new JPanel(new FlowLayout());
		pnlFold.add(pnlFold1);
		
        logger.finest("Creates scatterPlot");
		plot = new ScatterPlot(this);

        logger.finest("Creates scatterPlotToolBar");
        toolbar = new ScatterPlotToolBar(((ActionListener) plot));

		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

		JPanel pnlPlot = new JPanel(new BorderLayout());
		pnlPlot.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlot.setBackground(Color.white);

		pnlPlot.add(toolbar, BorderLayout.EAST);
		pnlPlot.add(plot, BorderLayout.CENTER);

		JPanel pnlPlotName = new JPanel(new BorderLayout());
		pnlPlotName.add(pnlName, BorderLayout.NORTH);
		pnlPlotName.add(pnlPlot, BorderLayout.CENTER);

		JPanel pnl1 = new JPanel(new BorderLayout());
		pnl1.add(pnlPlotName, BorderLayout.CENTER);
		pnl1.add(pnlX, BorderLayout.SOUTH);

		JPanel pnl2 = new JPanel();
		pnl2.setLayout(new BoxLayout(pnl2, BoxLayout.Y_AXIS));
		pnl2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnl2.add(Box.createRigidArea(new Dimension(0, 50)));
		pnl2.add(Box.createVerticalGlue());
		pnl2.add(pnlY);
		pnl2.add(Box.createRigidArea(new Dimension(0, 20)));
		pnl2.add(pnlFold);
		pnl2.add(Box.createVerticalGlue());

		JPanel pnlWorkspace = new JPanel(new BorderLayout());
		pnlWorkspace.add(pnl1, BorderLayout.CENTER);
		pnlWorkspace.add(pnl2, BorderLayout.WEST);

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
			plot.getToolTipGenerator().setDomainsIndexes(x, y);
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
			dataSet.updateListofAppliedSelection();
			plot.setSeriesColor(dataSet);
			plot.getXYPlot().datasetChanged(
					new DatasetChangeEvent(plot, dataSet));
			return;
		}

		if (command.equals("SETUP_AXES")) {
			AxesSetupDialog dialog = new AxesSetupDialog(plot.getXYPlot());
			dialog.setVisible(true);
		}

		if (command.equals("ON_LINE")) {
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

			/*DataFile df = dataSet.getDataFile();
			SearchKEGGDialog dialog = new SearchKEGGDialog(df.getType(), df
					.getDataPoint(index).getName());
			dialog.setVisible(true);*/
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

		if (command.equals("CROP_SEL")) {
			boolean visible = plot.getRawDataVisibleStatus();
			plot.setRawDataVisible(!visible);
		}

	
	}

	public void setPeakList(PeakList peakList, JList list) {

		dataSet = new ScatterPlotDataSet(peakList, 1, 0, list, this);
		this.peakList = peakList;
		setDomainsValues(dataSet.getDomainsNames());
		activeButtons();
		plot.startDatasetCounter();
		plot.addDataset(dataSet);

	}

	public void setDomainsValues(String[] domains) {
		comboY.setModel(new DefaultComboBoxModel(domains));
		comboX.setModel(new DefaultComboBoxModel(domains));
		comboX.setSelectedIndex(1);
	}

	public void activeButtons() {
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
		float valueX = (float) plot.getXYPlot().getDomainCrosshairValue();
		float valueY = (float) plot.getXYPlot().getRangeCrosshairValue();
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

