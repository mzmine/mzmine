/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.tools.tools_autoparam.optimizer.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.DataImportWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WorkflowDdaWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.DataImportWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.workflows.WorkflowDDA;
import java.io.File;
import org.junit.jupiter.api.Test;

class WizardOptimizationProblemCurrentBaselineTest {

  @Test
  void preservesAnOutOfSearchDomainCurrentValueAndDisablesExportsOnlyInTheCopy() {
    final WizardSequence current = new WizardSequence();
    final var dataImport = DataImportWizardParameterFactory.Data.create();
    final var massSpectrometer = MassSpectrometerWizardParameterFactory.QTOF.create();
    final var workflow = new WorkflowDDA().create();
    current.add(dataImport);
    current.add(massSpectrometer);
    current.add(workflow);
    // This value intentionally need not fit an optimizer search domain. Current must not rebuild
    // or clamp it before the evaluator receives its detached copy.
    massSpectrometer.setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, 123_456d);
    workflow.setParameter(WorkflowDdaWizardParameters.exportPath, true);
    dataImport.setParameter(DataImportWizardParameters.fileNames,
        new File[]{new File("current-input.raw")});

    final WizardSequence evaluationCopy = WizardOptimizationProblem.copyForEvaluation(current);
    evaluationCopy.get(WizardPart.DATA_IMPORT).orElseThrow().setParameter(
        DataImportWizardParameters.fileNames, new File[]{new File("estimate-input.raw")});
    final var copiedMassSpectrometer = evaluationCopy.get(WizardPart.MS).orElseThrow();
    final var copiedWorkflow = evaluationCopy.get(WizardPart.WORKFLOW).orElseThrow();

    assertEquals(123_456d, copiedMassSpectrometer.getValue(
        MassSpectrometerWizardParameters.minimumFeatureHeight));
    assertFalse(copiedWorkflow.getValue(WorkflowDdaWizardParameters.exportPath));
    assertTrue(workflow.getValue(WorkflowDdaWizardParameters.exportPath));
    assertTrue(WizardOptimizationProblem.sequencesMatchForEvaluation(current, evaluationCopy));
  }
}
