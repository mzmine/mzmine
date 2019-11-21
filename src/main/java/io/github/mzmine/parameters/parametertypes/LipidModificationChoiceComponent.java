package io.github.mzmine.parameters.parametertypes;

import javax.swing.JButton;

import io.github.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.AddLipidModificationAction;
import io.github.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.ExportLipidModificationsAction;
import io.github.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.ImportLipidModificationsAction;
import io.github.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.LipidModification;
import io.github.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.RemoveLipidModificationsAction;

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
