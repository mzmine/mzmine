/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.massvoltammogram.utils;

import com.google.common.collect.Range;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.util.scans.ScanUtils;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ArrayUtils;

public class MassvoltammogramUtils {

  private MassvoltammogramUtils() {
    throw new IllegalStateException("Utility class");
  }

  public static List<MassvoltammogramScan> extractMZRangeFromScan(List<MassvoltammogramScan> scans,
      Range<Double> mzRange) {

    //Initializing a list to add the new scans to.
    final List<MassvoltammogramScan> scansInMzRange = new ArrayList<>();

    //Going over all scans and extracting the mz-range.
    for (MassvoltammogramScan scan : scans) {

      //Initializing DoubleArrayLists to add the m/z and intensity values to.
      final List<Double> mzs = new ArrayList<>();
      final List<Double> intensities = new ArrayList<>();

      //Extracting values inside the m/z-range from the scan.
      for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {

        if (mzRange.contains(scan.getMz(i))) {
          intensities.add(scan.getIntensity(i));
          mzs.add(scan.getMz(i));
        }
      }

      //Adding the extracted scan to the list of scans
      scansInMzRange.add(new MassvoltammogramScan(mzs, intensities, scan.getPotential(),
          scan.getMassSpectrumType()));
    }

    return scansInMzRange;
  }

  /**
   * Adds datapoints with an intensity of 0 around each datapoint if the mass spectra are centroid.
   * Empty datapoints are added 0.0001 m/z before and after each datapoint, so the data gets
   * visualized correctly by the plot.
   *
   * @param scans The list of scans as arrays to be processed.
   */
  public static void addZerosToCentroidData(List<MassvoltammogramScan> scans) {

    //Setting the number format for the mz-values.
    final NumberFormat mzFormat = MZmineCore.getConfiguration().getMZFormat();

    //The delta mz at which new datapoints will be added around the existing datapoints.
    final double deltaMZ = 0.0001;

    //Going over all scans in the massvoltammogram.
    for (MassvoltammogramScan scan : scans) {

      //Continuing if the current scan is not centroid.
      if (!scan.getMassSpectrumType().isCentroided()) {
        continue;
      }

      //Initializing lists to add the mz and intensity values to.
      final List<Double> mzs = new ArrayList<>();
      final List<Double> intensities = new ArrayList<>();

      //Going over all datapoints in the scan.
      for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {

        //Adding a datapoint with 0 intensity directly in front of the scans datapoint.
        mzs.add(Double.parseDouble(mzFormat.format(scan.getMz(i) - deltaMZ)));
        intensities.add(0d);

        //Adding the datapoint itself.
        mzs.add(scan.getMz(i));
        intensities.add(scan.getIntensity(i));

        //Adding a datapoint with 0 intensity directly after the scans datapoint.
        mzs.add(Double.parseDouble(mzFormat.format(scan.getMz(i) + deltaMZ)));
        intensities.add(0d);
      }

      scan.setMzs(mzs);
      scan.setIntensities(intensities);
    }
  }

