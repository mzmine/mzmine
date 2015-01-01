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

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sf.mzmine.util.ExitCode;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;

public class AxesSetupDialog extends JDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private ValueAxis xAxis;
    private ValueAxis yAxis;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    // Buttons
    private JButton btnOK, btnApply, btnCancel;

    private JFormattedTextField fieldXMin;
    private JFormattedTextField fieldXMax;
    private JFormattedTextField fieldXTick;

    private JFormattedTextField fieldYMin;
    private JFormattedTextField fieldYMax;
    private JFormattedTextField fieldYTick;

    private JCheckBox checkXAutoRange, checkXAutoTick;
    private JCheckBox checkYAutoRange, checkYAutoTick;

    /**
     * Constructor
     */
    public AxesSetupDialog(JFrame parent, XYPlot plot) {

	// Make dialog modal
	super(parent, true);

	xAxis = plot.getDomainAxis();
	yAxis = plot.getRangeAxis();

	NumberFormat defaultFormatter = NumberFormat.getNumberInstance();
	NumberFormat xAxisFormatter = defaultFormatter;
	if (xAxis instanceof NumberAxis)
	    xAxisFormatter = ((NumberAxis) xAxis).getNumberFormatOverride();
	NumberFormat yAxisFormatter = defaultFormatter;
	if (yAxis instanceof NumberAxis)
	    yAxisFormatter = ((NumberAxis) yAxis).getNumberFormatOverride();

	// Create labels and fields
	JLabel lblXTitle = new JLabel(xAxis.getLabel());
	JLabel lblXAutoRange = new JLabel("Auto range");
	JLabel lblXMin = new JLabel("Minimum");
	JLabel lblXMax = new JLabel("Maximum");
	JLabel lblXAutoTick = new JLabel("Auto tick size");
	JLabel lblXTick = new JLabel("Tick size");

	JLabel lblYTitle = new JLabel(yAxis.getLabel());
	JLabel lblYAutoRange = new JLabel("Auto range");
	JLabel lblYMin = new JLabel("Minimum");
	JLabel lblYMax = new JLabel("Maximum");
	JLabel lblYAutoTick = new JLabel("Auto tick size");
	JLabel lblYTick = new JLabel("Tick size");

	checkXAutoRange = new JCheckBox();
	checkXAutoRange.addActionListener(this);
	checkXAutoTick = new JCheckBox();
	checkXAutoTick.addActionListener(this);
	fieldXMin = new JFormattedTextField(xAxisFormatter);
	fieldXMax = new JFormattedTextField(xAxisFormatter);
	fieldXTick = new JFormattedTextField(xAxisFormatter);

	checkYAutoRange = new JCheckBox();
	checkYAutoRange.addActionListener(this);
	checkYAutoTick = new JCheckBox();
	checkYAutoTick.addActionListener(this);
	fieldYMin = new JFormattedTextField(yAxisFormatter);
	fieldYMax = new JFormattedTextField(yAxisFormatter);
	fieldYTick = new JFormattedTextField(yAxisFormatter);

	// Create a panel for labels and fields
	JPanel pnlLabelsAndFields = new JPanel(new GridLayout(0, 2));

	pnlLabelsAndFields.add(lblXTitle);
	pnlLabelsAndFields.add(new JPanel());
	pnlLabelsAndFields.add(lblXAutoRange);
	pnlLabelsAndFields.add(checkXAutoRange);
	pnlLabelsAndFields.add(lblXMin);
	pnlLabelsAndFields.add(fieldXMin);
	pnlLabelsAndFields.add(lblXMax);
	pnlLabelsAndFields.add(fieldXMax);
	if (xAxis instanceof NumberAxis) {
	    pnlLabelsAndFields.add(lblXAutoTick);
	    pnlLabelsAndFields.add(checkXAutoTick);
	    pnlLabelsAndFields.add(lblXTick);
	    pnlLabelsAndFields.add(fieldXTick);
	}

	// Empty row
	pnlLabelsAndFields.add(new JPanel());
	pnlLabelsAndFields.add(new JPanel());

	pnlLabelsAndFields.add(lblYTitle);
	pnlLabelsAndFields.add(new JPanel());
	pnlLabelsAndFields.add(lblYAutoRange);
	pnlLabelsAndFields.add(checkYAutoRange);
	pnlLabelsAndFields.add(lblYMin);
	pnlLabelsAndFields.add(fieldYMin);
	pnlLabelsAndFields.add(lblYMax);
	pnlLabelsAndFields.add(fieldYMax);
	if (yAxis instanceof NumberAxis) {
	    pnlLabelsAndFields.add(lblYAutoTick);
	    pnlLabelsAndFields.add(checkYAutoTick);
	    pnlLabelsAndFields.add(lblYTick);
	    pnlLabelsAndFields.add(fieldYTick);
	}

	// Create buttons
	JPanel pnlButtons = new JPanel();
	btnOK = new JButton("OK");
	btnOK.addActionListener(this);
	btnApply = new JButton("Apply");
	btnApply.addActionListener(this);
	btnCancel = new JButton("Cancel");
	btnCancel.addActionListener(this);

	pnlButtons.add(btnOK);
	pnlButtons.add(btnApply);
	pnlButtons.add(btnCancel);

	// Put everything into a main panel
	JPanel pnlAll = new JPanel(new BorderLayout());
	pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	add(pnlAll);

	pnlAll.add(pnlLabelsAndFields, BorderLayout.CENTER);
	pnlAll.add(pnlButtons, BorderLayout.SOUTH);

	pack();

	setTitle("Please set ranges for axes");
	setResizable(false);
	setLocationRelativeTo(parent);

	getValuesToControls();

    }

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

	Object src = ae.getSource();

	if (src == btnOK) {
	    if (setValuesToPlot()) {
		exitCode = ExitCode.OK;
		dispose();
	    }
	}

	if (src == btnApply) {
	    if (setValuesToPlot())
		getValuesToControls();
	}

	if (src == btnCancel) {
	    exitCode = ExitCode.CANCEL;
	    dispose();
	}

	if ((src == checkXAutoRange) || (src == checkYAutoRange)
		| (src == checkXAutoTick) || (src == checkYAutoTick))
	    updateAutoRangeAvailability();

    }

    private void getValuesToControls() {

	checkXAutoRange.setSelected(xAxis.isAutoRange());
	fieldXMin.setValue(xAxis.getRange().getLowerBound());
	fieldXMax.setValue(xAxis.getRange().getUpperBound());
	if (xAxis instanceof NumberAxis) {
	    checkXAutoTick.setSelected(xAxis.isAutoTickUnitSelection());
	    fieldXTick.setValue(((NumberAxis) xAxis).getTickUnit().getSize());
	}

	checkYAutoRange.setSelected(yAxis.isAutoRange());
	fieldYMin.setValue(yAxis.getRange().getLowerBound());
	fieldYMax.setValue(yAxis.getRange().getUpperBound());
	if (yAxis instanceof NumberAxis) {
	    checkYAutoTick.setSelected(yAxis.isAutoTickUnitSelection());
	    fieldYTick.setValue(((NumberAxis) yAxis).getTickUnit().getSize());
	}

	updateAutoRangeAvailability();
    }

    private void updateAutoRangeAvailability() {
	if (checkXAutoRange.isSelected()) {
	    fieldXMax.setEnabled(false);
	    fieldXMin.setEnabled(false);
	} else {
	    fieldXMax.setEnabled(true);
	    fieldXMin.setEnabled(true);
	}

	if (checkXAutoTick.isSelected()) {
	    fieldXTick.setEnabled(false);
	} else {
	    fieldXTick.setEnabled(true);
	}

	if (checkYAutoRange.isSelected()) {
	    fieldYMax.setEnabled(false);
	    fieldYMin.setEnabled(false);
	} else {
	    fieldYMax.setEnabled(true);
	    fieldYMin.setEnabled(true);
	}

	if (checkYAutoTick.isSelected()) {
	    fieldYTick.setEnabled(false);
	} else {
	    fieldYTick.setEnabled(true);
	}

    }

    private boolean setValuesToPlot() {
	if (checkXAutoRange.isSelected()) {

	    xAxis.setAutoRange(true);

	} else {

	    double lower = ((Number) fieldXMin.getValue()).doubleValue();
	    double upper = ((Number) fieldXMax.getValue()).doubleValue();
	    if (lower > upper) {
		displayMessage("Invalid " + xAxis.getLabel() + " range.");
		return false;
	    }
	    xAxis.setRange(lower, upper);

	}

	if (xAxis instanceof NumberAxis) {

	    if (checkXAutoTick.isSelected()) {
		xAxis.setAutoTickUnitSelection(true);

	    } else {
		double tickSize = ((Number) fieldXTick.getValue())
			.doubleValue();
		((NumberAxis) xAxis).setTickUnit(new NumberTickUnit(tickSize));
	    }

	}

	if (checkYAutoRange.isSelected()) {

	    yAxis.setAutoRange(true);

	} else {

	    double lower = ((Number) fieldYMin.getValue()).doubleValue();
	    double upper = ((Number) fieldYMax.getValue()).doubleValue();
	    if (lower > upper) {
		displayMessage("Invalid " + yAxis.getLabel() + " range.");
		return false;
	    }
	    yAxis.setRange(lower, upper);
	}

	if (yAxis instanceof NumberAxis) {

	    if (checkYAutoTick.isSelected()) {
		yAxis.setAutoTickUnitSelection(true);

	    } else {
		double tickSize = ((Number) fieldYTick.getValue())
			.doubleValue();
		((NumberAxis) yAxis).setTickUnit(new NumberTickUnit(tickSize));
	    }

	}

	return true;
    }

    private void displayMessage(String msg) {
	try {
	    logger.info(msg);
	    JOptionPane.showMessageDialog(this, msg, "Error",
		    JOptionPane.ERROR_MESSAGE);
	} catch (Exception exce) {
	}
    }

    /**
     * Method for reading exit code
     * 
     */
    public ExitCode getExitCode() {
	return exitCode;
    }

}
