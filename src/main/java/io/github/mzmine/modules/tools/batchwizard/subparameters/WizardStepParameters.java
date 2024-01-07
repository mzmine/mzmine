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
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.builders.WizardBatchBuilder;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.impl.ComposedParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * This is a preset for a specific {@link WizardPart}. It was created by
 * {@link WizardParameterFactory} {@link #getFactory()}. In a {@link WizardSequence} the presets
 * define the actual processing workflow that is built in {@link WizardBatchBuilder}
 */
public abstract sealed class WizardStepParameters extends ComposedParameterSet implements
    Comparable<WizardStepParameters> permits IonInterfaceWizardParameters,
    AnnotationWizardParameters, DataImportWizardParameters, FilterWizardParameters,
    IonMobilityWizardParameters, MassSpectrometerWizardParameters, WorkflowWizardParameters {

  private final WizardPart part;
  private final WizardParameterFactory factory;
  private ParameterSet parameters;

  /**
   * @param part       the part in the workflow
   * @param factory    preset chosen from 1 or more choices
   * @param parameters array of parameters
   */
  public WizardStepParameters(WizardPart part, WizardParameterFactory factory,
      Parameter<?>... parameters) {
    this.parameters = new SimpleParameterSet(parameters).cloneParameterSet();
    this.part = part;
    this.factory = factory;
  }

  /**
   * Create map of all presets for every {@link WizardPart}
   *
   * @return map of part and list of presets
   */
  public static Map<WizardPart, List<WizardStepParameters>> createAllPresets() {
    return Arrays.stream(WizardPart.values())
        .collect(Collectors.toMap(part -> part, WizardPart::createPresetParameters));
  }

  @Override
  public String toString() {
    return factory.toString();
  }

  /**
   * The part describes the part in the workflow, like LC-tims-qTOF-MS
   *
   * @return final part
   */
  @NotNull
  public WizardPart getPart() {
    return part;
  }

  /**
   * The factory used to create this preset
   *
   * @return factory
   */
  @NotNull
  public WizardParameterFactory getFactory() {
    return factory;
  }

  /**
   * @return true if all parameters are set to the default values
   */
  public boolean hasDefaultParameters() {
    var defaultPreset = createDefaultParameterPreset();
    return defaultPreset != null && ParameterUtils.equalValues(defaultPreset.parameters,
        parameters);
  }

  /**
   * The unique id used for save load
   *
   * @return preset unique ID
   */
  @NotNull
  public String getUniquePresetId() {
    return factory.getUniqueId();
  }

  @Override
  protected ParameterSet getParamSet() {
    return parameters;
  }

  @Override
  protected void setParamSet(ParameterSet newParameters) {
    parameters = newParameters;
  }

  /**
   * @return the default parameters preset
   */
  public WizardStepParameters createDefaultParameterPreset() {
    return factory.create();
  }

  // for sorting
  @Override
  public int compareTo(@NotNull final WizardStepParameters o) {
    return part.compareTo(o.part);
  }

  /**
   * The title shown in the tabs and in the comboboxes
   *
   * @return preset.toString
   */
  @NotNull
  public String getPresetName() {
    return factory.toString();
  }

  /**
   * Reset this preset to the default parameters
   */
  public void resetToDefaults() {
    WizardStepParameters defaultPreset = createDefaultParameterPreset();
    ParameterUtils.copyParameters(defaultPreset, this);
  }
}
