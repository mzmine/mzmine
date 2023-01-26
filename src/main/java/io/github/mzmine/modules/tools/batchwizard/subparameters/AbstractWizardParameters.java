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

package io.github.mzmine.modules.tools.batchwizard.subparameters;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.factories.WizardParameterFactory;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.ComposedParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import org.jetbrains.annotations.NotNull;

public abstract sealed class AbstractWizardParameters extends ComposedParameterSet implements
    Comparable<AbstractWizardParameters> permits IonInterfaceWizardParameters,
    AnnotationWizardParameters, DataImportWizardParameters, FilterWizardParameters,
    IonMobilityWizardParameters, MassSpectrometerWizardParameters, WorkflowWizardParameters {

  private ParameterSet parameters;
  private final WizardPart part;
  private WizardParameterFactory preset;

  /**
   * @param part       the part in the workflow
   * @param preset     preset chosen from 1 or more choices
   * @param parameters array of parameters
   */
  public AbstractWizardParameters(WizardPart part, WizardParameterFactory preset,
      Parameter<?>... parameters) {
    this.parameters = new SimpleParameterSet(parameters).cloneParameterSet();
    this.part = part;
    this.preset = preset;
  }


  /**
   * The part describes the part in the workflow, like LC-tims-qTOF-MS
   *
   * @return final part
   */
  public WizardPart getPart() {
    return part;
  }

  public WizardParameterFactory getPreset() {
    return preset;
  }

  /**
   * Set the selected preset
   *
   * @param preset the new preset
   */
  public void setPreset(final WizardParameterFactory preset) {
    this.preset = preset;
  }

  @Override
  protected ParameterSet getParamSet() {
    return parameters;
  }

  @Override
  protected void setParamSet(ParameterSet newParameters) {
    parameters = newParameters;
  }

  // for sorting
  @Override
  public int compareTo(@NotNull final AbstractWizardParameters o) {
    return part.compareTo(o.part);
  }
}
