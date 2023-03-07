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
package io.github.mzmine.modules.dataprocessing.align_adap3;

import dulab.adap.common.algorithms.machineleanring.OptimizationParameters;
import dulab.adap.datamodel.Component;
import dulab.adap.datamodel.Peak;
import dulab.adap.datamodel.PeakInfo;
import dulab.adap.datamodel.Project;
import dulab.adap.datamodel.ReferenceComponent;
import dulab.adap.datamodel.Sample;
import dulab.adap.workflow.AlignmentParameters;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.datamodel.impl.SimpleIsotopePattern;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.adap.ADAPInterface;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(ADAP3AlignerTask.class.getName());

  private final MZmineProject project;
  private final ParameterSet parameters;

  private final FeatureList[] peakLists;

  private final String peakListName;

  private final Project alignment;

  public ADAP3AlignerTask(MZmineProject project, ParameterSet parameters, @Nullable
      MemoryMapStorage storage, @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);
    this.project = project;
    this.parameters = parameters;

    this.peakLists = parameters.getParameter(ADAP3AlignerParameters.PEAK_LISTS).getValue()
        .getMatchingFeatureLists();

    this.peakListName =
        parameters.getParameter(ADAP3AlignerParameters.NEW_PEAK_LIST_NAME).getValue();

    this.alignment = new Project();
  }

  @Override
  public String getTaskDescription() {
    return "ADAP Aligner, " + peakListName + " (" + peakLists.length + " feature lists)";
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
    logger.info("Started ADAP Peak Alignment");

    try {
      FeatureList peakList = alignPeaks();

      if (!isCanceled()) {
        project.addFeatureList(peakList);

        // QualityParameters.calculateQualityParameters(peakList);

        setStatus(TaskStatus.FINISHED);
        logger.info("Finished ADAP Peak Alignment");
      }
    } catch (IllegalArgumentException e) {
      errorMsg = "Incorrect Feature Lists:\n" + e.getMessage();
    } catch (Exception e) {
      errorMsg = "'Unknown error' during alignment. \n" + e.getMessage();
    } catch (Throwable t) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      logger.log(Level.SEVERE, "ADAP Alignment error", t);
    }

    // Report error
    if (errorMsg != null) {
      setErrorMessage(errorMsg);
      setStatus(TaskStatus.ERROR);
    }
  }

  private FeatureList alignPeaks() {

    // Collect all data files

    List<RawDataFile> allDataFiles = new ArrayList<>(peakLists.length);

    for (final FeatureList peakList : peakLists) {
      RawDataFile[] dataFiles = peakList.getRawDataFiles().toArray(RawDataFile[]::new);
      if (dataFiles.length != 1)
        throw new IllegalArgumentException(
            "Found more then one data " + "file in some of the peaks lists");

      allDataFiles.add(dataFiles[0]);
    }

    // Perform alignment

    for (int i = 0; i < peakLists.length; ++i) {

      FeatureList peakList = peakLists[i];

      Sample sample = new Sample(i);

      for (final FeatureListRow row : peakList.getRows()) {
        Component component = getComponent(row);
        if (component != null)
          sample.addComponent(component);
      }

      alignment.addSample(sample);
    }

    process();

    // Create new feature list
    final ModularFeatureList alignedPeakList =
        new ModularFeatureList(peakListName, getMemoryMapStorage(), allDataFiles.toArray(new RawDataFile[0]));

    int rowID = 0;

    List<ReferenceComponent> alignedComponents = alignment.getComponents();

    Collections.sort(alignedComponents);

    for (final ReferenceComponent referenceComponent : alignedComponents) {

      ModularFeatureListRow newRow = new ModularFeatureListRow(alignedPeakList, ++rowID);

      for (int i = 0; i < referenceComponent.size(); ++i) {

        Component component = referenceComponent.getComponent(i);
        Peak peak = component.getBestPeak();
        peak.getInfo().mzValue(component.getMZ());

        FeatureList featureList = findPeakList(referenceComponent.getSampleID(i));


        FeatureListRow row =
            findPeakListRow(referenceComponent.getSampleID(i), peak.getInfo().peakID);

        if (row == null)
          throw new IllegalStateException(
              String.format("Cannot find a feature list row for fileId = %d and peakId = %d",
                  referenceComponent.getSampleID(), peak.getInfo().peakID));

        RawDataFile file = row.getRawDataFiles().get(0);
//        List<Scan> scanNumbers = row.getBestFeature().getScanNumbers();
        // Create a new MZmine feature
        Feature feature = ADAPInterface.peakToFeature(alignedPeakList, featureList, file, peak);

        // Add spectrum as an isotopic pattern
        DataPoint[] spectrum = component.getSpectrum().entrySet().stream()
            .map(e -> new SimpleDataPoint(e.getKey(), e.getValue())).toArray(DataPoint[]::new);

        feature.setIsotopePattern(
            new SimpleIsotopePattern(spectrum, -1, IsotopePattern.IsotopePatternStatus.PREDICTED,
                "Spectrum"));

        newRow.addFeature(file, feature);
      }

      // Save alignment score
      SimpleFeatureInformation peakInformation =
          (SimpleFeatureInformation) newRow.getFeatureInformation();
      if (peakInformation == null)
        peakInformation = new SimpleFeatureInformation();
      peakInformation.addProperty("Alignment score",
          Double.toString(referenceComponent.getScore()));
      newRow.setFeatureInformation(peakInformation);

      alignedPeakList.addRow(newRow);
    }

    alignedPeakList.getAppliedMethods().add(new SimpleFeatureListAppliedMethod(
        ADAP3AlignerModule.class, parameters, getModuleCallDate()));


    for(int i = 0 ; i < peakLists.length ;i ++){
       RawDataFile f = peakLists[i].getRawDataFile(0);
       alignedPeakList.setSelectedScans(f, peakLists[i].getSeletedScans(f));
    }
    return alignedPeakList;
  }

  /**
   * Convert a {@link FeatureListRow} with one {@link Feature} into {@link Component}.
   *
   * @param row an instance of {@link FeatureListRow}. This parameter cannot be null.
   * @return an instance of {@link Component} or null if the row doesn't contain any peaks or
   *         isotope patterns.
   */
  @Nullable
  private Component getComponent(final FeatureListRow row) {

    if (row.getNumberOfFeatures() == 0)
      return null;

    // Read Spectrum information
    NavigableMap<Double, Double> spectrum = new TreeMap<>();

    IsotopePattern pattern = row.getBestIsotopePattern();

    if (pattern == null)
      throw new IllegalArgumentException("ADAP Alignment requires mass "
          + "spectra (or isotopic patterns) of peaks. No spectra found.");

    for (DataPoint dataPoint : pattern)
      spectrum.put(dataPoint.getMZ(), dataPoint.getIntensity());

    // Read Chromatogram
    final Feature peak = row.getBestFeature();
    NavigableMap<Double, Double> chromatogram = new TreeMap<>();

    for (int i = 0; i < peak.getNumberOfDataPoints(); i++) {
      final DataPoint dataPoint = peak.getDataPointAtIndex(i);
      if (dataPoint != null)
        chromatogram.put(Double.valueOf(String.valueOf(peak.getRetentionTimeAtIndex(i))),
            dataPoint.getIntensity());
    }

    return new Component(null,
        new Peak(chromatogram, new PeakInfo().mzValue(peak.getMZ()).peakID(row.getID())), spectrum,
        null);
  }

  /**
   * Call the alignment from the ADAP package.
   *
   */
  private void process() {
    AlignmentParameters params = new AlignmentParameters()
        .sampleCountRatio(
            parameters.getParameter(ADAP3AlignerParameters.SAMPLE_COUNT_RATIO).getValue())
        .retTimeRange(parameters.getParameter(ADAP3AlignerParameters.RET_TIME_RANGE).getValue()
            .getTolerance())
        .scoreTolerance(parameters.getParameter(ADAP3AlignerParameters.SCORE_TOLERANCE).getValue())
        .scoreWeight(parameters.getParameter(ADAP3AlignerParameters.SCORE_WEIGHT).getValue())
        .maxShift(2 * parameters.getParameter(ADAP3AlignerParameters.RET_TIME_RANGE).getValue()
            .getTolerance())
        .eicScore(parameters.getParameter(ADAP3AlignerParameters.EIC_SCORE).getValue()).mzRange(
            parameters.getParameter(ADAP3AlignerParameters.MZ_RANGE).getValue().getMzTolerance());

    params.optimizationParameters = new OptimizationParameters().gradientTolerance(1e-6).alpha(1e-4)
        .maxIterationCount(4000).verbose(false);

    alignment.alignSamples(params);
  }

  /**
   * Find the existing {@link FeatureListRow} for a given feature list ID and row ID.
   *
   * @param peakListID number of a feature list in the array of {@link FeatureList}. The numeration
   *        starts with 0.
   * @param rowID integer that is returned by method getId() of {@link FeatureListRow}.
   * @return an instance of {@link FeatureListRow} if an existing row is found. Otherwise it returns
   *         null.
   */
  @Nullable
  private FeatureListRow findPeakListRow(final int peakListID, final int rowID) {

    // Find feature list
    FeatureList peakList = findPeakList(peakListID);
    if (peakList == null)
      return null;

    // Find row
    FeatureListRow row = null;
    for (final FeatureListRow r : peakList.getRows())
      if (rowID == r.getID()) {
        row = r;
        break;
      }

    return row;
  }

  /**
   * Find the existing {@link FeatureList} for a given feature list ID.
   *
   * @param peakListId number of a feature list in the array of {@link FeatureList}. The numeration
   *        starts with 0.
   * @return an instance of {@link FeatureList} if a feature list is found, or null.
   */
  @Nullable
  private FeatureList findPeakList(int peakListId) {
    FeatureList peakList = null;
    for (int i = 0; i < peakLists.length; ++i)
      if (peakListId == i) {
        peakList = peakLists[i];
        break;
      }
    return peakList;
  }
}
