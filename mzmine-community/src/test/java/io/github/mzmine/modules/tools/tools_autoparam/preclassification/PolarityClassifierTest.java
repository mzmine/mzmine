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

import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.modules.tools.batchwizard.subparameters.custom_parameters.WizardMsPolarity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PolarityClassifierTest {

  /**
   * @param namesAndPolarities alternating file name and polarity
   */
  private static @NotNull Map<String, PolarityType> files(Object... namesAndPolarities) {
    final Map<String, PolarityType> files = new LinkedHashMap<>();
    for (int i = 0; i < namesAndPolarities.length; i += 2) {
      files.put((String) namesAndPolarities[i], (PolarityType) namesAndPolarities[i + 1]);
    }
    return files;
  }

  private static void assertFixed(@NotNull WizardMsPolarity expected,
      @NotNull PreclassificationDecision<WizardMsPolarity> decision) {
    Assertions.assertEquals(new PreclassificationFixed<>(expected), decision);
  }

  private static @NotNull String assertConflict(
      @NotNull PreclassificationDecision<WizardMsPolarity> decision) {
    return Assertions.assertInstanceOf(PreclassificationConflict.class, decision).message();
  }

  @Test
  void singlePolaritySwitchesWithoutDialog() {
    assertFixed(WizardMsPolarity.Positive,
        PolarityClassifier.decide(files("a", PolarityType.POSITIVE, "b", PolarityType.POSITIVE),
            WizardMsPolarity.No_filter));
    assertFixed(WizardMsPolarity.Positive,
        PolarityClassifier.decide(files("a", PolarityType.POSITIVE, "b", PolarityType.ANY),
            WizardMsPolarity.No_filter));
    assertFixed(WizardMsPolarity.Negative,
        PolarityClassifier.decide(files("a", PolarityType.ANY, "b", PolarityType.NEGATIVE),
            WizardMsPolarity.No_filter));
  }

  @Test
  void polaritySwitchingFilesNeedAChoice() {
    final PreclassificationDecision<WizardMsPolarity> decision = PolarityClassifier.decide(
        files("a", PolarityType.ANY, "b", PolarityType.ANY), WizardMsPolarity.No_filter);
    final PreclassificationChoice<?> choice = Assertions.assertInstanceOf(
        PreclassificationChoice.class, decision);
    Assertions.assertEquals(List.of(WizardMsPolarity.Positive, WizardMsPolarity.Negative),
        choice.options());
  }

  @Test
  void positiveAndNegativeOnlyFilesConflict() {
    for (final WizardMsPolarity wizard : WizardMsPolarity.values()) {
      final String message = assertConflict(PolarityClassifier.decide(
          files("pos", PolarityType.POSITIVE, "mixed", PolarityType.ANY, "neg",
              PolarityType.NEGATIVE), wizard));
      Assertions.assertTrue(message.contains("pos") && message.contains("neg"), message);
    }
  }

  @Test
  void wizardPolarityMustExistInAllFiles() {
    assertFixed(WizardMsPolarity.Negative,
        PolarityClassifier.decide(files("a", PolarityType.ANY, "b", PolarityType.NEGATIVE),
            WizardMsPolarity.Negative));

    final String message = assertConflict(PolarityClassifier.decide(
        files("mixed", PolarityType.ANY, "posOnly", PolarityType.POSITIVE),
        WizardMsPolarity.Negative));
    Assertions.assertTrue(message.contains("posOnly") && !message.contains("mixed"), message);

    assertConflict(PolarityClassifier.decide(files("unknown", PolarityType.UNKNOWN),
        WizardMsPolarity.Positive));
  }

  @Test
  void filesWithoutPolarityInformation() {
    assertFixed(WizardMsPolarity.No_filter,
        PolarityClassifier.decide(files("a", PolarityType.UNKNOWN, "b", PolarityType.UNKNOWN),
            WizardMsPolarity.No_filter));

    final String message = assertConflict(PolarityClassifier.decide(
        files("unknown", PolarityType.UNKNOWN, "pos", PolarityType.POSITIVE),
        WizardMsPolarity.No_filter));
    Assertions.assertTrue(message.contains("unknown"), message);
  }
}
