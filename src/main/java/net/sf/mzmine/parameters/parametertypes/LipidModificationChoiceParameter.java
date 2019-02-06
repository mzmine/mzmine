package net.sf.mzmine.parameters.parametertypes;

import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.lipidmodifications.LipidModification;

public class LipidModificationChoiceParameter extends MultiChoiceParameter<LipidModification> {
  /**
   * Create the parameter.
   *
   * @param name name of the parameter.
   * @param description description of the parameter.
   */
  public LipidModificationChoiceParameter(String name, String description,
      LipidModification[] choices, int minNumbers) {
    super(name, description, choices, choices, 0);
  }

  @Override
  public MultiChoiceComponent createEditingComponent() {
    return new LipidModificationChoiceComponent(getChoices());
  }

  @Override
  public void setValueFromComponent(final MultiChoiceComponent component) {
    super.setValueFromComponent(component);
    setChoices((LipidModification[]) component.getChoices());
  }

  @Override
  public void setValueToComponent(MultiChoiceComponent component, LipidModification[] newValue) {
    super.setValueToComponent(component, newValue);
    setChoices((LipidModification[]) component.getChoices());
  }

  @Override
  public LipidModificationChoiceParameter cloneParameter() {

    final LipidModificationChoiceParameter copy = new LipidModificationChoiceParameter(getName(),
        getDescription(), getChoices(), getChoices().length);
    copy.setChoices(getChoices());
    copy.setValue(getValue());
    return copy;
  }
}
