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

package io.github.mzmine.modules.dataprocessing.id_ms2search;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;


class Ms2SearchTask extends AbstractTask {

  private Logger logger = Logger.getLogger(this.getClass().getName());

  private int finishedRows, totalRows;
  private FeatureList peakList1;
  private FeatureList peakList2;

  private MZTolerance mzTolerance;
  private ParameterSet parameters;
  private double scoreThreshold;
  private double intensityThreshold;
  private int minimumIonsMatched;

  /**
   * @param parameters
   */
  public Ms2SearchTask(ParameterSet parameters, FeatureList peakList1, FeatureList peakList2, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.peakList1 = peakList1;
    this.peakList2 = peakList2;
    this.parameters = parameters;

    mzTolerance = parameters.getParameter(Ms2SearchParameters.mzTolerance).getValue();

    scoreThreshold = parameters.getParameter(Ms2SearchParameters.scoreThreshold).getValue();

    intensityThreshold = parameters.getParameter(Ms2SearchParameters.intensityThreshold).getValue();

    minimumIonsMatched = parameters.getParameter(Ms2SearchParameters.minimumIonsMatched).getValue();
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
   */
  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0)
      return 0;
    return ((double) finishedRows) / totalRows;
  }

  /**
   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
   */
  @Override
  public String getTaskDescription() {
    return "MS2 similarity comparison between " + peakList1 + " and " + peakList2;
  }

  /**
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    logger.info("Starting MS2 similarity search between " + peakList1 + " and " + peakList2
        + " with mz tolerance:" + mzTolerance.getPpmTolerance());

    Ms2SearchResult searchResult;
    FeatureListRow rows1[] = peakList1.getRows().toArray(FeatureListRow[]::new);
    FeatureListRow rows2[] = peakList2.getRows().toArray(FeatureListRow[]::new);

    int rows1Length = rows1.length;
    int rows2Length = rows2.length;

    totalRows = rows1Length;

    for (int i = 0; i < rows1Length; i++) {
      for (int j = 0; j < rows2Length; j++) {
        Feature featureA = rows1[i].getBestFeature();
        Feature featureB = rows2[j].getBestFeature();
        // Complication. The "best" peak, may not have the "best"
        // fragmentation
        Scan scanA = rows1[i].getMostIntenseFragmentScan();
        Scan scanB = rows2[j].getMostIntenseFragmentScan();

        searchResult =
            simpleMS2similarity(scanA, scanB, intensityThreshold, mzTolerance);

        // Report the final score to the peaklist identity
        if (searchResult != null && searchResult.getScore() > scoreThreshold
            && searchResult.getNumIonsMatched() >= minimumIonsMatched)
          this.addMS2Identity(rows1[i], featureA, featureB, searchResult);

        if (isCanceled())
          return;
      }

      // Update progress bar
      finishedRows++;
    }

    // Add task description to peakList
    ((ModularFeatureList) peakList1).addDescriptionOfAppliedTask(
        new SimpleFeatureListAppliedMethod("Identification of similar MS2s",
            Ms2SearchModule.class, parameters, getModuleCallDate()));

    setStatus(TaskStatus.FINISHED);

    logger.info("Finished MS2 similarity search for " + peakList1 + "against" + peakList2);

  }

  private Ms2SearchResult simpleMS2similarity(Scan scanMS2A, Scan scanMS2B,
      double intensityThreshold, MZTolerance mzRange) {

    double runningScoreTotal = 0.0;
    double mzRangePPM = mzRange.getPpmTolerance();

    List<DataPoint> matchedIons = new ArrayList<DataPoint>();

    // Fetch 1st feature MS2 scan.
    // int ms2ScanNumberA = featureA.getMostIntenseFragmentScanNumber();
    // Scan scanMS2A = featureA.getDataFile().getScan(ms2ScanNumberA);

    // Fetch 2nd feature MS2 scan.
    // int ms2ScanNumberB = featureB.getMostIntenseFragmentScanNumber();
    // Scan scanMS2B = featureB.getDataFile().getScan(ms2ScanNumberB);

    if (scanMS2A == null || scanMS2B == null) {
      return null;
    }

    // Fetch centroided data
    MassList massListA = scanMS2A.getMassList();
    MassList massListB = scanMS2B.getMassList();

    if (massListA == null) {
      // Will this work properly? As this function isn't directly the
      // task?
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Scan " + scanMS2A.getDataFile().getName() + " #" + scanMS2A.getScanNumber()
          + " does not have a mass list");
      return null;
    }

    if (massListB == null) {
      // Will this work properly? As this function isn't directly the
      // task?
      setStatus(TaskStatus.ERROR);
      setErrorMessage("Scan " + scanMS2B.getDataFile().getName() + " #" + scanMS2B.getScanNumber()
          + " does not have a mass list");
      return null;
    }

    DataPoint[] ionsA = null;
    DataPoint[] ionsB = null;

    ionsA = massListA.getDataPoints();
    ionsB = massListB.getDataPoints();

    if (ionsA == null || ionsB == null || ionsA.length == 0 || ionsB.length == 0) {
      // Fall back to profile data?
      // Profile / raw data.
      // ionsA = scanMS2A.getDataPointsOverIntensity(intensityThreshold);
      // ionsB = scanMS2B.getDataPointsOverIntensity(intensityThreshold);
      return null;
    }

    // Compare every ion peak in MS2 scan A, to every ion peak in MS2 scan
    // B.
    double ionsBMaxMZ = ionsB[ionsB.length - 1].getMZ();
    for (int i = 0; i < ionsA.length; i++) {

      double iMZ = ionsA[i].getMZ();
      double mzRangeAbsolute = iMZ * 1e-6 * mzRangePPM;

      if (iMZ - mzRangeAbsolute > ionsBMaxMZ)
        break; // Potential speedup heuristic. If any i is greater than
               // the max of j, no more
               // matches are possible.

      for (int j = 0; j < ionsB.length; j++) {

        double jMZ = ionsB[j].getMZ();

        if (iMZ < jMZ - mzRangeAbsolute)
          break; // Potential speedup heuristic. iMZ smaller than jMZ.
                 // Skip the rest of the j's as
                 // they can only increase.

        if (Math.abs(iMZ - jMZ) < mzRangeAbsolute) {
          runningScoreTotal += ionsA[i].getIntensity() * ionsB[j].getIntensity();
          matchedIons.add(ionsA[i]);
        }

      }
    }
    Ms2SearchResult result = new Ms2SearchResult(runningScoreTotal, "simple", matchedIons);
    return result;
  }

  /**
   * Add new identity based on fragmentation similarity to the row
   */
  private void addMS2Identity(FeatureListRow row1, Feature featureA, Feature featureB,
      Ms2SearchResult searchResult) {
    Ms2Identity newIdentity = new Ms2Identity(featureA, featureB, searchResult);
    row1.addFeatureIdentity(newIdentity, false);
  }
}
