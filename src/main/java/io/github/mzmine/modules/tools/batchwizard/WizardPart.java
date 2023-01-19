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

import io.github.mzmine.modules.tools.batchwizard.subparameters.AbstractWizardIonInterfaceParameters.IonInterfaceDefaults;
import io.github.mzmine.modules.tools.batchwizard.subparameters.AbstractWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardAnnotationParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardDataImportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardExportParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardFilterParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardIonMobilityParameters.ImsDefaults;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardMassSpectrometerParameters.MsInstrumentDefaults;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardWorkflowParameters.WorkflowDefaults;
import java.util.Arrays;
import java.util.List;

/**
 * Describes the sequence of steps in the wizard. Elements should stay in correct order.
 */
public enum WizardPart {
  DATA_IMPORT, CHROMATOGRAPHY, IMS, MS, FILTER, ANNOTATION, EXPORT, WORKFLOW;

  /**
   * 1 String for parts with only one preset. Parts with more presets are managed by an
   * {@link Enum}
   *
   * @return array of one string or the values of an enum - which implements
   */
  public Object[] getDefaultPresets() {
    return switch (this) {
      // only one option
      case DATA_IMPORT -> new String[]{WizardDataImportParameters.ONLY_PRESET};
      case FILTER -> new String[]{WizardFilterParameters.ONLY_PRESET};
      case EXPORT -> new String[]{WizardExportParameters.ONLY_PRESET};
      case ANNOTATION -> new String[]{WizardAnnotationParameters.ONLY_PRESET};
      // multiple options
      case CHROMATOGRAPHY -> IonInterfaceDefaults.values();
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
  public List<AbstractWizardParameters<?>> createPresetParameters() {
    return switch (this) {
      // single values
      case DATA_IMPORT -> List.of(new WizardDataImportParameters());
      case FILTER -> List.of(new WizardFilterParameters());
      case ANNOTATION -> List.of(new WizardAnnotationParameters());
      case EXPORT -> List.of(new WizardExportParameters());
      // enums
      case CHROMATOGRAPHY, IMS, MS, WORKFLOW ->
          (List<AbstractWizardParameters<?>>) (List<? extends AbstractWizardParameters<?>>) Arrays.stream(
              getDefaultPresets()).map(p -> ((WizardParameterFactory) p).create()).toList();
    };
  }

}
