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

package io.github.mzmine.modules.tools.tools_autoparam.preclassification;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.batchwizard.WizardSequence;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PreclassificationTest {

  private static final ComboParameter<String> STUB = new ComboParameter<>("Stub", "",
      new String[]{"a", "b", "c"}, "a");

  private static @NotNull PreclassificationResult resolve(
      @NotNull RawDataClassifier<?>... classifiers) {
    final ParameterSet parameters = new SimpleParameterSet(STUB.cloneParameter(),
        PreclassificationParameters.polarity.cloneParameter());
    return Preclassification.resolve(List.of(), new WizardSequence(), List.of(classifiers),
        parameters);
  }

  @Test
  void fixedDecisionsSetTheValueWithoutChoice() {
    final PreclassificationResult resolved = resolve(
        new StubClassifier(ClassifierDecision.fixed("c")), new PolarityClassifier());
    Assertions.assertFalse(resolved.hasConflicts());
    Assertions.assertFalse(resolved.needsUserChoice());
    Assertions.assertEquals("c", resolved.parameters().getValue(STUB));
    // no files and no wizard polarity: nothing to filter
    Assertions.assertEquals(WizardMsPolarity.No_filter,
        resolved.parameters().getValue(PreclassificationParameters.polarity));
  }

  @Test
  void choicesLimitTheComboAndAreCollected() {
    final PreclassificationResult resolved = resolve(
        new StubClassifier(ClassifierDecision.choice(List.of("b", "c"), "why")));
    Assertions.assertTrue(resolved.needsUserChoice());
    Assertions.assertEquals(List.of("why"), resolved.choiceMessages());

    final ComboParameter<String> combo = resolved.parameters().getParameter(STUB);
    Assertions.assertSame(combo, resolved.choiceParameters().getFirst());
    Assertions.assertEquals(List.of("b", "c"), List.copyOf(combo.getChoices()));
    Assertions.assertEquals("b", combo.getValue());
    // the static parameter must stay untouched
    Assertions.assertEquals(List.of("a", "b", "c"), List.copyOf(STUB.getChoices()));
  }

  @Test
  void conflictsOfAllClassifiersAreCollected() {
    final PreclassificationResult resolved = resolve(
        new StubClassifier(ClassifierDecision.conflict("first")),
        new StubClassifier(ClassifierDecision.fixed("c")),
        new StubClassifier(ClassifierDecision.conflict("second")));
    Assertions.assertTrue(resolved.hasConflicts());
    Assertions.assertEquals(List.of("first", "second"), resolved.conflicts());
  }

  /**
   * Returns the given decision for {@link #STUB}.
   */
  private record StubClassifier(@NotNull ClassifierDecision<String> decision) implements
      RawDataClassifier<String> {

    @Override
    public @NotNull UserParameter<String, ?> parameter() {
      return STUB;
    }

    @Override
    public @NotNull ClassifierDecision<String> decide(
        @NotNull List<@NotNull RawDataFile> files, @NotNull WizardSequence wizard) {
      return decision;
    }
  }
}
