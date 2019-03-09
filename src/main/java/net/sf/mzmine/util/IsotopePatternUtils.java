package net.sf.mzmine.util;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import com.google.common.collect.Range;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.IsotopePattern.IsotopePatternStatus;
import net.sf.mzmine.datamodel.impl.ExtendedIsotopePattern;
import net.sf.mzmine.datamodel.impl.SimpleIsotopePattern;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopeCompositionResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPIsotopePatternResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult;
import net.sf.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult.ResultType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class IsotopePatternUtils {

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
   * Searches for an isotope pattern (pattern) of the data point dp within an array of data points
   * by the patterns m/z differences.
   * 
   * @param dp the base peak with lowest m/z.
   * @param originalDataPoints All the data points in consideration for isotope peaks.
   * @param mzTolerance the m/z tolerance.
   * @param pattern The isotope pattern to search for.
   * @param mzrange the range of m/z to search for isotope peaks.
   * @return dp will be modified and given an DPPIsotopePatternResult.
   */
  public static ProcessedDataPoint findIsotopePatterns(ProcessedDataPoint dp,
      ProcessedDataPoint[] originalDataPoints, MZTolerance mzTolerance,
      ExtendedIsotopePattern pattern, Range<Double> mzrange, int maxCharge) {
    // dp is the peak we are currently searching an isotope pattern for

    if (maxCharge < 1)
      return dp;

    if (!mzrange.contains(dp.getMZ()))
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
      for (int isotopeIndex = 0; isotopeIndex < numIsotopes; isotopeIndex++) {
        ProcessedDataPoint p = bestdp[isotopeIndex];
        if (p.resultTypeExists(ResultType.ISOTOPECOMPOSITION))
          ((DPPIsotopeCompositionResult) p.getFirstResultByType(ResultType.ISOTOPECOMPOSITION))
              .add(pattern.getIsotopeComposition(isotopeIndex));
        else
          p.addResult(new DPPIsotopeCompositionResult(pattern.getIsotopeComposition(isotopeIndex)));
      }

      IsotopePattern patternx =
          new SimpleIsotopePattern(bestdp, IsotopePatternStatus.DETECTED, pattern.getDescription()); // pattern.getDescription()
                                                                                                     // is
      dp.addResult(new DPPIsotopePatternResult(patternx, bestdp, i_charge));
    }

    return dp;
  }

  /**
   * Loops through an array of data points and merges isotope patterns together from high m/z to low
   * m/z.
   * 
   * @param originalDataPoints
   * @return
   */
  public static List<ProcessedDataPoint> mergeDetectedPatterns(
      ProcessedDataPoint[] originalDataPoints, int maxCharge) {

    // now we looped through all dataPoints and link the found isotope patterns together
    // we start from the back so we can just accumulate them by merging the linked the
    // peaks/patterns
    List<ProcessedDataPoint> results = new ArrayList<ProcessedDataPoint>();

    for (int i_dp = originalDataPoints.length - 1; i_dp >= 0; i_dp--) {
      ProcessedDataPoint dp = originalDataPoints[i_dp];

      mergeIsotopePatternResults(dp, results, maxCharge);
      // mergeIsotopePatternResults(dp);

    }

    results.sort((o1, o2) -> {
      return Double.compare(o1.getMZ(), o2.getMZ());
    });

    return results;
  }

  /*
   * public static ProcessedDataPoint mergeIsotopePatternResults(ProcessedDataPoint dp) {
   * 
   * if (dp == null) return dp;
   * 
   * if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) { // ok, the data point does have an
   * isotope pattern assigned to it List<DPPResult<?>> patternResults =
   * dp.getAllResultsByType(ResultType.ISOTOPEPATTERN); List<Integer> charges = getChargeStates(dp);
   * logger.info(format.format(dp.getMZ()) + " has the charge states: " + charges.toString());
   * 
   * // logger.info("m/z " + dp.getMZ() + " has " + patternResults.size() + " pattern results.");
   * 
   * List<ProcessedDataPoint> patterndp = new ArrayList<>();
   * 
   * Collection<DPPResult<?>> newResults = new ArrayList<>(); // let's check if the peaks in the
   * isotope pattern have isotope patterns assigned to them
   * 
   * DPPIsotopeCompositionResult baseComposition = (DPPIsotopeCompositionResult)
   * dp.getFirstResultByType(ResultType.ISOTOPECOMPOSITION);
   * 
   * for (Integer charge : charges) {
   * 
   * patterndp.add(dp);
   * 
   * for (int k_pr = 0; k_pr < patternResults.size(); k_pr++) { // we get every isotope pattern,
   * that has been assigned to dp DPPIsotopePatternResult patternResult = (DPPIsotopePatternResult)
   * patternResults.get(k_pr);
   * 
   * if (patternResult.getCharge() != charge) continue;
   * 
   * ProcessedDataPoint[] linkedDataPoints = patternResult.getLinkedDataPoints();
   * dp.removeResult(patternResult);
   * 
   * for (ProcessedDataPoint p : linkedDataPoints) {
   * 
   * if(dp == p) continue;
   * 
   * if(p.resultTypeExists(ResultType.ISOTOPEPATTERN)) mergeIsotopePatternResults(p);
   * 
   * if (!patterndp.contains(p)) { patterndp.add(p);
   * 
   * if (baseComposition != null) ((DPPIsotopeCompositionResult) p
   * .getFirstResultByType(ResultType.ISOTOPECOMPOSITION)) .addAll(baseComposition.getValue()); } }
   * 
   * }
   * 
   * // remove duplicates by adding to a hash set Set<ProcessedDataPoint> set = new
   * LinkedHashSet<>(); set.addAll(patterndp); patterndp.clear(); patterndp.addAll(set);
   * 
   * patterndp.sort((o1, o2) -> { return Double.compare(o1.getMZ(), o2.getMZ()); });
   * ProcessedDataPoint[] array = patterndp.toArray(new ProcessedDataPoint[0]); patterndp.clear();
   * 
   * newResults.add(new DPPIsotopePatternResult(new SimpleIsotopePattern(array,
   * IsotopePatternStatus.DETECTED, format.format(dp.getMZ())), array, charge.intValue())); }
   * 
   * dp.removeResults(patternResults); dp.addAllResults(newResults); logger.finest("FINAL: " +
   * format.format(dp.getMZ()) + " pattern: " + getResultIsoComp( (DPPIsotopePatternResult)
   * dp.getFirstResultByType(ResultType.ISOTOPEPATTERN))); }
   * 
   * 
   * return dp; }
   * 
   * public static List<ProcessedDataPoint> getAllPatternResultDatapoints(ProcessedDataPoint dp, int
   * charge) {
   * 
   * Set<ProcessedDataPoint> dataPoints = new LinkedHashSet<>();
   * 
   * if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) { List<DPPResult<?>> patternResults =
   * dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);
   * 
   * for (int k_pr = 0; k_pr < patternResults.size(); k_pr++) { // we get every isotope pattern,
   * that has been assigned to dp DPPIsotopePatternResult patternResult = (DPPIsotopePatternResult)
   * patternResults.get(k_pr);
   * 
   * if(patternResult.getCharge() == charge) { for(ProcessedDataPoint p :
   * patternResult.getLinkedDataPoints()) dataPoints.add(p); } } }
   * 
   * List<ProcessedDataPoint> results = new ArrayList<>(); results.addAll(dataPoints); results.sort(
   * (o1, o2) -> { return Double.compare(o1.getMZ(), o2.getMZ()); }); return results; }
   */

  /**
   * Checks if a data point has isotope pattern results. In case that a peak of that isotope pattern
   * contains an isotope pattern itself, they will be merged together and added to the isotope
   * pattern of the original peak.
   * 
   * @param dp
   * @param results
   * @return
   */
  public static ProcessedDataPoint mergeIsotopePatternResults(ProcessedDataPoint dp,
      List<ProcessedDataPoint> results, int maxCharge) {

    if (dp.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
      // ok, the data point does have an isotope pattern assigned to it
      List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);

      // logger.info("m/z " + dp.getMZ() + " has " + patternResults.size() + " pattern results.");

      // let's check if the peaks in the isotope pattern have isotope patterns assigned to them
      for (int k_pr = 0; k_pr < patternResults.size(); k_pr++) {
        // we get every isotope pattern, that has been assigned to dp
        DPPIsotopePatternResult patternResult = (DPPIsotopePatternResult) patternResults.get(k_pr);
        ProcessedDataPoint[] linkedDataPoints = patternResult.getLinkedDataPoints();
        IsotopePattern pattern = patternResult.getValue();
        int basePatternCharge = patternResult.getCharge();

        List<ProcessedDataPoint> dpToAdd = new ArrayList<>();

        // logger.info("Pattern " + k_pr + " contains " + linkedDataPoints.length + " peaks at " +
        // dataPointsToString(linkedDataPoints));

        // check the data points linked in the isotope pattern, if they contain isotope patterns
        // themselves, if yes, add them to our list
        for (ProcessedDataPoint linked : linkedDataPoints) {
          // the base peak of the isotope pattern is the current peak
          if (linked == dp)
            continue;

          // logger.info("Checking m/z " + linked.getMZ() + " for isotope patterns.");

          // does the linked peak in the isotope pattern have an isotope pattern associated with
          // it?
          if (linked.resultTypeExists(ResultType.ISOTOPEPATTERN)) {
            List<DPPResult<?>> linkedPatternResults =
                linked.getAllResultsByType(ResultType.ISOTOPEPATTERN);

            // go through every associated isotope pattern and add the data points to the list
            for (int l_lpr = 0; l_lpr < linkedPatternResults.size(); l_lpr++) {
              DPPIsotopePatternResult linkedPatternResult =
                  (DPPIsotopePatternResult) linkedPatternResults.get(l_lpr);

              // logger.info("adding isotope pattern of m/z " + format.format(linked.getMZ()) + "
              // ("
              // + dataPointsToString(linkedPatternResult.getLinkedDataPoints()) + ")"
              // + " to " + format.format(dp.getMZ()));

              // if the charge is the same, we can add this
              if (linkedPatternResult.getCharge() == basePatternCharge) {
                // now add the linked isotope pattern's data points
                dpToAdd.addAll(Arrays.asList(linkedPatternResult.getLinkedDataPoints()));

                // TODO: later on, think of something to keep track of the composition here
                // now we update the isotope composition.
                for (ProcessedDataPoint p : linkedPatternResult.getLinkedDataPoints()) {
                  if (p == linked)
                    continue;
                  ((DPPIsotopeCompositionResult) p
                      .getFirstResultByType(ResultType.ISOTOPECOMPOSITION))
                          .addAll(((DPPIsotopeCompositionResult) linked
                              .getFirstResultByType(ResultType.ISOTOPECOMPOSITION)).getValue());
                }

                // since we merged the isotope patterns now, we remove it from the linked peak
                linked.removeResult(linkedPatternResult);

                // since we merged the isotope pattern of the linked peak to dp, we can remove it
                // here, to avoid duplicates
                results.remove(linked);
              }
            }

          } // else logger.info("No pattern at " + format.format(linked.getMZ()));
          dpToAdd.add(linked);
        }
        dpToAdd.addAll(Arrays.asList(patternResult.getLinkedDataPoints()));

        // remove duplicates by adding to a hash set
        Set<ProcessedDataPoint> set = new LinkedHashSet<>();
        set.addAll(dpToAdd);
        dpToAdd.clear();
        dpToAdd.addAll(set);

        dpToAdd.sort((o1, o2) -> {
          return Double.compare(o1.getMZ(), o2.getMZ());
        });



        dp.removeResult(patternResult);
        ProcessedDataPoint[] dpToAddArray = dpToAdd.toArray(new ProcessedDataPoint[0]);
        // can we just pass all previously linked peaks here?
        dp.addResult(new DPPIsotopePatternResult(
            new SimpleIsotopePattern(dpToAddArray, IsotopePatternStatus.DETECTED, ""), dpToAddArray,
            basePatternCharge));

        // logger.info("Adding datapoints " + dataPointsToString(dpToAddArray) + " to " +
        // format.format(dp.getMZ()));
      }

      // now we merged all sub results, but we might still have more than one isotope pattern in
      // dp, so let's merge them
      int bestCharge = getBestChargeState(dp, maxCharge);
      List<DPPResult<?>> newPatternResults = dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);
      List<ProcessedDataPoint> pattern = new ArrayList<>();
      for (int x = 0; x < newPatternResults.size(); x++) {
        DPPIsotopePatternResult newPatternResult =
            (DPPIsotopePatternResult) newPatternResults.get(x);
        if (bestCharge == newPatternResult.getCharge())
          pattern.addAll(Arrays.asList(newPatternResult.getLinkedDataPoints()));
        dp.removeResult(newPatternResult); // remove anyway, even if it's not the best charge state
      }

      Set<ProcessedDataPoint> set = new LinkedHashSet<>();
      set.addAll(pattern);
      pattern.clear();
      pattern.addAll(set);
      pattern.sort((o1, o2) -> {
        return Double.compare(o1.getMZ(), o2.getMZ());
      });

      ProcessedDataPoint[] patternArray = pattern.toArray(new ProcessedDataPoint[0]);
      dp.addResult(new DPPIsotopePatternResult(new SimpleIsotopePattern(patternArray,
          IsotopePatternStatus.DETECTED, format.format(dp.getMZ())), patternArray, bestCharge));
      logger.finest("FINAL: " + format.format(dp.getMZ()) + " pattern: " + getResultIsoComp(
          (DPPIsotopePatternResult) dp.getFirstResultByType(ResultType.ISOTOPEPATTERN)));
      results.add(dp);
    }

    return dp;
  }

  public static int getBestChargeState(ProcessedDataPoint dp, int maxCharge) {
    int chargePeakNum[] = new int[maxCharge + 1]; // index represents the charge

    List<DPPResult<?>> patternResults = dp.getAllResultsByType(ResultType.ISOTOPEPATTERN);
    for (int x = 0; x < patternResults.size(); x++) {
      DPPIsotopePatternResult pattern = (DPPIsotopePatternResult) patternResults.get(x);
      chargePeakNum[pattern.getCharge()] += pattern.getValue().getNumberOfDataPoints();
    }

    int bestCharge = 1;
    int highestNum = 0;
    for (int i = 1; i < chargePeakNum.length; i++) {
      if (chargePeakNum[i] > highestNum) {
        bestCharge = i;
        highestNum = chargePeakNum[i];
      }
    }
    return bestCharge;
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
      if(c.length() > 2)
        c = c.substring(0, c.length() - 2);
      str += format.format(dp.getMZ()) + " (" + c + "), ";
    }
    str = str.substring(0, str.length() - 2);
    return str;
  }
}
