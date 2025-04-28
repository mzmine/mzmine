/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.parameters.parametertypes.statistics;


import io.github.mzmine.modules.dataanalysis.significance.ttest.TTestSamplingConfig;
import io.github.mzmine.parameters.PropertyParameter;
import io.github.mzmine.parameters.UserParameter;
import java.util.Collection;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

public class TTestConfigurationParameter implements
    PropertyParameter<StorableTTestConfiguration, TTestConfigurationComponent> {

  private final String name;
  private final String desc;
  private final String XML_COLUMN_ATTR = "column";
  private final String XML_SAMPLING_ATTR = "sampling";
  private final String XML_GRP_A_ATTR = "selected_a";
  private final String XML_GRP_B_ATTR = "selected_b";

  @Nullable
  private StorableTTestConfiguration value;

  public TTestConfigurationParameter(String name, String desc) {
    this(name, desc, null);
  }

  public TTestConfigurationParameter(String name, String desc,
      @Nullable StorableTTestConfiguration value) {
    this.name = name;
    this.desc = desc;
    this.value = value;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public @Nullable StorableTTestConfiguration getValue() {
    return value;
  }

  @Override
  public void setValue(@Nullable StorableTTestConfiguration newValue) {
    this.value = newValue;
  }

  @Override
  public boolean checkValue(Collection<String> errorMessages) {
    if (value == null || value.column() == null || value.groupA() == null
        || value.groupB() == null) {
      errorMessages.add(
          "Invalid t-Test parameter configuration " + (value != null ? value.toString()
              : "configuration is null"));
      return false;
    }
    return true;
  }


  @Override
  public void loadValueFromXML(Element xmlElement) {
    final String colName = xmlElement.getAttribute(XML_COLUMN_ATTR);
    final TTestSamplingConfig sampling = TTestSamplingConfig.parseOrElse(
        xmlElement.getAttribute(XML_SAMPLING_ATTR), TTestSamplingConfig.UNPAIRED);
    final String a = xmlElement.getAttribute(XML_GRP_A_ATTR);
    final String b = xmlElement.getAttribute(XML_GRP_B_ATTR);

    value = new StorableTTestConfiguration(sampling, colName, a, b);
  }

  @Override
  public void saveValueToXML(Element xmlElement) {
    if (value == null) {
      return;
    }

    // save enum name as identifier for valueOf
    xmlElement.setAttribute(XML_SAMPLING_ATTR, value.samplingConfig().name());
    xmlElement.setAttribute(XML_COLUMN_ATTR, value.column());
    xmlElement.setAttribute(XML_GRP_A_ATTR, value.groupA());
    xmlElement.setAttribute(XML_GRP_B_ATTR, value.groupB());
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public TTestConfigurationComponent createEditingComponent() {
    return new TTestConfigurationComponent();
  }

  @Override
  public void setValueFromComponent(TTestConfigurationComponent tTestConfigurationComponent) {
    this.value = tTestConfigurationComponent.getValue();
  }

  @Override
  public void setValueToComponent(TTestConfigurationComponent tTestConfigurationComponent,
      @Nullable StorableTTestConfiguration newValue) {
    tTestConfigurationComponent.setValue(newValue);
  }

  @Override
  public UserParameter<StorableTTestConfiguration, TTestConfigurationComponent> cloneParameter() {
    return new TTestConfigurationParameter(name, desc, value);
  }
}
