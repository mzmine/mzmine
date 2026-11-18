/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.tools_autoparam.estimation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.tools_autoparam.preclassification.PreclassificationParameters;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class WizardParameterEstimationApplicabilityTest {

  @ParameterizedTest
  @EnumSource(value = IonInterfaceWizardParameterFactory.class,
      names = {"MALDI", "LDI", "DESI", "SIMS"})
  void unsupportedInterfacesFailBeforeReadingOrReplacingMassLists(
      final @NotNull IonInterfaceWizardParameterFactory ionInterface) {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.ION_INTERFACE, ionInterface.create());
    final RawDataFile file = mock(RawDataFile.class);
    final var preclassification = new PreclassificationParameters();
    final IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> WizardParameterEstimationResult.estimate(List.of(file), null, sequence,
            preclassification, _ -> true, null, () -> false));
    assertEquals(WizardParameterEstimationResult.UNSUPPORTED_WORKFLOW_MESSAGE, error.getMessage());
    verifyNoInteractions(file);
  }

  @ParameterizedTest
  @EnumSource(value = IonInterfaceWizardParameterFactory.class,
      names = {"HPLC", "UHPLC", "HILIC", "GC_CI", "GC_EI", "DIRECT_INFUSION", "FLOW_INJECT"})
  void supportedInterfacesPermitEstimation(
      final @NotNull IonInterfaceWizardParameterFactory ionInterface) {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.ION_INTERFACE, ionInterface.create());
    assertDoesNotThrow(() -> WizardParameterEstimationResult.requireSupportedWorkflow(sequence));
  }
}
