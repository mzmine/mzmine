/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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
package net.sf.mzmine.modules.peaklistmethods.alignment.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Logger;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peaklistmethods.alignment.ransac.RowVsRowScore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.MZTolerance;
import net.sf.mzmine.parameters.parametertypes.RTTolerance;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.PeakListRowSorter;
import net.sf.mzmine.util.PeakUtils;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.SortingDirection;
import net.sf.mzmine.util.SortingProperty;
import org.apache.commons.math.stat.regression.SimpleRegression;

public class GraphAlignerTask extends AbstractTask {

        private final Logger logger = Logger.getLogger(this.getClass().getName());
        private PeakList peakLists[], alignedPeakList;
        // Processed rows counter
        private int processedRows, totalRows;
        // Parameters
        private String peakListName;
        private MZTolerance mzTolerance;
        private RTTolerance rtTolerance;
        private double rtAbsoluteTolerance;
        private int regressionWindow, minPeakWindow;
        private double progress;
        private boolean sameChargeRequired;
        private ParameterSet parameters;
        // ID counter for the new peaklist
        private int newRowID = 1;
        private Random rand;

        public GraphAlignerTask(PeakList[] peakLists, ParameterSet parameters) {
                this.parameters = parameters;
                this.peakLists = peakLists;

                // Get parameter values for easier use
                peakListName = parameters.getParameter(GraphAlignerParameters.peakListName).getValue();

                mzTolerance = parameters.getParameter(GraphAlignerParameters.MZTolerance).getValue();

                rtTolerance = parameters.getParameter(GraphAlignerParameters.RTTolerance).getValue();

                rtAbsoluteTolerance = parameters.getParameter(GraphAlignerParameters.RTToleranceValueAbs).getDouble();

                minPeakWindow = parameters.getParameter(GraphAlignerParameters.minPeaksInTheGraph).getInt();

                regressionWindow = parameters.getParameter(GraphAlignerParameters.regressionWindow).getInt();

                sameChargeRequired = parameters.getParameter(GraphAlignerParameters.SameChargeRequired).getValue();

                rand = new Random();
        }

        public String getTaskDescription() {
                return "Graph aligner, " + peakListName + " (" + peakLists.length + " peak lists)";
        }

        public double getFinishedPercentage() {
                if (totalRows == 0) {
                        return 0f;
                }
                return progress;
        }

        /**
         * @see Runnable#run()
         */
        @Override
        public void run() {

                setStatus(TaskStatus.PROCESSING);
                logger.info("Running Graph aligner");

                // Remember how many rows we need to process.
                for (int i = 0; i < peakLists.length; i++) {
                        totalRows += peakLists[i].getNumberOfRows() * 3;
                }

                // Collect all data files
                List<RawDataFile> allDataFiles = new ArrayList<RawDataFile>();
                for (PeakList peakList : peakLists) {

                        for (RawDataFile dataFile : peakList.getRawDataFiles()) {

                                // Each data file can only have one column in aligned peak list
                                if (allDataFiles.contains(dataFile)) {
                                        setStatus(TaskStatus.ERROR);
                                        errorMessage = "Cannot run alignment, because file "
                                                + dataFile + " is present in multiple peak lists";
                                        return;
                                }

                                allDataFiles.add(dataFile);
                        }
                }
                // Create a new aligned peak list
                alignedPeakList = new SimplePeakList(peakListName,
                        allDataFiles.toArray(new RawDataFile[0]));

                HashMap<PeakListRow, PeakListRow> alignmentMapping = new HashMap<PeakListRow, PeakListRow>();
                for (PeakList peakList : peakLists) {
                        PeakListRow allRows[] = peakList.getRows();
                        //for each row in the main file which contains all the samples align until that moment.. get the graph of peaks..
                        for (PeakListRow row : allRows) {
                                Range mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());
                                Range rtRange = rtTolerance.getToleranceRange(row.getAverageRT());

                                // Get all rows of the aligned peaklist within parameter limits
                                PeakListRow candidateRows[] = alignedPeakList.getRowsInsideScanAndMZRange(rtRange, mzRange);

                                double bestScore = 100000;
                                PeakListRow bestCandidate = null;
                                for (PeakListRow candidate : candidateRows) {
                                        double score = this.getScore(peakList, row, alignedPeakList, candidate);
                                        if (score < bestScore) {
                                                bestScore = score;
                                                bestCandidate = candidate;
                                        }
                                }
                                if (bestCandidate != null) {
                                        alignmentMapping.put(row, bestCandidate);
                                }


                                progress = (double) processedRows++ / (double) totalRows;

                        }

                        alignmentMapping = this.getAlignmentMap(allRows, this.getRegressionList(alignmentMapping, peakList));

                        // Align all rows using mapping
                        for (PeakListRow row : allRows) {

                                PeakListRow targetRow = alignmentMapping.get(row);

                                // If we have no mapping for this row, add a new one
                                if (targetRow == null) {
                                        targetRow = new SimplePeakListRow(newRowID);
                                        newRowID++;
                                        alignedPeakList.addRow(targetRow);
                                }

                                // Add all peaks from the original row to the aligned row
                                for (RawDataFile file : row.getRawDataFiles()) {
                                        targetRow.addPeak(file, row.getPeak(file));
                                }

                                // Add all non-existing identities from the original row to the
                                // aligned row
                                PeakUtils.copyPeakListRowProperties(row, targetRow);

                                processedRows++;

                        }
                }

