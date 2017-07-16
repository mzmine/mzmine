package net.sf.mzmine.parameters.dialogs;

import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.util.Hashtable;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.GridBagPanel;

public class FooterSetupDialog extends ParameterSetupDialog {

	protected String footerMessage;
	
	public FooterSetupDialog(Window parent, boolean valueCheckRequired, ParameterSet parameters) {
		super(parent, valueCheckRequired, parameters);
	}
	
	public FooterSetupDialog(Window parent, boolean valueCheckRequired, ParameterSet parameters,
			String message) {
		
		super(parent, valueCheckRequired);
		
		valueCheckRequired = valueCheckRequired;
        parameterSet = parameters;
        helpID = GUIUtils.generateHelpID(parameters);

        parametersAndComponents = new Hashtable<String, JComponent>();

        footerMessage = message;
        addDialogComponents();

        updateMinimumSize();
        pack();

        setLocationRelativeTo(parent);
	}
	
	@Override
	public void addDialogComponents() {
		// Main panel which holds all the components in a grid
        mainPanel = new GridBagPanel();

        int rowCounter = 0;
        int vertWeightSum = 0;

        // Create labels and components for each parameter
        for (Parameter p : parameterSet.getParameters()) {

            if (!(p instanceof UserParameter))
                continue;
            UserParameter up = (UserParameter) p;

            JComponent comp = up.createEditingComponent();
            comp.setToolTipText(up.getDescription());

            // Set the initial value
            Object value = up.getValue();
            if (value != null)
                up.setValueToComponent(comp, value);

            // Add listeners so we are notified about any change in the values
            addListenersToComponent(comp);

            // By calling this we make sure the components will never be resized
            // smaller than their optimal size
            comp.setMinimumSize(comp.getPreferredSize());

            comp.setToolTipText(up.getDescription());

            JLabel label = new JLabel(p.getName());
            mainPanel.add(label, 0, rowCounter);
            label.setLabelFor(comp);

            parametersAndComponents.put(p.getName(), comp);

            JComboBox t = new JComboBox();
            int comboh = t.getPreferredSize().height;
            int comph = comp.getPreferredSize().height;
            

            // Multiple selection will be expandable, other components not
            int verticalWeight = comph > 2 * comboh ? 1 : 0;
            vertWeightSum += verticalWeight;
            
            mainPanel.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight,
                    GridBagConstraints.VERTICAL);

            rowCounter++;

        }
        
        // Footer
        JPanel pnlFoot = new JPanel();        
        GUIUtils.addLabel(pnlFoot, footerMessage);
        mainPanel.add(pnlFoot, 0, 90, 3, 1);

        // Add a single empty cell to the 99th row. This cell is expandable
        // (weightY is 1), therefore the other components will be
        // aligned to the top, which is what we want
        // JComponent emptySpace = (JComponent) Box.createVerticalStrut(1);
        // mainPanel.add(emptySpace, 0, 99, 3, 1, 0, 1);

        // Create a separate panel for the buttons
        JPanel pnlButtons = new JPanel();
        

        btnOK = GUIUtils.addButton(pnlButtons, "OK", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);

        /*
         * Last row in the table will be occupied by the buttons. We set the row
         * number to 100 and width to 3, spanning the 3 component columns
         * defined above.
         */
        if (vertWeightSum == 0) {
            mainPanel.add(Box.createGlue(), 0, 99, 3, 1, 1, 1);
        }
        mainPanel.addCenter(pnlButtons, 0, 150, 3, 1);

        // Add some space around the widgets
        GUIUtils.addMargin(mainPanel, 10);

        // Add the main panel as the only component of this dialog
        add(mainPanel);

        pack();
	}

}