  /**
   * Adds datapoints with an intensity of 0 to the beginning and the end of each scan, so they start
   * and ent at the same m/z-value.
   *
   * @param scans The scans to be aligned.
   */
  public static void alignScans(List<MassvoltammogramScan> scans, Range<Double> userInputMzRange,
      Range<Double> rawDataMzRange) {

    //Going over all MassvoltammogramScans in the massvoltammogram.
    for (MassvoltammogramScan scan : scans) {

      //Adding the datapoints to scans if they are empty.
      if (scan.isEmpty()) {
        if (userInputMzRange.lowerEndpoint() > rawDataMzRange.lowerEndpoint()) {
          prependEmptyDataPoint(scan, userInputMzRange.lowerEndpoint());

        } else if (userInputMzRange.lowerEndpoint() < rawDataMzRange.lowerEndpoint()) {
          prependEmptyDataPoint(scan, rawDataMzRange.lowerEndpoint());
        }

        if (userInputMzRange.upperEndpoint() < rawDataMzRange.upperEndpoint()) {
          appendEmptyDataPoint(scan, userInputMzRange.upperEndpoint());

        } else if (userInputMzRange.upperEndpoint() > rawDataMzRange.upperEndpoint()) {
          appendEmptyDataPoint(scan, rawDataMzRange.upperEndpoint());
        }

        continue;
      }

      //Adds a datapoint with 0 intensity to the beginning of a scan, if no other datapoint is found.
      if (scan.getMinMz() > userInputMzRange.lowerEndpoint()
          && userInputMzRange.lowerEndpoint() > rawDataMzRange.lowerEndpoint()) {
        prependEmptyDataPoint(scan, userInputMzRange.lowerEndpoint());

      } else if (scan.getMinMz() > userInputMzRange.lowerEndpoint()
          && userInputMzRange.lowerEndpoint() < rawDataMzRange.lowerEndpoint()) {
        prependEmptyDataPoint(scan, rawDataMzRange.lowerEndpoint());
      }

      //Adds a datapoint with 0 intensity to the end of a scan, if no other datapoint is found.
      if (scan.getMaxMz() < userInputMzRange.upperEndpoint()
          && userInputMzRange.upperEndpoint() < rawDataMzRange.upperEndpoint()) {
        appendEmptyDataPoint(scan, userInputMzRange.upperEndpoint());

      } else if (scan.getMaxMz() < userInputMzRange.upperEndpoint()
          && userInputMzRange.upperEndpoint() > rawDataMzRange.upperEndpoint()) {
        appendEmptyDataPoint(scan, rawDataMzRange.upperEndpoint());
      }
    }
  }

  /**
   * Removes datapoints with low intensity values. Keeps datapoints with intensity of 0.
   *
   * @param scans        Dataset the datapoints will be removed from.
   * @param maxIntensity Max intensity of all datapoints in the dataset.
   * @return Returns the filtered list of massvoltammogramScans. Thereby all signals lower than 0.1%
   * of the max intensity are considered as irrelevant for the visualisation and are removed.
   */
  public static List<MassvoltammogramScan> removeNoise(List<MassvoltammogramScan> scans,
      double maxIntensity) {

    //Initializing a list to add the filtered MassvoltammogramScans to.
    final List<MassvoltammogramScan> filteredScans = new ArrayList<>();

    //Going over all scans in the massvoltammogram.
    for (MassvoltammogramScan scan : scans) {

      //Initializing lists to save the filtered data to.
      final List<Double> filteredMZ = new ArrayList<>();
      final List<Double> filteredIntensities = new ArrayList<>();

      //Initializing the intensity threshold.
      final double intensityThreshold =
          maxIntensity * 0.001; // Signals lower than 0.1% of the max intensity will be removed.

      //Adding the first value of the scan.
      filteredMZ.add(scan.getMinMz());
      filteredIntensities.add(scan.getIntensity(0));

      //Adding all other datapoints if the intensity is 0 or bigger than the intensity threshold.
      for (int i = 1; i < scan.getNumberOfDatapoints() - 1; i++) {

        //Adding the datapoint if its intensity exceeds the threshold.
        if (scan.getIntensity(i) >= intensityThreshold) {
          filteredMZ.add(scan.getMz(i));
          filteredIntensities.add(scan.getIntensity(i));

          //Setting the datapoints intensity to 0 if it's below the threshold.
        } else if (scan.getIntensity(i) < intensityThreshold) {
          filteredMZ.add(scan.getMz(i));
          filteredIntensities.add(0d);
        }
      }

      //Adding the last value of the scan.
      filteredMZ.add(scan.getMaxMz());
      filteredIntensities.add(scan.getIntensity(scan.getNumberOfDatapoints() - 1));

      //Adding a new MassvoltammogramScan to the list of filtered scans.
      filteredScans.add(
          new MassvoltammogramScan(filteredMZ, filteredIntensities, scan.getPotential(),
              scan.getMassSpectrumType()));
    }

    //Removing excess zeros.
    removeExcessZeros(filteredScans);

    return filteredScans;
  }

