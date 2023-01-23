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

package io.github.mzmine.modules.io.export_features_sirius;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MassList;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.msmsspectramerge.MergeMode;
import io.github.mzmine.modules.tools.msmsspectramerge.MergedSpectrum;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeModule;
import io.github.mzmine.modules.tools.msmsspectramerge.MsMsSpectraMergeParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SiriusExportTaskNew extends AbstractTask {

  private static final Logger logger = Logger.getLogger(SiriusExportTaskNew.class.getName());

  private final ParameterSet parameters;
  private final ModularFeatureList[] featureLists;
  private final File fileName;
  private final boolean mergeEnabled;
  private final MsMsSpectraMergeParameters mergeParameters;
  private final double minimumRelativeNumberOfScans;
  private final MZTolerance mzTol;
  private final Boolean excludeEmptyMSMS;
  private final Boolean excludeMultiCharge;
  private final Boolean excludeMultimers;
  private final Boolean needAnnotation;
  private final Boolean renumberID;
  private final int totalRows;
  public static final String plNamePattern = "{}";
  final NumberFormats format = MZmineCore.getConfiguration().getExportFormats();
  private final MergeMode mergeMode;


  protected SiriusExportTaskNew(ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);

    this.parameters = parameters;
    this.featureLists = parameters.getParameter(SiriusExportParameters.FEATURE_LISTS).getValue()
        .getMatchingFeatureLists();
    this.fileName = parameters.getParameter(SiriusExportParameters.FILENAME).getValue();
    this.mergeEnabled = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER).getValue();
    this.mergeParameters = parameters.getParameter(SiriusExportParameters.MERGE_PARAMETER)
        .getEmbeddedParameters();

    minimumRelativeNumberOfScans = mergeEnabled ? mergeParameters.getParameter(
        MsMsSpectraMergeParameters.REL_SIGNAL_COUNT_PARAMETER).getValue() : 0d;

    // new parameters related to ion identity networking and feature grouping
    mzTol = parameters.getParameter(SiriusExportParameters.MZ_TOL).getValue();
    excludeEmptyMSMS = parameters.getParameter(SiriusExportParameters.EXCLUDE_EMPTY_MSMS)
        .getValue();
    excludeMultiCharge = parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTICHARGE)
        .getValue();
    excludeMultimers = parameters.getParameter(SiriusExportParameters.EXCLUDE_MULTIMERS).getValue();
    needAnnotation = parameters.getParameter(SiriusExportParameters.NEED_ANNOTATION).getValue();
    mergeMode = mergeParameters.getParameter(MsMsSpectraMergeParameters.MERGE_MODE).getValue();
    // experimental
    renumberID = parameters.getParameter(SiriusExportParameters.RENUMBER_ID).getValue();

    totalRows = Arrays.stream(featureLists).mapToInt(FeatureList::getNumberOfRows).sum();
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    int totalExported = 0;
    // Process feature lists
    for (FeatureList featureList : featureLists) {

      // Filename
      final File curFile = getFileForFeatureList(substitute, featureList);
      if (curFile == null) {
        setErrorMessage("Could not create directories for file " + curFile + " for writing.");
        setStatus(TaskStatus.ERROR);
        return;
      }

      // Open file
      try (BufferedWriter writer = Files.newBufferedWriter(curFile.toPath(),
          StandardCharsets.UTF_8)) {
        logger.fine(() -> String.format("Exporting SIRIUS mgf for feature list: %s to file %s",
            featureList.getName(), curFile.getAbsolutePath()));
        totalExported += exportFeatureList(featureList, writer);
      } catch (IOException e) {
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not open file " + curFile + " for writing.");
        logger.log(Level.WARNING, String.format(
            "Error writing SIRIUS mgf format to file: %s for feature list: %s. Message: %s",
            curFile.getAbsolutePath(), featureList.getName(), e.getMessage()), e);
        return;
      }
    }

  }

  private int exportFeatureList(FeatureList featureList, BufferedWriter writer) {
    int exportedRows = 0;

    for (FeatureListRow row : featureList.getRows()) {
      if (isCanceled()) {
        return exportedRows;
      }

      if (!checkFeatureCriteria(row)) {
        exportedRows++;
        continue;
      }

      exportedRows++;

      List<MergedSpectrum> mergedSpectra = getMergedMs2Spectra(mergeMode, row);

    }
  }

  private List<MergedSpectrum> getMergedMs2Spectra(MergeMode mergeMode, FeatureListRow row) {

    List<Object> spectra = new ArrayList<>();
    final MsMsSpectraMergeModule merger = MZmineCore.getModuleInstance(
        MsMsSpectraMergeModule.class);

    switch (mergeMode) {
      case SAME_SAMPLE -> {
        for (Feature f : row.getFeatures()) {
          if (f.getFeatureStatus() == FeatureStatus.DETECTED) {
            final Scan bestMS2 = f.getMostIntenseFragmentScan();
            if (bestMS2 == null) {
              continue;
            }
            if (!checkMassList(bestMS2, bestMS2.getMassList())) {
              return null;
            }
            if (excludeEmptyMSMS && bestMS2.getMassList().getNumberOfDataPoints() <= 0) {
              continue;
            }

            MergedSpectrum spectrum = merger.mergeFromSameSample(mergeParameters, f)
                .filterByRelativeNumberOfScans(minimumRelativeNumberOfScans);
            spectra.add(spectrum);
          }
        }

      }
      case CONSECUTIVE_SCANS -> {
        for (Feature f : row.getFeatures()) {
          if (f.getFeatureStatus() == FeatureStatus.DETECTED) {
            final Scan bestMS2 = f.getMostIntenseFragmentScan();
            if (bestMS2 == null) {
              continue;
            }
            if (!checkMassList(bestMS2, bestMS2.getMassList())) {
              return null;
            }
            if (excludeEmptyMSMS && bestMS2.getMassList().getNumberOfDataPoints() <= 0) {
              continue;
            }

            final List<MergedSpectrum> mergedSpectra = merger.mergeConsecutiveScans(mergeParameters,
                f);
            for (MergedSpectrum spectrum : mergedSpectra) {
              spectra.add(spectrum.filterByRelativeNumberOfScans(minimumRelativeNumberOfScans));
            }
          }
        }
      }

      case ACROSS_SAMPLES -> {
        // merge everything into one
        MergedSpectrum spectrum = merger.mergeAcrossSamples(mergeParameters, row)
            .filterByRelativeNumberOfScans(minimumRelativeNumberOfScans);
        spectra.add(spectrum);
      }
    }
  }

  private boolean checkFeatureCriteria(final FeatureListRow row) {

    if (!row.hasMs2Fragmentation()) {
      return false;
    }

    if (excludeMultiCharge && row.getRowCharge() > 1) {
      return false;
    }

    IonIdentity adduct = row.getBestIonIdentity();
    if (needAnnotation && adduct == null) {
      return false;
    }

    if (excludeMultimers && adduct != null && adduct.getIonType().getMolecules() > 1) {
      return false;
    }

    return true;
  }

  private boolean checkMassList(Scan scan, MassList massList) {
    if (massList == null) {
      setErrorMessage("A mass list was missing for scan " + ScanUtils.scanToString(scan, true)
          + ". Maybe rerun mass detection on MS2 and MS1 without scan filtering (e.g., by retention time range).");
      setStatus(TaskStatus.ERROR);
      return false;
    }
    return true;
  }

  @Nullable
  private File getFileForFeatureList(boolean substitute, FeatureList featureList) {
    File tmpFile = fileName;
    if (substitute) {
      // Cleanup from illegal filename characters
      String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
      // Substitute
      String newFilename = fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
      tmpFile = new File(newFilename);
    }
    final File curFile = FileAndPathUtil.getRealFilePath(tmpFile, "mgf");

    if (!FileAndPathUtil.createDirectory(curFile.getParentFile())) {
      return null;
    }
    return curFile;
  }
}
