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

package io.github.mzmine.modules.dataprocessing.id_cliquems.cliquemsimplementation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.mutable.MutableDouble;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.modules.dataprocessing.id_cliquems.CliqueMSTask;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.util.scans.ScanUtils;
import javafx.util.Pair;

/**
 * This class contains all the data members and functions for finding cliques or groups using
 * CliqueMSR algorithm. Input is peakList and corresponding rawDatafile, and parameters to the
 * algorithm.
 */
public class ComputeCliqueModule {

  private final Logger logger = Logger.getLogger(getClass().getName());

  private final AnClique anClique;
  private final FeatureList peakList;
  private List<PeakData> peakDataList;
  private final RawDataFile rawDataFile;

  private final CliqueMSTask drivertask;

  // variables to update progress
  private final MutableDouble progress;

  // cosine correlation matrix calculated over columns of EIC matrix
  private double[][] cosineCorrelation;

  public ComputeCliqueModule(FeatureList peakList, RawDataFile rdf, MutableDouble progress,
      CliqueMSTask task) {
    this.rawDataFile = rdf;
    this.peakList = peakList;
    this.progress = progress;
    drivertask = task;
    peakDataList = getPeakDataFromPeaks(peakList, rdf);
    anClique = new AnClique(peakDataList, rdf);
  }

  /**
   * Extracts necessary data from rawdatafile
   *
   * @param peakList feature lists
   * @param dataFile raw Data File
   * @return PeakData contains sufficient data for cliqueMS algorithm
   */
  private List<PeakData> getPeakDataFromPeaks(FeatureList peakList, RawDataFile dataFile) {
    List<PeakData> peakDataList = new ArrayList<>();
    for (int i = 0; i < peakList.getRows().size(); i++) {
      FeatureListRow peak = peakList.getRows().get(i);
      double mz;
      double mzmin;
      double mzmax;
      float rt;
      float rtmin;
      float rtmax;
      float intensity;
      int peakListRowID;
      if (peak.hasFeature(dataFile)) {
        mz = peak.getFeature(dataFile).getMZ();
        mzmin = peak.getFeature(dataFile).getRawDataPointsMZRange().lowerEndpoint();
        mzmax = peak.getFeature(dataFile).getRawDataPointsMZRange().upperEndpoint();
        rt = peak.getFeature(dataFile).getRT();
        rtmin = peak.getFeature(dataFile).getRawDataPointsRTRange().lowerEndpoint();
        rtmax = peak.getFeature(dataFile).getRawDataPointsRTRange().upperEndpoint();
        intensity = peak.getFeature(dataFile).getHeight();
        peakListRowID = peak.getID();
        PeakData peakData =
            new PeakData(mz, mzmin, mzmax, rt, rtmin, rtmax, intensity, i + 1, peakListRowID);
        peakDataList.add(peakData);
      }
    }
    return peakDataList;
  }

  /**
   * EIC matrix is a axb (a = #rts or #scans, b = #features) dimension matrix. The ith column
   * corresponds for the ith feature. For each column, the jth row value is calculated as follows -
   * 1) If j does not lies in the index of rt range of the ith feature, value is 0 (EIC[j][i] = 0)
   * 2) Else, the value is the mean intensities for the jth scan's datapoints' intensities (only
   * those intensities whose corresponding mass in datapoint is in the mzRange of ith feature)
   * <p>
   * EIC matrix is further used to calculate cosine similarity matrix
   *
   * @param file raw data file
   * @param peakDataList contains peak data
   * @return double [][] EIC matrix
   */
  private double[][] getEIC(RawDataFile file, List<PeakData> peakDataList) {
    List<List<DataPoint>> dataPoints = new ArrayList<>(); // contains m/z and intensity data
    List<Double> rts = new ArrayList<>(); // holds Retention Time values in seconds
    for (Scan scan : file.getScans()) {
      rts.add(scan.getRetentionTime() * 60.0); // conversion for minutes to seconds
      List<DataPoint> dps =
          new ArrayList<DataPoint>(Arrays.asList(ScanUtils.extractDataPoints(scan)));

      dataPoints.add(dps);
    }
    // nrows = #rts , ncols = # peaks, already transposed as in R code
    double EIC[][] = new double[file.getScans().size()][peakDataList.size()];
    for (int i = 0; i < file.getScans().size(); i++) {
      for (int j = 0; j < peakDataList.size(); j++) {
        EIC[i][j] = 0.0;
      }
    }

    for (int i = 0; i < peakDataList.size(); i++) {
      if (drivertask.isCanceled()) {
        return EIC;
      }
      PeakData pd = peakDataList.get(i);
      int posrtmin = rts.indexOf(pd.getRtmin() * 60.0); // position where peak matches rtmin
      int posrtmax = rts.indexOf(pd.getRtmax() * 60.0); // position where peak matches rtmax

      for (int j = posrtmin; j < posrtmax; j += 1) {
        List<Double> intensities = new ArrayList<>();
        for (DataPoint dp : dataPoints.get(j)) {
          Double mzMin = pd.getMzmin();
          Double mzMax = pd.getMzmax();
          Double dp_mz = dp.getMZ();
          if (dp_mz <= mzMax && dp_mz >= mzMin) {
            intensities.add(dp.getIntensity());
          }
        }
        if (intensities.size() == 0) {
          EIC[j][i] = 0.0; // no effect
        } else {
          Double meanInt = 0.0;
          for (Double d : intensities) {
            meanInt += d;
          }
          meanInt /= intensities.size();
          EIC[j][i] = meanInt;
        }


      }
      // progress update
      this.progress.setValue(drivertask.EIC_PROGRESS * ((double) i / (double) peakDataList.size()));
    }

    return EIC;

  }

