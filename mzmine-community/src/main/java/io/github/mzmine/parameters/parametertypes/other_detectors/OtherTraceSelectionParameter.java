/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.parameters.parametertypes.other_detectors;

import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.PropertyParameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class OtherTraceSelectionParameter implements
    UserParameter<OtherTraceSelection, OtherTraceSelectionComponent>,
    PropertyParameter<OtherTraceSelection, OtherTraceSelectionComponent> {

  private final String name;
  private final String description;
  @NotNull
  private OtherTraceSelection value;
  private final Collection<OtherRawOrProcessed> otherRawOrProcessedChoices;

  public OtherTraceSelectionParameter(String name, String description) {
    this(name, description, OtherTraceSelection.rawUv());
  }

  public OtherTraceSelectionParameter(String name, String description,
      @NotNull OtherTraceSelection value) {
    this(name, description, value, List.of(OtherRawOrProcessed.values()));
  }

  public OtherTraceSelectionParameter(@NotNull OtherTraceSelection value) {
    this(value, List.of(OtherRawOrProcessed.values()));
  }

  public OtherTraceSelectionParameter(@NotNull OtherTraceSelection value, Collection<OtherRawOrProcessed> otherRawOrProcessedChoices) {
    this("Trace selection", """
        Select the traces you want to process.
        raw = unprocessed, raw detector traces. always remain unaltered and true to the raw data.
        preprocessed = preprocessed detector traces, e.g. after baseline correction or RT shifting/trimming.
         If no preprocessing has been applied, but 'preprocessed' is selected, mzmine will default back to 
         the raw traces.
        features = raw traces split into individual features (= chromatographic peaks). 
        """, value, otherRawOrProcessedChoices);
  }

  public OtherTraceSelectionParameter(String name, String description,
      @NotNull OtherTraceSelection value,
      Collection<OtherRawOrProcessed> otherRawOrProcessedChoices) {
    this.description = description;
    this.name = name;
    this.value = value;
    this.otherRawOrProcessedChoices = otherRawOrProcessedChoices;
  }

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public OtherTraceSelectionComponent createEditingComponent() {
    return new OtherTraceSelectionComponent(otherRawOrProcessedChoices);
  }

  @Override
  public void setValueFromComponent(OtherTraceSelectionComponent otherTraceSelectionComponent) {
    value = otherTraceSelectionComponent.getValue();
  }

  @Override
  public void setValueToComponent(OtherTraceSelectionComponent otherTraceSelectionComponent,
      @Nullable OtherTraceSelection newValue) {
    if (newValue == null) { // should not be the case, but have a default.
      newValue = OtherTraceSelection.rawUv();
    }
    otherTraceSelectionComponent.setValue(newValue);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public OtherTraceSelection getValue() {
    return value;
  }

  @Override
  public void setValue(@NotNull OtherTraceSelection newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null) {
      return false;
    }
    return true;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    final NodeList childNodes = xmlElement.getElementsByTagName("selection");
    final Node item = childNodes.item(0);
    this.value = OtherTraceSelection.loadFromXml((Element) item);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    final Element valueElement = xmlElement.getOwnerDocument().createElement("selection");
    xmlElement.appendChild(valueElement);
    value.saveToXml(valueElement);
  }

  @Override
  public UserParameter<OtherTraceSelection, OtherTraceSelectionComponent> cloneParameter() {
    return new OtherTraceSelectionParameter(name, description, value.copy());
  }
}
