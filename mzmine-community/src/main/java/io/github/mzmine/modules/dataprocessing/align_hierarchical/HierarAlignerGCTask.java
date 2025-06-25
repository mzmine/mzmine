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

package io.github.mzmine.modules.dataprocessing.align_hierarchical;
//
//import io.github.mzmine.datamodel.FeatureIdentity;
//import io.github.mzmine.datamodel.MZmineProject;
//import io.github.mzmine.datamodel.RawDataFile;
//import io.github.mzmine.datamodel.features.Feature;
//import io.github.mzmine.datamodel.features.FeatureList;
//import io.github.mzmine.datamodel.features.FeatureListRow;
//import io.github.mzmine.datamodel.features.ModularFeature;
//import io.github.mzmine.datamodel.features.ModularFeatureList;
//import io.github.mzmine.datamodel.features.ModularFeatureListRow;
//import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
//import io.github.mzmine.main.MZmineCore;
//import io.github.mzmine.parameters.ParameterSet;
//import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
//import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
//import io.github.mzmine.taskcontrol.AbstractTask;
//import io.github.mzmine.taskcontrol.TaskStatus;
//import io.github.mzmine.util.FeatureUtils;
//import io.github.mzmine.util.MemoryMapStorage;
//import io.github.mzmine.util.SortingDirection;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.Format;
//import java.time.Instant;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.Hashtable;
//import java.util.List;
//import java.util.Set;
//import java.util.Vector;
//import java.util.logging.Logger;
//import org.gnf.clustering.DataSource;
//import org.gnf.clustering.DistanceMatrix;
//import org.gnf.clustering.FloatSource1D;
//import org.gnf.clustering.LinkageMode;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//public class HierarAlignerGCTask extends AbstractTask {
//
//
//  public static String TASK_NAME = "Hierarchical aligner (GC)";
//
//  private final MZmineProject project;
//  private FeatureList peakLists[];
//  private FeatureList alignedPeakList;
//
//  // Processed rows counter
//  private int processedRows, totalRows;
//
//  private String peakListName;
//  private LinkageMode linkageStartegyType;
//
//  // private boolean use_hybrid_K;
//  // private int hybrid_K_value;
//
//  private boolean saveRAMratherThanCPU_1;
//  private boolean saveRAMratherThanCPU_2;
//  //
//  // private boolean useOldestRDFAncestor;
//  private MZTolerance mzTolerance;
//  private RTTolerance rtTolerance;
//  private double mzWeight, rtWeight;
//  private double minScore;
//  // private double idWeight;
//  //
//  // private boolean useApex, useKnownCompoundsAsRef;
//  // private boolean useDetectedMzOnly;
//  // private RTTolerance rtToleranceAfter;
//
//  private boolean exportDendrogramAsTxt;
//  private File dendrogramTxtFilename;
//
//  /**
//   * GLG HACK: temporary removed for clarity private boolean sameIDRequired, sameChargeRequired,
//   * compareIsotopePattern;
//   **/
//  private ParameterSet parameters;
//
//  // ID counter for the new peaklist
//  private int newRowID = 1;
//
//  //
//  private Format rtFormat = MZmineCore.getConfiguration().getRTFormat();
//
//  //
//  private final double maximumScore; // = 1.0d;
//  // For comparing small differences.
//  public static final double EPSILON = 0.0000001;
//
//  private static final boolean DEBUG = false;
//  private static final boolean DEBUG_2 = false;
//  List<FeatureListRow> full_rows_list;
//
//  private ClustererType CLUSTERER_TYPE;
//
//  public static final boolean USE_DOUBLE_PRECISION_FOR_DIST = false;
//
//  private ClusteringProgression clustProgress;
//
//  // Minimum score ever.
//  // TODO: better use "Double.MIN_VALUE" rather than zero (it has consequences
//  // !!!!)
//  // (0.0 is fine for 'Dot Product' method, but not for 'Person Correlation')
//  //// public static final double MIN_SCORE_ABSOLUTE = Double.MIN_VALUE;
//  public static final double MIN_SCORE_ABSOLUTE = 0.0;
//
//  HierarAlignerGCTask(MZmineProject project, ParameterSet parameters, @Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
//    super(storage, moduleCallDate);
//
//    this.project = project;
//    this.parameters = parameters;
//
//    peakLists = parameters.getParameter(HierarAlignerGCParameters.peakLists).getValue()
//        .getMatchingFeatureLists();
//
//    peakListName = parameters.getParameter(HierarAlignerGCParameters.peakListName).getValue();
//
//    // saveRAMratherThanCPU_1 = parameters
//    // .getParameter(HierarAlignerGCParameters.saveRAMratherThanCPU_1)
//    // .getValue();
//    // saveRAMratherThanCPU_2 = parameters
//    // .getParameter(HierarAlignerGCParameters.saveRAMratherThanCPU_2)
//    // .getValue();
//    saveRAMratherThanCPU_1 = false;
//    saveRAMratherThanCPU_2 = false;
//
//    mzTolerance = parameters.getParameter(HierarAlignerGCParameters.MZTolerance).getValue();
//    rtTolerance = parameters.getParameter(HierarAlignerGCParameters.RTTolerance).getValue();
//
//    mzWeight = parameters.getParameter(HierarAlignerGCParameters.MZWeight).getValue();
//
//    rtWeight = parameters.getParameter(HierarAlignerGCParameters.RTWeight).getValue();
//
//    minScore = parameters.getParameter(HierarAlignerGCParameters.minScore).getValue();
//
//    // idWeight = parameters.getParameter(JoinAlignerParameters.IDWeight)
//    // .getValue();
//    // idWeight = 0.0;
//
//    // ***
//    //// useApex = parameters.getParameter(
//    //// JoinAlignerGCParameters.useApex).getValue();
//    // useApex = true;
//    // ***
//
//    exportDendrogramAsTxt =
//        parameters.getParameter(HierarAlignerGCParameters.exportDendrogramTxt).getValue();
//    dendrogramTxtFilename =
//        parameters.getParameter(HierarAlignerGCParameters.dendrogramTxtFilename).getValue();
//
//    /**
//     * GLG HACK: temporarily removed for clarity sameChargeRequired = parameters.getParameter(
//     * JoinAlignerParameters.SameChargeRequired).getValue();
//     *
//     * sameIDRequired = parameters.getParameter( JoinAlignerParameters.SameIDRequired).getValue();
//     *
//     * compareIsotopePattern = parameters.getParameter(
//     * JoinAlignerParameters.compareIsotopePattern).getValue();
//     **/
//
//    //
//
//    // CLUSTERER_TYPE = parameters
//    // .getParameter(HierarAlignerGCParameters.clusterer_type)
//    // .getValue();// .ordinal();
//    CLUSTERER_TYPE = ClustererType.CACHED;
//
//    //
//    ClusteringLinkageStrategyType linkageStartegyType_0 =
//        parameters.getParameter(HierarAlignerGCParameters.linkageStartegyType_0).getValue();
//    switch (linkageStartegyType_0) {
//      case SINGLE:
//        linkageStartegyType = LinkageMode.MIN;
//        break;
//      case AVERAGE:
//        linkageStartegyType = LinkageMode.AVG;
//        break;
//      case COMPLETE:
//        linkageStartegyType = LinkageMode.MAX;
//        break;
//      default:
//        break;
//    }
//
//    // this.hybrid_K_value = parameters.getParameter(
//    // JoinAlignerGCParameters.hybrid_K_value).getValue();
//
//    //
//    maximumScore = mzWeight + rtWeight;
//
//    //
//    clustProgress = new ClusteringProgression();
//  }
//
//  /**
//   * @see io.github.mzmine.taskcontrol.Task#getTaskDescription()
//   */
//  @Override
//  public String getTaskDescription() {
//    return "Join aligner GC, " + peakListName + " (" + peakLists.length + " feature lists)";
//  }
//
//  /**
//   * @see io.github.mzmine.taskcontrol.Task#getFinishedPercentage()
//   */
//  @Override
//  public double getFinishedPercentage() {
//    if (totalRows == 0)
//      return 0f;
//    // return (double) processedRows / (double) totalRows;
//    double progress =
//        (processedRows + (clustProgress.getProgress() * totalRows / 3.0d)) / totalRows;
//    // logger.info(">> THE progress: " + progress);
//    // logger.info("Caught progress: " +
//    // clustProgress.getProgress());
//    return progress;
//  }
//
//  /**
//   * @see Runnable#run()
//   */
//  @Override
//  public void run() {
//
//    // Check options validity
//    if ((Math.abs(mzWeight) < EPSILON) && (Math.abs(rtWeight) < EPSILON)) {
//      setStatus(TaskStatus.ERROR);
//      setErrorMessage("Cannot run alignment, all the weight parameters are zero!");
//      return;
//    }
//
//    setStatus(TaskStatus.PROCESSING);
//    logger.info("Running join aligner");
//
//    // TIME STUFF
//    long startTime, endTime;
//    float ms;
//    //
//    if (DEBUG)
//      startTime = System.currentTimeMillis();
//
//    // MEMORY STUFF
//    Runtime run_time = Runtime.getRuntime();
//    Long prevTotal = 0l;
//    Long prevFree = run_time.freeMemory();
//    if (DEBUG)
//      printMemoryUsage(logger, run_time, prevTotal, prevFree, "START TASK...");
//
//    // Remember how many rows we need to process. Each row will be processed
//    // /*twice*/ three times:
//    // - first for score calculation
//    // - second for creating linkages
//    // - third for actual alignment
//    for (int i = 0; i < peakLists.length; i++) {
//      totalRows += peakLists[i].getNumberOfRows() * 3;
//    }
//
//    // Collect all data files
//    Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
//    for (FeatureList peakList : peakLists) {
//
//      for (RawDataFile dataFile : peakList.getRawDataFiles()) {
//
//        // Each data file can only have one column in aligned feature
//        // list
//        if (allDataFiles.contains(dataFile)) {
//          setStatus(TaskStatus.ERROR);
//          setErrorMessage("Cannot run alignment, because file " + dataFile
//              + " is present in multiple feature lists");
//          return;
//        }
//
//        allDataFiles.add(dataFile);
//      }
//    }
//
//    // Create a new aligned feature list
//    alignedPeakList = new ModularFeatureList(peakListName, getMemoryMapStorage(), allDataFiles.toArray(new RawDataFile[0]));
//
//    if (DEBUG)
//      printMemoryUsage(logger, run_time, prevTotal, prevFree, "COMPOUND DETECTED");
//
//    /** Alignment mapping **/
//    // Iterate source feature lists
//    Hashtable<ModularFeatureListRow, Object[]> infoRowsBackup = new Hashtable<ModularFeatureListRow, Object[]>();
//
//    // Since clustering is now order independent, option removed!
//    // Build comparison order
//    ArrayList<Integer> orderIds = new ArrayList<Integer>();
//    for (int i = 0; i < peakLists.length; ++i) {
//      orderIds.add(i);
//    }
//    Integer[] newIds = orderIds.toArray(new Integer[orderIds.size()]);
//    //
//
//    // TriangularMatrix distances = null;
//    DistanceMatrix distancesGNF_Tri = null;
//    DistanceMatrix distancesGNF_Tri_Bkp = null;
//
//    int nbPeaks = 0;
//    for (int i = 0; i < newIds.length; ++i) {
//      FeatureList peakList = peakLists[newIds[i]];
//      nbPeaks += peakList.getNumberOfRows();
//    }
//
//    // If 'Hybrid' or no distance matrix: no need for a matrix
//    if (CLUSTERER_TYPE == ClustererType.HYBRID || !saveRAMratherThanCPU_1) {
//      // distances = new double[nbPeaks][nbPeaks];
//
//      int nRowCount = nbPeaks;
//      distancesGNF_Tri = new DistanceMatrixTriangular1D2D(nRowCount);
//    }
//
//    full_rows_list = new ArrayList<>();
//
//    for (int i = 0; i < newIds.length; ++i) {
//
//      FeatureList peakList = peakLists[newIds[i]];
//
//      FeatureListRow allRows[] = peakList.getRows().toArray(FeatureListRow[]::new);
//      for (int j = 0; j < allRows.length; ++j) {
//
//        FeatureListRow row = allRows[j];
//        full_rows_list.add(row);
//      }
//    }
//
//    RowVsRowDistanceProvider distProvider = new RowVsRowDistanceProvider(project,
//        // useOldestRDFAncestor,
//        // rtAdjustementMapping,
//        full_rows_list, mzWeight, rtWeight,
//        // useApex,
//        // useKnownCompoundsAsRef,
//        // useDetectedMzOnly,
//        // rtToleranceAfter,
//        maximumScore);
//
//    // If 'Hybrid' or no distance matrix: no need for a matrix
//    if (CLUSTERER_TYPE == ClustererType.HYBRID || !saveRAMratherThanCPU_1) {
//
//      for (int x = 0; x < nbPeaks; ++x) {
//
//        for (int y = x; y < nbPeaks; ++y) {
//
//          float dist = (float) distProvider.getRankedDistance(x, y, mzTolerance.getMzTolerance(),
//              rtTolerance.getTolerance(), minScore);
//
//          // if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
//          // distances.set(x, y , dist);
//          // else
//          distancesGNF_Tri.setValue(x, y, dist);
//
//        }
//
//        processedRows++;
//        if (DEBUG)
//          logger.info(
//              "Treating lists: " + (Math.round(100 * processedRows / (double) nbPeaks)) + " %");
//
//      }
//    }
//    if (DEBUG)
//      printMemoryUsage(logger, run_time, prevTotal, prevFree, "DISTANCES COMPUTED");
//
//    //////
//    double max_dist = maximumScore; // Math.abs(row.getBestPeak().getRT() -
//                                    // k_row.getBestPeak().getRT()) /
//                                    // ((RangeUtils.rangeLength(rtRange) /
//                                    // 2.0));
//
//    // String newickCluster;
//    List<List<Integer>> gnfClusters = null;
//
//    //////
//
//    boolean do_verbose = true;
//    boolean do_cluster = true;
//    boolean do_print = (exportDendrogramAsTxt);
//    boolean do_data = false;
//
//    org.gnf.clustering.Node[] arNodes = null;
//    int nRowCount = full_rows_list.size();
//
//    String[] rowNames = null;
//    if (do_print) {
//      rowNames = new String[nRowCount];
//      for (int i = 0; i < nRowCount; i++) {
//        // rowNames[i] = "ID_" + i + "_" +
//        // full_rows_list.get(i).getID();
//        Feature peak = full_rows_list.get(i).getBestFeature();
//        double rt = peak.getRT();
//        int end = peak.getRawDataFile().getName().indexOf(" ");
//        String short_fname = peak.getRawDataFile().getName().substring(0, end);
//        rowNames[i] = "@" + rtFormat.format(rt) + "^[" + short_fname + "]";
//      }
//    }
//    String outputPrefix = null;
//
//    if (CLUSTERER_TYPE == ClustererType.CLASSIC) { // Pure Hierar!
//
//      outputPrefix = "hierar_0";
//
//      throw new IllegalStateException(
//          "'" + ClustererType.CLASSIC.toString() + "' algorithm not yet implemented!");
//
//    } else if (CLUSTERER_TYPE == ClustererType.CACHED) { // Pure Hierar!
//
//      // TODO: ...!
//      if (DEBUG_2)
//        logger.info(distancesGNF_Tri.toString());
//
//      if (saveRAMratherThanCPU_2) { // Requires: distances values will be
//                                    // recomputed on demand during
//                                    // "getValidatedClusters_3()"
//        distancesGNF_Tri_Bkp = null; // No duplicate backup storage!
//      } else { // Otherwise, backing up the distance matrix (matrix being
//               // deeply changed during "clusterDM()", then no more
//               // exploitable)
//        distancesGNF_Tri_Bkp = new DistanceMatrixTriangular1D2D(distancesGNF_Tri);
//        if (DEBUG)
//          printMemoryUsage(logger, run_time, prevTotal, prevFree, "GNF CLUSTERER BACKUP MATRIX");
//      }
//
//      if (DEBUG)
//        logger.info("Clustering...");
//      if (distancesGNF_Tri != null)
//        arNodes = org.gnf.clustering.sequentialcache.SequentialCacheClustering
//            .clusterDM(distancesGNF_Tri, linkageStartegyType, null, nRowCount);
//
//      distancesGNF_Tri = null;
//      System.gc();
//
//      if (DEBUG)
//        printMemoryUsage(logger, run_time, prevTotal, prevFree, "GNF CLUSTERER DONE");
//
//      if (DEBUG_2)
//        logger.info(distancesGNF_Tri.toString());
//
//      if (DEBUG_2)
//        for (int i = 0; i < arNodes.length; i++) {
//          logger.info("Node " + i + ": " + arNodes[i]);
//        }
//
//      // TODO: Use usual interfacing ...
//      // ClusteringResult<org.gnf.clustering.Node> clust_res = new
//      // ClusteringResult<>(
//      // Arrays.asList(arNodes), null, 0, null);
//
//      outputPrefix = "hierar_1";
//
//    } else if (CLUSTERER_TYPE == ClustererType.HYBRID) { // Hybrid!
//
//      throw new IllegalStateException(
//          "'" + ClustererType.HYBRID.toString() + "' algorithm not yet implemented!");
//
//    }
//
//    // Sort Nodes by correlation score (Required in
//    // 'getValidatedClusters_3')
//    int[] rowOrder = new int[nRowCount];
//    if (DEBUG)
//      logger.info("Sorting tree nodes...");
//    org.gnf.clustering.Utils.NodeSort(arNodes, nRowCount - 2, 0, rowOrder);
//
//    if (do_cluster) {
//
//      gnfClusters = getValidatedClusters_3(arNodes, 0.0f, newIds.length, max_dist,
//          distancesGNF_Tri_Bkp, distProvider);
//
//      // -- Print
//      if (DEBUG_2 && do_verbose)
//        for (int i = 0; i < gnfClusters.size(); i++) {
//          List<Integer> cl = gnfClusters.get(i);
//          String str = "";
//          for (int j = 0; j < cl.size(); j++) {
//            int r = cl.get(j);
//            str += cl.get(j) + "^(" + full_rows_list.get(r).getID() + ", "
//                + full_rows_list.get(r).getAverageRT() + ")" + " ";
//          }
//          logger.info(str);
//        }
//    }
//
//    // File output
//
//    int ext_pos = dendrogramTxtFilename.getAbsolutePath().lastIndexOf(".");
//    outputPrefix = dendrogramTxtFilename.getAbsolutePath().substring(0, ext_pos);
//    String outGtr = outputPrefix + ".gtr";
//    String outCdt = outputPrefix + ".cdt";
//
//    if (DEBUG)
//      logger.info("Writing output to file...");
//
//    int nColCount = 1;
//    String[] colNames = new String[nColCount];
//    colNames[nColCount - 1] = "Id";
//    String sep = "\t";
//
//    if (do_print) {
//      try {
//
//        float[] arFloats = new float[nRowCount];
//        for (int i = 0; i < arFloats.length; i++) {
//          arFloats[i] = i / 2.0f;
//        }
//        DataSource source = (do_data) ? new FloatSource1D(arFloats, nRowCount, nColCount) : null;
//
//        /* org.gnf.clustering.Utils. */HierarAlignerGCTask.GenerateCDT(outCdt, source/* null */,
//            nRowCount, nColCount, sep, rowNames, colNames, rowOrder);
//      } catch (IOException e) {
//        // TODO Auto-generated catch block
//        e.printStackTrace();
//      }
//
//      org.gnf.clustering.Utils./* JoinAlignerGCTask. */WriteTreeToFile(outGtr, nRowCount - 1,
//          arNodes, true);
//
//      if (DEBUG)
//        printMemoryUsage(logger, run_time, prevTotal, prevFree, "GNF CLUSTERER FILES PRINTED");
//
//    }
//
//    ////// Arrange row clustered list with method 0,1,2
//    List<List<FeatureListRow>> clustersList = new ArrayList<>();
//
//    // TODO: ...!
//    // Build feature list row clusters
//    for (List<Integer> cl : gnfClusters) {
//
//      List<FeatureListRow> rows_cluster = new ArrayList<>();
//      for (int i = 0; i < cl.size(); i++) {
//        rows_cluster.add(full_rows_list.get(cl.get(i)));
//      }
//      clustersList.add(rows_cluster);
//      //
//      processedRows += rows_cluster.size();
//    }
//
//    if (DEBUG)
//      printMemoryUsage(logger, run_time, prevTotal, prevFree, "GNF CLUSTERER CLUSTER_LIST");
//
//    // DEBUG stuff: REMOVE !!!
//    /** printAlignedPeakList(clustersList); */
//
//    // Fill alignment table: One row per cluster
//    for (List<FeatureListRow> cluster : clustersList) {
//
//      if (isCanceled())
//        return;
//
//      FeatureListRow targetRow = new ModularFeatureListRow((ModularFeatureList) alignedPeakList, newRowID);
//      newRowID++;
//      alignedPeakList.addRow(targetRow);
//      //
//      infoRowsBackup.put((ModularFeatureListRow) targetRow, new Object[] {new HashMap<RawDataFile, Double[]>(),
//          new HashMap<RawDataFile, FeatureIdentity>(), new HashMap<RawDataFile, Double>()});
//
//      for (FeatureListRow row : cluster) {
//
//        // Add all non-existing identities from the original row to the
//        // aligned row
//        // Set the preferred identity
//        targetRow.setPreferredFeatureIdentity(row.getPreferredFeatureIdentity());
//
//        // Add all peaks from the original row to the aligned row
//        // for (RawDataFile file : row.getRawDataFiles()) {
//        for (RawDataFile file : alignedPeakList.getRawDataFiles()) {
//
//          if (Arrays.asList(row.getRawDataFiles()).contains(file)) {
//
//            Feature originalPeak = row.getFeature(file);
//            if (originalPeak != null) {
//
//              targetRow.addFeature(file, new ModularFeature((ModularFeatureList) alignedPeakList,originalPeak));
//
//            } else {
//              setStatus(TaskStatus.ERROR);
//              setErrorMessage("Cannot run alignment, no originalPeak");
//              return;
//            }
//
//          }
//
//        }
//
//        // Copy all possible peak identities, if these are not already
//        // present
//        for (FeatureIdentity identity : row.getPeakIdentities()) {
//          FeatureIdentity clonedIdentity = (FeatureIdentity) identity.clone();
//          if (!FeatureUtils.containsIdentity(targetRow, clonedIdentity))
//            targetRow.addFeatureIdentity(clonedIdentity, false);
//        }
//
//        // processedRows++;
//
//      }
//
//    }
//
//    // ----------------------------------------------------------------------
//
//    /** Post-processing... **/
//    // Build reference RDFs index: We need an ordered reference here, to be
//    // able to parse
//    // correctly while reading back stored info
//    RawDataFile[] rdf_sorted = alignedPeakList.getRawDataFiles().toArray(RawDataFile[]::new);
//    Arrays.sort(rdf_sorted, new RawDataFileSorter(SortingDirection.Ascending));
//
//    // Process
//    for (FeatureListRow targetRow : infoRowsBackup.keySet()) {
//
//      if (isCanceled())
//        return;
//
//      // Refresh averaged RTs...
//      // TODO: .update()
//      //((ModularFeatureListRow) targetRow).update();
//
//    }
//
//    //
//    if (DEBUG) {
//      endTime = System.currentTimeMillis();
//      ms = (endTime - startTime);
//      logger.info("## >> Whole JoinAlignerGCTask processing took " + Float.toString(ms) + " ms.");
//    }
//
//    // ----------------------------------------------------------------------
//
//    // Add new aligned feature list to the project
//    this.project.addFeatureList(alignedPeakList);
//
//    if (DEBUG) {
//      for (RawDataFile rdf : alignedPeakList.getRawDataFiles())
//        logger.info("RDF: " + rdf);
//    }
//
//    // Add task description to peakList
//    alignedPeakList.addDescriptionOfAppliedTask(
//        new SimpleFeatureListAppliedMethod(HierarAlignerGCTask.TASK_NAME,
//            HierarAlignerGcModule.class, parameters, getModuleCallDate()));
//
//    logger.info("Finished join aligner GC");
//    setStatus(TaskStatus.FINISHED);
//
//  }
//
//  /**
//   * Two clusters can be merged if and only if: - The resulting merged cluster: (their parent)
//   * doesn't exceed 'level' leaves - The distance between them two is acceptable (close enough)
//   */
//  // private List<List<Integer>> getValidatedClusters_3(
//  // /*ClusteringResult clusteringResult*/org.gnf.clustering.Node[] arNodes,
//  // float minCorrValue, int level, double max_dist, DistanceMatrix1D distMtx
//  // /*,Set<Integer> flatLeaves*/) {
//  private List<List<Integer>> getValidatedClusters_3(org.gnf.clustering.Node[] arNodes,
//      float minCorrValue, int level, double max_dist, DistanceMatrix distMtx,
//      RowVsRowDistanceProvider distProvider) {
//
//    List<List<Integer>> validatedClusters = new ArrayList<>();
//
//    int nNodes = arNodes.length; // distMtx.getRowCount() - 1;
//    if (DEBUG)
//      logger.info("nNodes=" + nNodes + " | arNodes.length=" + arNodes.length);
//
//    boolean do_compute_best = false;
//
//    float maxCorrDist = 0;
//    if (do_compute_best) {
//      for (int nNode = 0; nNode < nNodes; nNode++) {
//
//        if (arNodes[nNode].m_fDistance > maxCorrDist)
//          maxCorrDist = (float) arNodes[nNode].m_fDistance;
//
//      }
//      if (maxCorrDist < 1)
//        maxCorrDist = 1;
//    }
//
//    // Find nodes that matched with good quality (> minCorrValue)
//    // (Assuming the 'arNodes' as already sorted on this very same criteria)
//    // @See org.gnf.clustering.Utils.NodeSort()
//    List<Integer> bestNodes = new ArrayList<>();
//    if (do_compute_best) {
//      for (int nNode = 0; nNode < nNodes; nNode++) {
//
//        org.gnf.clustering.Node node = arNodes[nNode];
//
//        float corr_val = (float) ((maxCorrDist - node.m_fDistance) / maxCorrDist);
//        // And if matched good enough
//        if (corr_val >= minCorrValue) {
//
//          bestNodes.add(nNode);
//        } else {
//          break; // Because 'arNodes' is sorted (do not break
//                 // otherwise!)
//        }
//      }
//    }
//    if (DEBUG_2)
//      logger.info("###BEST NODES (size:" + bestNodes.size() + "): " + bestNodes.toString());
//
//    // Find nodes that can be clusters (nb leaves < level)
//    // for (int nBest: bestNodes) {
//
//    if (DEBUG_2)
//      logger.info("##START TRACING HIERARCHY... (starting from furthest node)");
//
//    // TODO: ... Implement all stuff for cutoff, right now, just browsing
//    // the whole tree from very worst
//    // correlation scoring node
//    // int nBest = bestNodes.get(bestNodes.size() - 1); //112; //
//    int nBest = arNodes.length - 1; // 112; //
//    if (nBest < 0) {
//      nBest = -nBest - 1;
//    }
//
//    if (DEBUG_2)
//      logger.info("#TRACING BEST NODE '" + nBest + "' :");
//    // **validatedClusters.addAll(recursive_validate_clusters_3(arNodes,
//    // nBest, level, max_dist, distMtx));
//    validatedClusters.addAll(
//        recursive_validate_clusters_3(arNodes, nBest, level, max_dist, distMtx, distProvider));
//
//    if (DEBUG) {
//      // Check integrity
//      Set<Integer> leaves = new HashSet<>();
//      for (List<Integer> clust_leaves : validatedClusters) {
//
//        leaves.addAll(clust_leaves);
//      }
//      logger.info("Leafs are (count:" + leaves.size() + "):");
//      logger.info(Arrays.toString(leaves.toArray()));
//    }
//    // -
//    if (DEBUG_2)
//      printValidatedClusters_3(validatedClusters);
//
//    return validatedClusters;
//  }
//
//  // -
//  List<List<Integer>> recursive_validate_clusters_3(org.gnf.clustering.Node[] arNodes, int nNode,
//      int level, /*
//                  * float minCorrValue,
//                  */
//      double max_dist, DistanceMatrix distMtx, RowVsRowDistanceProvider distProvider) {
//
//    List<List<Integer>> validatedClusters = new ArrayList<>();
//
//    // int nNodes = arNodes.length;
//
//    if (nNode < 0) {
//      nNode = -nNode - 1;
//    }
//
//    // WARN: Skip the trees's super parent in any case !!
//    if (nNode >= arNodes.length) {
//      /* nNode = 0; */ return validatedClusters;
//    }
//
//    org.gnf.clustering.Node node = arNodes[nNode];
//
//    // Is leaf parent node => no need to go further: this is a cluster!
//    boolean is_dual_leaf_node = (node.m_nLeft >= 0 && node.m_nRight >= 0);
//    if (DEBUG_2)
//      logger.info("\n# >>>>>>>>>>>>> BEGIN ITERATING NODE #" + nNode + " <<<<<<<<<<<< '"
//          + node.toString() + "' (Is dual leaf? " + is_dual_leaf_node + ")");
//    // if (!is_dual_leaf_node) {
//
//    List<Integer> leaves = getLeafIds(arNodes, nNode);
//
//    if (DEBUG_2) {
//
//      logger.info("#NB lEAVES " + leaves.size() + " (expected lvl: " + level + ")");
//      // if (leaves.size() <= level) {
//
//      logger.info("#GET INTO NODE lEAVES " + node.toString());
//
//      // If can be a cluster: Is the current node a splitting point?
//      // List<Integer> left_leaves = getLeafIds(arNodes, node.m_nLeft);
//      // List<Integer> right_leaves = getLeafIds(arNodes, node.m_nRight);
//      // //
//      // logger.info("getLeafIds(arNodes, " + node.m_nLeft + "): "
//      // + left_leaves);
//      // logger.info("getLeafIds(arNodes, " + node.m_nRight + "): "
//      // + right_leaves);
//
//      logger.info("getLeafIds(arNodes, " + nNode + "): " + leaves);
//    }
//
//    // Check validity
//    boolean node_is_cluster = true;
//    float max_dist_2 = Float.MIN_VALUE;
//    // -
//    boolean nb_leaves_ok = (leaves.size() <= level);
//    // -
//    // Compare distances of each leaf to each other to check cluster's
//    // consistency
//    if (nb_leaves_ok) {
//      for (int i = 0; i < leaves.size(); i++) {
//        for (int j = i + 1; j < leaves.size(); j++) {
//
//          // Get distance between left and right leafs
//          float dist = 0.0f;
//          if (distMtx != null) {
//            dist = distMtx.getValue(leaves.get(i), leaves.get(j));
//          } else {
//
//            dist = (float) distProvider.getRankedDistance(leaves.get(i), leaves.get(j),
//                // RangeUtils.rangeLength(mzRange) / 2.0,
//                // RangeUtils.rangeLength(rtRange) / 2.0,
//                mzTolerance.getMzTolerance(), rtTolerance.getTolerance(), minScore);
//
//          }
//          if (max_dist_2 < dist) {
//            max_dist_2 = dist;
//          }
//          if (DEBUG_2)
//            logger.info("dist(" + leaves.get(i) + "," + leaves.get(j) + ") = " + dist);
//        }
//      }
//    }
//    node_is_cluster = nb_leaves_ok && (max_dist_2 >= 0f && max_dist_2 < max_dist + EPSILON);
//
//    if (DEBUG_2)
//      logger.info("#IS CLUSTER? " + node_is_cluster + " (nb_leaves_ok: " + nb_leaves_ok
//          + ", max_dist: " + max_dist_2 + " < " + max_dist + ")");
//
//    // If valid, keep as is...
//    if (node_is_cluster) {
//
//      if (DEBUG_2)
//        logger.info("#CLUSTER OK " + node_is_cluster + " (" + max_dist_2 + " / " + max_dist + ")");
//
//      validatedClusters.add(leaves);
//    }
//    // Otherwise, split! (ie. iterate through left and right branches)
//    else {
//
//      // Is node: Recurse on left
//      if (node.m_nLeft < 0)
//        validatedClusters.addAll(recursive_validate_clusters_3(arNodes, node.m_nLeft, level, /*
//                                                                                              * minCorrValue,
//                                                                                              */
//            max_dist, distMtx, distProvider));
//      // Is leaf: Append
//      else
//        validatedClusters.add(Arrays.asList(new Integer[] {node.m_nLeft}));
//      // -
//      // Is node: Recurse on right
//      if (node.m_nRight < 0)
//        validatedClusters.addAll(recursive_validate_clusters_3(arNodes, node.m_nRight, level, /*
//                                                                                               * minCorrValue,
//                                                                                               */
//            max_dist, distMtx, distProvider));
//      // Is leaf: Append
//      else
//        validatedClusters.add(Arrays.asList(new Integer[] {node.m_nRight}));
//
//    }
//
//    if (DEBUG_2)
//      logger.info("# >>>>>>>>>>>>> END ITERATING NODE #" + nNode + " <<<<<<<<<<<< '"
//          + node.toString() + "' (Is dual leaf? " + is_dual_leaf_node + ")");
//
//    return validatedClusters;
//  }
//
//  // -
//  List<Integer> getLeafIds(org.gnf.clustering.Node[] arNodes,
//      int nNode/* org.gnf.clustering.Node parentNode *//*
//                                                        * , List<Integer> doneNodes
//                                                        */) {
//
//    List<Integer> leafIds = new ArrayList<>();
//
//    // logger.info("(0) nNode: " + nNode);
//    if (nNode < 0) {
//      nNode = -nNode - 1;
//    }
//    // logger.info("(1) nNode: " + nNode);
//
//    // WARN: Skip the trees's super parent in any case !!
//    if (nNode >= arNodes.length) {
//      /* nNode = 0; */ return leafIds;
//    }
//
//    if (arNodes[nNode].m_nLeft < 0) // Is node => recurse ...
//                                    // getLeafIds(arNodes,
//                                    // -arNodes[nNode].m_nLeft - 1)
//      leafIds.addAll(getLeafIds(arNodes, arNodes[nNode].m_nLeft));
//    else // Is leaf => append
//      leafIds.add(arNodes[nNode].m_nLeft);
//
//    if (arNodes[nNode].m_nRight < 0) // Is node => recurse
//      leafIds.addAll(getLeafIds(arNodes, arNodes[nNode].m_nRight));
//    else // Is leaf => append
//      leafIds.add(arNodes[nNode].m_nRight);
//
//    return leafIds;
//  }
//
//  // -
//  void printValidatedClusters_3(List<List<Integer>> validatedClusters) {
//
//    int i = 0;
//    for (List<Integer> cl : validatedClusters) {
//
//      logger.info("CLUST#" + i + ": " + cl.toString());
//      i++;
//    }
//  }
//
//  public static double getAdjustedRT(double rt, double b_offset, double a_scale) {
//    double delta_rt = a_scale * rt + b_offset;
//    return (rt + delta_rt);
//  }
//
//  public static double getReverseAdjustedRT(double rt, double b_offset, double a_scale) {
//    double delta_rt = a_scale * rt + b_offset;
//    return (rt - delta_rt);
//  }
//
//  public static void GenerateCDT(final String outFileName, DataSource source, int nRowCount,
//      int nColCount, String separator, final String[] rowNames, final String[] colNames,
//      int[] rowOrder) throws IOException {
//
//    FileWriter fstream = new FileWriter(outFileName);
//    BufferedWriter writer = new BufferedWriter(fstream);
//
//    String outHead = "GID\tDESCR\tNAME" + ((source != null) ? "\t" : "");
//    if (source != null) { // Case no additional columns, except info ones
//      for (int i = 0; i < nColCount - 1; i++)
//        outHead += colNames[i] + "\t";
//      outHead += colNames[nColCount - 1] + "\n";
//    } else {
//      outHead += "\n";
//    }
//    writer.write(outHead);
//    for (int i = 0; i < nRowCount; i++) {
//      int n = rowOrder[i];
//      String outRow = "GENE" + (org.gnf.clustering.Utils.IntToStr(n + 1)) + "X\t";
//      outRow += rowNames[n] + "\t" + rowNames[n] + ((source != null) ? "\t" : "");
//      if (source != null) {
//        for (int j = 0; j < nColCount - 1; j++)
//          outRow += org.gnf.clustering.Utils.FloatToStr(source.getValue(n, j), -1) + "\t";
//        outRow += org.gnf.clustering.Utils.FloatToStr(source.getValue(n, nColCount - 1), -1);
//      }
//      if (i < nRowCount - 1)
//        outRow += "\n";
//      writer.write(outRow);
//    }
//    writer.close();
//
//  }
//
//  /**
//   * MEMORY check stuffs
//   */
//  static final int MB = 1024 * 1024;
//
//  static int toMB(long bytes) {
//    return (int) Math.rint(bytes / MB);
//  }
//
//  // -
//  // Prints in MegaBytes
//  public static void printMemoryUsage(Logger logger, Runtime rt, Long prevTotal, Long prevFree,
//      String prefix) {
//
//    long max = rt.maxMemory();
//    long total = rt.totalMemory();
//    long free = rt.freeMemory();
//    if (total != prevTotal || free != prevFree) {
//      long used = total - free;
//      long prevUsed = (prevTotal - prevFree);
//      logger.info("## [" + prefix + "] MEM USAGE [max: " + toMB(max) + "] ## >>> Total: "
//          + toMB(total) + ", Used: " + toMB(used) + ", ∆Used: " + toMB(used - prevUsed) + ", Free: "
//          + toMB(free) + ", ∆Free: " + toMB(free - prevFree));
//      prevTotal = total;
//      prevFree = free;
//    }
//  }
//
//}
