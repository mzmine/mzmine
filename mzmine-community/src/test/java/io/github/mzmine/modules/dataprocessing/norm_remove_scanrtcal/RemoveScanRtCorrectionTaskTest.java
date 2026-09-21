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

package io.github.mzmine.modules.dataprocessing.norm_remove_scanrtcal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.AbstractRtCorrectionFunction;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.rawfilemethod.ApplyRtCorrectionToRawFileModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.rawfilemethod.ApplyRtCorrectionToRawFileParameters;
import io.github.mzmine.parameters.ParameterSet;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import javafx.collections.FXCollections;
import org.junit.jupiter.api.Test;

class RemoveScanRtCorrectionTaskTest {

  @Test
  void removesCompleteStackForAllCorrectionFiles() {
    final RawDataFile rawA = rawFile();
    final RawDataFile rawB = rawFile();
    final FeatureListAppliedMethod first = correction("2026-01-01T10:00:00Z", rawA, rawB);
    final FeatureListAppliedMethod second = correction("2026-01-01T10:01:00Z", rawA, rawB);
    final FeatureListAppliedMethod clear = clearMethod("2026-01-01T10:02:00Z");
    rawA.getAppliedMethods().addAll(first, second, clear);
    rawB.getAppliedMethods().addAll(first, second, clear);

    assertTrue(RemoveScanRtCorrectionTask.removeRevertedRtCorrectionsIfAllFilesCleared(
        List.of(rawA, rawB)));
    assertTrue(rawA.getAppliedMethods().isEmpty());
    assertTrue(rawB.getAppliedMethods().isEmpty());
  }

  @Test
  void keepsCompleteHistoryForPartialCorrectionCohort() {
    final RawDataFile rawA = rawFile();
    final RawDataFile rawB = rawFile();
    final FeatureListAppliedMethod correction = correction("2026-01-01T10:00:00Z", rawA, rawB);
    final FeatureListAppliedMethod clear = clearMethod("2026-01-01T10:01:00Z");
    rawA.getAppliedMethods().addAll(correction, clear);
    rawB.getAppliedMethods().add(correction);

    assertFalse(RemoveScanRtCorrectionTask.removeRevertedRtCorrectionsIfAllFilesCleared(
        List.of(rawA)));
    assertEquals(List.of(correction, clear), rawA.getAppliedMethods());
    assertEquals(List.of(correction), rawB.getAppliedMethods());
  }

  @Test
  void doesNotPartiallyRemoveStackAcrossProcessingBarrier() {
    final RawDataFile raw = rawFile();
    final FeatureListAppliedMethod first = correction("2026-01-01T10:00:00Z", raw);
    final FeatureListAppliedMethod processingStep = otherMethod("2026-01-01T10:01:00Z");
    final FeatureListAppliedMethod second = correction("2026-01-01T10:02:00Z", raw);
    final FeatureListAppliedMethod clear = clearMethod("2026-01-01T10:03:00Z");
    raw.getAppliedMethods().addAll(first, processingStep, second, clear);

    assertFalse(RemoveScanRtCorrectionTask.removeRevertedRtCorrectionsIfAllFilesCleared(
        List.of(raw)));
    assertEquals(List.of(first, processingStep, second, clear), raw.getAppliedMethods());
  }

  @Test
  void removesCorrectionAfterPreviousClear() {
    final RawDataFile raw = rawFile();
    final FeatureListAppliedMethod first = correction("2026-01-01T10:00:00Z", raw);
    final FeatureListAppliedMethod clear = clearMethod("2026-01-01T10:01:00Z");
    final FeatureListAppliedMethod second = correction("2026-01-01T10:02:00Z", raw);
    final FeatureListAppliedMethod finalClear = clearMethod("2026-01-01T10:03:00Z");
    raw.getAppliedMethods().addAll(first, clear, second, finalClear);

    assertTrue(RemoveScanRtCorrectionTask.removeRevertedRtCorrectionsIfAllFilesCleared(
        List.of(raw)));
    assertEquals(List.of(first, clear), raw.getAppliedMethods());
  }

  @Test
  void manualClearCompactsExistingIndividualClearOperations() {
    final RawDataFile rawA = rawFile();
    final RawDataFile rawB = rawFile();
    final FeatureListAppliedMethod correction = correction("2026-01-01T10:00:00Z", rawA, rawB);
    final FeatureListAppliedMethod clearA = clearMethod("2026-01-01T10:01:00Z");
    final FeatureListAppliedMethod clearB = clearMethod("2026-01-01T10:02:00Z");
    rawA.getAppliedMethods().addAll(correction, clearA);
    rawB.getAppliedMethods().addAll(correction, clearB);

    assertTrue(RemoveScanRtCorrectionTask.removeRevertedRtCorrectionsIfAllFilesCleared(
        List.of(rawA, rawB)));
    assertTrue(rawA.getAppliedMethods().isEmpty());
    assertTrue(rawB.getAppliedMethods().isEmpty());
  }

  private static RawDataFile rawFile() {
    final RawDataFile raw = mock(RawDataFile.class);
    when(raw.getAppliedMethods()).thenReturn(FXCollections.observableArrayList());
    return raw;
  }

  private static FeatureListAppliedMethod correction(String date, RawDataFile... files) {
    final FeatureListAppliedMethod method = mock(FeatureListAppliedMethod.class);
    final ParameterSet parameters = mock(ParameterSet.class);
    final List<AbstractRtCorrectionFunction> corrections = Arrays.stream(files).map(file -> {
      final AbstractRtCorrectionFunction correction = mock(AbstractRtCorrectionFunction.class);
      when(correction.getRawDataFile()).thenReturn(file);
      return correction;
    }).toList();

    when(method.getModule()).thenReturn(new ApplyRtCorrectionToRawFileModule());
    when(method.getModuleCallDate()).thenReturn(Instant.parse(date));
    when(method.getParameters()).thenReturn(parameters);
    when(parameters.getValue(ApplyRtCorrectionToRawFileParameters.calis)).thenReturn(corrections);
    return method;
  }

  private static FeatureListAppliedMethod clearMethod(String date) {
    final FeatureListAppliedMethod method = mock(FeatureListAppliedMethod.class);
    when(method.getModule()).thenReturn(new RemoveScanRtCorrectionModule());
    when(method.getModuleCallDate()).thenReturn(Instant.parse(date));
    return method;
  }

  private static FeatureListAppliedMethod otherMethod(String date) {
    final FeatureListAppliedMethod method = mock(FeatureListAppliedMethod.class);
    when(method.getModule()).thenReturn(mock(MZmineModule.class));
    when(method.getModuleCallDate()).thenReturn(Instant.parse(date));
    return method;
  }
}