  // TODO make use of sparse matrix, make algo time efficient

  /**
   * Computes cosine correlation of data (EIC) matrix over the columns, so for ixj dimension EIC
   * matrix, the [x,y]th element of cosine correlation matrix contains the cosine similarity between
   * the xth and the yth column of EIC matrix, so cosine correlation matrix has dimension jxj
   *
   * @param data EIC matrix
   * @return cosine correlation matrix
   */
  private double[][] cosCorrbyColumn(double[][] data) {
    int row = data.length, col = data[0].length;
    double[][] corr = new double[col][col];

    for (int i = 0; i < col; i++) {
      for (int j = 0; j < col; j++) {
        corr[i][j] = 0.0;
      }
    }

    for (int i = 0; i < col; i++) {
      for (int j = 0; j < col; j++) {
        if (drivertask.isCanceled()) {
          return corr;
        }
        double modi = 0.0, modj = 0.0;
        for (int k = 0; k < row; k++) {
          corr[i][j] += data[k][i] * data[k][j];
          modi += data[k][i] * data[k][i];
          modj += data[k][j] * data[k][j];
        }
        modi = Math.sqrt(modi);
        modj = Math.sqrt(modj);
        corr[i][j] = corr[i][j] / (modi * modj);
      }
      // update progress
      this.progress.setValue(
          drivertask.EIC_PROGRESS + drivertask.MATRIX_PROGRESS * ((double) (i + 1) / (double) col));
    }
    return corr;
  }


  /**
   * identify peaks with very similar cosine correlation, m/z, rt and intensity
   *
   * @param cosineCorr cosine correlation matrix
   * @param peakDataList contains features' information
   * @param mzdiff tolerance value for mz
   * @param intdiff tolerance value for intensity
   * @param rtdiff tolerance value for rt
   * @return node ID of similar features
   */
  private List<Integer> similarFeatures(double[][] cosineCorr, List<PeakData> peakDataList,
      MZTolerance mzdiff, RTTolerance rtdiff, double intdiff) {
    // find all elements in cosineCorr with i<j and value > 0.99
    List<Integer> edgeX = new ArrayList<>();
    List<Integer> edgeY = new ArrayList<>();
    for (int i = 0; i < cosineCorr.length; i++) {
      for (int j = i + 1; j < cosineCorr[0].length; j++) {
        if (cosineCorr[i][j] > 0.99) {
          edgeX.add(i);
          edgeY.add(j);
        }
      }
    }
    List<Integer> nodesToDelete = new ArrayList<>();
    List<Integer> identicalNodes = new ArrayList<>();
    if (edgeX.size() > 0) {
      for (int i = 0; i < edgeX.size(); i++) {
        PeakData p1 = peakDataList.get(edgeX.get(i));
        PeakData p2 = peakDataList.get(edgeY.get(i));
        Range<Double> mz_Range = mzdiff.getToleranceRange(p1.getMz());
        Range<Float> rt_Range = rtdiff.getToleranceRange((float) p1.getRt());
        double error_int = Math.abs(p1.getIntensity() - p2.getIntensity()) / p1.getIntensity();
        if ((mz_Range.contains(p2.getMz())) && (rt_Range.contains((float) p2.getRt()))
            && (error_int < intdiff)) {
          Integer node = (edgeX.get(i) < edgeY.get(i) ? edgeX.get(i) : edgeY.get(i));
          identicalNodes.add((edgeX.get(i) >= edgeY.get(i) ? edgeX.get(i) : edgeY.get(i)));
          nodesToDelete.add(node);
        }
      }
    }

    HashMap<PeakData, FeatureListRow> peakMap = new HashMap<>(); // map b/w peakData and peakListRow
    for (PeakData pd : peakDataList) {
      for (FeatureListRow row : peakList.getRows()) {
        if (pd.getPeakListRowID() == row.getID()) {
          peakMap.put(pd, row);
        }
      }
    }

    // annotate peakList for nodes to be deleted
    for (int i = 0; i < nodesToDelete.size(); i++) {
      Integer nodeToDeleted = nodesToDelete.get(i);
      Integer nodeToReplace = identicalNodes.get(i);
      PeakData pdNodeToDelete = peakDataList.get(nodeToDeleted);
      PeakData pdNodeToReplace = peakDataList.get(nodeToReplace);
      FeatureListRow row = peakMap.get(pdNodeToDelete);
      row.setComment("Similar to peak: " + pdNodeToReplace.getPeakListRowID());

    }

    Collections.sort(nodesToDelete);
    return nodesToDelete;
  }

