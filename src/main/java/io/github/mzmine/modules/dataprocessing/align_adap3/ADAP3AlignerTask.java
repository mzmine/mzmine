/* Copyright 2006-2019 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.modules.dataprocessing.align_adap3;

import dulab.adap.common.algorithms.machineleanring.OptimizationParameters;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.datamodel.Project;
import dulab.adap.datamodel.ReferenceComponent;
import dulab.adap.datamodel.Sample;
import dulab.adap.workflow.AlignmentParameters;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakIdentity;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.impl.*;
import io.github.mzmine.modules.tools.qualityparameters.QualityParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.adap.ADAPInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerTask extends AbstractTask {

    private static final Logger LOG = Logger
            .getLogger(ADAP3AlignerTask.class.getName());

    private final MZmineProject project;
    private final ParameterSet parameters;

    private final PeakList[] peakLists;

    private final String peakListName;

    private final Project alignment;

    public ADAP3AlignerTask(MZmineProject project, ParameterSet parameters) {

        this.project = project;
        this.parameters = parameters;

        this.peakLists = parameters
                .getParameter(ADAP3AlignerParameters.PEAK_LISTS).getValue()
                .getMatchingPeakLists();

        this.peakListName = parameters
                .getParameter(ADAP3AlignerParameters.NEW_PEAK_LIST_NAME)
                .getValue();

        this.alignment = new Project();
    }

    @Override
    public String getTaskDescription() {
        return "ADAP Aligner, " + peakListName + " (" + peakLists.length
                + " feature lists)";
    }

    @Override
    public double getFinishedPercentage() {
        return alignment.getProcessedPercent();
    }

    @Override
    public void cancel() {
        super.cancel();

        this.alignment.cancel();
    }

    @Override
    public void run() {

        if (isCanceled())
            return;

        String errorMsg = null;

        setStatus(TaskStatus.PROCESSING);
        LOG.info("Started ADAP Peak Alignment");

        try {
            PeakList peakList = alignPeaks();

            if (!isCanceled()) {
                project.addPeakList(peakList);

                QualityParameters.calculateQualityParameters(peakList);

                setStatus(TaskStatus.FINISHED);
                LOG.info("Finished ADAP Peak Alignment");
            }
        } catch (IllegalArgumentException e) {
            errorMsg = "Incorrect Feature Lists:\n" + e.getMessage();
        } catch (Exception e) {
            errorMsg = "'Unknown error' during alignment. \n" + e.getMessage();
        } catch (Throwable t) {
            setStatus(TaskStatus.ERROR);
            setErrorMessage(t.getMessage());
            LOG.log(Level.SEVERE, "ADAP Alignment error", t);
        }

        // Report error
        if (errorMsg != null) {
            setErrorMessage(errorMsg);
            setStatus(TaskStatus.ERROR);
        }
    }

    private PeakList alignPeaks() {

        // Collect all data files

        List<RawDataFile> allDataFiles = new ArrayList<>(peakLists.length);

        for (final PeakList peakList : peakLists) {
            RawDataFile[] dataFiles = peakList.getRawDataFiles();
            if (dataFiles.length != 1)
                throw new IllegalArgumentException("Found more then one data "
                        + "file in some of the peaks lists");

            allDataFiles.add(dataFiles[0]);
        }

        // Perform alignment

        for (int i = 0; i < peakLists.length; ++i) {

            PeakList peakList = peakLists[i];

            Sample sample = new Sample(i);

            for (final PeakListRow row : peakList.getRows()) {
                Component component = getComponent(row);
                if (component != null)
                    sample.addComponent(component);
            }

            alignment.addSample(sample);
        }

        process();

        // Create new feature list
        final PeakList alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[0]));

        int rowID = 0;

        List<ReferenceComponent> alignedComponents = alignment.getComponents();

        Collections.sort(alignedComponents);

        for (final ReferenceComponent referenceComponent : alignedComponents) {

            SimplePeakListRow newRow = new SimplePeakListRow(++rowID);
            for (int i = 0; i < referenceComponent.size(); ++i) {

                Component component = referenceComponent.getComponent(i);
                Peak peak = component.getBestPeak();
                peak.getInfo().mzValue(component.getMZ());

                PeakListRow row = findPeakListRow(
                        referenceComponent.getSampleID(i),
                        peak.getInfo().peakID);

                if (row == null)
                    throw new IllegalStateException(String.format(
                            "Cannot find a feature list row for fileId = %d and peakId = %d",
                            referenceComponent.getSampleID(),
                            peak.getInfo().peakID));

                RawDataFile file = row.getRawDataFiles()[0];

                // Create a new MZmine feature
                Feature feature = ADAPInterface.peakToFeature(file, peak);

                // Add spectrum as an isotopic pattern
                DataPoint[] spectrum = component.getSpectrum().entrySet()
                        .stream()
                        .map(e -> new SimpleDataPoint(e.getKey(), e.getValue()))
                        .toArray(DataPoint[]::new);

                feature.setIsotopePattern(new SimpleIsotopePattern(spectrum,
                        IsotopePattern.IsotopePatternStatus.PREDICTED,
                        "Spectrum"));

                newRow.addPeak(file, feature);
            }

            // Save alignment score
            SimplePeakInformation peakInformation = (SimplePeakInformation) newRow
                    .getPeakInformation();
            if (peakInformation == null)
                peakInformation = new SimplePeakInformation();
            peakInformation.addProperty("Alignment score",
                    Double.toString(referenceComponent.getScore()));
            newRow.setPeakInformation(peakInformation);

            alignedPeakList.addRow(newRow);
        }

        return alignedPeakList;
    }

    /**
     * Convert a {@link PeakListRow} with one {@link Feature} into
     * {@link Component}.
     *
     * @param row
     *            an instance of {@link PeakListRow}. This parameter cannot be
     *            null.
     * @return an instance of {@link Component} or null if the row doesn't
     *         contain any peaks or isotope patterns.
     */
    @Nullable
    private Component getComponent(final PeakListRow row) {

        if (row.getNumberOfPeaks() == 0)
            return null;

        // Read Spectrum information
        NavigableMap<Double, Double> spectrum = new TreeMap<>();

        IsotopePattern pattern = row.getBestIsotopePattern();

        if (pattern == null)
            throw new IllegalArgumentException("ADAP Alignment requires mass "
                    + "spectra (or isotopic patterns) of peaks. No spectra found.");

        for (DataPoint dataPoint : pattern.getDataPoints())
            spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());

        // Read Chromatogram
        final Feature peak = row.getBestPeak();
        final RawDataFile dataFile = peak.getDataFile();

        NavigableMap<Double, Double> chromatogram = new TreeMap<>();

        for (final int scan : peak.getScanNumbers()) {
            final DataPoint dataPoint = peak.getDataPoint(scan);
            if (dataPoint != null)
                chromatogram.put(dataFile.getScan(scan).getRetentionTime(),
                        dataPoint.getIntensity());
        }

        return new Component(
                null, new Peak(chromatogram, new PeakInfo()
                        .mzValue(peak.getMZ()).peakID(row.getID())),
                spectrum, null);
    }

    /**
     * Call the alignment from the ADAP package.
     *
     * @param alignment
     *            an instance of {@link Project} containing all samples and
     *            peaks to be aligned.
     */
    private void process() {
        AlignmentParameters params = new AlignmentParameters()
                .sampleCountRatio(parameters
                        .getParameter(ADAP3AlignerParameters.SAMPLE_COUNT_RATIO)
                        .getValue())
                .retTimeRange(parameters
                        .getParameter(ADAP3AlignerParameters.RET_TIME_RANGE)
                        .getValue().getTolerance())
                .scoreTolerance(parameters
                        .getParameter(ADAP3AlignerParameters.SCORE_TOLERANCE)
                        .getValue())
                .scoreWeight(parameters
                        .getParameter(ADAP3AlignerParameters.SCORE_WEIGHT)
                        .getValue())
                .maxShift(2 * parameters
                        .getParameter(ADAP3AlignerParameters.RET_TIME_RANGE)
                        .getValue().getTolerance())
                .eicScore(parameters
                        .getParameter(ADAP3AlignerParameters.EIC_SCORE)
                        .getValue())
                .mzRange(
                        parameters.getParameter(ADAP3AlignerParameters.MZ_RANGE)
                                .getValue().getMzTolerance());

        params.optimizationParameters = new OptimizationParameters()
                .gradientTolerance(1e-6).alpha(1e-4).maxIterationCount(4000)
                .verbose(false);

        alignment.alignSamples(params);
    }

    /**
     * Find the existing {@link PeakListRow} for a given feature list ID and row
     * ID.
     *
     * @param peakListID
     *            number of a feature list in the array of {@link PeakList}. The
     *            numeration starts with 0.
     * @param rowID
     *            integer that is returned by method getId() of
     *            {@link PeakListRow}.
     * @return an instance of {@link PeakListRow} if an existing row is found.
     *         Otherwise it returns null.
     */
    @Nullable
    private PeakListRow findPeakListRow(final int peakListID, final int rowID) {

        // Find feature list
        PeakList peakList = findPeakList(peakListID);
        if (peakList == null)
            return null;

        // Find row
        PeakListRow row = null;
        for (final PeakListRow r : peakList.getRows())
            if (rowID == r.getID()) {
                row = r;
                break;
            }

        return row;
    }

    /**
     * Find the existing {@link PeakList} for a given feature list ID.
     * 
     * @param peakListId
     *            number of a feature list in the array of {@link PeakList}. The
     *            numeration starts with 0.
     * @return an instance of {@link PeakList} if a feature list is found, or
     *         null.
     */
    @Nullable
    private PeakList findPeakList(int peakListId) {
        PeakList peakList = null;
        for (int i = 0; i < peakLists.length; ++i)
            if (peakListId == i) {
                peakList = peakLists[i];
                break;
            }
        return peakList;
    }
}
