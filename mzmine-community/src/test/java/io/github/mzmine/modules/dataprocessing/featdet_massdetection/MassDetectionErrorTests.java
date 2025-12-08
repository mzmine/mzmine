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

package io.github.mzmine.modules.dataprocessing.featdet_massdetection;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.gui.preferences.VendorImportParameters;
import io.github.mzmine.gui.preferences.WatersLockmassParameters;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.tof.TofMassDetector;
import io.github.mzmine.modules.io.import_rawdata_all.AdvancedSpectraImportParameters;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import resolver_tests.FilesToImport;
import testutils.MZmineTestUtil;

public class MassDetectionErrorTests {

  private static final Logger logger = Logger.getLogger(MassDetectionErrorTests.class.getName());

  private static void exportResults(String name, List<MassDetectionErrorStatistics> results,
      int scanStep) {
    try (var writer = CSVParsingUtils.createDefaultWriter(new File(
        "C:\\Users\\Steffen\\PyCharmMiscProject\\centroiding_errors\\Errors_peaks_%s.csv".formatted(
            name)), '\t', WriterOptions.REPLACE)) {
      writer.writeNext(
          new String[]{"scan", "mz_vendor", "i_vendor", "mz_mzmine", "i_mzmine", "error_abs",
              "error_ppm"});
      for (int i = 0; i < results.size(); i++) {
        MassDetectionErrorStatistics result = results.get(i);
        for (MassDetectionError matchedSignal : result.matchedSignals()) {
          writer.writeNext(
              new String[]{String.valueOf(i * scanStep), String.valueOf(matchedSignal.vendorMz()),
                  String.valueOf(matchedSignal.vendorIntensity()),
                  String.valueOf(matchedSignal.mz()), String.valueOf(matchedSignal.intensity()),
                  matchedSignal.errorAbs(), matchedSignal.errorPpm()});
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @ParameterizedTest
  @EnumSource(MassDetectionErrorSource.class)
  void testTof(MassDetectionErrorSource source) {

    MZmineTestUtil.cleanProject();
    FilesToImport vendorImport = FilesToImport.centroid(source.vendorFile, source.vendorNoise,
        source.vendorNoise);
    FilesToImport profileImport = FilesToImport.tof(source.mzmineFile, source.mzmineNoise,
        source.mzmineNoise, source.intensityCalc);
    final MZTolerance tol = new MZTolerance(0.005, 5);

    final RawDataFile vendorFile = vendorImport.runImport().getFirst();
    final RawDataFile profileFile = profileImport.runImport().getFirst();

    assert vendorFile.getNumOfScans() == profileFile.getNumOfScans();

    List<MassDetectionErrorStatistics> results = new ArrayList<>();
    final int scanStep = 1;
    for (int i = 0; i < vendorFile.getNumOfScans(); i += scanStep) {
      final MassSpectrum vendorScan = vendorFile.getScan(i).getMassList();
      final MassSpectrum centroidedScan = profileFile.getScan(i).getMassList();
      MassDetectionErrorStatistics stats = MassDetectionErrorStatistics.of(vendorScan,
          centroidedScan, tol, source.vendorNoise);
      results.add(stats);
    }

    exportResults(source.name, results, scanStep);
  }

  @Test
  public void testBruker() {
    MZmineTestUtil.cleanProject();
    final var brukerImport = new FilesToImport(List.of(MassDetectionErrorSource.BRUKER.mzmineFile),
        AdvancedSpectraImportParameters.create(null, null, null, null,
            Range.closed(1123.2d, 1124.4d),
            new ScanSelection(Range.closed(584, 590), null, null, null, PolarityType.ANY,
                MassSpectrumType.ANY, MsLevelFilter.of(1), null), false),
        VendorImportParameters.create(false, VendorImportParameters.DEFAULT_WATERS_OPTION,
            VendorImportParameters.DEFAULT_WATERS_LOCKMASS_ENABLED,
            WatersLockmassParameters.createDefault(),
            VendorImportParameters.DEFAULT_THERMO_EXCEPTION_SIGNALS));

    final RawDataFile file = brukerImport.runImport().getFirst();
    final Scan scan = file.getScans().getLast();

    final TofMassDetector massDetector = new TofMassDetector(1, AbundanceMeasure.Height);
    double[][] massValues = massDetector.getMassValues(scan);

    logger.info(Arrays.toString(massValues));
  }

  enum MassDetectionErrorSource {
    WATERS("D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-MS DDA\\pos\\050325_029.raw",
        100,
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-MS DDA\\pos\\050325_029_copy.raw",
        30, AbundanceMeasure.Area, "Waters"), //
    AGILENT(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Agilent\\Agilent 6546_Zamboni\\mzML\\BEH30mm_5min_LipidMix_DDA.mzML",
        100,
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Agilent\\Agilent 6546_Zamboni\\BEH30mm_5min_LipidMix_DDA.d",
        100, AbundanceMeasure.Height, "Agilent"), //
    BRUKER(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Bruker\\timsTOF_tsf\\timsTOF_autoMSMS_Urine_6min_pos.mzML",
        100,
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Bruker\\timsTOF_tsf\\timsTOF_autoMSMS_Urine_6min_pos.d",
        100, AbundanceMeasure.Height, "Bruker"),
    ;

    final double vendorNoise;
    final double mzmineNoise;
    final @NotNull AbundanceMeasure intensityCalc;
    final String vendorFile;
    final String mzmineFile;
    final String name;

    MassDetectionErrorSource(String vendorFile, double vendorNoise, String mzmineFile,
        double mzmineNoise, AbundanceMeasure intensityCalc, String name) {
      this.vendorFile = vendorFile;
      this.vendorNoise = vendorNoise;
      this.mzmineFile = mzmineFile;
      this.mzmineNoise = mzmineNoise;
      this.intensityCalc = intensityCalc;
      this.name = name;
    }
  }
}
