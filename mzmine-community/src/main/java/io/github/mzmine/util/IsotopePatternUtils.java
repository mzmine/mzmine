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

package io.github.mzmine.util;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import io.github.mzmine.datamodel.impl.MultiChargeStateIsotopePattern;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopicPeakResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IIsotope;

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class IsotopePatternUtils {

  public static final double C13_MZ = 13.003355;
  public static final double C13_MZ_DELTA = 1.003355;
  public static final double C13_REL_ABUNDANCE = 0.0108;
  private static final Logger logger = Logger.getLogger(IsotopePatternUtils.class.getName());
  private static final NumberFormat format = MZmineCore.getConfiguration().getMZFormat();

  /**
   * Finds data points with the best m/z differences (lowest) to a predicted isotope peak.
   *
   * @param dp                 The base data point.
   * @param originalDataPoints Array of all peaks in consideration
   * @param i_dp               the start index inside originalDataPoints to search for.
   * @param mzTolerance        m/z Tolerance range.
   * @param isoMzDiff          m/z difference added by an isotope peak.
   * @return
   */
  public static DataPoint findBestMZDiff(DataPoint dp, DataPoint[] originalDataPoints, int i_dp,
      MZTolerance mzTolerance, double isoMzDiff) {

    Double bestppm = null;
    DataPoint bestdp = null;

    // look for isotope peaks in the spectrum by ascending mz
    for (int j_p = i_dp + 1; j_p < originalDataPoints.length; j_p++) {

      DataPoint p = originalDataPoints[j_p];

      // if the data point p is below the mz tolerance range, we go for
      // the next one.
      if (p.getMZ() < mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff).lowerEndpoint()) {
        continue;
      }

      // if the m/z of this data point (p) is bigger than the m/z of (dp +
      // pattern width +
      // merge) then we don't need to check anymore
      if (p.getMZ() > mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff).upperEndpoint()) {
        break;
      }

      // now check the ppm difference and compare the mass differences
      double ppm = getPpmDiff(p.getMZ(), dp.getMZ() + isoMzDiff);
      if (bestppm == null) {
        bestppm = ppm;
        bestdp = p;
      } else if (bestppm != null && Math.abs(ppm) < Math.abs(bestppm.doubleValue())) {
        bestppm = ppm;
        bestdp = p;
      }

    } // end of ascending datapoint mz loop
    // now we checked all peaks upcoming peaks in the spectrum

    return bestdp;
  }

  /**
   * Searches for an isotopic peaks (pattern) of the data point dp within an array of data points by
   * the elements m/z differences.
   *
   * @param dp                 the base peak with lowest m/z.
   * @param originalDataPoints All the data points in consideration for isotope peaks.
   * @param mzTolerance        the m/z tolerance.
   * @param pattern            The isotope pattern of an element to search for.
   * @param mzrange            the range of m/z to search for isotope peaks.
   * @return dp will be modified and given an DPPIsotopePatternResult.
   */
  public static ProcessedDataPoint findIsotopicPeaks(ProcessedDataPoint dp,
      ProcessedDataPoint[] originalDataPoints, MZTolerance mzTolerance,
      SimpleIsotopePattern pattern, Range<Double> mzrange, int maxCharge) {
    // dp is the peak we are currently searching an isotope pattern for

    if (maxCharge < 1 || !mzrange.contains(dp.getMZ())) {
      return dp;
    }

    int i_dp = ArrayUtils.indexOf(dp, originalDataPoints);

    int numIsotopes = pattern.getNumberOfDataPoints();

    for (int i_charge = 1; i_charge <= maxCharge; i_charge++) {

      Double bestppm[] = new Double[numIsotopes];
      ProcessedDataPoint[] bestdp = new ProcessedDataPoint[numIsotopes];

      bestppm[0] = 0.0;
      bestdp[0] = dp;

      // in this loop we go though every isotope and check if we can find
      // a peak that fits for
      // every isotope
      for (int isotopeindex = 1; isotopeindex < numIsotopes; isotopeindex++) {

        // this is the mass difference the current isotope peak would
        // add to the base peak.
        double isoMzDiff = pattern.getMzValue(isotopeindex) - pattern.getMzValue(0);

        bestdp[isotopeindex] = (ProcessedDataPoint) findBestMZDiff(dp, originalDataPoints, i_dp,
            mzTolerance, isoMzDiff);
      } // end of isotopeindex loop

      // ok we finished looking for the isotope peaks, let's see what we
      // found
      // check if results are good, we must have found a peak for every
      // isotope of the current
      // element, else we have to discard the results
      for (int isotopeindex = 0; isotopeindex < numIsotopes; isotopeindex++) {
        if (bestdp[isotopeindex] == null) {
          return dp;
        }
      }

      // ok every peak has been found, now assign the pattern and link the
      // data points
      // this adds the isotope to the assigned peak, so we can keep track
      // of the elemental
      // composition later on
      for (int isotopeIndex = 1; isotopeIndex < numIsotopes; isotopeIndex++) { // TODO;
        // changed
        // to
        // 1
        // here
        ProcessedDataPoint p = bestdp[isotopeIndex];
        dp.addResult(
            new DPPIsotopicPeakResult(p, pattern.getIsotopeComposition(isotopeIndex), i_charge));
      }
    }

    return dp;
  }

  /**
   * Scans a ProcessedDataPoint (= parent) for DPPIsotopicPeakResults. If results exist, these
   * results are scanned for isotopic peak results, too. If results exist here aswell, they are
   * merged into the parent. This method recursively calls itself and will merge all results into
   * the parent peak. The results of the child peaks will be removed, the isotopic composition is
   * updated on every merge step, making it possible to be evaluated in later steps.
   * <p>
   * Please note, that on bigger isotope patterns the parent might contain a peak twice. This has
   * the following reason (e.g.:) Let's assume an isotope pattern of C1 Cl1
   * <p>
   * This isotope pattern will have the following compositions: A: 12C, 35Cl
   * <p>
   * B: 13C, 35Cl
   * <p>
   * C: 12C, 37Cl D: 13C, 37Cl
   * <p>
   * When using the findIsotopicPeaks method, the following assignments will be made:
   * <p>
   * A -> B (13C of A) A -> C (37Cl of A) B -> D (37Cl of B) C -> D (13C of C)
   * <p>
   * As you can see, D has been assigned twice. This is correct behaviour of the method, but if the
   * convertIsotopicPeakResultsToPattern method was called now, it would contain peak D twice, even
   * though there was only one peak. Comparing isotope patterns now would lead to wrong results.
   * This is why the use of sortAndRemoveDuplicateIsotopicPeakResults before converting to an
   * isotope pattern is recommended.
   *
   * @param parent The peak to process
   */
  public static void mergeIsotopicPeakResults(ProcessedDataPoint parent) {

    List<DPPIsotopicPeakResult> iprs = getIsotopicPeakResults(parent);

    if (iprs.isEmpty()) {
      return;
    }

    List<Integer> charges = getChargeStates(iprs);

    for (DPPIsotopicPeakResult ipr : iprs) {
      ProcessedDataPoint child = ipr.getValue();
      if (child == parent) {
        continue;
      }

      mergeIsotopicPeakResults(child);

      List<DPPIsotopicPeakResult> childIPRS = getIsotopicPeakResults(child);
      for (DPPIsotopicPeakResult childIPR : childIPRS) {

        if (charges.contains(childIPR.getCharge())) {
          // make new result, combine isotopes
          DPPIsotopicPeakResult newResult = new DPPIsotopicPeakResult(childIPR.getValue(),
              ipr.getIsotope() + "" + childIPR.getIsotope(), childIPR.getCharge());

          parent.addResult(newResult);
          child.removeResult(childIPR);
        }
      }
    }
  }

  /**
   * Takes all DPPIsotopicPeakResults of one charge and puts them into one isotope pattern (per
   * charge).
   *
   * @param dp          A ProcessedDataPoint
   * @param keepResults if false, all DPPIsotopicPeakResults will be removed from this data point.
   */
  public static void convertIsotopicPeakResultsToPattern(ProcessedDataPoint dp,
      boolean keepResults) {
    sortAndRemoveDuplicateIsotopicPeakResult(dp);
    List<DPPIsotopicPeakResult> iprs = getIsotopicPeakResults(dp);

    if (iprs.isEmpty()) {
      return;
    }

    List<Integer> charges = getChargeStates(iprs);
    List<ProcessedDataPoint> peaks = new ArrayList<>();
    List<String> isotopes = new ArrayList<>();

    peaks.add(dp);
    isotopes.add("");

    for (int charge : charges) {
      for (DPPIsotopicPeakResult ipr : iprs) {
        if (ipr.getCharge() == charge) {
          peaks.add(ipr.getValue());
          isotopes.add(mergeIsotopicPeakDescription(ipr.getIsotope()));
        }
      }
      ProcessedDataPoint dps[] = peaks.toArray(new ProcessedDataPoint[0]);
      String[] isos = isotopes.toArray(new String[0]);

      SimpleIsotopePattern pattern = new SimpleIsotopePattern(dps, 1, IsotopePatternStatus.DETECTED,
          format.format(dp.getMZ()) /* + Arrays.toString(isos) */, isos);

      dp.addResult(new DPPIsotopePatternResult(pattern, dps, charge));

      peaks.clear();
      isotopes.clear();
    }
    if (!keepResults) {
      dp.removeAllResultsByType(ResultType.ISOTOPICPEAK);
    }
  }

  public static String makePatternSuggestion(String[] composition) {

    String[] isotopes = getIsotopesFromComposition(composition);

    String[] elements = getElementsFromIsotopes(isotopes);

    int[] maxNumIsos = getIsotopeOccurrence(composition, isotopes);

    // System.out.println(Arrays.toString(isotopes));
    // System.out.println(Arrays.toString(maxNumIsos));
    // System.out.println(Arrays.toString(elements));

    // match elements and isotope count

    int[] elementCount = new int[elements.length];
    for (int i = 0; i < elements.length; i++) {
      String element = elements[i];
      for (int j = 0; j < isotopes.length; j++) {
        String isotope = isotopes[j];
        if (isotope.endsWith(element)) {
          if (elementCount[i] < maxNumIsos[j]) {
            // System.out.println("Isotope count of " + isotope +
            // "(" + maxNumIsos[j] + ")"
            // + " is bigger than element count of" + element + "("
            // + elementCount[i] + ")");
            elementCount[i] = maxNumIsos[j];
          }
        }
      }
    }

    String formula = "";
    for (int i = 0; i < elements.length; i++) {
      formula += elements[i] + elementCount[i];
    }

    return formula;
  }

  /**
   * Compresses the peak description from [37]Cl[37]Cl to [37]Cl2
   *
   * @param descr
   * @return
   */
  public static String mergeIsotopicPeakDescription(String descr) {

    HashSet<String> set = new HashSet<>();

    String merged = "";

    String[] isotopes = descr.split(Pattern.quote("["));
    for (String isotope : isotopes) {
      set.add("[" + isotope);
    }
    set.remove("[");

    for (String str : set) {
      String count = "";
      int c = StringUtils.countMatches(descr, str);
      if (c > 1) {
        count = String.valueOf(c);
      }

      merged += str + count;
    }
    return merged;
  }

  /**
   * Returns the maximum number of isotope occurrences within a full isotope pattern.
   *
   * @param comps
   * @param isotopes
   * @return
   */
  public static int[] getIsotopeOccurrence(String[] comps, String[] isotopes) {
    int[] max = new int[isotopes.length];
    int[] counts = new int[isotopes.length];

    for (int x = 0; x < comps.length; x++) {
      String comp = comps[x];
      for (int i = 0; i < isotopes.length; i++) {

        if (comp.contains(isotopes[i])) {
          String[] str = comp.split(Pattern.quote(isotopes[i]));
          if (str.length > 1 && !str[1].startsWith("[")) {
            int end = str[1].indexOf("[");
            if (end < 0) {
              end = str[1].length();
            }
            counts[i] = Integer.valueOf(str[1].substring(0, end));
          } else {
            counts[i] = 1;
          }
        }

        if (counts[i] > max[i]) {
          max[i] = counts[i];
        }
      }
    }

    return max;
  }

  /**
   * @param comps Isotope composition of an isotope pattern in the format [13]C[37]Cl[13]C
   * @return Array of all occurring isotopes within comp
   */
  public static String[] getIsotopesFromComposition(String[] comps) {
    HashSet<String> set = new HashSet<>();

    for (String comp : comps) {
      // the elements are allocated between ] [ e.g.: [13]C[37]Cl[13]C
      String[] isotopes = comp.split(Pattern.quote("["));
      for (String isotope : isotopes) {
        isotope = StringUtils.stripEnd(isotope, "0123456789");
        set.add("[" + isotope);
      }
    }
    set.remove("["); // gets added by default due to split, removing should
    // be faster than a check
    return set.toArray(new String[0]);
  }

  /**
   * @param isotopes Array of strings, each String must contain one expression like [37]Cl
   * @return Array of only element strings, no duplicates
   */
  public static String[] getElementsFromIsotopes(String[] isotopes) {
    HashSet<String> set = new HashSet<>();

    for (String isotope : isotopes) {
      // the elements are allocated between ] [ e.g.: [13]C[37]Cl[13]C
      int close = isotope.indexOf(']');
      String element = isotope.substring(close + 1);
      set.add(element);
    }
    return set.toArray(new String[0]);
  }

  public static void main(String[] args) {
    String[] str = {"", "[13]C", "[13]C2", "[37]Cl", "[13]C[37]Cl", "[37]Cl2[13]C"};
    String[] i = getIsotopesFromComposition(str);
    String[] e = getElementsFromIsotopes(i);
    int[] c = getIsotopeOccurrence(str, i);

    // System.out.println(Arrays.toString(str));
    // System.out.println(Arrays.toString(i));
    // System.out.println(Arrays.toString(c));
    // System.out.println(Arrays.toString(e));

    makePatternSuggestion(str);
    // System.out.println(mergeIsotopicPeakDescription(str[0]));
  }

  /**
   * Sorts DPPIsotopicPeakResults by m/z and removes duplicates
   *
   * @param dp
   */
  public static void sortAndRemoveDuplicateIsotopicPeakResult(ProcessedDataPoint dp) {
    List<DPPIsotopicPeakResult> results = getIsotopicPeakResults(dp);

    Collections.sort(results, (o1, o2) -> {
      return Double.compare(o1.getValue().getMZ(), o2.getValue().getMZ());
    });

    for (int i = 0; i < results.size() - 1; i++) {
      DPPIsotopicPeakResult a = results.get(i);
      DPPIsotopicPeakResult b = results.get(i + 1);
      if (a.getValue() == b.getValue()) {
        // logger.info("removed duplicates at positions " + i + ", " +
        // j);
        results.remove(a);
      }
    }

    dp.removeAllResultsByType(ResultType.ISOTOPICPEAK);
    for (DPPIsotopicPeakResult r : results) {
      dp.addResult(r);
    }
  }

  /**
   * Returns a list of all DPPIsotopicPeakResults
   *
   * @param dp the ProcessedDataPoint to gather the list from.
   * @return List of all results, empty if no such results exists.
   */
  public static @NotNull List<DPPIsotopicPeakResult> getIsotopicPeakResults(
      @NotNull ProcessedDataPoint dp) {
    List<DPPIsotopicPeakResult> results = new ArrayList<>();

    if (!dp.resultTypeExists(ResultType.ISOTOPICPEAK)) {
      return results;
    }

    List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPICPEAK);

    for (int i = 0; i < patternResults.size(); i++) {
      results.add((DPPIsotopicPeakResult) patternResults.get(i));
    }

    return results;
  }

  /**
   * @param dp a processed data point.
   * @return an empty list if no isotope pattern was detected, a list of the charge states if there
   * was at least one charge detected.
   */
  public static List<Integer> getChargeStates(ProcessedDataPoint dp) {
    List<Integer> charges = new ArrayList<>();

    List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);
    for (int x = 0; x < patternResults.size(); x++) {
      DPPIsotopePatternResult pattern = (DPPIsotopePatternResult) patternResults.get(x);

      boolean add = true;
      for (int i = 0; i < charges.size(); i++) {
        if (charges.get(i).intValue() == pattern.getCharge()) {
          add = false;
        }
      }
      if (add) {
        charges.add(pattern.getCharge());
      }
    }

    return charges;
  }

  public static @NotNull List<Integer> getChargeStates(List<DPPIsotopicPeakResult> iprs) {
    List<Integer> charges = new ArrayList<Integer>();

    for (DPPIsotopicPeakResult ipr : iprs) {
      if (!charges.contains(ipr.getCharge())) {
        charges.add(ipr.getCharge());
      }
    }

    return charges;
  }

  public static double getPpmDiff(double realmz, double calcmz) {
    return 10E6 * (realmz - calcmz) / calcmz;
  }

  public static IsotopePattern checkOverlappingIsotopes(IsotopePattern pattern, IIsotope[] isotopes,
      double mergeWidth, double minAbundance) {
    double basemz = pattern.getMzValue(0);
    List<DataPoint> newPeaks = new ArrayList<DataPoint>();

    double isotopeBaseMass = 0d;
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() > minAbundance) {
        isotopeBaseMass = isotope.getExactMass();
        logger.info("isotopeBaseMass of " + isotope.getSymbol() + " = " + isotopeBaseMass);
        break;
      }
    }

    // loop all new isotopes
    for (IIsotope isotope : isotopes) {
      if (isotope.getNaturalAbundance() < minAbundance) {
        continue;
      }
      // the difference added by the heavier isotope peak
      double possiblemzdiff = isotope.getExactMass() - isotopeBaseMass;
      if (possiblemzdiff < 0.000001) {
        continue;
      }
      boolean add = true;
      for (DataPoint patternDataPoint : pattern) {
        // here check for every peak in the pattern, if a new peak would
        // overlap
        // if it overlaps good, we dont need to add a new peak

        int i = 1;
        do {
          if (Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff) <= mergeWidth) {
            // TODO: maybe we should do a average of the masses? i
            // can'T say if it makes sense,
            // since
            // we're just looking for isotope mass differences and
            // dont look at the total
            // composition,
            // so we dont know the intensity ratios
            logger.info(
                "possible overlap found: " + i + " * pattern dp = " + patternDataPoint.getMZ()
                + "\toverlaps with " + isotope.getMassNumber() + isotope.getSymbol() + " (" + (
                    isotopeBaseMass - isotope.getExactMass()) + ")\tdiff: " + Math.abs(
                    patternDataPoint.getMZ() * i - possiblemzdiff));
            add = false;
          }
          i++;
          // logger.info("do");
        } while (patternDataPoint.getMZ() * i <= possiblemzdiff + mergeWidth
                 && patternDataPoint.getMZ() != 0.0);
      }

      if (add) {
        newPeaks.add(new SimpleDataPoint(possiblemzdiff, 1));
      }
    }

    // now add all new mzs to the isotopePattern
    // DataPoint[] newDataPoints = new SimpleDataPoint[dp.length +
    // newPeaks.size()];
    // for (DataPoint p : dp) {
    // newPeaks.add(p);
    // }
    newPeaks.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return new SimpleIsotopePattern(newPeaks.toArray(new DataPoint[0]), pattern.getCharge(),
        IsotopePatternStatus.PREDICTED, "");
  }

  public static boolean check13CPattern(IsotopePattern pattern, double mainMZ, MZTolerance mzTol,
      int maxCharge) {
    return check13CPattern(pattern, mainMZ, mzTol, maxCharge, true, null, true);
  }

  /**
   * Evaluate if mainMZ defines the monoisotopic (or at least the smallest detected isotope) by
   * checking the +1 signal with 13C isotope abundances and delta m/z. Returns false, if the pattern
   * contains a 13C isotope with matching abundance preceding the main signal.
   *
   * @param pattern                   the isotope pattern
   * @param mainMZ                    the row m/z that should be the monoisotopic m/z
   * @param mzTol                     tolerance to match signals. All signals in range will be
   *                                  considered - only one has to match the criteria
   * @param maxCharge                 maximum allowed charge. will test charge 1<=max
   * @param excludeIfMainIs13CIsotope return false if mainMZ is found to be a 13C isotope. Checks
   *                                  the -1 signal mz and intensity
   * @param excludedMzDiffs           option to exclude specific isotopes. {@link
   *                                  IsotopesUtils#getIsotopeRecord(String, int)} Method will
   *                                  return false when a preceding signal is found matching the
   *                                  intensity and m/z difference of a provided isotope (m/z
   *                                  difference to the maximum abundant isotope)
   * @param applyMinCEstimation       uses some estimates for minimum number of C atoms derived from
   *                                  COCONUT database. Should include at least 99.9 % of formulas
   * @return only true if the +1 peak for 13C isotope is found. false otherwise or if there is a
   * preceding 13C isotope or one of the excluded isotopes (e.g., 18O)
   */
  public static boolean check13CPattern(IsotopePattern pattern, double mainMZ, MZTolerance mzTol,
      int maxCharge, boolean excludeIfMainIs13CIsotope, @Nullable Isotope[] excludedMzDiffs,
      boolean applyMinCEstimation) {

    if (pattern instanceof SimpleIsotopePattern simple && simple.getCharge() > 0) {
      // handle one isotope patter with defined charge state - only use this charge state
      return check13CPatternForChargeState(pattern, pattern.getCharge(), mainMZ, mzTol,
          excludeIfMainIs13CIsotope, excludedMzDiffs, applyMinCEstimation);
    } else if (pattern instanceof MultiChargeStateIsotopePattern multi) {
      for (IsotopePattern patternForCharge : multi.getPatterns()) {
        // if for any pattern a 13C isotope pattern was detected: true, return true
        if (check13CPatternForChargeState(patternForCharge, patternForCharge.getCharge(), mainMZ,
            mzTol, excludeIfMainIs13CIsotope, excludedMzDiffs, applyMinCEstimation)) {
          return true;
        }
      }
    } else {
      // look at +1 peak for 13C
      // exclude all -1 peaks from excludedMzDiffs
      // if for any pattern a 13C isotope pattern was detected: true, return true
      for (int charge = 1; charge <= maxCharge; charge++) {
        if (check13CPatternForChargeState(pattern, charge, mainMZ, mzTol, excludeIfMainIs13CIsotope,
            excludedMzDiffs, applyMinCEstimation)) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean check13CPatternForChargeState(IsotopePattern pattern, int charge,
      double mainMZ, MZTolerance mzTol, boolean excludeIfMainIs13CIsotope,
      Isotope[] excludedMzDiffs, boolean applyMinCEstimation) {
    int maxIndex = findMaxIndex(pattern, mainMZ, mzTol, 0);
    if (maxIndex < 0) {
      // should be there - maybe picked with different mz tol
      logger.warning(
          () -> String.format("No main signal found for isotope pattern of main m/z=%.4f, mzTol=%s",
              mainMZ, mzTol));
      // no main peak found
      return false;
    }

    // highest main signal
    final double newMainMZ = pattern.getMzValue(maxIndex);
    final double mainHeight = pattern.getIntensityValue(maxIndex);

    // looks at a -1 peak
    if (excludedMzDiffs != null) {
      for (Isotope excludedMzDiff : excludedMzDiffs) {
        // open limits for the minimum ratio - which defines the maximum intensity of the preceding signal
        double minRatio = excludedMzDiff.relativeIntensity() * 0.2;
        double maxRatio =
            estimateMaxXAtoms(excludedMzDiff, mainMZ, charge) * excludedMzDiff.relativeIntensity()
            * 1.15;
        double isotopeMZ = newMainMZ - (excludedMzDiff.deltaMass() / charge);
        Range<Double> estimatedIntensityRange = Range.closed(mainHeight / maxRatio,
            mainHeight / minRatio);
        boolean mainIsIsotopeSignal = hasSignalMatchingIntensityRange(pattern, isotopeMZ, mzTol,
            estimatedIntensityRange, maxIndex - 1, -1);
        if (mainIsIsotopeSignal) {
          return false;
        }
      }
    }

    // estimate min max carbons
    double estimatedMinC = applyMinCEstimation ? estimateMinCAtoms(newMainMZ, charge) : 2;
    double estimatedMaxC = estimateMaxCAtoms(newMainMZ, charge);

    // only possible if mass defect is too high (e.g., for multiply charged)
    if (estimatedMinC >= estimatedMaxC) {
      return false;
    }

    // add some tolerance on lower and upper bounds
    double minRatio = estimatedMinC * C13_REL_ABUNDANCE * 0.85;
    double maxRatio = estimatedMaxC * C13_REL_ABUNDANCE * 1.15;

    // if signal has preceeding 13C signal within intensity range - flag as isotope and return false
    if (excludeIfMainIs13CIsotope) {
      Range<Double> estimatedIntensityRange = Range.closed(mainHeight / maxRatio,
          mainHeight / minRatio);
      double isotopeMZ = newMainMZ - (C13_MZ_DELTA / charge);
      boolean mainIsIsotopeSignal = hasSignalMatchingIntensityRange(pattern, isotopeMZ, mzTol,
          estimatedIntensityRange, maxIndex - 1, -1);
      if (mainIsIsotopeSignal) {
        return false;
      }
    }

    // check if this signal is actually the +1 peak
    // +1 carbon isotope peak
    // when found - still check if other condition for other charge states result in false
    // when charge state is 2, the +2 13C signal may be found as a potential +1 13C signal
    double isotopeMZ = newMainMZ + (C13_MZ_DELTA / charge);
    Range<Double> estimatedIntensityRange = Range.closed(mainHeight * minRatio,
        mainHeight * maxRatio);
    return hasSignalMatchingIntensityRange(pattern, isotopeMZ, mzTol, estimatedIntensityRange,
        maxIndex + 1, 1);
  }

  private static double estimateMaxXAtoms(Isotope isotope, double mz, int charge) {
    if (mz <= 0) {
      return -1;
    }
    // just estimate a very high number of possible isotopes
    // 2 isotopes per carbon
    return mz * charge / (6 + isotope.exactMass());
  }

  private static double estimateMaxCAtoms(double mz, int charge) {
    if (mz <= 0) {
      return -1;
    }
    // highly aromatic rings C + 0.1 H (just an estimate)
    // Kind and Fiehn 7 golden rules 0.1 <= H/C <= 6
    return mz / 12.1 * charge;
  }

  private static double estimateMinCAtoms(double mz, int charge) {
    if (mz <= 0) {
      return -1;
    }
    // linear equation through lower bound of COCONUT data base m/z values and min carbon
    // contains 99.9% of structures
    double minCarbon = mz * 0.0225 - 5;

    // linear equation for mass defect only below 0.5
    // contains 99.9% of structures
    final double massDefect = mz - Math.floor(mz);
    if (massDefect <= 0.5 && charge == 1) {
      minCarbon = Math.max(minCarbon, massDefect * 55 - 4);
    }
    return Math.max(2, minCarbon) * charge;
  }

  private static int findMaxIndex(IsotopePattern pattern, double mainMZ, MZTolerance mzTol,
      int startIndex) {
    final Range<Double> mainMZrange = mzTol.getToleranceRange(mainMZ);
    int maxIndex = -1;
    double maxIntensity = -1;
    final int size = pattern.getNumberOfDataPoints();
    for (int index = startIndex; index < size; index++) {
      double mz = pattern.getMzValue(index);
      if (mz < mainMZrange.lowerEndpoint()) {
        continue;
      } else if (mz > mainMZrange.upperEndpoint()) {
        break;
      } else {
        double intensity = pattern.getIntensityValue(index);
        if (intensity >= maxIntensity) {
          maxIntensity = intensity;
          maxIndex = index;
        }
      }
    }
    return maxIndex;
  }

  /**
   * @param pattern
   * @param mainMZ
   * @param mzTol
   * @param startIndex
   * @return an index range that contains all
   */
  private static IntRange findIndexRange(IsotopePattern pattern, double mainMZ, MZTolerance mzTol,
      int startIndex) {
    return findIndexRange(pattern, mainMZ, mzTol, startIndex, 1);
  }

  private static IntRange findIndexRange(IsotopePattern pattern, double mainMZ, MZTolerance mzTol,
      int startIndex, int direction) {
    final Range<Double> mainMZrange = mzTol.getToleranceRange(mainMZ);
    final int size = pattern.getNumberOfDataPoints();
    int firstMatch = -1;
    for (int index = startIndex; index < size && index >= 0; index += direction) {
      double mz = pattern.getMzValue(index);
      if (mz < mainMZrange.lowerEndpoint()) {
        continue;
      } else if (mz > mainMZrange.upperEndpoint()) {
        return new IntRange(firstMatch, index);
      } else if (firstMatch == -1) {
        firstMatch = index;
      }
    }
    return firstMatch == -1 ? IntRange.EMPTY : new IntRange(firstMatch, size);
  }

  /**
   * Search for signals in mz and intensity ranges
   *
   * @param startIndex start at this index
   * @param direction  iterate in this direction (-1 or +1)
   * @return true if signal found in mz and intensity ranges. false otherwise
   */
  private static boolean hasSignalMatchingIntensityRange(IsotopePattern pattern, double searchMZ,
      MZTolerance mzTol, Range<Double> intensityRange, int startIndex, int direction) {
    final Range<Double> mainMZrange = mzTol.getToleranceRange(searchMZ);
    final int size = pattern.getNumberOfDataPoints();
    if (direction > 0) {
      for (int index = startIndex; index < size; index++) {
        double mz = pattern.getMzValue(index);
        if (mz < mainMZrange.lowerEndpoint()) {
          continue;
        } else if (mz > mainMZrange.upperEndpoint()) {
          // no signal found
          return false;
        } else if (intensityRange.contains(pattern.getIntensityValue(index))) {
          // in intensity range, in mz range
          return true;
        }
      }
    } else {
      // find preceeding signals
      for (int index = startIndex; index >= 0; index--) {
        double mz = pattern.getMzValue(index);
        if (mz < mainMZrange.lowerEndpoint()) {
          return false;
        } else if (mz > mainMZrange.upperEndpoint()) {
          // no signal found
          continue;
        } else if (intensityRange.contains(pattern.getIntensityValue(index))) {
          // in intensity range, in mz range
          return true;
        }
      }
    }
    return false;
  }
}