                // Add new aligned peak list to the project
                MZmineProject currentProject = MZmineCore.getCurrentProject();
                currentProject.addPeakList(alignedPeakList);

                // Add task description to peakList
                alignedPeakList.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
                        "Graph aligner", parameters));

                logger.info("Finished Graph aligner");
                setStatus(TaskStatus.FINISHED);


        }

        /**
         * A graph is created using the highest peaks of the first sample in
         * relation with the peak to be aligned from the same sample. The graph
         * for the second sample is created by searching the nearest peaks to the
         * peaks in the first sample.
         * Obtains a score comparing the topology of the two graphs.
         * @param peakList Peak list to be aligned.
         * @param row Peak to be aligned.
         * @param alignedPeakList Master peak list.
         * @param candidate Candidate peak to be aligned with "row".
         * @return The score of the alignment of these two peaks.
         */
        private double getScore(PeakList peakList, PeakListRow row, PeakList alignedPeakList, PeakListRow candidate) {
                List<PeakListRow> coords1 = getBestPeaks(peakList.getRows());
                PeakListRow[] coords2 = alignedPeakList.getRows();
                double score = 0;
                int peakCount = 0;
                for (PeakListRow coord1 : coords1) {
                        double bestDifference = 100000000.0;
                        for (PeakListRow coord2 : coords2) {
                                double diff1 = Math.abs((row.getAverageRT() - coord1.getAverageRT()) - (candidate.getAverageRT() - coord2.getAverageRT()));
                                double diff2 = Math.abs((row.getAverageMZ() - coord1.getAverageMZ()) - (candidate.getAverageMZ() - coord2.getAverageMZ()));

                                if ((diff1 + diff2) < bestDifference) {
                                        bestDifference = diff1 + diff2;
                                }
                        }
                        peakCount++;
                        score += bestDifference;
                }
                return score / peakCount;
        }

        /**
         * Returns a list with the highest peaks from the original peak list.
         * @param rows List of peaks.
         * @return List of X highest peaks from the peak list. X is a parameter defined by the user if the peak list contains more that X peaks.
         */
        private List<PeakListRow> getBestPeaks(PeakListRow[] rows) {
                List<PeakListRow> peakRows = new ArrayList<PeakListRow>();
                peakRows.addAll(Arrays.asList(rows));
                Collections.sort(peakRows, new PeakListRowSorter(SortingProperty.Height, SortingDirection.Descending));
                int nPeaks = peakRows.size();
                if (peakRows.size() > this.minPeakWindow) {
                        nPeaks = this.minPeakWindow;
                }
                return peakRows.subList(0, nPeaks);
        }


        /**
         * Creates a list of real matches selecting the candidates after correcting the
         * retention times using the regression equations.
         * @param allRows Peaks from the sample to be aligned.
         * @param regressions A list of linear regressions from the fragmented chromatogram.
         * @return List of corresponding peaks.
         */
        private HashMap<PeakListRow, PeakListRow> getAlignmentMap(PeakListRow allRows[], HashMap<Range, SimpleRegression> regressions) {

                // Create a table of mappings for best scores
                HashMap<PeakListRow, PeakListRow> alignmentMapping = new HashMap<PeakListRow, PeakListRow>();

                if (alignedPeakList.getNumberOfRows() < 1) {
                        return alignmentMapping;
                }

                // Create a sorted set of scores matching
                TreeSet<RowVsRowScore> scoreSet = new TreeSet<RowVsRowScore>();                

                for (PeakListRow row : allRows) {
                        // Calculate limits for a row with which the row can be aligned
                        Range mzRange = mzTolerance.getToleranceRange(row.getAverageMZ());

                        double rt = -1;

                        Iterator it = regressions.entrySet().iterator();
                        while (it.hasNext()) {
                                Map.Entry pairs = (Map.Entry) it.next();
                                Range r = (Range) pairs.getKey();
                                SimpleRegression function = (SimpleRegression) pairs.getValue();
                                if (r.contains(row.getAverageRT())) {
                                        rt = function.predict(row.getAverageRT());
                                        break;
                                }

                        }

                        if (Double.isNaN(rt)) {
                                rt = row.getAverageRT();
                        }

                        if (rt > -1) {
                                RTTolerance rtAbsTolerance = new RTTolerance(true, rtAbsoluteTolerance);
                                Range rtRange = rtAbsTolerance.getToleranceRange(rt);

                                // Get all rows of the aligned peaklist within parameter limits
                                PeakListRow candidateRows[] = alignedPeakList.getRowsInsideScanAndMZRange(rtRange,
                                        mzRange);

                                for (PeakListRow candidate : candidateRows) {
                                        RowVsRowScore score;
                                        if (sameChargeRequired && (!PeakUtils.compareChargeState(row, candidate))) {
                                                continue;
                                        }

                                        try {
                                                score = new RowVsRowScore(row, candidate, mzTolerance.getTolerance(),
                                                        rtAbsoluteTolerance, rt);

                                                scoreSet.add(score);

                                        } catch (Exception e) {
                                                e.printStackTrace();
                                                setStatus(TaskStatus.ERROR);
                                                return null;
                                        }
                                }
                        }
                        processedRows++;
                }

                // Iterate scores by descending order
                Iterator<RowVsRowScore> scoreIterator = scoreSet.iterator();
                while (scoreIterator.hasNext()) {

                        RowVsRowScore score = scoreIterator.next();

                        // Check if the row is already mapped
                        if (alignmentMapping.containsKey(score.getPeakListRow())) {
                                continue;
                        }

                        // Check if the aligned row is already filled
                        if (alignmentMapping.containsValue(score.getAlignedRow())) {
                                continue;
                        }

                        alignmentMapping.put(score.getPeakListRow(), score.getAlignedRow());

                }

                return alignmentMapping;
        }

        /**
         * Creates a list of linear regression equations by cutting the chromatogram in pieces
         * and using the points selected previously using the graph comparison scoring.
         * @param alignmentMappingSource List of matches using graph comparison scoring.
         * @param peakList Peak list to be aligned.
         * @return List of Simple regression for each range of retention time.
         */
        private HashMap<Range, SimpleRegression> getRegressionList(HashMap<PeakListRow, PeakListRow> alignmentMappingSource, PeakList peakList) {
                HashMap<Range, SimpleRegression> regressionList = new HashMap<Range, SimpleRegression>();

                for (int i = 0; i < peakList.getRowsRTRange().getMax(); i = i + this.regressionWindow) {
                        SimpleRegression regression = new SimpleRegression();
                        Range range = new Range(i, i + this.regressionWindow);
                        PeakListRow[] rows = peakList.getRowsInsideScanRange(range);
                        for (PeakListRow row : rows) {
                                PeakListRow candidate = alignmentMappingSource.get(row);
                                if (candidate != null) {
                                        regression.addData(row.getAverageRT(), candidate.getAverageRT());
                                }
                        }
                        regressionList.put(range, regression);
                }

                return regressionList;
        }

        public Object[] getCreatedObjects() {
                return new Object[]{alignedPeakList};
        }
}
