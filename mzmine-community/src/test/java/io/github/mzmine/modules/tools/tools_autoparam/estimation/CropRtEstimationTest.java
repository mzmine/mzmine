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

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.SimpleRange.SimpleFloatRange;
import io.github.mzmine.modules.tools.batchwizard.WizardPart;
import io.github.mzmine.modules.tools.batchwizard.subparameters.IonInterfaceHplcWizardParameters;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatistics;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CropRtEstimationTest {

  /**
   * @param rtStart start of the MS1 retention time range of the raw file
   * @param rtEnd   end of the MS1 retention time range of the raw file
   * @param range   the effective range, null if none was detected
   */
  private static @NotNull DataFileStatistics file(float rtStart, float rtEnd,
      @Nullable SimpleFloatRange range) {
    final RawDataFile raw = Mockito.mock(RawDataFile.class);
    Mockito.when(raw.getDataRTRange(1)).thenReturn(Range.closed(rtStart, rtEnd));
    return new DataFileStatistics(raw, List.of(), range);
  }

  private static @NotNull ParameterEstimationContext context(
      @NotNull DataFileStatistics @NotNull ... files) {
    return new ParameterEstimationContext(
        new RawDataAnalysis(List.of(files), new double[0], new double[0], new double[0],
            new double[0], new double[0], new double[0], Map.of()),
        ParameterEstimationTestData.sequence());
  }

  private static @NotNull Range<Double> cropRange(@NotNull ParameterEstimationContext context) {
    return context.sequence().get(WizardPart.ION_INTERFACE).orElseThrow()
        .getValue(IonInterfaceHplcWizardParameters.cropRtRange);
  }

  @Test
  void medianStartAndEndIfAtLeastHalfOfTheFilesHaveARange() {
    final ParameterEstimate<Range<Double>> estimate = ParameterEstimators.cropRtRange(
        context(file(0, 10, new SimpleFloatRange(0.2f, 8.0f)),
            file(0, 10, new SimpleFloatRange(0.3f, 8.4f)),
            file(0, 10, new SimpleFloatRange(1.0f, 8.2f)), file(0, 10, null)));
    Assertions.assertEquals(ValueOrigin.RAW_DATA, estimate.origin());
    Assertions.assertEquals(0.3, estimate.initialValue().lowerEndpoint(), 1e-6);
    Assertions.assertEquals(8.2, estimate.initialValue().upperEndpoint(), 1e-6);
  }

  @Test
  void unionOfTheRawRangesIfFewerThanHalfOfTheFilesHaveARange() {
    final ParameterEstimate<Range<Double>> estimate = ParameterEstimators.cropRtRange(
        context(file(0.01f, 9.5f, new SimpleFloatRange(0.3f, 8.4f)), file(0.02f, 10f, null),
            file(0.05f, 9.8f, null)));
    Assertions.assertEquals(ValueOrigin.RAW_DATA, estimate.origin());
    Assertions.assertEquals(0.01, estimate.initialValue().lowerEndpoint(), 1e-6);
    Assertions.assertEquals(10, estimate.initialValue().upperEndpoint(), 1e-6);
  }

  @Test
  void estimateReplacesTheWizardPreset() {
    // no file has a range, the union still overrides the preset start of 0.5 min
    final ParameterEstimationContext context = context(file(0f, 12f, null));
    PreparedParameterSet.prepare(context).applyEstimates(context.sequence());
    Assertions.assertEquals(0, cropRange(context).lowerEndpoint(), 1e-6);
    Assertions.assertEquals(12, cropRange(context).upperEndpoint(), 1e-6);
  }

  @Test
  void withoutFilesThePresetIsKept() {
    final ParameterEstimationContext context = context();
    final Range<Double> preset = cropRange(context);
    final ParameterEstimate<Range<Double>> estimate = ParameterEstimators.cropRtRange(context);
    Assertions.assertEquals(ValueOrigin.PRESET_DEFAULT, estimate.origin());
    Assertions.assertEquals(preset, estimate.initialValue());
  }

  @Test
  void cropRangeIsEstimateOnly() {
    Assertions.assertFalse(OptimizationParameterRegistry.allSolutions()
        .contains(OptimizationParameterRegistry.CROP_RT));
    Assertions.assertTrue(
        OptimizationParameterRegistry.forSequence(ParameterEstimationTestData.sequence())
            .contains(OptimizationParameterRegistry.CROP_RT));
    // single choice, the value cannot be changed by a search
    final PreparedParameter<Range<Double>> prepared = OptimizationParameterRegistry.CROP_RT.prepare(
        context(file(0, 10, new SimpleFloatRange(0.5f, 9f))));
    Assertions.assertEquals(prepared.initialValue(), prepared.searchDomain().decode(1));
  }
}
