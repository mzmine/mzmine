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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.tools.batchwizard.WizardParameterChanges.Source;
import io.github.mzmine.modules.tools.batchwizard.subparameters.MassSpectrometerWizardParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.WizardStepParameters;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.IonInterfaceWizardParameterFactory;
import io.github.mzmine.modules.tools.batchwizard.subparameters.factories.MassSpectrometerWizardParameterFactory;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class WizardParameterChangesTest {

  private static @NotNull WizardSequence sequence() {
    final WizardSequence sequence = new WizardSequence();
    sequence.set(WizardPart.ION_INTERFACE, IonInterfaceWizardParameterFactory.HPLC.create());
    sequence.set(WizardPart.MS, MassSpectrometerWizardParameterFactory.QTOF.create());
    return sequence;
  }

  private static @NotNull WizardSequence copy(@NotNull WizardSequence source) {
    final WizardSequence copy = new WizardSequence();
    for (final WizardStepParameters step : source) {
      final WizardStepParameters stepCopy = step.getFactory().create();
      ParameterUtils.copyParameters(step, stepCopy);
      copy.add(stepCopy);
    }
    return copy;
  }

  @Test
  void identicalSequencesHaveNoChanges() {
    final WizardSequence before = sequence();
    final WizardParameterChanges changes = WizardParameterChanges.diff(before, copy(before),
        Source.ESTIMATION);
    Assertions.assertTrue(changes.isEmpty(), () -> "Unexpected changes " + changes.changes());
  }

  @Test
  void detectsChangedValues() {
    final WizardSequence before = sequence();
    final WizardSequence after = copy(before);
    final WizardStepParameters ms = after.get(WizardPart.MS).orElseThrow();
    final Double oldHeight = ms.getValue(MassSpectrometerWizardParameters.minimumFeatureHeight);
    final MZTolerance newTolerance = new MZTolerance(0.0123, 7.5);
    ms.setParameter(MassSpectrometerWizardParameters.minimumFeatureHeight, oldHeight * 3);
    ms.setParameter(MassSpectrometerWizardParameters.scanToScanMzTolerance, newTolerance);

    final WizardParameterChanges changes = WizardParameterChanges.diff(before, after,
        Source.ESTIMATION);

    final Set<String> changedNames = changes.changes().stream()
        .map(change -> change.parameter().getName()).collect(Collectors.toSet());
    Assertions.assertEquals(Set.of(MassSpectrometerWizardParameters.minimumFeatureHeight.getName(),
        MassSpectrometerWizardParameters.scanToScanMzTolerance.getName()), changedNames);

    final List<WizardParameterChange> msChanges = changes.forPart(WizardPart.MS);
    Assertions.assertEquals(2, msChanges.size());
    Assertions.assertTrue(changes.forPart(WizardPart.ION_INTERFACE).isEmpty());

    final WizardParameterChange heightChange = msChanges.stream().filter(
            change -> change.parameter().getName()
                .equals(MassSpectrometerWizardParameters.minimumFeatureHeight.getName())).findFirst()
        .orElseThrow();
    Assertions.assertEquals(oldHeight, heightChange.oldValue());
    Assertions.assertEquals(oldHeight * 3, heightChange.newValue());

    final WizardParameterChange toleranceChange = msChanges.stream().filter(
            change -> change.parameter().getName()
                .equals(MassSpectrometerWizardParameters.scanToScanMzTolerance.getName())).findFirst()
        .orElseThrow();
    Assertions.assertEquals(newTolerance, toleranceChange.newValue());

    Assertions.assertTrue(changes.withoutPart(WizardPart.MS).isEmpty());
  }
}
