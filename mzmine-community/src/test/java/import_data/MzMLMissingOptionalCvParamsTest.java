/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package import_data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.project.ProjectService;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import testutils.MZmineTestUtil;

/**
 * Tests mzML import of files that omit optional scan-level CV params (MS:1000528 lowest observed
 * m/z and MS:1000527 highest observed m/z). This is valid mzML and occurs with converters like
 * SCIEX MS Converter. Previously caused a NullPointerException in
 * BuildingMzMLMsScan.getDataPointMZRange() because it dereferenced mzBinaryDataInfo after it was
 * cleared.
 */
@TestInstance(Lifecycle.PER_CLASS)
@DisabledOnOs(OS.MAC)
public class MzMLMissingOptionalCvParamsTest {

  private static final String TEST_FILE = "rawdatafiles/additional/sciex_no_mz_range_cv.mzML";
  private static final String TEST_FILE_NO_WINDOW_LIMITS = "rawdatafiles/additional/no_scan_window_limits.mzML";

  @BeforeAll
  void initialize() {
    MZmineTestUtil.startMzmineCore();
  }

  @AfterAll
  void tearDown() {
    MZmineTestUtil.cleanProject();
  }

  @Test
  @DisplayName("Import mzML without lowest/highest observed m/z CV params should not crash")
  void testImportWithoutMzRangeCvParams() throws InterruptedException {
    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(List.of(TEST_FILE), 60);

    RawDataFile[] dataFiles = ProjectService.getProject().getDataFiles();
    assertEquals(1, dataFiles.length, "Expected 1 imported data file");

    RawDataFile raw = dataFiles[0];
    assertNotNull(raw);
    assertEquals(2, raw.getNumOfScans(), "Expected 2 scans total");
    assertEquals(1, raw.getNumOfScans(1), "Expected 1 MS1 scan");
    assertEquals(1, raw.getNumOfScans(2), "Expected 1 MS2 scan");

    // Verify scans have valid data and getDataPointMZRange does not throw
    for (Scan scan : raw.getScans()) {
      assertTrue(scan.getNumberOfDataPoints() > 0,
          "Scan " + scan.getScanNumber() + " should have data points");

      Range<Double> mzRange = scan.getDataPointMZRange();
      assertNotNull(mzRange, "Scan " + scan.getScanNumber() + " should have a valid mz range");
      assertTrue(mzRange.lowerEndpoint() > 0, "Lower mz bound should be positive");
      assertTrue(mzRange.upperEndpoint() > mzRange.lowerEndpoint(),
          "Upper mz bound should be greater than lower");
    }

    // Verify MS1 scan has expected values (mz: 100, 200, 300)
    Scan ms1 = raw.getScan(0);
    assertEquals(1, ms1.getMSLevel());
    assertEquals(3, ms1.getNumberOfDataPoints());
    Range<Double> ms1Range = ms1.getDataPointMZRange();
    assertEquals(100.0, ms1Range.lowerEndpoint(), 0.01);
    assertEquals(300.0, ms1Range.upperEndpoint(), 0.01);

    // Verify scanning mz range resolved from referenceableParamGroupRef in scanWindow
    Range<Double> scanningRange = ms1.getScanningMZRange();
    assertNotNull(scanningRange);
    assertEquals(50.0, scanningRange.lowerEndpoint(), 0.01,
        "Scan window lower limit should be resolved from referenceableParamGroupRef");
    assertEquals(500.0, scanningRange.upperEndpoint(), 0.01,
        "Scan window upper limit should be resolved from referenceableParamGroupRef");

    // Verify MS2 scan has expected values (mz: 105, 150)
    Scan ms2 = raw.getScan(1);
    assertEquals(2, ms2.getMSLevel());
    assertEquals(2, ms2.getNumberOfDataPoints());
    Range<Double> ms2Range = ms2.getDataPointMZRange();
    assertEquals(105.0, ms2Range.lowerEndpoint(), 0.01);
    assertEquals(150.0, ms2Range.upperEndpoint(), 0.01);
  }

  /**
   * Tests the fallback path in getDataPointMZRange when scan window lower/upper limit CV params are
   * completely absent (not even via referenceableParamGroupRef). This directly exercises the NPE fix
   * in BuildingMzMLMsScan.getDataPointMZRange where mzBinaryDataInfo is null after clearUnusedData.
   */
  @Test
  @DisplayName("Import mzML with empty scan window (no limits) should compute mz range from data")
  void testImportWithEmptyScanWindow() throws InterruptedException {
    MZmineTestUtil.cleanProject();
    MZmineTestUtil.importFiles(List.of(TEST_FILE_NO_WINDOW_LIMITS), 60);

    RawDataFile[] dataFiles = ProjectService.getProject().getDataFiles();
    assertEquals(1, dataFiles.length, "Expected 1 imported data file");

    RawDataFile raw = dataFiles[0];
    assertNotNull(raw);
    assertEquals(1, raw.getNumOfScans(), "Expected 1 scan");

    Scan scan = raw.getScan(0);
    assertEquals(1, scan.getMSLevel());
    assertEquals(3, scan.getNumberOfDataPoints());

    // This is the critical assertion: getScanningMZRange falls through to getDataPointMZRange
    // because scan window has no lower/upper limits. Without the NPE fix, this crashes.
    Range<Double> scanningRange = scan.getScanningMZRange();
    assertNotNull(scanningRange, "Scanning range should not be null");
    assertEquals(100.0, scanningRange.lowerEndpoint(), 0.01,
        "Scanning range should fall back to data point mz range lower bound");
    assertEquals(300.0, scanningRange.upperEndpoint(), 0.01,
        "Scanning range should fall back to data point mz range upper bound");
  }
}
