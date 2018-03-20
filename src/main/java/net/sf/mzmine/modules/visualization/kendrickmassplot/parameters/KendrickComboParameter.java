package net.sf.mzmine.modules.visualization.kendrickmassplot.parameters;

import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import org.w3c.dom.Element;
import net.sf.mzmine.parameters.UserParameter;

public class KendrickComboParameter<ValueType>
    implements UserParameter<ValueType, KendrickComboComponent<ValueType>> {

  private String name, description;
  private ValueType choices[], value;
  private ActionListener l;

  public KendrickComboParameter(String name, String description, ValueType choices[],
      ValueType defaultValue, ActionListener l) {
    this.name = name;
    this.description = description;
    this.choices = choices;
    this.value = defaultValue;
    this.l = l;
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public KendrickComboComponent<ValueType> createEditingComponent() {
    KendrickComboComponent<ValueType> kendrickComboComponent =
        new KendrickComboComponent<ValueType>(choices, l);
    return kendrickComboComponent;
  }

  @Override
  public ValueType getValue() {
    return value;
  }

  public ValueType[] getChoices() {
    return choices;
  }

  public void setChoices(ValueType newChoices[]) {
    this.choices = newChoices;
  }

  @Override
  public void setValue(ValueType value) {
    this.value = value;
  }

  @Override
  public KendrickComboParameter<ValueType> cloneParameter() {
    KendrickComboParameter<ValueType> copy =
        new KendrickComboParameter<ValueType>(name, description, choices, value, l);
    copy.value = this.value;
    return copy;
  }

  @Override
  public void setValueFromComponent(KendrickComboComponent<ValueType> component) {
    Object selectedItem = component.getSelectedItem();
    if (selectedItem == null) {
      value = null;
      return;
    }
    if (!Arrays.asList(choices).contains(selectedItem)) {
      throw new IllegalArgumentException(
          "Invalid value for parameter " + name + ": " + selectedItem);
    }
    int index = component.getSelectedIndex();
    if (index < 0)
      return;

    value = choices[index];
  }

  @Override
  public void setValueToComponent(KendrickComboComponent<ValueType> component, ValueType newValue) {
    component.setSelectedItem(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    String elementString = xmlElement.getTextContent();
    if (elementString.length() == 0)
      return;
    for (ValueType option : choices) {
      if (option.toString().equals(elementString)) {
        value = option;
        break;
      }
    }
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value.toString());
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

}