  /**
   * Removes neighbouring zeros from the whole dataset.
   *
   * @param scans The list of scans the excess zeros will be removed from.
   */
  public static void removeExcessZeros(List<MassvoltammogramScan> scans) {

    for (MassvoltammogramScan scan : scans) {

      //Removing all excess zeros from the scan.
      final double[][] filteredScan = ScanUtils.removeExtraZeros(
          new double[][]{scan.getMzs(), scan.getIntensities()});

      scan.setMzs(filteredScan[0]);
      scan.setIntensities(filteredScan[1]);
    }
  }

  /**
   * Finds a power of 10 to scale the z-axis by.
   *
   * @param maxIntensity The max intensity of the dataset.
   * @return Returns the divisor all intensities need to be divided by.
   */
  public static double getDivisor(double maxIntensity) {

    //Getting for the next smallest power of ten to use as the divisor.
    double power = Math.log10(maxIntensity);

    return Math.pow(10, Math.floor(power));
  }

  /**
   * Finds the maximal intensity in a set of MassvoltammogramScans.
   *
   * @param scans The list of MassvoltammogramScans the maximal intensity will be extracted from.
   * @return Returns the maximal intensity over all MassvoltammogramScans.
   */
  public static double getMaxIntensity(List<MassvoltammogramScan> scans) {

    //Setting the initial max intensity to 0.
    double maxIntensity = 0;

    //Going over every datapoint in all the scans and comparing the intensity to the current max intensity.
    for (MassvoltammogramScan scan : scans) {
      for (int i = 0; i < scan.getNumberOfDatapoints(); i++) {
        if (scan.getIntensity(i) > maxIntensity) {
          maxIntensity = scan.getIntensity(i);
        }
      }
    }
    return maxIntensity;
  }

  /**
   * @param superscript Integer that will be converted to superscript.
   * @return Returns the integer converted to superscript string.
   */
  public static String toSuperscript(int superscript) {
    final StringBuilder output = new StringBuilder();

    //Converting the input integer to a string.
    final String input = Integer.toString(superscript);

    //Exchanging every numeric character of the string to superscript.
    for (int i = 0; i < input.length(); i++) {
      if (Character.getNumericValue(input.charAt(i)) == 0) {
        output.append("⁰");
      } else if (Character.getNumericValue(input.charAt(i)) == 1) {
        output.append("¹");
      } else if (Character.getNumericValue(input.charAt(i)) == 2) {
        output.append("²");
      } else if (Character.getNumericValue(input.charAt(i)) == 3) {
        output.append("³");
      } else if (Character.getNumericValue(input.charAt(i)) == 4) {
        output.append("⁴");
      } else if (Character.getNumericValue(input.charAt(i)) == 5) {
        output.append("⁵");
      } else if (Character.getNumericValue(input.charAt(i)) == 6) {
        output.append("⁶");
      } else if (Character.getNumericValue(input.charAt(i)) == 7) {
        output.append("⁷");
      } else if (Character.getNumericValue(input.charAt(i)) == 8) {
        output.append("⁸");
      } else if (Character.getNumericValue(input.charAt(i)) == 9) {
        output.append("⁹");
      }
    }
    return output.toString();
  }

  /**
   * Adds a datapoint with an intensity of 0 for the given mz at the beginning of the dataset.
   *
   * @param scan The scan the datapoint will be added to.
   * @param mz   The datapoints mz-value
   */
  private static void prependEmptyDataPoint(MassvoltammogramScan scan, double mz) {

    scan.setMzs(ArrayUtils.addAll(new double[]{mz}, scan.getMzs()));
    scan.setIntensities(ArrayUtils.addAll(new double[]{0d}, scan.getIntensities()));
  }

  /**
   * Adds a datapoint with an intensity of 0 for the given mz at the end of the dataset.
   *
   * @param scan The scan the datapoint will be added to.
   * @param mz   The datapoints mz-value.
   */
  private static void appendEmptyDataPoint(MassvoltammogramScan scan, double mz) {

    scan.setMzs(ArrayUtils.add(scan.getMzs(), mz));
    scan.setIntensities(ArrayUtils.add(scan.getIntensities(), 0));
  }
}




