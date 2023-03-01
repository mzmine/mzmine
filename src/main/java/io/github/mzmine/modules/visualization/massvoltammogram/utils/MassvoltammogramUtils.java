/*
 * Copyright 2006-2022 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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

      scan.setMzsFromList(mzs);
      scan.setIntensitiesFromList(intensities);
    }
  }

  /**
   * Adds datapoints with an intensity of 0 to the beginning and the end of each scan, so they start
   * and ent at the same m/z-value.
   *
   * @param scans The scans to be aligned.
   */
  public static void aligneScans(List<MassvoltammogramScan> scans) {

    //The min and max mz-values of the whole massvoltammogram the scans will be aligned to.
    final double minMz = getMinMZ(scans);
    final double maxMz = getMaxMZ(scans);

    //Going over all MassvoltammogramScans in the massvoltammogram.
    for (MassvoltammogramScan scan : scans) {

      //Adding a datapoint with intensity 0 at the beginning if the scans min mz is
      // bigger than the min mz in the massvoltammogram.
      if (scan.getMinMz() > minMz) {
        scan.setMzs(ArrayUtils.addAll(new double[]{minMz}, scan.getMzs()));
        scan.setIntensities(ArrayUtils.addAll(new double[]{0d}, scan.getIntensities()));
      }

      //Adding a datapoint with intensity 0 at the end if the scans max mz is smaller than
      //the max mz in the massvoltammogram.
      if (scan.getMaxMz() < maxMz) {

        scan.setMzs(ArrayUtils.add(scan.getMzs(), maxMz));
        scan.setIntensities(ArrayUtils.add(scan.getIntensities(), 0));
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
        if (scan.getIntensity(i) > intensityThreshold || scan.getIntensity(i) == 0) {
          filteredMZ.add(scan.getMz(i));           //Adding the m/z-value.
          filteredIntensities.add(scan.getIntensity(i));  //Adding the intensity-value.
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
  private static void removeExcessZeros(List<MassvoltammogramScan> scans) {

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
   * Method to get the min m/z-value from a list of scans.
   *
   * @param scans The scans.
   * @return Returns the minimal m/z-value.
   */
  private static double getMinMZ(List<MassvoltammogramScan> scans) {

    //Setting the absolute minimal m/z-value equal to the first scans minimal m/z-value.
    double absoluteMinMz = scans.get(0).getMinMz();

    //Checking all the other scans in the list weather there is an even smaller m/z-value.
    for (int i = 1; i < scans.size(); i++) {

      if (scans.get(i).getMinMz() < absoluteMinMz) {
        absoluteMinMz = scans.get(i).getMinMz();
      }
    }

    return absoluteMinMz;
  }


  /**
   * Method to get the max m/z-value from a list of scans.
   *
   * @param scans The scans.
   * @return Returns the maximal m/z-value.
   */
  private static double getMaxMZ(List<MassvoltammogramScan> scans) {

    //Setting the absolute maximal m/z-value equal to the first scans maximal m/z-value.
    double absoluteMaxMz = scans.get(0).getMaxMz();

    //Checking all the other scans in the list weather there is a bigger m/z-value.
    for (int i = 1; i < scans.size(); i++) {

      if (scans.get(i).getMaxMz() > absoluteMaxMz) {
        absoluteMaxMz = scans.get(i).getMaxMz();
      }
    }

    return absoluteMaxMz;
  }
}




