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

package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.modules.tools.batchwizard.subparameters.MassDetectorWizardOptions;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.MzToleranceSearchOptions;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.RawDataParameterEstimation;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DataFileStatisticsDashboardPaneTest {

  @Test
  void derivesMarkersFromTheSameStatisticsAsTheSinglePassEstimate() {
    final double[] values = {3d, 1d, 5d, 2d, 4d};

    Assertions.assertEquals(3d,
        DataFileStatisticsDashboardPane.rawDataEstimate(StatisticsPlotType.FWHM, values,
            MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL));
    Assertions.assertEquals(3d,
        DataFileStatisticsDashboardPane.rawDataEstimate(StatisticsPlotType.LOWEST_ISOTOPE_HEIGHT,
            values, MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL));
    Assertions.assertEquals(1.5d,
        DataFileStatisticsDashboardPane.rawDataEstimate(StatisticsPlotType.EDGE_INTENSITY, values,
            MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL));
    Assertions.assertEquals(2d,
        DataFileStatisticsDashboardPane.rawDataEstimate(StatisticsPlotType.ISOTOPE_DATA_POINTS,
            values, MassDetectorWizardOptions.ABSOLUTE_NOISE_LEVEL));
  }

  @Test
  void namesTheParameterEstimatedByEachMarker() {
    Assertions.assertEquals("FWHM",
        DataFileStatisticsDashboardPane.rawDataEstimateTarget(StatisticsPlotType.FWHM));
    Assertions.assertEquals("minimum height", DataFileStatisticsDashboardPane.rawDataEstimateTarget(
        StatisticsPlotType.LOWEST_ISOTOPE_HEIGHT));
    Assertions.assertEquals("MS1 noise level",
        DataFileStatisticsDashboardPane.rawDataEstimateTarget(StatisticsPlotType.EDGE_INTENSITY));
    Assertions.assertEquals("minimum consecutive scans",
        DataFileStatisticsDashboardPane.rawDataEstimateTarget(
            StatisticsPlotType.ISOTOPE_DATA_POINTS));
    // The categorical tolerance plot supplies its own marker label.
    Assertions.assertEquals("", DataFileStatisticsDashboardPane.rawDataEstimateTarget(
        StatisticsPlotType.BEST_TOLERANCE_FREQUENCY));
  }

  @Test
  void usesTheDefaultToleranceEstimateWithoutObservedSignals() {
    Assertions.assertEquals(MzToleranceSearchOptions.ALL_TOLERANCE_OPTIONS.get(4),
        RawDataParameterEstimation.estimateMzTolerance(List.of()));
  }

  @Test
  void omitsAbsoluteNoiseMarkerForFactorOfLowestSignalData() {
    final Double estimate = DataFileStatisticsDashboardPane.rawDataEstimate(
        StatisticsPlotType.EDGE_INTENSITY, new double[]{1d, 2d, 3d},
        MassDetectorWizardOptions.FACTOR_OF_LOWEST_SIGNAL);

    Assertions.assertNull(estimate);
  }
}
