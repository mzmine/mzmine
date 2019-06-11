package net.sf.mzmine.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.openscience.cdk.interfaces.IIsotope;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopeCompositionResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopicPeakResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class IsotopePatternUtils2 {

  private static final Logger logger = Logger.getLogger(IsotopePatternUtils.class.getName());
  private static final NumberFormat format = MZmineCore.getConfiguration().getMZFormat();;

  /**
   * Finds data points with the best m/z differences (lowest) to a predicted isotope peak.
   * 
   * @param dp The base data point.
   * @param originalDataPoints Array of all peaks in consideration
   * @param i_dp the start index inside originalDataPoints to search for.
   * @param mzTolerance m/z Tolerance range.
   * @param isoMzDiff m/z difference added by an isotope peak.
   * @return
   */
  public static DataPoint findBestMZDiff(DataPoint dp, DataPoint[] originalDataPoints, int i_dp,
      MZTolerance mzTolerance, double isoMzDiff) {

    Double bestppm = null;
    DataPoint bestdp = null;

    // look for isotope peaks in the spectrum by ascending mz
    for (int j_p = i_dp + 1; j_p < originalDataPoints.length; j_p++) {

      DataPoint p = originalDataPoints[j_p];

      // if the data point p is below the mz tolerance range, we go for the next one.
      if (p.getMZ() < mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff).lowerEndpoint())
        continue;

      // if the m/z of this data point (p) is bigger than the m/z of (dp + pattern width +
      // merge) then we don't need to check anymore
      if (p.getMZ() > mzTolerance.getToleranceRange(dp.getMZ() + isoMzDiff).upperEndpoint())
        break;

      // now check the ppm difference and compare the mass differences
      double ppm = getPpmDiff(p.getMZ(), dp.getMZ() + isoMzDiff);
      if (bestppm == null) {
        bestppm = new Double(ppm);
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
   * @param dp the base peak with lowest m/z.
   * @param originalDataPoints All the data points in consideration for isotope peaks.
   * @param mzTolerance the m/z tolerance.
   * @param pattern The isotope pattern of an element to search for.
   * @param mzrange the range of m/z to search for isotope peaks.
   * @return dp will be modified and given an DPPIsotopePatternResult.
   */
  public static ProcessedDataPoint findIsotopicPeaks(ProcessedDataPoint dp,
      ProcessedDataPoint[] originalDataPoints, MZTolerance mzTolerance,
      ExtendedIsotopePattern pattern, Range<Double> mzrange, int maxCharge) {
    // dp is the peak we are currently searching an isotope pattern for

    if (maxCharge < 1 || !mzrange.contains(dp.getMZ()))
      return dp;

    int i_dp = ArrayUtils.indexOf(dp, originalDataPoints);

    int numIsotopes = pattern.getDataPoints().length;

    for (int i_charge = 1; i_charge <= maxCharge; i_charge++) {

      Double bestppm[] = new Double[numIsotopes];
      ProcessedDataPoint[] bestdp = new ProcessedDataPoint[numIsotopes];

      bestppm[0] = 0.0;
      bestdp[0] = dp;

      // in this loop we go though every isotope and check if we can find a peak that fits for
      // every isotope
      for (int isotopeindex = 1; isotopeindex < numIsotopes; isotopeindex++) {

        // this is the mass difference the current isotope peak would add to the base peak.
        double isoMzDiff =
            pattern.getDataPoints()[isotopeindex].getMZ() - pattern.getDataPoints()[0].getMZ();

        bestdp[isotopeindex] = (ProcessedDataPoint) findBestMZDiff(dp, originalDataPoints, i_dp,
            mzTolerance, isoMzDiff);
      } // end of isotopeindex loop

      // ok we finished looking for the isotope peaks, let's see what we found
      // check if results are good, we must have found a peak for every isotope of the current
      // element, else we have to discard the results
      for (int isotopeindex = 0; isotopeindex < numIsotopes; isotopeindex++)
        if (bestdp[isotopeindex] == null)
          return dp;

      // ok every peak has been found, now assign the pattern and link the data points
      // this adds the isotope to the assigned peak, so we can keep track of the elemental
      // composition later on
      for (int isotopeIndex = 1; isotopeIndex < numIsotopes; isotopeIndex++) { // TODO; changed to 1
                                                                               // here
        ProcessedDataPoint p = bestdp[isotopeIndex];
        dp.addResult(
            new DPPIsotopicPeakResult(p, pattern.getIsotopeComposition(isotopeIndex), i_charge));
      }

      // IsotopePattern patternx =
      // new SimpleIsotopePattern(bestdp, IsotopePatternStatus.DETECTED, pattern.getDescription());
      // // pattern.getDescription()
      // is
      // dp.addResult(new DPPIsotopePatternResult(patternx, bestdp, i_charge));
    }

    return dp;
  }

  // new-new
  public static void mergeIsotopicPeakResults(ProcessedDataPoint parent) {

    List<DPPIsotopicPeakResult> iprs = getIsotopicPeakResults(parent);

    if (iprs.isEmpty())
      return;

    List<Integer> charges = getChargeStates(iprs);

    for (DPPIsotopicPeakResult ipr : iprs) {
      ProcessedDataPoint child = ipr.getPeak();
      if (child == parent)
        continue;

      mergeIsotopicPeakResults(child);

      List<DPPIsotopicPeakResult> childIPRS = getIsotopicPeakResults(child);
      for (DPPIsotopicPeakResult childIPR : childIPRS) {

        if (charges.contains(childIPR.getCharge())) {
          // make new result, combine isotopes
          DPPIsotopicPeakResult newResult = new DPPIsotopicPeakResult(childIPR.getPeak(),
              ipr.getIsotope() + " " + childIPR.getIsotope(), childIPR.getCharge());

          parent.addResult(newResult);
          child.removeResult(childIPR);
          // childIPRS.remove(childIPR);
        }
      }
      // child.removeAllResultsByType(ResultType.ISOTOPICPEAK);
      // iprs.remove(ipr);
    }
  }

  /**
   * Returns a list of all DPPIsotopicPeakResults
   * 
   * @param dp the ProcessedDataPoint to gather the list from.
   * @return List of all results, empty if no such results exists.
   */
  public static @Nonnull List<DPPIsotopicPeakResult> getIsotopicPeakResults(
      @Nonnull ProcessedDataPoint dp) {
    List<DPPIsotopicPeakResult> results = new ArrayList<>();

    if (!dp.resultTypeExists(ResultType.ISOTOPICPEAK))
      return results;

    List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPICPEAK);

    for (int i = 0; i < patternResults.size(); i++)
      results.add((DPPIsotopicPeakResult) patternResults.get(i));

    return results;
  }

  /**
   * Takes all DPPIsotopicPeakResults of one charge and puts them into one isotope pattern (per
   * charge).
   * 
   * @param dp A ProcessedDataPoint
   * @param keepResults if false, all DPPIsotopicPeakResults will be removed from this data point.
   */
  public static void convertIsotopicPeakResultsToPattern(ProcessedDataPoint dp,
      boolean keepResults) {
    sortAndRemoveDuplicateIsotopicPeakResult(dp);
    List<DPPIsotopicPeakResult> iprs = getIsotopicPeakResults(dp);

    if (iprs.isEmpty())
      return;

    List<Integer> charges = getChargeStates(iprs);
    List<ProcessedDataPoint> peaks = new ArrayList<>();
    List<String> isotopes = new ArrayList<>();

    peaks.add(dp);
    isotopes.add("");

    for (int charge : charges) {
      for (DPPIsotopicPeakResult ipr : iprs) {
        if (ipr.getCharge() == charge) {
          peaks.add(ipr.getPeak());
          isotopes.add(ipr.getIsotope());
        }
      }
      ProcessedDataPoint dps[] = peaks.toArray(new ProcessedDataPoint[0]);
      String[] isos = isotopes.toArray(new String[0]);

      ExtendedIsotopePattern pattern =
          new ExtendedIsotopePattern(dps, IsotopePatternStatus.DETECTED,
              format.format(dp.getMZ()) + " (" + dp.getAllResultsByType(ResultType.ISOTOPICPEAK) + ")", isos);

      dp.addResult(new DPPIsotopePatternResult(pattern,
          (ProcessedDataPoint[]) pattern.getDataPoints(), charge));

      peaks.clear();
      isotopes.clear();
    }
    if (!keepResults)
      dp.removeAllResultsByType(ResultType.ISOTOPICPEAK);
  }

  public static void sortAndRemoveDuplicateIsotopicPeakResult(ProcessedDataPoint dp) {
    List<DPPIsotopicPeakResult> results = getIsotopicPeakResults(dp);

    for (int i = 0; i < results.size() - 1; i++) {
      DPPIsotopicPeakResult a = results.get(i);
      for (int j = i + 1; j < results.size(); j++) {
        DPPIsotopicPeakResult b = results.get(j);
        if (a.getPeak() == b.getPeak()) {
//          logger.info("removed duplicates at positions " + i + ", " + j);
          results.remove(a);
        }
      }
    }
    
    Collections.sort(results, (o1, o2) -> {
      return Double.compare(o1.getPeak().getMZ(), o2.getPeak().getMZ());      
    });
    dp.removeAllResultsByType(ResultType.ISOTOPICPEAK);
    for(DPPIsotopicPeakResult r : results)
      dp.addResult(r);
  }

  // old-new
  public static void mergeIsotopePatternResults(ProcessedDataPoint dp) {
    if (!dp.resultTypeExists(ResultType.ISOTOPEPATTERN))
      return;

    List<DPPIsotopePatternResult> patternResults = getIsotopePatternResuls(dp);
    List<DPPResult<?>> newResults = new ArrayList<>();

    for (DPPIsotopePatternResult dpPatternResult : patternResults) {
      ProcessedDataPoint[] dpPattern = dpPatternResult.getLinkedDataPoints();

      int patternCharge = dpPatternResult.getCharge();

      for (ProcessedDataPoint p : dpPattern) {
        List<DPPIsotopePatternResult> pPatternResults = getIsotopePatternResuls(p);

        for (DPPIsotopePatternResult pPatternResult : pPatternResults) {
          if (pPatternResult.getCharge() != patternCharge)
            continue;

          ProcessedDataPoint[] dataPoints = pPatternResult.getLinkedDataPoints();
          p.removeResult(pPatternResult);

          newResults.add(new DPPIsotopePatternResult(
              new SimpleIsotopePattern(dataPoints, IsotopePatternStatus.DETECTED, ""), dataPoints,
              patternCharge));
        }
      }
    }

    dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);
    dp.addAllResults(newResults);

    logger.finest("-------------------------");
    for (DPPResult<?> result : newResults)
      logger.finest("FINAL: " + format.format(dp.getMZ()) + " pattern: "
          + getResultIsoComp((DPPIsotopePatternResult) result));

    // TODO: test
  }

  public static @Nonnull List<DPPIsotopePatternResult> getIsotopePatternResuls(
      @Nonnull ProcessedDataPoint dp) {
    List<DPPIsotopePatternResult> results = new ArrayList<>();

    if (!dp.resultTypeExists(ResultType.ISOTOPEPATTERN))
      return results;

    List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);

    for (int i = 0; i < patternResults.size(); i++)
      results.add((DPPIsotopePatternResult) patternResults.get(i));

    return results;
  }

  /**
   * 
   * @param dp a processed data point.
   * @return an empty list if no isotope pattern was detected, a list of the charge states if there
   *         was at least one charge detected.
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

  public static @Nonnull List<Integer> getChargeStates(List<DPPIsotopicPeakResult> iprs) {
    List<Integer> charges = new ArrayList<Integer>();

    for (DPPIsotopicPeakResult ipr : iprs) {
      if (!charges.contains(ipr.getCharge()))
        charges.add(ipr.getCharge());
    }

    return charges;
  }

  public static double getPpmDiff(double realmz, double calcmz) {
    return 10E6 * (realmz - calcmz) / calcmz;
  }

  private static String getResultIsoComp(DPPIsotopePatternResult result) {
    String str = "";
    for (ProcessedDataPoint dp : result.getLinkedDataPoints()) {
      String c = "";
      DPPIsotopeCompositionResult comps =
          (DPPIsotopeCompositionResult) dp.getFirstResultByType(ResultType.ISOTOPECOMPOSITION);
      for (String comp : comps.getValue())
        c += comp + ", ";
      if (c.length() > 2)
        c = c.substring(0, c.length() - 2);
      str += format.format(dp.getMZ()) + " (" + c + "), ";
    }
    str = str.substring(0, str.length() - 2);
    return str;
  }

  public static IsotopePattern checkOverlappingIsotopes(IsotopePattern pattern, IIsotope[] isotopes,
      double mergeWidth, double minAbundance) {
    DataPoint[] dp = pattern.getDataPoints();
    double basemz = dp[0].getMZ();
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
      if (isotope.getNaturalAbundance() < minAbundance)
        continue;
      // the difference added by the heavier isotope peak
      double possiblemzdiff = isotope.getExactMass() - isotopeBaseMass;
      if (possiblemzdiff < 0.000001)
        continue;
      boolean add = true;
      for (DataPoint patternDataPoint : dp) {
        // here check for every peak in the pattern, if a new peak would overlap
        // if it overlaps good, we dont need to add a new peak

        int i = 1;
        do {
          if (Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff) <= mergeWidth) {
            // TODO: maybe we should do a average of the masses? i can'T say if it makes sense,
            // since
            // we're just looking for isotope mass differences and dont look at the total
            // composition,
            // so we dont know the intensity ratios
            logger.info("possible overlap found: " + i + " * pattern dp = "
                + patternDataPoint.getMZ() + "\toverlaps with " + isotope.getMassNumber()
                + isotope.getSymbol() + " (" + (isotopeBaseMass - isotope.getExactMass())
                + ")\tdiff: " + Math.abs(patternDataPoint.getMZ() * i - possiblemzdiff));
            add = false;
          }
          i++;
          // logger.info("do");
        } while (patternDataPoint.getMZ() * i <= possiblemzdiff + mergeWidth
            && patternDataPoint.getMZ() != 0.0);
      }

      if (add)
        newPeaks.add(new SimpleDataPoint(possiblemzdiff, 1));
    }

    // now add all new mzs to the isotopePattern
    // DataPoint[] newDataPoints = new SimpleDataPoint[dp.length + newPeaks.size()];
    for (DataPoint p : dp) {
      newPeaks.add(p);
    }
    newPeaks.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return new SimpleIsotopePattern(newPeaks.toArray(new DataPoint[0]),
        IsotopePatternStatus.PREDICTED, "");
  }
}
