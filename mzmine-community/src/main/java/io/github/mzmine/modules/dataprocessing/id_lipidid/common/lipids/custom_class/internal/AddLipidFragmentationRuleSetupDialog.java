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

package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.internal;

import io.github.mzmine.datamodel.IonizationType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.identification.LipidFragmentationRuleType;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.LipidAnnotationLevel;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;

public class AddLipidFragmentationRuleSetupDialog extends ParameterSetupDialog {

  private ParameterSet parameters;
  private PolarityType polarityType;
  private IonizationType ionizationType;
  private LipidFragmentationRuleType lipidFragmentationRuleType;
  private LipidAnnotationLevel lipidAnnotationLevel;
  private String formula;

  public AddLipidFragmentationRuleSetupDialog(boolean valueCheckRequired, ParameterSet parameters) {
    super(valueCheckRequired, parameters);
    this.parameters = parameters;
    this.polarityType = parameters.getParameter(AddLipidFragmentationRuleParameters.polarity).getValue();
    this.ionizationType = parameters.getParameter(AddLipidFragmentationRuleParameters.ionizationMethod).getValue();
    this.lipidFragmentationRuleType = parameters.getParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleType).getValue();
    this.lipidAnnotationLevel = parameters.getParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel).getValue();
    this.formula = parameters.getParameter(AddLipidFragmentationRuleParameters.formula).getValue();
  }

  @Override
  protected void parametersChanged() {
    super.parametersChanged();
    this.updateParameterSetFromComponents();
    polarityType = parameters.getParameter(AddLipidFragmentationRuleParameters.polarity).getValue();
    ionizationType = parameters.getParameter(AddLipidFragmentationRuleParameters.ionizationMethod).getValue();
    lipidFragmentationRuleType = parameters.getParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleType).getValue();
    lipidAnnotationLevel = parameters.getParameter(AddLipidFragmentationRuleParameters.lipidFragmentationRuleInformationLevel).getValue();
    formula = parameters.getParameter(AddLipidFragmentationRuleParameters.formula).getValue();
  }


}
