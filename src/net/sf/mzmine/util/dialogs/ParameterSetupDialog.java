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

package net.sf.mzmine.util.dialogs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.ParameterType;
import net.sf.mzmine.data.impl.SimpleParameter;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.components.ExtendedCheckBox;
import net.sf.mzmine.util.components.HelpButton;

/**
 * This class represents the parameter setup dialog shown to the user before
 * processing
 * 
 * TODO: this class needs to be easily inheritable, modules should be able to
 * add their own components
 * 
 */
public class ParameterSetupDialog extends JDialog implements ActionListener {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	public static final int TEXTFIELD_COLUMNS = 10;

	private ExitCode exitCode = ExitCode.UNKNOWN;

	protected String helpID;

	// Parameters and their representation in the dialog
	public Hashtable<Parameter, JComponent> parametersAndComponents;

	// Buttons
	protected JButton btnOK, btnCancel, btnAuto, btnHelp;

	// Panels
	private JPanel pnlLabels, pnlUnits, pnlButtons;
	public JPanel labelsAndFields, pnlFields;

	// Derived classed may add their components to this panel
	protected JPanel pnlAll;

	private SimpleParameterSet parameters;
	private Hashtable<Parameter, Object> autoValues;
	private Vector<ExtendedCheckBox> customCheckBoxes;
	private JTextField customItemTextField;

	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters) {
		this(title, parameters, null, null);
	}

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters,
			String helpID) {
		this(title, parameters, null, helpID);
	}

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters,
			Hashtable<Parameter, Object> autoValues) {
		this(title, parameters, autoValues, null);
	}

	/**
	 * Constructor
	 */
	public ParameterSetupDialog(String title, SimpleParameterSet parameters,
			Hashtable<Parameter, Object> autoValues, String helpID) {

		// Make dialog modal
		super(MZmineCore.getDesktop().getMainFrame(), true);

		this.parameters = parameters;
		this.autoValues = autoValues;

		// Check if there are any parameters
		Parameter[] allParameters = parameters.getParameters();
		if ((allParameters == null) || (allParameters.length == 0)) {
			dispose();
		}

		parametersAndComponents = new Hashtable<Parameter, JComponent>();

		// panels for labels, text fields and units
		pnlLabels = new JPanel();// new GridLayout(0, 1));
		pnlLabels.setLayout(new BoxLayout(pnlLabels, BoxLayout.PAGE_AXIS));
		pnlFields = new JPanel();// new GridLayout(0, 1));
		pnlFields.setLayout(new BoxLayout(pnlFields, BoxLayout.PAGE_AXIS));
		pnlUnits = new JPanel();// new GridLayout(0, 1));
		pnlUnits.setLayout(new BoxLayout(pnlUnits, BoxLayout.PAGE_AXIS));

		pnlFields.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pnlLabels.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
		pnlUnits.setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));

		// Create labels and components for each parameter

		for (int i = 0; i < allParameters.length; i++) {
			Parameter p = allParameters[i];

			// create labels
			JLabel lblLabel = new JLabel(p.getName());
			pnlLabels.add(lblLabel);

			String unitStr = "";
			if (p.getUnits() != null) {
				unitStr = p.getUnits();
			}
			JLabel unitLabel = new JLabel(unitStr);
			pnlUnits.add(unitLabel);

			Object[] possibleValues = p.getPossibleValues();
			if ((possibleValues != null)
					&& (p.getType() != ParameterType.MULTIPLE_SELECTION)) {
				JComboBox combo = new JComboBox();
				for (Object value : possibleValues) {
					combo.addItem(value);
					if (value == parameters.getParameterValue(p))
						combo.setSelectedItem(value);
				}

				int height = (int) combo.getPreferredSize().getHeight();
				lblLabel.setMaximumSize(new Dimension((int) lblLabel
						.getPreferredSize().getWidth(), height));
				unitLabel.setMaximumSize(new Dimension((int) unitLabel
						.getPreferredSize().getWidth(), height));

				combo.setToolTipText(p.getDescription());
				parametersAndComponents.put(p, combo);
				lblLabel.setLabelFor(combo);
				pnlFields.add(combo);
				continue;
			}

			JComponent comp = null;

			NumberFormat format = p.getNumberFormat();
			if (format == null)
				format = NumberFormat.getNumberInstance();

			switch (p.getType()) {

			case STRING:
				JTextField txtField = new JTextField();
				txtField.setColumns(TEXTFIELD_COLUMNS);
				comp = txtField;
				break;
			case INTEGER:
			case DOUBLE:

				JFormattedTextField fmtField = new JFormattedTextField(format);
				fmtField.setColumns(TEXTFIELD_COLUMNS);
				comp = fmtField;
				break;

			case RANGE:

				JFormattedTextField minTxtField = new JFormattedTextField(
						format);
				JFormattedTextField maxTxtField = new JFormattedTextField(
						format);
				minTxtField.setColumns(TEXTFIELD_COLUMNS);
				maxTxtField.setColumns(TEXTFIELD_COLUMNS);
				JPanel panel = new JPanel(new FlowLayout());
				panel.add(minTxtField);
				GUIUtils.addLabel(panel, " - ");
				panel.add(maxTxtField);
				comp = panel;
				break;

			case BOOLEAN:
				JCheckBox checkBox = new JCheckBox();
				comp = checkBox;
				break;

			case MULTIPLE_SELECTION:

				JPanel peakCheckBoxesPanel = new JPanel();
				peakCheckBoxesPanel.setBackground(Color.white);
				peakCheckBoxesPanel.setLayout(new BoxLayout(
						peakCheckBoxesPanel, BoxLayout.Y_AXIS));
				customCheckBoxes = new Vector<ExtendedCheckBox>();
				customItemTextField = new JTextField(5);

				int vertSize = 0;
				for (Object genericObject : p.getPossibleValues()) {
					ExtendedCheckBox ecb = new ExtendedCheckBox(genericObject,
							false);
					customCheckBoxes.add(ecb);
					ecb.setAlignmentX(Component.LEFT_ALIGNMENT);
					peakCheckBoxesPanel.add(ecb);
					vertSize += ecb.getPreferredSize().getHeight();
				}

				JScrollPane peakPanelScroll = new JScrollPane(
						peakCheckBoxesPanel,
						ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
						ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				peakPanelScroll.setPreferredSize(new Dimension(1, vertSize));
				comp = peakPanelScroll;
				break;

			case CUSTOM:

				JTextField nameTxtField = new JTextField(TEXTFIELD_COLUMNS);
				JTextField valueTxtField = new JTextField(TEXTFIELD_COLUMNS);
				JPanel customPanel = new JPanel(new FlowLayout());
				customPanel.add(nameTxtField);
				GUIUtils.addLabel(customPanel, " @ ");
				customPanel.add(valueTxtField);
				comp = customPanel;
				break;

			}

			int height = (int) comp.getPreferredSize().getHeight();
			lblLabel.setMaximumSize(new Dimension((int) lblLabel
					.getPreferredSize().getWidth(), height));
			unitLabel.setMaximumSize(new Dimension((int) unitLabel
					.getPreferredSize().getWidth(), height));

			comp.setToolTipText(p.getDescription());
			parametersAndComponents.put(p, comp);
			lblLabel.setLabelFor(comp);
			pnlFields.add(comp);

			// set the value of the component
			setValue(p, parameters.getParameterValue(p));

		}

		// Buttons
		pnlButtons = new JPanel();
		btnOK = new JButton("OK");
		btnOK.addActionListener(this);
		btnCancel = new JButton("Cancel");
		btnCancel.addActionListener(this);
		pnlButtons.add(btnOK);
		pnlButtons.add(btnCancel);

		if (autoValues != null) {
			btnAuto = new JButton("Set automatically");
			btnAuto.addActionListener(this);
			pnlButtons.add(btnAuto);
		}

		if (helpID != null) {
			this.helpID = helpID;
			btnHelp = new HelpButton(helpID);
			pnlButtons.add(btnHelp);
		}

		// Panel collecting all labels, fileds and units
		labelsAndFields = new JPanel(new BorderLayout());
		labelsAndFields.add(pnlLabels, BorderLayout.WEST);
		labelsAndFields.add(pnlFields, BorderLayout.CENTER);
		labelsAndFields.add(pnlUnits, BorderLayout.EAST);

		// Panel where everything is collected
		pnlAll = new JPanel(new BorderLayout());
		pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(pnlAll);

		// Leave the BorderLayout.CENTER area empty, so that derived dialogs can
		// put their own controls in there
		pnlAll.add(labelsAndFields, BorderLayout.NORTH);
		pnlAll.add(pnlButtons, BorderLayout.SOUTH);

		pack();
		setTitle(title);
		setResizable(false);
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

	}

	/**
	 * Implementation for ActionListener interface
	 */
	public void actionPerformed(ActionEvent ae) {

		Object src = ae.getSource();

		if (src == btnOK) {
			parameters = buildParameterSet(parameters);
			
			if (parameters == null){
				exitCode = ExitCode.CANCEL;
				return;
			}
			exitCode = ExitCode.OK;
			dispose();
		}

		if (src == btnCancel) {
			exitCode = ExitCode.CANCEL;
			dispose();
		}

		if (src == btnAuto) {

			for (Parameter p : autoValues.keySet()) {
				setValue(p, autoValues.get(p));
			}

		}

	}

	/**
	 * Method for reading exit code
	 * 
	 */
	public ExitCode getExitCode() {
		return exitCode;
	}

	void setValue(Parameter p, Object value) {

		JComponent component = parametersAndComponents.get(p);
		if ((component == null) || (value == null))
			return;

		if (component instanceof JComboBox) {
			JComboBox combo = (JComboBox) component;
			combo.setSelectedItem(value);
			return;
		}

		switch (p.getType()) {
		case STRING:
			JTextField strField = (JTextField) component;
			String strValue = (String) value;
			strField.setText(strValue);
			break;
		case INTEGER:
		case DOUBLE:
			JFormattedTextField txtField = (JFormattedTextField) component;
			txtField.setValue(value);
			break;
		case RANGE:
			Range valueRange = (Range) value;
			JPanel panel = (JPanel) component;
			JFormattedTextField minField = (JFormattedTextField) panel
					.getComponent(0);
			minField.setValue(valueRange.getMin());
			JFormattedTextField maxField = (JFormattedTextField) panel
					.getComponent(2);
			maxField.setValue(valueRange.getMax());
			break;
		case BOOLEAN:
			JCheckBox checkBox = (JCheckBox) component;

			Boolean selected = (Boolean) value;
			checkBox.setSelected(selected);
			break;

		}
	}

	/**
	 * This function collect all the information from the form's filed and build
	 * the ParameterSet.
	 * 
	 */
	public SimpleParameterSet buildParameterSet(
			SimpleParameterSet underConstuctionParameter) {
		Iterator<Parameter> paramIter = parametersAndComponents.keySet()
				.iterator();
		while (paramIter.hasNext()) {
			Parameter p = paramIter.next();

			try {

				Object[] possibleValues = p.getPossibleValues();
				if ((possibleValues != null) 
					&& (p.getType() != ParameterType.MULTIPLE_SELECTION)){
					JComboBox combo = (JComboBox) parametersAndComponents
							.get(p);
					underConstuctionParameter.setParameterValue(p,
							possibleValues[combo.getSelectedIndex()]);
					continue;
				}

				switch (p.getType()) {
				case INTEGER:
					JFormattedTextField intField = (JFormattedTextField) parametersAndComponents
							.get(p);
					Integer newIntValue = ((Number) intField.getValue())
							.intValue();
					underConstuctionParameter.setParameterValue(p, newIntValue);
					break;
				case DOUBLE:
					JFormattedTextField doubleField = (JFormattedTextField) parametersAndComponents
							.get(p);
					Double newDoubleValue = ((Number) doubleField.getValue())
							.doubleValue();
					underConstuctionParameter.setParameterValue(p,
							newDoubleValue);
					break;
				case RANGE:
					JPanel panel = (JPanel) parametersAndComponents.get(p);
					JFormattedTextField minField = (JFormattedTextField) panel
							.getComponent(0);
					JFormattedTextField maxField = (JFormattedTextField) panel
							.getComponent(2);
					double minValue = ((Number) minField.getValue())
							.doubleValue();
					double maxValue = ((Number) maxField.getValue())
							.doubleValue();
					Range rangeValue = new Range(minValue, maxValue);
					underConstuctionParameter.setParameterValue(p, rangeValue);
					break;
				case STRING:
					JTextField stringField = (JTextField) parametersAndComponents
							.get(p);
					underConstuctionParameter.setParameterValue(p, stringField
							.getText());
					break;
				case BOOLEAN:
					JCheckBox checkBox = (JCheckBox) parametersAndComponents
							.get(p);
					Boolean newBoolValue = checkBox.isSelected();
					underConstuctionParameter
							.setParameterValue(p, newBoolValue);
					break;

				case MULTIPLE_SELECTION:
					Vector<Object> selectedGenericObject = new Vector<Object>();

					int numSelections = 0;
					for (ExtendedCheckBox box : customCheckBoxes) {
						if (box.isSelected()) {
							Object genericObject = box.getObject();
							selectedGenericObject.add(genericObject);
							numSelections += 1;
						}
					}

					logger.finest("Multiple seleccion de " + numSelections);
					
					((SimpleParameter) p)
							.setMultipleSelectedValues(selectedGenericObject
									.toArray(new Object[0]));
					underConstuctionParameter.setParameterValue(p, numSelections);
					break;

				case CUSTOM:
					JPanel customPanel = (JPanel) parametersAndComponents
							.get(p);
					JTextField nameField = (JTextField) customPanel
							.getComponent(0);
					JTextField valueField = (JTextField) customPanel
							.getComponent(2);
					
					String nameTextValue = nameField.getText();
					((SimpleParameter) p).setCustomName(nameTextValue);
					
					String valueText = valueField.getText();
					Object value = new Double(0.0);
					if ((valueText != null) && (valueText != "")){
						logger.finest("Valor de custom value " + valueText);
						value = (Number) Double.parseDouble(valueText);
					}
					
					underConstuctionParameter.setParameterValue(p,
							value);
					break;

				}

			} catch (Exception invalidValueException) {
				desktop.displayMessage(invalidValueException.getMessage());
				invalidValueException.printStackTrace();
				return null;
			}

		}
		return underConstuctionParameter;
	}

}
