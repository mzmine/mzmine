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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction;

import io.github.mzmine.datamodel.AbundanceMeasure;
import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.types.annotations.BlankSubtractionAnnotationType;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 */
public class FeatureListBlankSubtractionTask extends AbstractTask {

  private static Logger logger = Logger.getLogger(FeatureListBlankSubtractionTask.class.getName());
  private final int totalRows;
  private final int minBlankDetections;
  private final String suffix;
  private final boolean createDeletedFeatureList;
  private final boolean checkFoldChange;
  private final double foldChange;
  private final BlankSubtractionOptions keepBackgroundFeatures;
  private final RatioType ratioType;
  private final AbundanceMeasure quantType;

  private AtomicInteger processedRows = new AtomicInteger(0);
  private MZmineProject project;
  private FeatureListBlankSubtractionParameters parameters;
  private RawDataFilesSelection blankSelection;
  private List<RawDataFile> blankRaws;
  private ModularFeatureList originalFeatureList;

  public FeatureListBlankSubtractionTask(MZmineProject project,
      FeatureListBlankSubtractionParameters parameters, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate) {
    super(storage, moduleCallDate);

    this.project = project;
    this.parameters = parameters;
    this.blankSelection = parameters.getParameter(
        FeatureListBlankSubtractionParameters.blankRawDataFiles).getValue();
    this.blankRaws = List.of(blankSelection.getMatchingRawDataFiles().clone());
    this.originalFeatureList = parameters.getParameter(
            FeatureListBlankSubtractionParameters.alignedPeakList).getValue()
        .getMatchingFeatureLists()[0];
    this.minBlankDetections = parameters.getParameter(
        FeatureListBlankSubtractionParameters.minBlanks).getValue();
    this.keepBackgroundFeatures = parameters.getParameter(
        FeatureListBlankSubtractionParameters.keepBackgroundFeatures).getValue();
    this.suffix = parameters.getParameter(FeatureListBlankSubtractionParameters.suffix).getValue();
    this.createDeletedFeatureList = parameters.getParameter(
        FeatureListBlankSubtractionParameters.createDeleted).getValue();
    checkFoldChange = parameters.getParameter(FeatureListBlankSubtractionParameters.foldChange)
        .getValue();
    foldChange = parameters.getParameter(FeatureListBlankSubtractionParameters.foldChange)
        .getEmbeddedParameter().getValue();
    this.ratioType = parameters.getParameter(FeatureListBlankSubtractionParameters.ratioType)
        .getValue();
    this.quantType = parameters.getParameter(FeatureListBlankSubtractionParameters.quantType)
        .getValue();
    totalRows = originalFeatureList.getNumberOfRows();
    logger.info(
        String.format("Blank subtraction with quantifier '%s' and ratio '%s'", this.quantType,
            this.ratioType));

    setStatus(TaskStatus.WAITING);
    logger.setLevel(Level.FINEST);
  }

  @Override
  public String getTaskDescription() {
    return "Blank subtraction task on " + originalFeatureList.getName();
  }

