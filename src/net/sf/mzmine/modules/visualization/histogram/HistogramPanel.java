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

package net.sf.mzmine.modules.visualization.histogram;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramDataType;
import net.sf.mzmine.modules.visualization.histogram.histogramdatalabel.HistogramPlotDataset;
import net.sf.mzmine.util.NumberFormatter;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.general.DatasetChangeEvent;

public class HistogramPanel extends JPanel implements ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private JComboBox comboDataType;
	private JTextField numberBinField, widthBinField;
	private JButton btnUpdate;
	private HistogramToolBar toolbar;
	private Histogram histogram;
	private ActionListener histogramWindow;
	private HistogramPlotDataset dataSet;
	private PeakList peakList;
	private int numOfBins = 5;

	public HistogramPanel(ActionListener histogramWindow) {

		this.histogramWindow = histogramWindow;

		// Data type
		comboDataType = new JComboBox(HistogramDataType.values());

		DefaultListCellRenderer centerRenderer = new DefaultListCellRenderer() {
			public Component getListCellRendererComponent(JList jList,
					Object o, int i, boolean b, boolean b1) {
				JLabel rendrlbl = (JLabel) super.getListCellRendererComponent(
						jList, o, i, b, b1);
				rendrlbl.setHorizontalAlignment(SwingConstants.CENTER);
				return rendrlbl;
			}
		};

		comboDataType.setRenderer(centerRenderer);

		// Text fields
		numberBinField = new JTextField();
		numberBinField.setEnabled(true);
		numberBinField.setEditable(false);
		numberBinField.setHorizontalAlignment(JTextField.CENTER);
		numberBinField.setPreferredSize(new Dimension(40, numberBinField
				.getPreferredSize().height));

		widthBinField = new JTextField();
		widthBinField.setEnabled(true);
		widthBinField.setHorizontalAlignment(JTextField.RIGHT);
		widthBinField.setPreferredSize(new Dimension(100, widthBinField
				.getPreferredSize().height));

		// Buttons
		btnUpdate = new JButton("Update");
		btnUpdate.addActionListener(this);
		btnUpdate.setActionCommand("UPDATE");
		btnUpdate.setEnabled(true);

		// Panels
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
		c.ipadx = 5;
		c.gridx = 0;
		c.gridy = 1;
		pnlGrid.add(new JLabel("Number of bins"), c);
		c.gridx = 1;
		c.gridy = 1;
		pnlGrid.add(numberBinField, c);
		c.gridx = 2;
		c.gridy = 1;
		pnlGrid.add(new JLabel("Bin width"), c);
		c.gridx = 3;
		c.gridy = 1;
		pnlGrid.add(widthBinField, c);
		c.gridx = 4;
		c.gridy = 1;
		pnlGrid.add(new JLabel("Plotted type of data"), c);
		c.gridx = 5;
		c.gridy = 1;
		pnlGrid.add(comboDataType, c);

		c.gridx = 6;
		c.gridy = 1;
		pnlGrid.add(btnUpdate, c);

		// Creates plot and toolbar
		histogram = new Histogram(this);
		toolbar = new HistogramToolBar(((ActionListener) histogram));

		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(5, 5, 5, 5);

		JPanel pnlPlot = new JPanel(new BorderLayout());
		pnlPlot.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlot.setBackground(Color.white);

		pnlPlot.add(toolbar, BorderLayout.EAST);
		pnlPlot.add(histogram, BorderLayout.CENTER);

		JPanel pnlWorkspace = new JPanel(new BorderLayout());
		pnlWorkspace.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		pnlWorkspace.add(pnlPlot, BorderLayout.CENTER);
		pnlWorkspace.add(pnlGrid, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(pnlWorkspace, BorderLayout.CENTER);

	}

	public Histogram getPlot() {
		return histogram;
	}

	public ActionListener getMaster() {
		return histogramWindow;
	}

	public void actionPerformed(ActionEvent event) {
		String command = event.getActionCommand();
		if (command.equals("UPDATE")) {

			HistogramDataType dataType = (HistogramDataType) comboDataType
					.getSelectedItem();
			dataSet.setHistogramDataType(dataType);
			histogram.setAxisNumberFormat(dataType);

			XYPlot plot = histogram.getXYPlot();
			NumberAxis axis = (NumberAxis) plot.getDomainAxis();
			axis.setAutoRange(true);
			NumberFormatter formatter = (NumberFormatter) axis.getNumberFormatOverride();

			try {
				double width = formatter.parse(widthBinField.getText())
						.doubleValue();
				//if (dataSet.getBinWidth() != width) {
					dataSet.setBinWidth(width);
				//} else {
					//int numBins = Integer.parseInt(numberBinField.getText());
					//dataSet.setNumberOfBins(numBins);
				//}
			} catch (ParseException e) {
				
				MZmineCore.getDesktop().displayErrorMessage(
						"Incorrect format, selected data type = \"" + dataType.getText()
								+ "\", bin width format must be like \"" + formatter.getPattern()
								+ "\".");
				// e.printStackTrace();
			}

			dataSet.updateHistogramDataset();

			histogram.getXYPlot().datasetChanged(
					new DatasetChangeEvent(histogram, dataSet));

			updateFields();

			return;
		}
	}

	public void setPeakList(PeakList peakList) {

		HistogramDataType dataType = (HistogramDataType) comboDataType
				.getSelectedItem();
		dataSet = new HistogramPlotDataset(peakList, numOfBins, dataType);
		this.peakList = peakList;
		histogram.addDataset(dataSet, dataType);
		updateFields();
	}

	public void updateFields() {
		numberBinField.setText(String.valueOf(dataSet.getNumberOfBins()));
		XYPlot plot = histogram.getXYPlot();
		NumberAxis axis = (NumberAxis) plot.getDomainAxis();
		NumberFormat formatter = axis.getNumberFormatOverride();
		String widthText = formatter.format(dataSet.getBinWidth());
		widthBinField.setText(widthText);
		HistogramDataType dataType = (HistogramDataType) comboDataType
				.getSelectedItem();
		histogram.setTitle(dataSet.getPeakList().getName(), dataType.getText());
	}

	public HistogramPlotDataset getDataSet() {
		return dataSet;
	}

	public PeakList getPeakList() {
		return peakList;
	}
}
