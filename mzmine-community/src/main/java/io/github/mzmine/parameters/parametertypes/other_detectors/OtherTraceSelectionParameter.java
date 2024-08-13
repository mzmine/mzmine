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

import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class OtherTraceSelectionParameter implements
    UserParameter<OtherTraceSelection, OtherTraceSelectionComponent> {

  private final OtherTraceSelection value;
  private final String description;
  private final String name;

  @Override
  public String getDescription() {
    return description;
  }

  @Override
  public OtherTraceSelectionComponent createEditingComponent() {
    return null;
  }

  @Override
  public void setValueFromComponent(OtherTraceSelectionComponent otherTraceSelectionComponent) {

  }

  @Override
  public void setValueToComponent(OtherTraceSelectionComponent otherTraceSelectionComponent,
      @Nullable OtherTraceSelection newValue) {

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
  public void setValue(OtherTraceSelection newValue) {
    this.value = value;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    return false;
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {

  }

  @Override
  public void saveValueToXML(Element xmlElement) {

  }

  @Override
  public UserParameter<OtherTraceSelection, OtherTraceSelectionComponent> cloneParameter() {
    return null;
  }
}
