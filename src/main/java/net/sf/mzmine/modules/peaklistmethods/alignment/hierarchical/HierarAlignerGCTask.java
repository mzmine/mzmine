/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.alignment.hierarchical;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import org.gnf.clustering.DataSource;
import org.gnf.clustering.DistanceMatrix;
import org.gnf.clustering.FloatSource1D;
import org.gnf.clustering.LinkageMode;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimpleFeature;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.hierarchical.ClustererType;
//import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompound;
//import net.sf.mzmine.modules.peaklistmethods.normalization.rtadjuster.JDXCompoundsIdentificationSingleTask;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.SortingDirection;

public class HierarAlignerGCTask extends AbstractTask {

    private Logger logger = Logger.getLogger(this.getClass().getName());

    public static String TASK_NAME = "Hierarchical aligner (GC)";

    private final MZmineProject project;
    private PeakList peakLists[];
    private PeakList alignedPeakList;

    // Processed rows counter
    private int processedRows, totalRows;

    private String peakListName;
    private LinkageMode linkageStartegyType;

    // private boolean use_hybrid_K;
    // private int hybrid_K_value;

    private boolean saveRAMratherThanCPU_1;
    private boolean saveRAMratherThanCPU_2;
    //
    // private boolean useOldestRDFAncestor;
    private MZTolerance mzTolerance;
    private RTTolerance rtTolerance;
    private double mzWeight, rtWeight;
    private double minScore;
    // private double idWeight;
    //
    // private boolean useApex, useKnownCompoundsAsRef;
    // private boolean useDetectedMzOnly;
    // private RTTolerance rtToleranceAfter;

    private boolean exportDendrogramAsTxt;
    private File dendrogramTxtFilename;

    /**
     * GLG HACK: temporary removed for clarity private boolean sameIDRequired,
     * sameChargeRequired, compareIsotopePattern;
     **/
    private ParameterSet parameters;

    // ID counter for the new peaklist
    private int newRowID = 1;

    //
    private Format rtFormat = MZmineCore.getConfiguration().getRTFormat();

    //
    private final double maximumScore; // = 1.0d;
    // For comparing small differences.
    public static final double EPSILON = 0.0000001;

    private static final boolean DEBUG = false;
    private static final boolean DEBUG_2 = false;
    List<PeakListRow> full_rows_list;

    private ClustererType CLUSTERER_TYPE;

    public static final boolean USE_DOUBLE_PRECISION_FOR_DIST = false;

    private ClusteringProgression clustProgress;

    // Minimum score ever.
    // TODO: better use "Double.MIN_VALUE" rather than zero (it has consequences
    // !!!!)
    // (0.0 is fine for 'Dot Product' method, but not for 'Person Correlation')
    //// public static final double MIN_SCORE_ABSOLUTE = Double.MIN_VALUE;
    public static final double MIN_SCORE_ABSOLUTE = 0.0;

