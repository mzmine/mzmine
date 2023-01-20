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

import io.github.mzmine.modules.tools.batchwizard.subparameters.AbstractIonInterfaceWizardParameters.IonInterfaceDefaults;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AnnotationWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.FilterWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonMobilityWizardParameters.ImsDefaults;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters.MsInstrumentDefaults;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowWizardParameters.WorkflowDefaults;
import java.util.Arrays;
import java.util.List;

/**
 * Describes the sequence of steps in the wizard. Elements should stay in correct order.
 */
public enum WizardPart {
  DATA_IMPORT, ION_INTERFACE, IMS, MS, FILTER, ANNOTATION, WORKFLOW;

  /**
   * 1 String for parts with only one preset. Parts with more presets are managed by an
   * {@link Enum}
   *
   * @return array of one string or the values of an enum - which implements
   */
  public Object[] getDefaultPresets() {
    return switch (this) {
      // only one option
      case DATA_IMPORT -> new String[]{DataImportWizardParameters.ONLY_PRESET};
      case FILTER -> new String[]{FilterWizardParameters.ONLY_PRESET};
      case ANNOTATION -> new String[]{AnnotationWizardParameters.ONLY_PRESET};
      // multiple options
      case ION_INTERFACE -> IonInterfaceDefaults.values();
      case IMS -> ImsDefaults.values();
      case MS -> MsInstrumentDefaults.values();
      case WORKFLOW -> WorkflowDefaults.values();
    };
  }

  /**
   * Create all presets of this part
   *
   * @return list of presets
   */
  public List<WizardPreset> createPresetParameters() {
    return switch (this) {
      // single values
      case DATA_IMPORT -> List.of(DataImportWizardParameters.createPreset());
      case FILTER -> List.of(FilterWizardParameters.createPreset());
      case ANNOTATION -> List.of(AnnotationWizardParameters.createPreset());
      // enums
      case ION_INTERFACE, IMS, MS, WORKFLOW ->
          Arrays.stream(getDefaultPresets()).map(p -> ((WizardParameterFactory) p).create())
              .toList();
    };
  }

}
