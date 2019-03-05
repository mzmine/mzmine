package net.sf.mzmine.parameters.parametertypes;

import java.util.Collection;
import org.w3c.dom.Element;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingQueue;
import net.sf.mzmine.parameters.UserParameter;

public class ProcessingParameter implements UserParameter <DataPointProcessingQueue, ProcessingComponent> {

  private String name;
  private String description;
  private DataPointProcessingQueue value;
  
  public ProcessingParameter(String name, String description) {
    this.name = name;
    this.description = description;
//    if(queue == null)
//      this.value = DataPointProcessingManager.getInst().getProcessingQueue();
//    else
//      this.value = queue;
  }
  
  @Override
  public String getName() {
    return name;
  }

  @Override
  public DataPointProcessingQueue getValue() {
    return value;
  }

  @Override
  public void setValue(DataPointProcessingQueue newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if(value == null) {
      errorMessages.add("Queue has not been set up. (null)");
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    this.value = DataPointProcessingQueue.loadfromXML(xmlElement);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    value.saveToXML(xmlElement);
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public ProcessingComponent createEditingComponent() {
    ProcessingComponent comp = new ProcessingComponent();
    if(value != null)
      comp.setTreeViewProcessingItemsFromQueue(value);
    return comp;
  }

  @Override
  public void setValueFromComponent(ProcessingComponent component) {
    this.value = component.getProcessingQueueFromTreeView();
  }

  @Override
  public void setValueToComponent(ProcessingComponent component,
      DataPointProcessingQueue newValue) {
    component.setTreeViewProcessingItemsFromQueue(newValue);
  }

  @Override
  public UserParameter<DataPointProcessingQueue, ProcessingComponent> cloneParameter() {
    ProcessingParameter copy = new ProcessingParameter(name, description);
    copy.setValue(this.value);
    return copy;
  }

}
