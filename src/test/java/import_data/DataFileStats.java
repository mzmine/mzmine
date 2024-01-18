/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package import_data;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Frame;
import io.github.mzmine.datamodel.ImagingScan;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_all.AllSpectralDataImportParameters;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

public record DataFileStats(String fileName, boolean advanced, int numScans, int numScansMs1,
                            int numScansMs2,
                            int maxRawDataPoints, int maxCentroidDataPoints,
                            List<Integer> scanNumDataPoints, List<Double> scanTic,
                            List<String> scanType, List<Double> scanBasePeakMz,
                            List<Integer> scanNumber, List<String> scanMzRange,
                            List<Float> scanInjectTime, List<Integer> scanMsLevel,
                            List<String> scanPolarity, List<Double> scanPrecursorMz,
                            List<Integer> scanPrecursorCharge, List<Float> scanRetentionTime,
                            List<String> frameMobilityRange, List<Integer> imsMaxRawDataPoints,
                            List<Integer> imsMaxCentroidDataPoints,
                            List<Integer> imsScanNumMobScans, List<String> imageScanCoord) {

  public static final List<Integer> scanNumbers = List.of(0, 1, 5, 10, 20, 25, 50, 75, 100, 150,
      200, 300, 400, 600, 800, 1000, 1200, 1500);

  private static final Logger logger = Logger.getLogger(DataFileStats.class.getName());


  /**
   * Extract data for text
   */
  public static DataFileStats extract(RawDataFile raw) {
    var applied = raw.getAppliedMethods().get(0);
    boolean advanced = applied.getParameters()
        .getValue(AllSpectralDataImportParameters.advancedImport);

    int numScans = raw.getNumOfScans();
    int numScansMs1 = raw.getNumOfScans(1);
    int numScansMs2 = raw.getNumOfScans(2);
    int maxRawDataPoints = raw.getMaxRawDataPoints();
    int maxCentroidDataPoints = raw.getMaxCentroidDataPoints();
    var scanNumDataPoints = streamScans(raw).map(MassSpectrum::getNumberOfDataPoints).toList();
    var scanTic = streamScans(raw).map(MassSpectrum::getTIC).toList();
    var scanType = streamScans(raw).map(MassSpectrum::getSpectrumType).map(Objects::toString)
        .toList();
    var scanBasePeakMz = streamScans(raw).map(MassSpectrum::getBasePeakMz).toList();

    var scanNumber = streamScans(raw).map(Scan::getScanNumber).toList();
    var scanMzRange = streamScans(raw).map(Scan::getScanningMZRange).map(Range::toString).toList();
    var scanInjectTime = streamScans(raw).map(Scan::getInjectionTime).toList();
    var scanMsLevel = streamScans(raw).map(Scan::getMSLevel).toList();
    var scanPolarity = streamScans(raw).map(Scan::getPolarity).map(PolarityType::toString).toList();
    var scanPrecursorMz = streamScans(raw).map(Scan::getPrecursorMz).toList();
    var scanPrecursorCharge = streamScans(raw).map(Scan::getPrecursorCharge).toList();
    var scanRetentionTime = streamScans(raw).map(Scan::getRetentionTime).toList();

    // check ion mobility
    var frames = streamScans(raw).filter(scan -> scan instanceof Frame).map(scan -> (Frame) scan)
        .toList();

    var frameMobilityRange = frames.stream().map(Frame::getMobilityRange).map(Range::toString)
        .toList();
    var imsMaxRawDataPoints = frames.stream().map(Frame::getMaxMobilityScanRawDataPoints).toList();
    var imsMaxCentroidDataPoints = frames.stream().map(dataPoints -> {
      try {
        return dataPoints.getMaxMobilityScanMassListDataPoints();
      } catch (Exception ex) {
        return null;
      }
    }).filter(Objects::nonNull).toList();
    var imsScanNumMobScans = frames.stream().map(Frame::getMobilityScans).map(List::size).toList();

    // imaging
    var imagingScan = streamScans(raw).filter(scan -> scan instanceof ImagingScan)
        .map(scan -> (ImagingScan) scan).toList();
    var imageScanCoord = imagingScan.stream().map(ImagingScan::getCoordinates)
        .filter(Objects::nonNull).map(c -> STR."\{c.getX()},\{c.getY()}").toList();

    return new DataFileStats(raw.getFileName(), advanced, numScans, numScansMs1, numScansMs2,
        maxRawDataPoints, maxCentroidDataPoints, scanNumDataPoints, scanTic, scanType,
        scanBasePeakMz, scanNumber, scanMzRange, scanInjectTime, scanMsLevel, scanPolarity,
        scanPrecursorMz, scanPrecursorCharge, scanRetentionTime,
        // IMS
        frameMobilityRange, imsMaxRawDataPoints, imsMaxCentroidDataPoints, imsScanNumMobScans,
        // images
        imageScanCoord);
  }

  @NotNull
  private static Stream<Scan> streamScans(final RawDataFile raw) {
    return scanNumbers.stream().filter(i -> i < raw.getNumOfScans()).map(raw::getScan);
  }

  private static String convertToString(Object o) {
    return switch (o) {
      case null -> "null";
      case Float f -> f + "f"; // otherwise its double
      case Number n -> n.toString();
      case List<?> list -> "List.of(" + list.stream() //
          .filter(Objects::nonNull) // cannot handle null in List.of
          .map(DataFileStats::convertToString).collect(Collectors.joining(", ")) + ")";
      default -> STR."\"\{o}\"";
    };
  }

  /**
   * Test all fields for equality. This is used as expected results
   */
  public void test(RawDataFile actualRaw) {
    DataFileStats actual = extract(actualRaw);
    DataFileStats expected = this;

    RecordComponent[] fields = getClass().getRecordComponents();
    for (final RecordComponent field : fields) {
      // skip field like when RT has changed due to floating point parsing
//      if (field.getName().equals("scanRetentionTime")) {
//        continue;
//      }

      testField(expected, actual, field);
    }
  }

  private void testField(final DataFileStats expected, final DataFileStats actual,
      final RecordComponent field) {
    var actualVal = actual.getValue(field);
    var expectedVal = expected.getValue(field);

    if (actualVal instanceof List alist && expectedVal instanceof List elist) {
      Assertions.assertEquals(elist.size(), alist.size(),
          "Missmatching number of values in list for field %s in dataset %s".formatted(
              field.getName(), expected.fileName));
    }

    Assertions.assertEquals(expectedVal, actualVal,
        "Missmatch for field %s in dataset %s".formatted(field.getName(), expected.fileName));
  }

  public String printInstance() {
    String arguments = Arrays.stream(getClass().getRecordComponents()).map(this::getValue)
        .map(DataFileStats::convertToString).collect(Collectors.joining(", "));

    String s = STR."new import_data.DataFileStats(\{arguments})";
    return s;
  }

  private Object getValue(final RecordComponent field) {
    try {
      var value = field.getAccessor().invoke(this);
      if (value instanceof List list) {
        return list.stream().filter(Objects::nonNull).toList();
      }
      return value;
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
