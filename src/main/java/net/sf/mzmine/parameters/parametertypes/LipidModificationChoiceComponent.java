package net.sf.mzmine.parameters.parametertypes;

import javax.swing.JButton;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.AddLipidModificationAction;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.ExportLipidModificationsAction;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.ImportLipidModificationsAction;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.LipidModification;
import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.RemoveLipidModificationsAction;

public class LipidModificationChoiceComponent extends MultiChoiceComponent {

  public LipidModificationChoiceComponent(LipidModification[] theChoices) {
    super(theChoices);
    addButton(new JButton(new AddLipidModificationAction()));
    addButton(new JButton(new ImportLipidModificationsAction()));
    addButton(new JButton(new ExportLipidModificationsAction()));
    addButton(new JButton(new RemoveLipidModificationsAction()));
  }

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

}
