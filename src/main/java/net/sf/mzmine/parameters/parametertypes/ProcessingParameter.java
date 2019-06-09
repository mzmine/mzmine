package net.sf.mzmine.parameters.parametertypes;

import java.util.Collection;
import org.w3c.dom.Element;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPParameterValueWrapper;
import net.sf.mzmine.parameters.UserParameter;

public class ProcessingParameter
    implements UserParameter<DPPParameterValueWrapper, ProcessingComponent> {

  private String name;
  private String description;
  private DPPParameterValueWrapper value;

  public ProcessingParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = new DPPParameterValueWrapper();
    // if(queue == null)
    // this.value = DataPointProcessingManager.getInst().getProcessingQueue();
    // else
    // this.value = queue;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public DPPParameterValueWrapper getValue() {
    return value;
  }

  @Override
  public void setValue(DPPParameterValueWrapper newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      errorMessages.add("Queue has not been set up. (null)");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    this.value.loadfromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value != null) 
      value.saveValueToXML(xmlElement);
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ProcessingComponent createEditingComponent() {
    ProcessingComponent comp = new ProcessingComponent();
    // if(value != null)
    // comp.setTreeViewProcessingItemsFromQueue(value);
    return comp;
  }

  @Override
  public void setValueFromComponent(ProcessingComponent component) {
    this.value = component.getValueFromComponent();
  }

  @Override
  public void setValueToComponent(ProcessingComponent component,
      DPPParameterValueWrapper newValue) {
    component.setValueFromValueWrapper(newValue);
  }

  @Override
  public UserParameter<DPPParameterValueWrapper, ProcessingComponent> cloneParameter() {
    ProcessingParameter copy = new ProcessingParameter(name, description);
    copy.setValue(this.value.clone());
    return copy;
  }

}