  /**
   * Removes nodes that are too similar in rt, mz and intensity values
   *
   * @param cosinus cosine correlation matrix
   * @param peakDL peak Data list
   * @param mzdiff tolerance values for similarity
   * @param rtdiff tolerance values for similarity
   * @param intdiff tolerance values for similarity
   */
  private void filterFeatures(double[][] cosinus, List<PeakData> peakDL, MZTolerance mzdiff,
      RTTolerance rtdiff, double intdiff) {
    List<PeakData> modifiedPeakDataList = new ArrayList<>();
    List<Integer> deleteIndices = similarFeatures(cosinus, peakDL, mzdiff, rtdiff, intdiff);
    if (deleteIndices.size() == 0) {
      logger.log(Level.FINEST, "No feature deleted");
      return;
    }

    // remove the peakdata containing
    for (int i = 0; i < peakDataList.size(); i++) {
      PeakData pd = peakDataList.get(i);
      if (deleteIndices.contains(i)) {
        continue;
      }
      PeakData pdmod = new PeakData(pd);
      modifiedPeakDataList.add(pdmod);
    }

    double[][] modifiedCosineCorr =
        new double[cosinus.length - deleteIndices.size()][cosinus[0].length - deleteIndices.size()];
    // deleting row and columns of indices in deleteIndices
    int colShift = 0;

    for (int i = 0; i < cosinus.length; i++) {
      int rowShift = 0;
      if (colShift < deleteIndices.size() && i == deleteIndices.get(colShift)) {
        colShift++;
        continue;
      }
      for (int j = 0; j < cosinus[0].length; j++) {
        if (rowShift < deleteIndices.size() && j == deleteIndices.get(rowShift)) {
          rowShift++;
          continue;
        }
        modifiedCosineCorr[i - colShift][j - rowShift] = cosinus[i][j];
      }


    }

    this.cosineCorrelation = modifiedCosineCorr;
    anClique.changePeakDataList(modifiedPeakDataList);
    this.peakDataList = modifiedPeakDataList;
    logger.log(Level.FINEST, deleteIndices.size() + " features deleted.");


  }

  /**
   * This function is to assign a clique group value to nodes that do not have links, because they
   * did not appear in the edgelists
   */
  private void updateCliques() {
    List<Pair<Integer, Integer>> nodeCliqueList = this.anClique.getNetwork().getResultNodeClique();
    List<PeakData> ungroupedFeatures = new ArrayList<>();
    Integer maxClique = 0;
    for (Pair nodeClique : nodeCliqueList) {
      if (maxClique < (Integer) nodeClique.getKey()) {
        maxClique = (Integer) nodeClique.getKey();
      }
    }
    for (PeakData pd : this.peakDataList) {
      boolean present = false;
      for (Pair nodeClique : nodeCliqueList) {
        if (nodeClique.getKey().equals(pd.getNodeID())) {
          present = true;
          break;
        }
      }
      if (!present) {
        ungroupedFeatures.add(pd);
      }
    }
    for (PeakData pd : ungroupedFeatures) {
      maxClique += 1;
      Pair<Integer, Integer> p = new Pair<>(pd.getNodeID(), maxClique);
      nodeCliqueList.add(p);
    }

    Collections.sort(nodeCliqueList, (o1, o2) -> (o1.getKey() - o2.getKey()));
  }

  /**
   * Driver function for calculating clique groups
   *
   * @param filter filter similar features
   * @param mzdiff tolerance for mz similarity
   * @param rtdiff tolerance for rt similarity
   * @param intdiff tolerance for intensity similarity
   * @param tol tolerance for log likelihood function which is minimized for finding the clique.
   * @return AnClique object with calculated cliques.
   */
  public AnClique getClique(boolean filter, MZTolerance mzdiff, RTTolerance rtdiff, double intdiff,
      double tol) {

    if (anClique.cliquesFound) {
      logger.log(Level.WARNING, "cliques have already been computed!");
    }
    double EIC[][] = getEIC(rawDataFile, peakDataList);

    if (drivertask.isCanceled()) {
      return anClique;
    }

    this.cosineCorrelation = cosCorrbyColumn(EIC);
    if (drivertask.isCanceled()) {
      return anClique;
    }
    if (filter) {
      filterFeatures(cosineCorrelation, peakDataList, mzdiff, rtdiff, intdiff);
    }

    List<Integer> nodeIDList = new ArrayList<>();
    for (PeakData pd : peakDataList) {
      nodeIDList.add(pd.getNodeID());
    }
    anClique.getNetwork().returnCliques(cosineCorrelation, nodeIDList, tol, false, this.progress,
        this.drivertask);
    updateCliques();
    this.anClique.cliquesFound = true;
    this.anClique.computeCliqueFromResult();
    return this.anClique;
  }
}
