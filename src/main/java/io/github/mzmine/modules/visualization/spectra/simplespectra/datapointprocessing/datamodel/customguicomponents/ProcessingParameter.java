/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.customguicomponents;

import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.DataPointProcessingManager;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.DPPParameterValueWrapper;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class ProcessingParameter
    implements UserParameter<DPPParameterValueWrapper, ProcessingComponent> {

  private final String name;
  private final String description;
  private DPPParameterValueWrapper value;

  public ProcessingParameter(String name, String description) {
    this.name = name;
    this.description = description;
    this.value = new DPPParameterValueWrapper();
    // if(queue == null)
    // this.value =
    // DataPointProcessingManager.getInst().getProcessingQueue();
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
    return comp;
  }

  @Override
  public void setValueFromComponent(ProcessingComponent component) {
    this.value = component.getValueFromComponent();
    DataPointProcessingManager.getInst().updateParameters();
  }

  @Override
  public void setValueToComponent(ProcessingComponent component,
      @Nullable DPPParameterValueWrapper newValue) {
    if (newValue == null) {
      return;
    }
    component.setValueFromValueWrapper(newValue);
  }

  @Override
  public UserParameter<DPPParameterValueWrapper, ProcessingComponent> cloneParameter() {
    ProcessingParameter copy = new ProcessingParameter(name, description);
    copy.setValue(this.value.clone());
    return copy;
  }

}
