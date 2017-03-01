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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.scatterplot.scatterplotchart.ScatterPlotChart;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.SearchDefinition;
import net.sf.mzmine.util.SearchDefinitionType;
import net.sf.mzmine.util.components.CenteredListCellRenderer;

import com.google.common.collect.Range;

public class ScatterPlotBottomPanel extends JPanel implements ActionListener {

    private static final long serialVersionUID = 1L;

    private JComboBox<ScatterPlotAxisSelection> comboX, comboY;
    private JComboBox<String> comboFold;
    private JComboBox<SearchDefinitionType> comboSearchDataType;
    private JTextField txtSearchField;
    private JFormattedTextField minSearchField, maxSearchField;
    private JLabel labelRange;
    private JCheckBox labeledItems;

    private static final String[] foldXvalues = { "2", "4", "5", "8", "10",
	    "15", "20", "50", "100", "200", "1000" };

    private PeakList peakList;
    private ScatterPlotWindow window;
    private ScatterPlotChart chart;

    public ScatterPlotBottomPanel(ScatterPlotWindow window,
	    ScatterPlotChart chart, PeakList peakList) {

	this.window = window;
	this.peakList = peakList;
	this.chart = chart;

	// Axes combo boxes
	ScatterPlotAxisSelection axesOptions[] = ScatterPlotAxisSelection
		.generateOptionsForPeakList(peakList);

	comboX = new JComboBox<ScatterPlotAxisSelection>(axesOptions);
	comboX.addActionListener(this);
	comboX.setActionCommand("DATA_CHANGE");
	comboY = new JComboBox<ScatterPlotAxisSelection>(axesOptions);
	comboY.addActionListener(this);
	comboY.setActionCommand("DATA_CHANGE");

	// Fold
	comboFold = new JComboBox<String>(foldXvalues);
	comboFold.addActionListener(this);
	comboFold.setActionCommand("DATA_CHANGE");
	comboFold.setRenderer(new CenteredListCellRenderer());

	JPanel pnlFold = new JPanel(new FlowLayout());
	pnlFold.add(new JLabel("Fold (x)", SwingConstants.CENTER));
	pnlFold.add(comboFold);

	// Search
	txtSearchField = new JTextField();
	txtSearchField.selectAll();
	txtSearchField.setEnabled(true);
	txtSearchField.setPreferredSize(new Dimension(230, txtSearchField
		.getPreferredSize().height));

	minSearchField = new JFormattedTextField();
	minSearchField.selectAll();
	minSearchField.setVisible(false);
	minSearchField.setPreferredSize(new Dimension(100, minSearchField
		.getPreferredSize().height));

	labelRange = new JLabel("-");
	labelRange.setVisible(false);

	maxSearchField = new JFormattedTextField();
	maxSearchField.selectAll();
	maxSearchField.setVisible(false);
	maxSearchField.setPreferredSize(new Dimension(100, maxSearchField
		.getPreferredSize().height));

	comboSearchDataType = new JComboBox<SearchDefinitionType>(
		SearchDefinitionType.values());
	comboSearchDataType.addActionListener(this);
	comboSearchDataType.setActionCommand("SEARCH_DATA_TYPE");

	JPanel pnlGridSearch = new JPanel();
	pnlGridSearch.setLayout(new GridBagLayout());
	GridBagConstraints cSrch = new GridBagConstraints();
	cSrch.fill = GridBagConstraints.HORIZONTAL;
	cSrch.insets = new Insets(5, 5, 5, 5);
	cSrch.gridwidth = 1;

	cSrch.gridwidth = 3;
	cSrch.gridx = 0;
	cSrch.gridy = 0;
	pnlGridSearch.add(txtSearchField, cSrch);
	cSrch.gridwidth = 1;
	cSrch.gridx = 0;
	cSrch.gridy = 1;
	pnlGridSearch.add(minSearchField, cSrch);
	cSrch.gridx = 1;
	pnlGridSearch.add(labelRange, cSrch);
	cSrch.gridx = 2;
	pnlGridSearch.add(maxSearchField, cSrch);

	JPanel pnlSearch = new JPanel();
	pnlSearch.setBorder(BorderFactory.createTitledBorder(
		BorderFactory.createEtchedBorder(EtchedBorder.LOWERED),
		"Search by", TitledBorder.LEFT, TitledBorder.TOP));

	pnlSearch.add(comboSearchDataType);
	pnlSearch.add(pnlGridSearch);

	GUIUtils.addButton(pnlSearch, "Search", null, this, "SEARCH");

	// Show items
	labeledItems = new JCheckBox("Show item's labels");
	labeledItems.addActionListener(this);
	labeledItems.setHorizontalAlignment(SwingConstants.CENTER);
	labeledItems.setActionCommand("LABEL_ITEMS");

	// Bottom panel layout
	setLayout(new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints();

	c.fill = GridBagConstraints.HORIZONTAL;
	c.insets = new Insets(5, 5, 5, 5);
	c.ipadx = 50;
	c.gridwidth = 1;

	c.gridx = 0;
	c.gridy = 0;
	add(Box.createRigidArea(new Dimension(0, 10)), c);

	c.ipadx = 10;
	c.gridx = 0;
	c.gridy = 1;
	add(new JLabel("Axis X"), c);

	c.gridx = 1;
	c.gridy = 1;
	add(comboX, c);
	c.gridx = 2;
	c.gridy = 1;
	add(new JLabel("Axis Y"), c);
	c.gridx = 3;
	c.gridy = 1;
	add(comboY, c);
	c.gridx = 4;
	c.gridy = 1;
	add(labeledItems, c);

	c.gridwidth = 4;
	c.gridx = 0;
	c.gridy = 2;
	add(pnlSearch, c);

	c.gridx = 4;
	c.gridy = 2;
	add(pnlFold, c);

	// Activate the second item in the Y axis combo, this will also trigger
	// DATA_CHANGE event
	comboY.setSelectedIndex(1);

    }

    public void actionPerformed(ActionEvent event) {

	String command = event.getActionCommand();

	if (command.equals("DATA_CHANGE")) {

	    ScatterPlotAxisSelection optionX = (ScatterPlotAxisSelection) comboX
		    .getSelectedItem();
	    ScatterPlotAxisSelection optionY = (ScatterPlotAxisSelection) comboY
		    .getSelectedItem();

	    if ((optionX == null) || (optionY == null))
		return;

	    String foldText = foldXvalues[comboFold.getSelectedIndex()];
	    int foldValue = Integer.parseInt(foldText);
	    if (foldValue <= 0)
		foldValue = 2;

	    chart.setDisplayedAxes(optionX, optionY, foldValue);
	    return;
	}

	if (command.equals("LABEL_ITEMS")) {
	    chart.setItemLabels(labeledItems.isSelected());
	}

	if (command.equals("SEARCH")) {

	    SearchDefinitionType searchType = (SearchDefinitionType) comboSearchDataType
		    .getSelectedItem();
	    String searchRegex = txtSearchField.getText();
	    Number minValue = ((Number) minSearchField.getValue());
	    if (minValue == null)
		minValue = 0;
	    Number maxValue = ((Number) maxSearchField.getValue());
	    if (maxValue == null)
		maxValue = 0;
	    Range<Double> searchRange = Range.closed(minValue.doubleValue(),
		    maxValue.doubleValue());
	    try {
		SearchDefinition newSearch = new SearchDefinition(searchType,
			searchRegex, searchRange);
		chart.updateSearchDefinition(newSearch);
	    } catch (PatternSyntaxException pe) {
		MZmineCore.getDesktop().displayErrorMessage(window,
			"The regular expression's syntax is invalid: " + pe);
	    }
	    return;
	}

	if (command.equals("SEARCH_DATA_TYPE")) {

	    SearchDefinitionType searchType = (SearchDefinitionType) comboSearchDataType
		    .getSelectedItem();

	    switch (searchType) {

	    case MASS:
		minSearchField.setVisible(true);
		maxSearchField.setVisible(true);
		labelRange.setVisible(true);
		txtSearchField.setVisible(false);
		NumberFormat mzFormatter = MZmineCore.getConfiguration()
			.getMZFormat();
		Range<Double> mzRange = peakList.getRowsMZRange();
		DefaultFormatterFactory mzFormatFac = new DefaultFormatterFactory(
			new NumberFormatter(mzFormatter));
		minSearchField.setFormatterFactory(mzFormatFac);
		minSearchField.setValue(mzRange.lowerEndpoint());
		maxSearchField.setFormatterFactory(mzFormatFac);
		maxSearchField.setValue(mzRange.upperEndpoint());
		break;

	    case RT:
		minSearchField.setVisible(true);
		maxSearchField.setVisible(true);
		labelRange.setVisible(true);
		txtSearchField.setVisible(false);
		NumberFormat rtFormatter = MZmineCore.getConfiguration()
			.getRTFormat();
		Range<Double> rtRange = peakList.getRowsRTRange();
		DefaultFormatterFactory rtFormatFac = new DefaultFormatterFactory(
			new NumberFormatter(rtFormatter));
		minSearchField.setFormatterFactory(rtFormatFac);
		minSearchField.setValue(rtRange.lowerEndpoint());
		maxSearchField.setFormatterFactory(rtFormatFac);
		maxSearchField.setValue(rtRange.upperEndpoint());
		break;

	    case NAME:
		minSearchField.setVisible(false);
		maxSearchField.setVisible(false);
		labelRange.setVisible(false);
		txtSearchField.setVisible(true);
		break;
	    }

	    return;
	}

    }
}