    HierarAlignerGCTask(MZmineProject project, ParameterSet parameters) {

        this.project = project;
        this.parameters = parameters;

        peakLists = parameters.getParameter(HierarAlignerGCParameters.peakLists)
                .getValue().getMatchingPeakLists();

        peakListName = parameters
                .getParameter(HierarAlignerGCParameters.peakListName)
                .getValue();

        // saveRAMratherThanCPU_1 = parameters
        // .getParameter(HierarAlignerGCParameters.saveRAMratherThanCPU_1)
        // .getValue();
        // saveRAMratherThanCPU_2 = parameters
        // .getParameter(HierarAlignerGCParameters.saveRAMratherThanCPU_2)
        // .getValue();
        saveRAMratherThanCPU_1 = false;
        saveRAMratherThanCPU_2 = false;

        mzTolerance = parameters
                .getParameter(HierarAlignerGCParameters.MZTolerance).getValue();
        rtTolerance = parameters
                .getParameter(HierarAlignerGCParameters.RTTolerance).getValue();

        mzWeight = parameters.getParameter(HierarAlignerGCParameters.MZWeight)
                .getValue();

        rtWeight = parameters.getParameter(HierarAlignerGCParameters.RTWeight)
                .getValue();

        minScore = parameters.getParameter(HierarAlignerGCParameters.minScore)
                .getValue();

        // idWeight = parameters.getParameter(JoinAlignerParameters.IDWeight)
        // .getValue();
        // idWeight = 0.0;

        // ***
        //// useApex = parameters.getParameter(
        //// JoinAlignerGCParameters.useApex).getValue();
        // useApex = true;
        // ***

        exportDendrogramAsTxt = parameters
                .getParameter(HierarAlignerGCParameters.exportDendrogramTxt)
                .getValue();
        dendrogramTxtFilename = parameters
                .getParameter(HierarAlignerGCParameters.dendrogramTxtFilename)
                .getValue();

        /**
         * GLG HACK: temporarily removed for clarity sameChargeRequired =
         * parameters.getParameter(
         * JoinAlignerParameters.SameChargeRequired).getValue();
         * 
         * sameIDRequired = parameters.getParameter(
         * JoinAlignerParameters.SameIDRequired).getValue();
         * 
         * compareIsotopePattern = parameters.getParameter(
         * JoinAlignerParameters.compareIsotopePattern).getValue();
         **/

        //

        // CLUSTERER_TYPE = parameters
        // .getParameter(HierarAlignerGCParameters.clusterer_type)
        // .getValue();// .ordinal();
        CLUSTERER_TYPE = ClustererType.CACHED;

        //
        ClusteringLinkageStrategyType linkageStartegyType_0 = parameters
                .getParameter(HierarAlignerGCParameters.linkageStartegyType_0)
                .getValue();
        switch (linkageStartegyType_0) {
        case SINGLE:
            linkageStartegyType = LinkageMode.MIN;
            break;
        case AVERAGE:
            linkageStartegyType = LinkageMode.AVG;
            break;
        case COMPLETE:
            linkageStartegyType = LinkageMode.MAX;
            break;
        default:
            break;
        }

        // this.hybrid_K_value = parameters.getParameter(
        // JoinAlignerGCParameters.hybrid_K_value).getValue();

        //
        maximumScore = mzWeight + rtWeight;

        //
        clustProgress = new ClusteringProgression();
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getTaskDescription()
     */
    public String getTaskDescription() {
        return "Join aligner GC, " + peakListName + " (" + peakLists.length
                + " peak lists)";
    }

    /**
     * @see net.sf.mzmine.taskcontrol.Task#getFinishedPercentage()
     */
    public double getFinishedPercentage() {
        if (totalRows == 0)
            return 0f;
        // return (double) processedRows / (double) totalRows;
        double progress = (double) (processedRows
                + (clustProgress.getProgress() * (double) totalRows / 3.0d))
                / (double) totalRows;
        // System.out.println(">> THE progress: " + progress);
        // System.out.println("Caught progress: " +
        // clustProgress.getProgress());
        return progress;
    }

    /**
     * @see Runnable#run()
     */
    public void run() {

        // Check options validity
        if ((Math.abs(mzWeight) < EPSILON) && (Math.abs(rtWeight) < EPSILON)) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage(
                    "Cannot run alignment, all the weight parameters are zero!");
            return;
        }

        setStatus(TaskStatus.PROCESSING);
        logger.info("Running join aligner");

        // TIME STUFF
        long startTime, endTime;
        float ms;
        //
        startTime = System.currentTimeMillis();

        // MEMORY STUFF
        Runtime run_time = Runtime.getRuntime();
        Long prevTotal = 0l;
        Long prevFree = run_time.freeMemory();
        printMemoryUsage(run_time, prevTotal, prevFree, "START TASK...");

        // Remember how many rows we need to process. Each row will be processed
        // /*twice*/ three times:
        // - first for score calculation
        // - second for creating linkages
        // - third for actual alignment
        for (int i = 0; i < peakLists.length; i++) {
            totalRows += peakLists[i].getNumberOfRows() * 3;
        }

        // Collect all data files
        Vector<RawDataFile> allDataFiles = new Vector<RawDataFile>();
        for (PeakList peakList : peakLists) {

            for (RawDataFile dataFile : peakList.getRawDataFiles()) {

                // Each data file can only have one column in aligned peak list
                if (allDataFiles.contains(dataFile)) {
                    setStatus(TaskStatus.ERROR);
                    setErrorMessage("Cannot run alignment, because file "
                            + dataFile + " is present in multiple peak lists");
                    return;
                }

                allDataFiles.add(dataFile);
            }
        }

        // Create a new aligned peak list
        alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[0]));

        printMemoryUsage(run_time, prevTotal, prevFree, "COMPOUND DETECTED");

        /** Alignment mapping **/
        // Iterate source peak lists
        Hashtable<SimpleFeature, Double> rtPeaksBackup = new Hashtable<SimpleFeature, Double>();
        Hashtable<PeakListRow, Object[]> infoRowsBackup = new Hashtable<PeakListRow, Object[]>();

        // Since clustering is now order independent, option removed!
        // Build comparison order
        ArrayList<Integer> orderIds = new ArrayList<Integer>();
        for (int i = 0; i < peakLists.length; ++i) {
            orderIds.add(i);
        }
        Integer[] newIds = orderIds.toArray(new Integer[orderIds.size()]);
        //

        // TriangularMatrix distances = null;
        DistanceMatrix distancesGNF_Tri = null;
        DistanceMatrix distancesGNF_Tri_Bkp = null;

        int nbPeaks = 0;
        for (int i = 0; i < newIds.length; ++i) {
            PeakList peakList = peakLists[newIds[i]];
            nbPeaks += peakList.getNumberOfRows();
            logger.info("> Peaklist '" + peakList.getName() + "' [" + newIds[i]
                    + "] has '" + peakList.getNumberOfRows() + "' rows.");
        }

        // If 'Hybrid' or no distance matrix: no need for a matrix
        if (CLUSTERER_TYPE == ClustererType.HYBRID || !saveRAMratherThanCPU_1) {
            // distances = new double[nbPeaks][nbPeaks];

            int nRowCount = nbPeaks;
            distancesGNF_Tri = new DistanceMatrixTriangular1D2D(nRowCount);
        }

        full_rows_list = new ArrayList<>();

        for (int i = 0; i < newIds.length; ++i) {

            PeakList peakList = peakLists[newIds[i]];

            PeakListRow allRows[] = peakList.getRows();
            for (int j = 0; j < allRows.length; ++j) {

                PeakListRow row = allRows[j];
                full_rows_list.add(row);
            }
        }

        RowVsRowDistanceProvider distProvider = new RowVsRowDistanceProvider(
                project,
                // useOldestRDFAncestor,
                // rtAdjustementMapping,
                full_rows_list, mzWeight, rtWeight,
                // useApex,
                // useKnownCompoundsAsRef,
                // useDetectedMzOnly,
                // rtToleranceAfter,
                maximumScore);

        // If 'Hybrid' or no distance matrix: no need for a matrix
        if (CLUSTERER_TYPE == ClustererType.HYBRID || !saveRAMratherThanCPU_1) {

            for (int x = 0; x < nbPeaks; ++x) {

                for (int y = x; y < nbPeaks; ++y) {

                    float dist = (float) distProvider.getRankedDistance(x, y,
                            mzTolerance.getMzTolerance(),
                            rtTolerance.getTolerance(), minScore);

                    // if (CLUSTERER_TYPE == ClustererType.CLASSIC_OLD)
                    // distances.set(x, y , dist);
                    // else
                    distancesGNF_Tri.setValue(x, y, dist);

                }

                processedRows++;
                logger.info("Treating lists: "
                        + (Math.round(100 * processedRows / (double) nbPeaks))
                        + " %");

            }
        }

        printMemoryUsage(run_time, prevTotal, prevFree, "DISTANCES COMPUTED");

        //////
        double max_dist = maximumScore; // Math.abs(row.getBestPeak().getRT() -
                                        // k_row.getBestPeak().getRT()) /
                                        // ((RangeUtils.rangeLength(rtRange) /
                                        // 2.0));

        // String newickCluster;
        List<List<Integer>> gnfClusters = null;

        //////

        boolean do_verbose = true;
        boolean do_cluster = true;
        boolean do_print = (exportDendrogramAsTxt);
        boolean do_data = false;

        org.gnf.clustering.Node[] arNodes = null;
        int nRowCount = full_rows_list.size();

        String[] rowNames = null;
        if (do_print) {
            rowNames = new String[nRowCount];
            for (int i = 0; i < nRowCount; i++) {
                // rowNames[i] = "ID_" + i + "_" +
                // full_rows_list.get(i).getID();
                Feature peak = full_rows_list.get(i).getBestPeak();
                double rt = peak.getRT();
                int end = peak.getDataFile().getName().indexOf(" ");
                String short_fname = peak.getDataFile().getName().substring(0,
                        end);
                rowNames[i] = "@" + rtFormat.format(rt) + "^[" + short_fname
                        + "]";
            }
        }
        String outputPrefix = null;

        if (CLUSTERER_TYPE == ClustererType.CLASSIC) { // Pure Hierar!

            outputPrefix = "hierar_0";

            throw new IllegalStateException(
                    "'" + ClustererType.CLASSIC.toString()
                            + "' algorithm not yet implemented!");

        } else if (CLUSTERER_TYPE == ClustererType.CACHED) { // Pure Hierar!

            // TODO: ...!
            if (DEBUG_2)
                System.out.println(distancesGNF_Tri.toString());

            if (saveRAMratherThanCPU_2) { // Requires: distances values will be
                                          // recomputed on demand during
                                          // "getValidatedClusters_3()"
                distancesGNF_Tri_Bkp = null; // No duplicate backup storage!
            } else { // Otherwise, backing up the distance matrix (matrix being
                     // deeply changed during "clusterDM()", then no more
                     // exploitable)
                distancesGNF_Tri_Bkp = new DistanceMatrixTriangular1D2D(
                        distancesGNF_Tri);
                printMemoryUsage(run_time, prevTotal, prevFree,
                        "GNF CLUSTERER BACKUP MATRIX");
            }

            System.out.println("Clustering...");
            if (distancesGNF_Tri != null)
                arNodes = org.gnf.clustering.sequentialcache.SequentialCacheClustering
                        .clusterDM(distancesGNF_Tri, linkageStartegyType, null,
                                nRowCount);

            distancesGNF_Tri = null;
            System.gc();

            printMemoryUsage(run_time, prevTotal, prevFree,
                    "GNF CLUSTERER DONE");

            if (DEBUG_2)
                System.out.println(distancesGNF_Tri.toString());

            if (DEBUG_2)
                for (int i = 0; i < arNodes.length; i++) {
                    System.out.println("Node " + i + ": " + arNodes[i]);
                }

            // TODO: Use usual interfacing ...
            // ClusteringResult<org.gnf.clustering.Node> clust_res = new
            // ClusteringResult<>(
            // Arrays.asList(arNodes), null, 0, null);

            outputPrefix = "hierar_1";

        } else if (CLUSTERER_TYPE == ClustererType.HYBRID) { // Hybrid!

            throw new IllegalStateException(
                    "'" + ClustererType.HYBRID.toString()
                            + "' algorithm not yet implemented!");

        }

        // Sort Nodes by correlation score (Required in
        // 'getValidatedClusters_3')
        int[] rowOrder = new int[nRowCount];
        System.out.println("Sorting tree nodes...");
        org.gnf.clustering.Utils.NodeSort(arNodes, nRowCount - 2, 0, rowOrder);

        if (do_cluster) {

            gnfClusters = getValidatedClusters_3(arNodes, 0.0f, newIds.length,
                    max_dist, distancesGNF_Tri_Bkp, distProvider);

            // -- Print
            if (DEBUG_2 && do_verbose)
                for (int i = 0; i < gnfClusters.size(); i++) {
                    List<Integer> cl = gnfClusters.get(i);
                    String str = "";
                    for (int j = 0; j < cl.size(); j++) {
                        int r = cl.get(j);
                        str += cl.get(j) + "^(" + full_rows_list.get(r).getID()
                                + ", " + full_rows_list.get(r).getAverageRT()
                                + ")" + " ";
                    }
                    System.out.println(str);
                }
        }

        // File output

        int ext_pos = dendrogramTxtFilename.getAbsolutePath().lastIndexOf(".");
        outputPrefix = dendrogramTxtFilename.getAbsolutePath().substring(0,
                ext_pos);
        String outGtr = outputPrefix + ".gtr";
        String outCdt = outputPrefix + ".cdt";

        System.out.println("Writing output to file...");

        int nColCount = 1;
        String[] colNames = new String[nColCount];
        colNames[nColCount - 1] = "Id";
        String sep = "\t";

        if (do_print) {
            try {

                float[] arFloats = new float[nRowCount];
                for (int i = 0; i < arFloats.length; i++) {
                    arFloats[i] = i / 2.0f;
                }
                DataSource source = (do_data)
                        ? new FloatSource1D(arFloats, nRowCount, nColCount)
                        : null;

                /* org.gnf.clustering.Utils. */HierarAlignerGCTask.GenerateCDT(
                        outCdt, source/* null */, nRowCount, nColCount, sep,
                        rowNames, colNames, rowOrder);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            org.gnf.clustering.Utils./* JoinAlignerGCTask. */WriteTreeToFile(
                    outGtr, nRowCount - 1, arNodes, true);

            printMemoryUsage(run_time, prevTotal, prevFree,
                    "GNF CLUSTERER FILES PRINTED");

        }

        ////// Arrange row clustered list with method 0,1,2
        List<List<PeakListRow>> clustersList = new ArrayList<>();

        // TODO: ...!
        // Build peak list row clusters
        for (List<Integer> cl : gnfClusters) {

            List<PeakListRow> rows_cluster = new ArrayList<>();
            for (int i = 0; i < cl.size(); i++) {
                rows_cluster.add(full_rows_list.get(cl.get(i)));
            }
            clustersList.add(rows_cluster);
            //
            processedRows += rows_cluster.size();
        }

        printMemoryUsage(run_time, prevTotal, prevFree,
                "GNF CLUSTERER CLUSTER_LIST");

        // DEBUG stuff: REMOVE !!!
        /** printAlignedPeakList(clustersList); */

        // Fill alignment table: One row per cluster
        for (List<PeakListRow> cluster : clustersList) {

            if (isCanceled())
                return;

            PeakListRow targetRow = new SimplePeakListRow(newRowID);
            newRowID++;
            alignedPeakList.addRow(targetRow);
            //
            infoRowsBackup.put(targetRow,
                    new Object[] { new HashMap<RawDataFile, Double[]>(),
                            new HashMap<RawDataFile, PeakIdentity>(),
                            new HashMap<RawDataFile, Double>() });

            for (PeakListRow row : cluster) {

                // Add all non-existing identities from the original row to the
                // aligned row
                // Set the preferred identity
                targetRow.setPreferredPeakIdentity(
                        row.getPreferredPeakIdentity());

                // Add all peaks from the original row to the aligned row
                // for (RawDataFile file : row.getRawDataFiles()) {
                for (RawDataFile file : alignedPeakList.getRawDataFiles()) {

                    if (Arrays.asList(row.getRawDataFiles()).contains(file)) {

                        Feature originalPeak = row.getPeak(file);
                        if (originalPeak != null) {

                            targetRow.addPeak(file, originalPeak);

                        } else {
                            setStatus(TaskStatus.ERROR);
                            setErrorMessage(
                                    "Cannot run alignment, no originalPeak");
                            return;
                        }

                    }

                }

                // Copy all possible peak identities, if these are not already
                // present
                for (PeakIdentity identity : row.getPeakIdentities()) {
                    PeakIdentity clonedIdentity = (PeakIdentity) identity
                            .clone();
                    if (!PeakUtils.containsIdentity(targetRow, clonedIdentity))
                        targetRow.addPeakIdentity(clonedIdentity, false);
                }

                // processedRows++;

            }

        }

        // ----------------------------------------------------------------------

        // Restore real RT - for the sake of consistency
        // (the adjusted one was only useful during alignment process)
        // WARN: Must be done before "Post processing" part to take advantage
        // of the "targetRow.update()" used down there
        for (SimpleFeature peak : rtPeaksBackup.keySet()) {
            peak.setRT((double) rtPeaksBackup.get(peak));
        }

        /** Post-processing... **/
        // Build reference RDFs index: We need an ordered reference here, to be
        // able to parse
        // correctly while reading back stored info
        RawDataFile[] rdf_sorted = alignedPeakList.getRawDataFiles().clone();
        Arrays.sort(rdf_sorted,
                new RawDataFileSorter(SortingDirection.Ascending));

        // Process
        for (PeakListRow targetRow : infoRowsBackup.keySet()) {

            if (isCanceled())
                return;

            // Refresh averaged RTs...
            ((SimplePeakListRow) targetRow).update();

        }

        //
        endTime = System.currentTimeMillis();
        ms = (endTime - startTime);
        logger.info("## >> Whole JoinAlignerGCTask processing took "
                + Float.toString(ms) + " ms.");

        // ----------------------------------------------------------------------

        // Add new aligned peak list to the project
        this.project.addPeakList(alignedPeakList);

        for (RawDataFile rdf : alignedPeakList.getRawDataFiles())
            System.out.println("RDF: " + rdf);

        // Add task description to peakList
        alignedPeakList.addDescriptionOfAppliedTask(
                new SimplePeakListAppliedMethod(HierarAlignerGCTask.TASK_NAME,
                        parameters));

        logger.info("Finished join aligner GC");
        setStatus(TaskStatus.FINISHED);

    }

    /**
     * Two clusters can be merged if and only if: - The resulting merged
     * cluster: (their parent) doesn't exceed 'level' leaves - The distance
     * between them two is acceptable (close enough)
     */
    // private List<List<Integer>> getValidatedClusters_3(
    // /*ClusteringResult clusteringResult*/org.gnf.clustering.Node[] arNodes,
    // float minCorrValue, int level, double max_dist, DistanceMatrix1D distMtx
    // /*,Set<Integer> flatLeaves*/) {
    private List<List<Integer>> getValidatedClusters_3(
            org.gnf.clustering.Node[] arNodes, float minCorrValue, int level,
            double max_dist, DistanceMatrix distMtx,
            RowVsRowDistanceProvider distProvider) {

        List<List<Integer>> validatedClusters = new ArrayList<>();

        int nNodes = arNodes.length; // distMtx.getRowCount() - 1;
        System.out.println(
                "nNodes=" + nNodes + " | arNodes.length=" + arNodes.length);

        boolean do_compute_best = false;

        float maxCorrDist = 0;
        if (do_compute_best) {
            for (int nNode = 0; nNode < nNodes; nNode++) {

                if (arNodes[nNode].m_fDistance > maxCorrDist)
                    maxCorrDist = (float) arNodes[nNode].m_fDistance;

            }
            if (maxCorrDist < 1)
                maxCorrDist = 1;
        }

        // Find nodes that matched with good quality (> minCorrValue)
        // (Assuming the 'arNodes' as already sorted on this very same criteria)
        // @See org.gnf.clustering.Utils.NodeSort()
        List<Integer> bestNodes = new ArrayList<>();
        if (do_compute_best) {
            for (int nNode = 0; nNode < nNodes; nNode++) {

                org.gnf.clustering.Node node = arNodes[nNode];

                float corr_val = (float) ((maxCorrDist - node.m_fDistance)
                        / maxCorrDist);
                // And if matched good enough
                if (corr_val >= minCorrValue) {

                    bestNodes.add(nNode);
                } else {
                    break; // Because 'arNodes' is sorted (do not break
                           // otherwise!)
                }
            }
        }
        if (DEBUG_2)
            System.out.println("###BEST NODES (size:" + bestNodes.size() + "): "
                    + bestNodes.toString());

        // Find nodes that can be clusters (nb leaves < level)
        // for (int nBest: bestNodes) {
        System.out.println(
                "##START TRACING HIERARCHY... (starting from furthest node)");

        // TODO: ... Implement all stuff for cutoff, right now, just browsing
        // the whole tree from very worst
        // correlation scoring node
        // int nBest = bestNodes.get(bestNodes.size() - 1); //112; //
        int nBest = arNodes.length - 1; // 112; //
        if (nBest < 0) {
            nBest = -nBest - 1;
        }

        System.out.println("#TRACING BEST NODE '" + nBest + "' :");
        // **validatedClusters.addAll(recursive_validate_clusters_3(arNodes,
        // nBest, level, max_dist, distMtx));
        validatedClusters.addAll(recursive_validate_clusters_3(arNodes, nBest,
                level, max_dist, distMtx, distProvider));

        if (DEBUG) {
            // Check integrity
            Set<Integer> leaves = new HashSet<>();
            for (List<Integer> clust_leaves : validatedClusters) {

                leaves.addAll(clust_leaves);
            }
            System.out.println("Leafs are (count:" + leaves.size() + "):");
            System.out.println(Arrays.toString(leaves.toArray()));
        }
        // -
        if (DEBUG_2)
            printValidatedClusters_3(validatedClusters);

        return validatedClusters;
    }

    // -
    List<List<Integer>> recursive_validate_clusters_3(
            org.gnf.clustering.Node[] arNodes, int nNode,
            int level, /* float minCorrValue, */
            double max_dist, DistanceMatrix distMtx,
            RowVsRowDistanceProvider distProvider) {

        List<List<Integer>> validatedClusters = new ArrayList<>();

        // int nNodes = arNodes.length;

        if (nNode < 0) {
            nNode = -nNode - 1;
        }

        // WARN: Skip the trees's super parent in any case !!
        if (nNode >= arNodes.length) {
            /* nNode = 0; */ return validatedClusters;
        }

        org.gnf.clustering.Node node = arNodes[nNode];

        // Is leaf parent node => no need to go further: this is a cluster!
        boolean is_dual_leaf_node = (node.m_nLeft >= 0 && node.m_nRight >= 0);
        if (DEBUG_2)
            System.out.println("\n# >>>>>>>>>>>>> BEGIN ITERATING NODE #"
                    + nNode + " <<<<<<<<<<<< '" + node.toString()
                    + "' (Is dual leaf? " + is_dual_leaf_node + ")");
        // if (!is_dual_leaf_node) {

        List<Integer> leaves = getLeafIds(arNodes, nNode);

        if (DEBUG_2) {

            System.out.println("#NB lEAVES " + leaves.size()
                    + " (expected lvl: " + level + ")");
            // if (leaves.size() <= level) {

            System.out.println("#GET INTO NODE lEAVES " + node.toString());

            // If can be a cluster: Is the current node a splitting point?
            // List<Integer> left_leaves = getLeafIds(arNodes, node.m_nLeft);
            // List<Integer> right_leaves = getLeafIds(arNodes, node.m_nRight);
            // //
            // System.out.println("getLeafIds(arNodes, " + node.m_nLeft + "): "
            // + left_leaves);
            // System.out.println("getLeafIds(arNodes, " + node.m_nRight + "): "
            // + right_leaves);

            System.out.println("getLeafIds(arNodes, " + nNode + "): " + leaves);
        }

        // Check validity
        boolean node_is_cluster = true;
        float max_dist_2 = Float.MIN_VALUE;
        // -
        boolean nb_leaves_ok = (leaves.size() <= level);
        // -
        // Compare distances of each leaf to each other to check cluster's
        // consistency
        if (nb_leaves_ok) {
            for (int i = 0; i < leaves.size(); i++) {
                for (int j = i + 1; j < leaves.size(); j++) {

                    // Get distance between left and right leafs
                    float dist = 0.0f;
                    if (distMtx != null) {
                        dist = distMtx.getValue(leaves.get(i), leaves.get(j));
                    } else {

                        dist = (float) distProvider.getRankedDistance(
                                leaves.get(i), leaves.get(j),
                                // RangeUtils.rangeLength(mzRange) / 2.0,
                                // RangeUtils.rangeLength(rtRange) / 2.0,
                                mzTolerance.getMzTolerance(),
                                rtTolerance.getTolerance(), minScore);

                    }
                    if (max_dist_2 < dist) {
                        max_dist_2 = dist;
                    }
                    if (DEBUG_2)
                        System.out.println("dist(" + leaves.get(i) + ","
                                + leaves.get(j) + ") = " + dist);
                }
            }
        }
        node_is_cluster = nb_leaves_ok
                && (max_dist_2 >= 0f && max_dist_2 < max_dist + EPSILON);

        if (DEBUG_2)
            System.out.println("#IS CLUSTER? " + node_is_cluster
                    + " (nb_leaves_ok: " + nb_leaves_ok + ", max_dist: "
                    + max_dist_2 + " < " + max_dist + ")");

        // If valid, keep as is...
        if (node_is_cluster) {

            if (DEBUG_2)
                System.out.println("#CLUSTER OK " + node_is_cluster + " ("
                        + max_dist_2 + " / " + max_dist + ")");

            validatedClusters.add(leaves);
        }
        // Otherwise, split! (ie. iterate through left and right branches)
        else {

            // Is node: Recurse on left
            if (node.m_nLeft < 0)
                validatedClusters.addAll(recursive_validate_clusters_3(arNodes,
                        node.m_nLeft, level, /* minCorrValue, */
                        max_dist, distMtx, distProvider));
            // Is leaf: Append
            else
                validatedClusters
                        .add(Arrays.asList(new Integer[] { node.m_nLeft }));
            // -
            // Is node: Recurse on right
            if (node.m_nRight < 0)
                validatedClusters.addAll(recursive_validate_clusters_3(arNodes,
                        node.m_nRight, level, /* minCorrValue, */
                        max_dist, distMtx, distProvider));
            // Is leaf: Append
            else
                validatedClusters
                        .add(Arrays.asList(new Integer[] { node.m_nRight }));

        }

        if (DEBUG_2)
            System.out.println("# >>>>>>>>>>>>> END ITERATING NODE #" + nNode
                    + " <<<<<<<<<<<< '" + node.toString() + "' (Is dual leaf? "
                    + is_dual_leaf_node + ")");

        return validatedClusters;
    }

    // -
    List<Integer> getLeafIds(org.gnf.clustering.Node[] arNodes,
            int nNode/* org.gnf.clustering.Node parentNode *//*
                                                              * , List<Integer>
                                                              * doneNodes
                                                              */) {

        List<Integer> leafIds = new ArrayList<>();

        // System.out.println("(0) nNode: " + nNode);
        if (nNode < 0) {
            nNode = -nNode - 1;
        }
        // System.out.println("(1) nNode: " + nNode);

        // WARN: Skip the trees's super parent in any case !!
        if (nNode >= arNodes.length) {
            /* nNode = 0; */ return leafIds;
        }

        if (arNodes[nNode].m_nLeft < 0) // Is node => recurse ...
                                        // getLeafIds(arNodes,
                                        // -arNodes[nNode].m_nLeft - 1)
            leafIds.addAll(getLeafIds(arNodes, arNodes[nNode].m_nLeft));
        else // Is leaf => append
            leafIds.add(arNodes[nNode].m_nLeft);

        if (arNodes[nNode].m_nRight < 0) // Is node => recurse
            leafIds.addAll(getLeafIds(arNodes, arNodes[nNode].m_nRight));
        else // Is leaf => append
            leafIds.add(arNodes[nNode].m_nRight);

        return leafIds;
    }

    // -
    void printValidatedClusters_3(List<List<Integer>> validatedClusters) {

        int i = 0;
        for (List<Integer> cl : validatedClusters) {

            System.out.println("CLUST#" + i + ": " + cl.toString());
            i++;
        }
    }

    public static double getAdjustedRT(double rt, double b_offset,
            double a_scale) {
        double delta_rt = a_scale * rt + b_offset;
        return (rt + delta_rt);
    }

    public static double getReverseAdjustedRT(double rt, double b_offset,
            double a_scale) {
        double delta_rt = a_scale * rt + b_offset;
        return (rt - delta_rt);
    }

    public static void GenerateCDT(final String outFileName, DataSource source,
            int nRowCount, int nColCount, String separator,
            final String[] rowNames, final String[] colNames, int[] rowOrder)
            throws IOException {

        FileWriter fstream = new FileWriter(outFileName);
        BufferedWriter writer = new BufferedWriter(fstream);

        String outHead = "GID\tDESCR\tNAME" + ((source != null) ? "\t" : "");
        if (source != null) { // Case no additional columns, except info ones
            for (int i = 0; i < nColCount - 1; i++)
                outHead += colNames[i] + "\t";
            outHead += colNames[nColCount - 1] + "\n";
        } else {
            outHead += "\n";
        }
        writer.write(outHead);
        for (int i = 0; i < nRowCount; i++) {
            int n = rowOrder[i];
            String outRow = "GENE" + (org.gnf.clustering.Utils.IntToStr(n + 1))
                    + "X\t";
            outRow += rowNames[n] + "\t" + rowNames[n]
                    + ((source != null) ? "\t" : "");
            if (source != null) {
                for (int j = 0; j < nColCount - 1; j++)
                    outRow += org.gnf.clustering.Utils
                            .FloatToStr(source.getValue(n, j), -1) + "\t";
                outRow += org.gnf.clustering.Utils
                        .FloatToStr(source.getValue(n, nColCount - 1), -1);
            }
            if (i < nRowCount - 1)
                outRow += "\n";
            writer.write(outRow);
        }
        writer.close();

    }

    /**
     * MEMORY check stuffs
     */
    static final int MB = 1024 * 1024;

    static int toMB(long bytes) {
        return (int) Math.rint(bytes / MB);
    }

    // -
    // Prints in MegaBytes
    public static void printMemoryUsage(Runtime rt, Long prevTotal,
            Long prevFree, String prefix) {

        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        if (total != prevTotal || free != prevFree) {
            long used = total - free;
            long prevUsed = (prevTotal - prevFree);
            System.out.println("## [" + prefix + "] MEM USAGE [max: "
                    + toMB(max) + "] ## >>> Total: " + toMB(total) + ", Used: "
                    + toMB(used) + ", ∆Used: " + toMB(used - prevUsed)
                    + ", Free: " + toMB(free) + ", ∆Free: "
                    + toMB(free - prevFree));
            prevTotal = total;
            prevFree = free;
        }
    }

}
