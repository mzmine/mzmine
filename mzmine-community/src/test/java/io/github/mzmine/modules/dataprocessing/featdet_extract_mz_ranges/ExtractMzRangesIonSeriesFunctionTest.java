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

package io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.project.impl.RawDataFileImpl;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ExtractMzRangesIonSeriesFunctionTest {

  /**
   * The data points after the end of the narrow range still belong to the wide range around it
   */
  @Test
  void nestedRanges() {
    final RawDataFile file = new RawDataFileImpl("test", null, null);
    final Scan scan = new SimpleScan(file, 0, 1, 1f, null,
        new double[]{100d, 100.004d, 100.008d}, new double[]{1d, 3d, 5d},
        MassSpectrumType.CENTROIDED, PolarityType.POSITIVE, "", Range.closed(0d, 1000d));
    // the data access sizes its buffers by the scans of the file
    file.addScan(scan);
    final List<Scan> scans = List.of(scan);
    final List<Range<Double>> ranges = List.of(Range.closed(99.99, 100.01),
        Range.closed(99.999, 100.001));

    final BuildingIonSeries[] series = new ExtractMzRangesIonSeriesFunction(file, scans, ranges,
        ScanDataType.RAW, null).get();

    Assertions.assertEquals(2, series.length);
    Assertions.assertEquals(5d, series[0].getIntensities()[0]);
    Assertions.assertEquals(100.008d, series[0].getMzs()[0]);
    Assertions.assertEquals(1d, series[1].getIntensities()[0]);
  }
}
