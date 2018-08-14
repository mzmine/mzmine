package net.sf.mzmine.parameters.parametertypes;

public class LipidClassParameter extends MultiChoiceParameter<Object> {

  public LipidClassParameter(String name, String description, Object[] choices) {
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
    setChoices((Object[]) component.getChoices());
  }

  @Override
  public void setValueToComponent(MultiChoiceComponent component, Object[] newValue) {
    super.setValueToComponent(component, newValue);

    setChoices((Object[]) component.getChoices());
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
