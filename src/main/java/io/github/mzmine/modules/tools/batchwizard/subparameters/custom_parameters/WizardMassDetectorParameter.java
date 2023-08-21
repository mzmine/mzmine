/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters;

import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.parameters.UserParameter;
import java.util.Arrays;
import org.w3c.dom.Element;

public class WizardMassDetectorParameter extends
    CustomComboParameter<MassDetectorWizardOptions, WizardMassDetectorNoiseLevels> {

  protected static final String MS1_LEVEL_ATTRIBUTE = "ms1_level";
  protected static final String MSN_LEVEL_ATTRIBUTE = "msn_level";

  public WizardMassDetectorParameter(final String name, final String description,
      final MassDetectorWizardOptions[] options, final boolean valueRequired,
      final WizardMassDetectorNoiseLevels defaultValue) {
    super(name, description, options, valueRequired, defaultValue);
  }

  @Override
  public void loadValueFromXML(Element xmlElement) {
    options = Arrays.stream(xmlElement.getAttribute(CHOICES_ATTRIBUTE).split(","))
        .map(MassDetectorWizardOptions::valueOf).toArray(MassDetectorWizardOptions[]::new);

    String valueStr = xmlElement.getAttribute(VALUE_ATTRIBUTE);
    MassDetectorWizardOptions selected = null;
    if (valueStr != null && !"null".equals(valueStr) && !valueStr.isBlank()) {
      try {
        selected = MassDetectorWizardOptions.valueOf(valueStr);
      } catch (Exception ex) {
      }
      double ms1 = Double.parseDouble(xmlElement.getAttribute(MS1_LEVEL_ATTRIBUTE));
      double msn = Double.parseDouble(xmlElement.getAttribute(MSN_LEVEL_ATTRIBUTE));

      setValue(new WizardMassDetectorNoiseLevels(selected, ms1, msn));
      return;
    }
    // on error or null value
    setValue(null);
  }

  @Override
  public void saveValueToXML(final Element xmlElement) {
    super.saveValueToXML(xmlElement);
    if (value != null) {
      xmlElement.setAttribute(MS1_LEVEL_ATTRIBUTE, String.valueOf(value.getMs1NoiseLevel()));
      xmlElement.setAttribute(MSN_LEVEL_ATTRIBUTE, String.valueOf(value.getMsnNoiseLevel()));
    }
  }

  @Override
  public CustomComboComponent<MassDetectorWizardOptions, WizardMassDetectorNoiseLevels> createEditingComponent() {
    return new WizardMassDetectorComponent(options, value);
  }

  @Override
  public UserParameter<WizardMassDetectorNoiseLevels, CustomComboComponent<MassDetectorWizardOptions, WizardMassDetectorNoiseLevels>> cloneParameter() {
    return new WizardMassDetectorParameter(getName(), getDescription(), options, isValueRequired(),
        value.copy());
  }
}
