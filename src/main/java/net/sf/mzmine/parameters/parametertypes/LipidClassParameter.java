package net.sf.mzmine.parameters.parametertypes;

import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClasses;

public class LipidClassParameter extends MultiChoiceParameter<LipidClasses> {

  public LipidClassParameter(String name, String description, LipidClasses[] choices) {
    super(name, description, choices);
    // TODO Auto-generated constructor stub
  }


  @Override
  public MultiChoiceComponent createEditingComponent() {
    return new LipidClassComponent(getChoices());
  }

  @Override
  public void setValueFromComponent(final MultiChoiceComponent component) {
    super.setValueFromComponent(component);
    setChoices((LipidClasses[]) component.getChoices());
  }

  @Override
  public void setValueToComponent(MultiChoiceComponent component, LipidClasses[] newValue) {
    super.setValueToComponent(component, newValue);
    setChoices((LipidClasses[]) component.getChoices());
  }

  @Override
  public LipidClassParameter cloneParameter() {

    final LipidClassParameter copy =
        new LipidClassParameter(getName(), getDescription(), getChoices());
    copy.setChoices(getChoices());
    copy.setValue(getValue());
    return copy;
  }

}