  @Override
  public double getFinishedPercentage() {
    return processedRows.get() / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    NumberFormats guiFormats = MZmineCore.getConfiguration().getGuiFormats();

    if (!checkBlankSelection(originalFeatureList, blankRaws)) {
      setErrorMessage("Feature list " + originalFeatureList.getName()
          + " does no contain all selected blank raw data files.");
      setStatus(TaskStatus.ERROR);
      return;
    }

    // get the files that are not considered as blank
    final List<RawDataFile> nonBlankRaws = new ArrayList<>();
    for (RawDataFile file : originalFeatureList.getRawDataFiles()) {
      if (!blankRaws.contains(file)) {
        nonBlankRaws.add(file);
      }
    }
    logger.finest(() -> originalFeatureList.getName() + " contains " + nonBlankRaws.size()
        + " raw data files not classified as blank.");

    // create the feature list for the blank subtraction
    final ModularFeatureList notBackgroundAlignedFeaturesList = new ModularFeatureList(
        originalFeatureList.getName() + " " + suffix, getMemoryMapStorage(),
        keepBackgroundFeatures == BlankSubtractionOptions.KEEP
            ? originalFeatureList.getRawDataFiles() : nonBlankRaws);
    originalFeatureList.getRowTypes().values()
        .forEach(notBackgroundAlignedFeaturesList::addRowType);

    // use all samples that are not defined as blanks
    // if keepBackgroundFeatures is true, also include blank samples (i.e., all samples)
    if (keepBackgroundFeatures == BlankSubtractionOptions.KEEP) {
      originalFeatureList.getRawDataFiles().forEach(
          f -> notBackgroundAlignedFeaturesList.setSelectedScans(f,
              originalFeatureList.getSeletedScans(f)));
    } else {
      nonBlankRaws.forEach(f -> notBackgroundAlignedFeaturesList.setSelectedScans(f,
          originalFeatureList.getSeletedScans(f)));
    }

    // create feature list containing all background features and all samples
    final ModularFeatureList backgroundAlignedFeaturesList = new ModularFeatureList(
        originalFeatureList.getName() + " subtractedBackground", getMemoryMapStorage(),
        originalFeatureList.getRawDataFiles());
    originalFeatureList.getRowTypes().values().forEach(backgroundAlignedFeaturesList::addRowType);
    originalFeatureList.getRawDataFiles().forEach(
        f -> backgroundAlignedFeaturesList.setSelectedScans(f,
            originalFeatureList.getSeletedScans(f)));

    final List<FeatureListRow> notBackgroundAlignedFeaturesListRows = new ArrayList<>();
    final List<FeatureListRow> backgroundAlignedFeaturesListRows = new ArrayList<>();
    for (FeatureListRow originalRow : originalFeatureList.getRows()) {

      final List<Feature> notBackgroundFeaturesOfCurrentRow = new ArrayList<>();
      final List<Feature> backgroundFeaturesOfCurrentRow = new ArrayList<>();

      // check the featureRow in the blank samples
      int foundInNBlanks = 0;
      for (RawDataFile blankRaw : blankRaws) {
        if (originalRow.hasFeature(blankRaw)) {
          // save blank detections to a blank-list
          final Feature blankFeature = originalRow.getFeature(blankRaw);
          backgroundFeaturesOfCurrentRow.add(blankFeature);
          ++foundInNBlanks;
        }
      }

      double blankAbundance = -1;
      int foundInSamplesAsBackground = 0;
      if (notBackgroundFeaturesOfCurrentRow.size() < minBlankDetections || checkFoldChange) {
        blankAbundance =
            checkFoldChange ? getBlankAbundance(originalRow, blankRaws, quantType, ratioType) : 1d;
        // copy features from non-blank files.
        for (RawDataFile file : nonBlankRaws) {
          final Feature nonBlankFeature = originalRow.getFeature(file);
          // check if there's actually a feature
          if (nonBlankFeature != null
              && nonBlankFeature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
            // check if feature is more abundant than the blank samples
            double featureAbundance = getFeatureQuantifier(nonBlankFeature, quantType);
            if (!checkFoldChange || featureAbundance / blankAbundance >= foldChange) {
              // the feature is a true feature and not a background
              notBackgroundFeaturesOfCurrentRow.add(nonBlankFeature);
            } else {
              // the feature is indistinguishable from the blanks
              backgroundFeaturesOfCurrentRow.add(nonBlankFeature);
              foundInSamplesAsBackground++;
            }
          }
        }
      }

      // filtered features
      if (notBackgroundFeaturesOfCurrentRow.size() > 0) {
        // use notBackgroundFeatures in the new results feature list
        final ModularFeatureListRow featureListRow = new ModularFeatureListRow(
            notBackgroundAlignedFeaturesList, originalRow.getID(),
            (ModularFeatureListRow) originalRow, false);
        notBackgroundFeaturesOfCurrentRow.forEach(f -> featureListRow.addFeature(f.getRawDataFile(),
            new ModularFeature(notBackgroundAlignedFeaturesList, f)));

        // if the user wants to:
        // add background features also (e.g. for parameter optimization, statistics, visualization, etc.)
        if (keepBackgroundFeatures == BlankSubtractionOptions.KEEP) {
          backgroundFeaturesOfCurrentRow.forEach(f -> featureListRow.addFeature(f.getRawDataFile(),
              new ModularFeature(notBackgroundAlignedFeaturesList, f)));
        }

        if (this.createDeletedFeatureList) {
          final StringBuilder sb = new StringBuilder();
          sb.append("Not background: ");
          if (foundInNBlanks == 0) {
            sb.append(String.format(" found only in %3d / %3d (%4.1f%%) samples",
                notBackgroundFeaturesOfCurrentRow.size(), nonBlankRaws.size(),
                ((float) notBackgroundFeaturesOfCurrentRow.size()) / nonBlankRaws.size() * 100.));
            sb.append(String.format(" but not in any of the %3d blank samples", blankRaws.size()));
          } else {
            sb.append(String.format(" found in %3d / %3d (%4.1f%%) samples",
                notBackgroundFeaturesOfCurrentRow.size(), nonBlankRaws.size(),
                ((float) notBackgroundFeaturesOfCurrentRow.size()) / nonBlankRaws.size() * 100.));
            sb.append(String.format(" and in %3d / %3d (%4.1f%%) background samples (abundance %s)",
                foundInNBlanks, blankRaws.size(),
                ((float) foundInNBlanks) / blankRaws.size() * 100.,
                guiFormats.intensity(blankAbundance)));
          }
          featureListRow.set(BlankSubtractionAnnotationType.class, sb.toString());
        }
        notBackgroundAlignedFeaturesListRows.add(featureListRow);
      }

      // save background features to a new row
      if (backgroundFeaturesOfCurrentRow.size() > 0
          && notBackgroundFeaturesOfCurrentRow.size() == 0) {
        // use feature in the background results feature list
        final ModularFeatureListRow featureListRow = new ModularFeatureListRow(
            backgroundAlignedFeaturesList, originalRow.getID(), (ModularFeatureListRow) originalRow,
            false);
        backgroundFeaturesOfCurrentRow.forEach(f -> featureListRow.addFeature(f.getRawDataFile(),
            new ModularFeature(backgroundAlignedFeaturesList, f)));

        if (this.createDeletedFeatureList) {
          final StringBuilder sb = new StringBuilder();
          sb.append(String.format(
              "Background: Found in %3d / %3d (%4.1f%%) background samples (abundance %s) but not in any samples with higher abundances",
              foundInNBlanks, blankRaws.size(), ((float) foundInNBlanks) / blankRaws.size() * 100.,
              guiFormats.intensity(blankAbundance)));
          featureListRow.set(BlankSubtractionAnnotationType.class, sb.toString());
        }

        backgroundAlignedFeaturesListRows.add(featureListRow);
      }

      processedRows.getAndIncrement();
    }

    // Main feature list
    // create the filtered list so that the next step can use it
    notBackgroundAlignedFeaturesListRows.sort(FeatureListRowSorter.DEFAULT_RT);
    notBackgroundAlignedFeaturesListRows.forEach(notBackgroundAlignedFeaturesList::addRow);

    notBackgroundAlignedFeaturesList.getAppliedMethods()
        .addAll(originalFeatureList.getAppliedMethods());
    notBackgroundAlignedFeaturesList.getAppliedMethods().add(
        new SimpleFeatureListAppliedMethod(FeatureListBlankSubtractionModule.class, parameters,
            getModuleCallDate()));
    project.addFeatureList(notBackgroundAlignedFeaturesList);

    // Secondary feature list result
    // create the list with not-used features first so the used features are the last list to be
    // created and can be used in the next step when the "last-list" option is used
    if (this.createDeletedFeatureList) {
      backgroundAlignedFeaturesListRows.sort(FeatureListRowSorter.DEFAULT_RT);
      backgroundAlignedFeaturesListRows.forEach(backgroundAlignedFeaturesList::addRow);

      backgroundAlignedFeaturesList.getAppliedMethods()
          .addAll(originalFeatureList.getAppliedMethods());
      backgroundAlignedFeaturesList.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(FeatureListBlankSubtractionModule.class, parameters,
              getModuleCallDate()));
      project.addFeatureList(backgroundAlignedFeaturesList);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private double getFeatureQuantifier(Feature f, AbundanceMeasure quantType) {
    if (quantType == AbundanceMeasure.Height) {
      return f.getHeight();
    } else if (quantType == AbundanceMeasure.Area) {
      return f.getArea();
    }
    throw new RuntimeException("Unknown parameter");
  }

  private double getBlankAbundance(FeatureListRow row, Collection<RawDataFile> blankRaws,
      AbundanceMeasure quantType, RatioType ratioType) {
    double intensity = 0d;
    int numDetections = 0;

    for (RawDataFile file : blankRaws) {
      final Feature f = row.getFeature(file);
      if (f != null && f.getFeatureStatus() != FeatureStatus.UNKNOWN) {
        double quant = getFeatureQuantifier(f, quantType);

        if (ratioType == RatioType.AVERAGE) {
          intensity += quant;
          numDetections++;
        } else if (ratioType == RatioType.MAXIMUM) {
          intensity = Math.max(quant, intensity);
        }
      }
    }

    return ratioType == RatioType.AVERAGE && numDetections != 0 ? intensity / numDetections
        : intensity;
  }

  private boolean checkBlankSelection(FeatureList aligned, List<RawDataFile> blankRaws) {

    List<RawDataFile> flRaws = aligned.getRawDataFiles();

    for (int i = 0; i < blankRaws.size(); i++) {
      boolean contained = false;

      for (RawDataFile flRaw : flRaws) {
        if (blankRaws.get(i) == flRaw) {
          contained = true;
        }
      }

      if (!contained) {
        final int i1 = i;
        logger.info(() -> "Feature list " + aligned.getName() + " does not contain raw data files "
            + blankRaws.get(i1).getName());
        return false;
      }
    }

    logger.finest(
        () -> "Feature list " + aligned.getName() + " contains all selected blank raw data files.");
    return true;
  }

  enum RatioType {
    AVERAGE, MAXIMUM
  }

  enum BlankSubtractionOptions {
    KEEP("KEEP - Keep all features in that row"), REMOVE(
        "REMOVE - Only keep features above fold change");

    private String description;

    BlankSubtractionOptions(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return this.description;
    }
  }
}
