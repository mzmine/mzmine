package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.LipidModificationChoiceComponent;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.util.ExitCode;

public class AddLipidModificationAction extends AbstractAction {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  private LipidModification lipidModification = null;

  /**
   * Create the action.
   */
  public AddLipidModificationAction() {
    super("Add...");
  }

  public void actionPerformed(final ActionEvent e) {

    // Parent component.
    final LipidModificationChoiceComponent parent =
        (LipidModificationChoiceComponent) SwingUtilities
            .getAncestorOfClass(LipidModificationChoiceComponent.class, (Component) e.getSource());
    if (parent != null) {
      // Show dialog.
      final ParameterSet parameters = new AddLipidModificationParameters();
      if (parameters.showSetupDialog(MZmineCore.getDesktop().getMainWindow(),
          true) == ExitCode.OK) {
        // Create new lipid modification
        lipidModification = new LipidModification(
            parameters.getParameter(AddLipidModificationParameters.lipidModification).getValue());

        // Add to list of choices (if not already present).
        final Collection<LipidModification> choices = new ArrayList<LipidModification>(
            Arrays.asList((LipidModification[]) parent.getChoices()));
        if (!choices.contains(lipidModification)) {
          choices.add(lipidModification);
          parent.setChoices(choices.toArray(new LipidModification[choices.size()]));
        }
      }
    }
  }

  /**
   * Represents a lipid modification.
   */
  private static class AddLipidModificationParameters extends SimpleParameterSet {

    // lipid modification
    private static final StringParameter lipidModification =
        new StringParameter("Lipid modification", "Lipid modification");

    private AddLipidModificationParameters() {
      super(new Parameter[] {lipidModification});
    }
  }
}
