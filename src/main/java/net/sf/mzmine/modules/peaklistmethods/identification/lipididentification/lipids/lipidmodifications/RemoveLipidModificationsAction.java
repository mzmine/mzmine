package net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import net.sf.mzmine.parameters.parametertypes.LipidModificationChoiceComponent;

public class RemoveLipidModificationsAction extends AbstractAction {
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  /**
   * Create the action.
   */
  public RemoveLipidModificationsAction() {

    super("Remove");
    putValue(SHORT_DESCRIPTION, "Remove all lipid modification");

  }

  @Override
  public void actionPerformed(final ActionEvent e) {

    // Parent component.
    final LipidModificationChoiceComponent parent =
        (LipidModificationChoiceComponent) SwingUtilities
            .getAncestorOfClass(LipidModificationChoiceComponent.class, (Component) e.getSource());

    if (parent != null) {
      parent.setChoices(new LipidModification[0]);
    }
  }
}
