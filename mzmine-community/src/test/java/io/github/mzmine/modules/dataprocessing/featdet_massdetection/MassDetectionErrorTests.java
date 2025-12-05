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

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.util.CSVParsingUtils;
import io.github.mzmine.util.io.WriterOptions;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import resolver_tests.FilesToImport;
import testutils.MZmineTestUtil;

public class MassDetectionErrorTests {

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

    try (var writer = CSVParsingUtils.createDefaultWriter(new File(
        "C:\\Users\\Steffen\\PyCharmMiscProject\\centroiding_errors\\Errors_peaks_%s.csv".formatted(
            source.name)), '\t', WriterOptions.REPLACE)) {
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

  enum MassDetectionErrorSource {
    //    WATERS("D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-MS DDA\\pos\\050325_029.raw",
//        100,
//        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Waters\\LC-MS DDA\\pos\\050325_029_copy.raw",
//        30, AbundanceMeasure.Area, "Waters"), //
//    AGILENT(
//        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Agilent\\Agilent 6546_Zamboni\\mzML\\BEH30mm_5min_LipidMix_DDA.mzML",
//        100,
//        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Agilent\\Agilent 6546_Zamboni\\BEH30mm_5min_LipidMix_DDA.d",
//        30, AbundanceMeasure.Height, "Agilent"), //
    BRUKER(
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Bruker\\timsTOF_tsf\\timsTOF_autoMSMS_Urine_6min_pos.d",
        100,
        "D:\\OneDrive - mzio GmbH\\mzio\\Example data\\Bruker\\timsTOF_tsf\\timsTOF_autoMSMS_Urine_6min_pos.mzML",
        30, AbundanceMeasure.Height, "Bruker"),
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
