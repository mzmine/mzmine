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
package net.sf.mzmine.modules.peaklistmethods.alignment.adap3;

import dulab.adap.common.algorithms.machineleanring.OptimizationParameters;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.datamodel.Project;
import dulab.adap.datamodel.ReferenceComponent;
import dulab.adap.datamodel.Sample;
import dulab.adap.workflow.AlignmentParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.PeakIdentity;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.datamodel.impl.SimplePeakList;
import net.sf.mzmine.datamodel.impl.SimplePeakListRow;
import net.sf.mzmine.modules.peaklistmethods.qualityparameters.QualityParameters;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerTask extends AbstractTask {

    private static final Logger LOG = Logger.getLogger(
            ADAP3AlignerTask.class.getName());

    private final MZmineProject project;
    private final ParameterSet parameters;

    private final PeakList[] peakLists;

    private final String peakListName;

    private final Project alignment;

    public ADAP3AlignerTask(MZmineProject project, ParameterSet parameters) {

        this.project = project;
        this.parameters = parameters;

        this.peakLists = parameters.getParameter(
                ADAP3AlignerParameters.PEAK_LISTS)
                .getValue().getMatchingPeakLists();

        this.peakListName = parameters.getParameter(
                ADAP3AlignerParameters.NEW_PEAK_LIST_NAME).getValue();

        this.alignment = new Project();
    }

    @Override
    public String getTaskDescription() {
        return "ADAP Aligner, " + peakListName + " (" + peakLists.length
                + " peak lists)";
    }

    @Override
    public double getFinishedPercentage() {
        return alignment.getProcessedPercent();
    }

    @Override
    public void run() {

        if (!isCanceled()) {
            String errorMsg = null;

            setStatus(TaskStatus.PROCESSING);
            LOG.info("Started ADAP Peak Alignment");

            try {
                PeakList peakList = alignPeaks(peakLists);

                if (!isCanceled()) {
                    project.addPeakList(peakList);

                    QualityParameters.calculateQualityParameters(peakList);

                    setStatus(TaskStatus.FINISHED);
                    LOG.info("Finished ADAP Peak Alignment");
                }
            } catch (IllegalArgumentException e) {
                errorMsg = "Incorrect Peak Lists:\n" + e.getMessage();
            } catch (Exception e) {
                errorMsg = "'Unknown error' during alignment. \n"
                        + e.getMessage();
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
    }

    private PeakList alignPeaks(PeakList[] peakLists) {

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

        for (final PeakList peakList : peakLists) {

            Sample sample = new Sample(peakList.hashCode());

            for (final PeakListRow row : peakList.getRows())
                sample.addComponent(getComponent(row));

            alignment.addSample(sample);
        }

        process(alignment);

        // Create new peak list
        final PeakList alignedPeakList = new SimplePeakList(peakListName,
                allDataFiles.toArray(new RawDataFile[allDataFiles.size()]));

        int rowID = 0;

        List<ReferenceComponent> alignedComponents = alignment.getComponents();

        Collections.sort(alignedComponents);

        for (final ReferenceComponent component : alignedComponents) {
            final Peak peak = component.getBestPeak();

            PeakListRow refRow = findRow(peakLists, component.getSampleID(),
                    peak.getInfo().peakID);

            SimplePeakListRow newRow = new SimplePeakListRow(++rowID);

            newRow.addPeak(refRow.getRawDataFiles()[0], refRow.getBestPeak());

            for (int i = 0; i < component.size(); ++i) {
                PeakListRow row = findRow(peakLists,
                        component.getSampleID(i),
                        component.getComponent(i).getBestPeak().getInfo().peakID);

                if (row != refRow)
                    newRow.addPeak(row.getRawDataFiles()[0], row.getBestPeak());
            }


            PeakIdentity identity = refRow.getPreferredPeakIdentity();

            if (identity != null)
                newRow.addPeakIdentity(identity, true);

            newRow.setComment("Alignment Score = " + Double.toString(component.getScore()));

            // -----------------------------------------------
            // Determine the quantitative mass and intensities
            // -----------------------------------------------

            double mass = getQuantitativeMass(component);
            double mzTolerance = parameters.getParameter(ADAP3AlignerParameters.MZ_RANGE)
                    .getValue()
                    .getMzTolerance();

            SimplePeakInformation information = new SimplePeakInformation();
            information.addProperty("REFERENCE FILE", refRow.getRawDataFiles()[0].getName());
            information.addProperty("QUANTITATION MASS", Double.toString(mass));

            List<Component> components =
                    new ArrayList<>(component.getComponents());

            for (int i = 0; i < components.size(); ++i) {
                Component c = components.get(i);
                RawDataFile file = findFile(peakLists, component.getSampleID(i));

                double minDistance = Double.MAX_VALUE;
                double intensity = 0.0;

                for (Entry<Double, Double> e : c.getSpectrum().entrySet()) {
                    double mz = e.getKey();
                    double distance = Math.abs(mz - mass);

                    if (distance > mzTolerance) continue;

                    if (distance < minDistance) {
                        minDistance = distance;
                        intensity = e.getValue();
                    }
                }

                information.addProperty(
                        "QUANTITATION INTENSITY for " + file.getName(),
                        Double.toString(intensity));
            }

            newRow.setPeakInformation(information);

            alignedPeakList.addRow(newRow);
        }

        return alignedPeakList;
    }

    private Component getComponent(final PeakListRow row) {
        final PeakIdentity identity = row.getPreferredPeakIdentity();

        if (row.getNumberOfPeaks() == 0)
            throw new IllegalArgumentException("No peaks found");

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

        return new Component(null,
                new Peak(chromatogram, new PeakInfo()
                        .mzValue(peak.getMZ())
                        .peakID(row.hashCode())),
                spectrum, null);
    }

    private void process(Project alignment) {
        AlignmentParameters params = new AlignmentParameters()
                .sampleCountRatio(parameters.getParameter(
                        ADAP3AlignerParameters.SAMPLE_COUNT_RATIO).getValue())
                .retTimeRange(parameters.getParameter(
                        ADAP3AlignerParameters.RET_TIME_RANGE).getValue().getTolerance())
                .scoreTolerance(parameters.getParameter(
                        ADAP3AlignerParameters.SCORE_TOLERANCE).getValue())
                .scoreWeight(parameters.getParameter(
                        ADAP3AlignerParameters.SCORE_WEIGHT).getValue())
                .maxShift(2 * parameters.getParameter(
                        ADAP3AlignerParameters.RET_TIME_RANGE).getValue().getTolerance())
                .eicScore(parameters.getParameter(
                        ADAP3AlignerParameters.EIC_SCORE).getValue());

        params.optimizationParameters = new OptimizationParameters()
                .gradientTolerance(1e-6)
                .alpha(1e-4)
                .maxIterationCount(4000)
                .verbose(false);

        alignment.alignSamples(params);
    }

    private PeakListRow findRow(final PeakList[] peakLists,
                                final int listID, final int peakID) {
        // Find peak list

        PeakList peakList = null;
        for (final PeakList list : peakLists)
            if (listID == list.hashCode()) {
                peakList = list;
                break;
            }

        if (peakList == null) return null;

        // Find row

        PeakListRow row = null;
        for (final PeakListRow r : peakList.getRows())
            if (peakID == r.hashCode()) {
                row = r;
                break;
            }

        return row;
    }

    private RawDataFile findFile(final PeakList[] peakLists,
                                 final int listID) {
        // Find peak list

        PeakList peakList = null;
        for (final PeakList list : peakLists)
            if (listID == list.hashCode()) {
                peakList = list;
                break;
            }

        if (peakList == null)
            throw new IllegalArgumentException("Cannot find peak list "
                    + Integer.toString(listID));

        return peakList.getRawDataFile(0);
    }

    /**
     * Find Quantitative Mass for a list of components, as the m/z-value that
     * is closest to the average of components' m/z-values.
     *
     * @param refComponent reference component
     * @return quantitative mass
     */

    private double getQuantitativeMass(final ReferenceComponent refComponent) {
        List<Component> components = refComponent.getComponents();

        // ------------------------------------------
        // Round up m/z-values to the closest integer
        // ------------------------------------------

        List<Long> integerMZs = new ArrayList<>(components.size());
        for (Component c : components) integerMZs.add(Math.round(c.getMZ()));

        // ----------------------------------------
        // Find the most frequent integer m/z-value
        // ----------------------------------------

        Map<Long, Integer> counts = new HashMap<>();
        for (Long mz : integerMZs) {
            Integer count = counts.get(mz);
            if (count == null) count = 0;
            counts.put(mz, count + 1);
        }

        Long bestMZ = null;
        int maxCount = 0;
        for (Entry<Long, Integer> e : counts.entrySet()) {
            int count = e.getValue();

            if (maxCount < count) {
                maxCount = count;
                bestMZ = e.getKey();
            }
        }

        if (bestMZ == null)
            throw new IllegalArgumentException("Cannot find the most frequent m/z-value");

        // ----------------------------------------------------
        // Find m/z-value that is the closest to the integer mz
        // ----------------------------------------------------

        double minDistance = Double.MAX_VALUE;
        Double quantitativeMass = null;
        for (Component c : components) {
            double mz = c.getMZ();
            double distance = Math.abs(mz - bestMZ.doubleValue());
            if (distance < minDistance) {
                minDistance = distance;
                quantitativeMass = mz;
            }
        }

        if (quantitativeMass == null)
            throw new IllegalArgumentException("Cannot find the quantitative mass");

        return quantitativeMass;
    }
}
