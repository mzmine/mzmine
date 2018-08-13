package net.sf.mzmine.parameters.parametertypes;

import net.sf.mzmine.modules.peaklistmethods.identification.lipididentification.lipids.LipidClass;

public class LipidClassParameter extends MultiChoiceParameter<LipidClass> {

  public LipidClassParameter(String name, String description, LipidClass[] choices) {
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
    setChoices((LipidClass[]) component.getChoices());
  }

  @Override
  public void setValueToComponent(MultiChoiceComponent component, LipidClass[] newValue) {
    super.setValueToComponent(component, newValue);
    setChoices((LipidClass[]) component.getChoices());
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
