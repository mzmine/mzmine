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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.AnnotationWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.FilterWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonMobilityWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.WorkflowWizardParameterFactory;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Describes the sequence of steps in the wizard. Elements should stay in correct order.
 */
public enum WizardPart {
  DATA_IMPORT, ION_INTERFACE, IMS, MS, FILTER, ANNOTATION, WORKFLOW;

  @Override
  public String toString() {
    return switch (this) {
      case IMS, MS -> super.toString();
      case DATA_IMPORT -> "Data";
      case FILTER -> "Filter";
      case ANNOTATION -> "Annotation";
      case WORKFLOW -> "Workflow";
      case ION_INTERFACE -> "Sample introduction/ionization";
    };
  }

  /**
   * 1 String for parts with only one preset. Parts with more presets are managed by an
   * {@link Enum}
   *
   * @return array of one string or the values of an enum - which implements
   */
  public WizardParameterFactory[] getDefaultPresets() {
    return switch (this) {
      // only one option
      case DATA_IMPORT -> DataImportWizardParameterFactory.values();
      case FILTER -> FilterWizardParameterFactory.values();
      case ANNOTATION -> AnnotationWizardParameterFactory.values();
      // multiple options
      case ION_INTERFACE -> IonInterfaceWizardParameterFactory.values();
      case IMS -> IonMobilityWizardParameterFactory.values();
      case MS -> MassSpectrometerWizardParameterFactory.values();
      case WORKFLOW -> WorkflowWizardParameterFactory.values();
    };
  }

  /**
   * Create all presets of this part
   *
   * @return list of presets
   */
  public List<WizardStepParameters> createPresetParameters() {
    return Arrays.stream(getDefaultPresets()).map(WizardParameterFactory::create).toList();
  }

  /**
   * @param uniqeId unique id as saved to files by WizardParameterFactory
   * @return the WizardParameterFactory that matches the uniqueID
   */
  public Optional<WizardParameterFactory> getParameterFactory(final String uniqeId) {
    return Arrays.stream(getDefaultPresets()).filter(p -> p.getUniqueId().equals(uniqeId))
        .findFirst();
  }
}
