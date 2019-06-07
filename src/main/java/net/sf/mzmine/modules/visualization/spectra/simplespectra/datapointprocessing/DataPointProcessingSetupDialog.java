package net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.parametertypes.HiddenParameter;

public class DataPointProcessingSetupDialog extends ParameterSetupDialog {

  public DataPointProcessingSetupDialog(Window parent, boolean valueCheckRequired,
      ParameterSet parameters) {
    super(parent, valueCheckRequired, parameters);

  }

  @Override
  protected void addDialogComponents() {
    super.addDialogComponents();

    int rowCounter = 3;
    int vertWeightSum = 0;

    // Create labels and components for each parameter
    for (Parameter p : parameterSet.getParameters()) {

      if (!(p instanceof HiddenParameter))
        continue;

      UserParameter up = (UserParameter) (((HiddenParameter) p).getEmbeddedParameter());

      if ((!parameterSet.getParameter(DataPointProcessingParameters.differentiateMSn).getValue()
          && up == parameterSet.getParameter(DataPointProcessingParameters.processingMSx)
              .getEmbeddedParameter())
          || (parameterSet.getParameter(DataPointProcessingParameters.differentiateMSn).getValue()
              && up == parameterSet.getParameter(DataPointProcessingParameters.processingMSn)
                  .getEmbeddedParameter())
          || (parameterSet.getParameter(DataPointProcessingParameters.differentiateMSn).getValue()
              && up == parameterSet.getParameter(DataPointProcessingParameters.processingMS1)
                  .getEmbeddedParameter())) {

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

        mainPanel.add(comp, 1, rowCounter, 1, 1, 1, verticalWeight, GridBagConstraints.VERTICAL);

        rowCounter++;
      }
    }

    pack();
  }

  @Override
  public void actionPerformed(ActionEvent ae) {
    super.actionPerformed(ae);
    UserParameter up = parameterSet.getParameter(DataPointProcessingParameters.differentiateMSn);
    if (ae.getSource().equals(parametersAndComponents.get(up.getName()))) {
      // since we want to redraw in the update routine, we need to change the value manually here,
      // else it wont be updated
      up.setValue(!(Boolean)up.getValue());
      
      this.updateParameterSetFromComponents();
      this.remove(mainPanel);
      this.addDialogComponents();
      this.updateMinimumSize();
      this.pack();
      this.repaint();
    }
  }

  @Override
  public void parametersChanged() {
    super.parametersChanged();


  }
}
