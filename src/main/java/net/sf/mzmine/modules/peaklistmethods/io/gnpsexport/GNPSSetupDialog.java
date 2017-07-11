package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.util.ExitCode;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.GridBagPanel;

public class GNPSSetupDialog extends JDialog
implements ActionListener, DocumentListener {
	
	 private static final long serialVersionUID = 1L;

    private ExitCode exitCode = ExitCode.UNKNOWN;

    private String helpID;

    // Parameters and their representation in the dialog
    protected ParameterSet paramSet;
    private final Map<String, JComponent> parametersAndComponents;

    // If true, the dialog won't allow the OK button to proceed, unless all
    // parameters pass the value check. This is undesirable in the BatchMode
    // setup dialog, where some parameters need to be set in advance according
    // to values that are not yet imported etc.
    private final boolean valueCheckRequired;

    // Buttons
    private JButton btnOK, btnCancel;

	
	
	protected GridBagPanel mainPanel;

    /**
     * Constructor
     */
    public GNPSSetupDialog(Window parent, boolean valueCheckRequired, ParameterSet parameters) {
//		super(parent, valueCheckRequired, parameters);
		
        this.valueCheckRequired = valueCheckRequired;
        this.paramSet = parameters;        
        this.helpID = GUIUtils.generateHelpID(parameters);

        parametersAndComponents = new Hashtable<String, JComponent>();

        addDialogComponents();

        updateMinimumSize();
        pack();

        setLocationRelativeTo(parent);
	}

    /**
     * This method must be called each time when a component is added to
     * mainPanel. It will ensure the minimal size of the dialog is set to the
     * minimum size of the mainPanel plus a little extra, so user cannot resize
     * the dialog window smaller.
     */
    protected void updateMinimumSize() {
        Dimension panelSize = mainPanel.getMinimumSize();
        Dimension minimumSize = new Dimension(panelSize.width + 50,
                panelSize.height + 50);
        setMinimumSize(minimumSize);
    }

    /**
     * Constructs all components of the dialog
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void addDialogComponents() {

        // Main panel which holds all the components in a grid
        mainPanel = new GridBagPanel();

        int rowCounter = 0;
        int vertWeightSum = 0;

        // Create labels and components for each parameter
        for (Parameter p : this.paramSet.getParameters()) {

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

            System.out.println(verticalWeight);
            
            mainPanel.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight,
                    GridBagConstraints.VERTICAL);

            rowCounter++;

        }
        
        // Footer
        JPanel pnlFoot = new JPanel();        
        GUIUtils.addLabel(pnlFoot, 
        		"<html>GNPS Module Disclaimer:" + 
        		"<br>    - If you use the GNPS export module for GNPS web-platform, cite MZmine2 paper and the following article:"+
        		"<br>     Wang et al., Nature Biotechnology 34.8 (2016): 828-837." +
        		"<br>    - See the documentation about MZmine2 data pre-processing for GNPS (<a href=\"\">http://gnps.ucsd.edu/</a>) molecular " +
        		"<br>     networking and MS/MS spectral library search. <a href=\"\">https://bix-lab.ucsd.edu/display/Public/GNPS+data+analysis+workflow+2.0</a></html>");
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

    /**
     * Implementation for ActionListener interface
     */
    public void actionPerformed(ActionEvent ae) {

        Object src = ae.getSource();

        if (src == btnOK) {
            closeDialog(ExitCode.OK);
        }

        if (src == btnCancel) {
            closeDialog(ExitCode.CANCEL);
        }

        if ((src instanceof JCheckBox) || (src instanceof JComboBox)) {
            parametersChanged();
        }

    }

    /**
     * Method for reading exit code
     */
    public ExitCode getExitCode() {
        return exitCode;
    }

    public JComponent getComponentForParameter(Parameter<?> p) {
        return parametersAndComponents.get(p.getName());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void updateParameterSetFromComponents() {
        for (Parameter<?> p : paramSet.getParameters()) {
            if (!(p instanceof UserParameter))
                continue;
            UserParameter up = (UserParameter) p;
            JComponent component = parametersAndComponents.get(p.getName());
            up.setValueFromComponent(component);
        }
    }

    protected int getNumberOfParameters() {
        return paramSet.getParameters().length;
    }

    /**
     * This method may be called by some of the dialog components, for example
     * as a result of double-click by user
     */
    public void closeDialog(ExitCode exitCode) {
        if (exitCode == ExitCode.OK) {
            // commit the changes to the parameter set
            updateParameterSetFromComponents();

            if (valueCheckRequired) {
                ArrayList<String> messages = new ArrayList<String>();
                boolean allParametersOK = paramSet
                        .checkParameterValues(messages);

                if (!allParametersOK) {
                    StringBuilder message = new StringBuilder(
                            "Please check the parameter settings:\n\n");
                    for (String m : messages) {
                        message.append(m);
                        message.append("\n");
                    }
                    MZmineCore.getDesktop().displayMessage(this,
                            message.toString());
                    return;
                }
            }
        }
        this.exitCode = exitCode;
        dispose();

    }

    /**
     * This method does nothing, but it is called whenever user changes the
     * parameters. It can be overridden in extending classes to update the
     * preview components, for example.
     */
    protected void parametersChanged() {

    }

    private void addListenersToComponent(JComponent comp) {
        if (comp instanceof JTextComponent) {
            JTextComponent textComp = (JTextComponent) comp;
            textComp.getDocument().addDocumentListener(this);
        }
        if (comp instanceof JComboBox) {
            JComboBox<?> comboComp = (JComboBox<?>) comp;
            comboComp.addActionListener(this);
        }
        if (comp instanceof JCheckBox) {
            JCheckBox checkComp = (JCheckBox) comp;
            checkComp.addActionListener(this);
        }
        if (comp instanceof JPanel) {
            JPanel panelComp = (JPanel) comp;
            for (int i = 0; i < panelComp.getComponentCount(); i++) {
                Component child = panelComp.getComponent(i);
                if (!(child instanceof JComponent))
                    continue;
                addListenersToComponent((JComponent) child);
            }
        }
    }

    @Override
    public void changedUpdate(DocumentEvent event) {
        parametersChanged();
    }

    @Override
    public void insertUpdate(DocumentEvent event) {
        parametersChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent event) {
        parametersChanged();
    }

    public boolean isValueCheckRequired() {
        return valueCheckRequired;
    }

}
