package net.sf.mzmine.modules.visualization.kendrickmassplot.parameters;

import java.util.Collection;
import javax.swing.BorderFactory;
import org.w3c.dom.Element;
import net.sf.mzmine.parameters.UserParameter;

public class CustomKendrickMassParameter
    implements UserParameter<String, CustomKendrickMassComponent> {

  private String name, description, value;
  private int inputsize = 20;
  private boolean valueRequired = true;

  public CustomKendrickMassParameter(String name, String description, String defaultValue,
      boolean valueRequired) {
    this.name = name;
    this.description = description;
    this.value = defaultValue;
    this.valueRequired = valueRequired;

  }

  /**
   * @see net.sf.mzmine.data.Parameter#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @see net.sf.mzmine.data.Parameter#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public CustomKendrickMassComponent createEditingComponent() {
    CustomKendrickMassComponent customKendrickMassComponent =
        new CustomKendrickMassComponent(inputsize);
    customKendrickMassComponent.setBorder(BorderFactory.createCompoundBorder(
        customKendrickMassComponent.getBorder(), BorderFactory.createEmptyBorder(0, 4, 0, 0)));
    return customKendrickMassComponent;
  }

  public String getValue() {
    return value;
  }

  @Override
  public void setValue(String value) {
    this.value = value;
  }

  @Override
  public CustomKendrickMassParameter cloneParameter() {
    CustomKendrickMassParameter copy =
        new CustomKendrickMassParameter(name, description, value, valueRequired);
    copy.setValue(this.getValue());
    return copy;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void setValueFromComponent(CustomKendrickMassComponent component) {
    value = component.getText();
  }

  @Override
  public void setValueToComponent(CustomKendrickMassComponent component, String newValue) {
    component.setText(newValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    value = xmlElement.getTextContent();
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null)
      return;
    xmlElement.setTextContent(value);
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (!valueRequired)
      return true;
    if ((value == null) || (value.trim().length() == 0)) {
      errorMessages.add(name + " is not set properly");
      return false;
    }
    return true;
  }

  public void enabledComponent(CustomKendrickMassComponent component) {
    component.setEnabled();
  }

  public void disableComponent(CustomKendrickMassComponent component) {
    component.setDisabled();
  }

}
