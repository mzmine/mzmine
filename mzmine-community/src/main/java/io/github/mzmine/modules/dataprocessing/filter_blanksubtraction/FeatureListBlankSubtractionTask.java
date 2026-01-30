/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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
import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelection;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
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
  /**
   * always set and always >=1
   */
  private final double foldChange;
  private final BlankSubtractionOptions subtractionOption;
  private final RatioType ratioType;
  private final AbundanceMeasure quantType;
  private final OriginalFeatureListOption handleOriginal;

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
    this.handleOriginal = parameters.getValue(FeatureListBlankSubtractionParameters.handleOriginal);
    this.blankSelection = parameters.getParameter(
        FeatureListBlankSubtractionParameters.blankRawDataFiles).getValue();
    this.blankRaws = List.of(blankSelection.getMatchingRawDataFiles().clone());
    this.originalFeatureList = parameters.getParameter(
            FeatureListBlankSubtractionParameters.alignedPeakList).getValue()
        .getMatchingFeatureLists()[0];
    this.minBlankDetections = Math.max(1,
        parameters.getParameter(FeatureListBlankSubtractionParameters.minBlanks).getValue());
    this.subtractionOption = parameters.getParameter(
        FeatureListBlankSubtractionParameters.subtractionOption).getValue();
    this.suffix = parameters.getParameter(FeatureListBlankSubtractionParameters.suffix).getValue();
    this.createDeletedFeatureList = parameters.getParameter(
        FeatureListBlankSubtractionParameters.createDeleted).getValue();
    var fcCheck = parameters.getOptionalValue(FeatureListBlankSubtractionParameters.foldChange);
    // always set foldChange so it can be used without checks
    foldChange = Math.max(1, fcCheck.orElse(1d));
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
    // blanks are always removed
    final ModularFeatureList notBackgroundAlignedFeaturesList = FeatureListUtils.createCopyWithoutRows(
        originalFeatureList, suffix, getMemoryMapStorage(), nonBlankRaws, null, null);

    // create feature list containing all background features and all samples
    final ModularFeatureList backgroundAlignedFeaturesList = FeatureListUtils.createCopyWithoutRows(
        originalFeatureList, "subtractedBackground", getMemoryMapStorage(), null, null);

    final List<FeatureListRow> notBackgroundAlignedFeaturesListRows = new ArrayList<>();
    final List<FeatureListRow> backgroundAlignedFeaturesListRows = new ArrayList<>();
    for (FeatureListRow originalRow : originalFeatureList.getRows()) {

      final List<Feature> notBackgroundFeaturesOfCurrentRow = getRowFeatures(originalRow,
          nonBlankRaws);
      final List<Feature> backgroundFeaturesOfCurrentRow = getRowFeatures(originalRow, blankRaws);
      int foundInNBlanks = backgroundFeaturesOfCurrentRow.size();

      final List<Feature> featuresToKeep = new ArrayList<>();
      final List<Feature> featuresToRemove = new ArrayList<>(backgroundFeaturesOfCurrentRow);

      // used for fold change and for formatting of annotation
      double blankAbundance = getAbundance(backgroundFeaturesOfCurrentRow, quantType, ratioType);

      // minBlankDetections is 1 or higher
      if (foundInNBlanks < minBlankDetections || blankAbundance <= 0d) {
        // keep whole row because detections in blank is unused
        featuresToKeep.addAll(notBackgroundFeaturesOfCurrentRow);
      } else if (subtractionOption == BlankSubtractionOptions.MAXIMUM_FEATURE) {
        // if one feature is higher than blanks keep all features in that row
        double maxFeatureAbundance = getAbundance(notBackgroundFeaturesOfCurrentRow, quantType,
            RatioType.MAXIMUM);
        if (maxFeatureAbundance >= blankAbundance * foldChange) {
          featuresToKeep.addAll(notBackgroundFeaturesOfCurrentRow);
        } else {
          featuresToRemove.addAll(notBackgroundFeaturesOfCurrentRow);
        }
      } else if (subtractionOption == BlankSubtractionOptions.EACH_FEATURE) {
        // check the intensity of each feature in samples
        for (Feature nonBlankFeature : notBackgroundFeaturesOfCurrentRow) {
          // check if feature is more abundant than the blank samples
          double featureAbundance = getFeatureQuantifier(nonBlankFeature, quantType);
          if (featureAbundance >= blankAbundance * foldChange) {
            featuresToKeep.add(nonBlankFeature);
          } else {
            // the feature is indistinguishable from the blanks
            featuresToRemove.add(nonBlankFeature);
          }
        }
      } else {
        throw new IllegalStateException("Unhandled option");
      }

      // filtered features
      if (!featuresToKeep.isEmpty()) {
        // new results feature list
        final ModularFeatureListRow featureListRow = new ModularFeatureListRow(
            notBackgroundAlignedFeaturesList, originalRow.getID(),
            (ModularFeatureListRow) originalRow, false);
        featuresToKeep.forEach(f -> featureListRow.addFeature(f.getRawDataFile(),
            new ModularFeature(notBackgroundAlignedFeaturesList, f)));

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

      //
      // only add rows if there were more sample features in there and not only blank features
      final boolean hasSampleFeaturesRemoved =
          featuresToRemove.size() > backgroundFeaturesOfCurrentRow.size();
      if (createDeletedFeatureList && hasSampleFeaturesRemoved) {
        // empty row so add to other list if requested
        // use feature in the background results feature list
        final ModularFeatureListRow featureListRow = new ModularFeatureListRow(
            backgroundAlignedFeaturesList, originalRow.getID(), (ModularFeatureListRow) originalRow,
            false);
        featuresToRemove.forEach(f -> featureListRow.addFeature(f.getRawDataFile(),
            new ModularFeature(backgroundAlignedFeaturesList, f)));

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "Background: Found in %3d / %3d (%4.1f%%) background samples (abundance %s) but not in any samples with higher abundances",
            foundInNBlanks, blankRaws.size(), ((float) foundInNBlanks) / blankRaws.size() * 100.,
            guiFormats.intensity(blankAbundance)));
        featureListRow.set(BlankSubtractionAnnotationType.class, sb.toString());

        backgroundAlignedFeaturesListRows.add(featureListRow);
      }

      processedRows.getAndIncrement();
    }

    // Main feature list
    // create the filtered list so that the next step can use it
    notBackgroundAlignedFeaturesListRows.sort(FeatureListRowSorter.DEFAULT_RT);
    notBackgroundAlignedFeaturesListRows.forEach(notBackgroundAlignedFeaturesList::addRow);

    final SimpleFeatureListAppliedMethod appliedMethod = new SimpleFeatureListAppliedMethod(
        FeatureListBlankSubtractionModule.class, parameters, getModuleCallDate());

    notBackgroundAlignedFeaturesList.getAppliedMethods().add(appliedMethod);
    project.addFeatureList(notBackgroundAlignedFeaturesList);

    // Secondary feature list result
    // create the list with not-used features first so the used features are the last list to be
    // created and can be used in the next step when the "last-list" option is used
    if (this.createDeletedFeatureList) {
      backgroundAlignedFeaturesListRows.sort(FeatureListRowSorter.DEFAULT_RT);
      backgroundAlignedFeaturesListRows.forEach(backgroundAlignedFeaturesList::addRow);

      backgroundAlignedFeaturesList.getAppliedMethods().add(appliedMethod);
      project.addFeatureList(backgroundAlignedFeaturesList);
    }

    if (handleOriginal == OriginalFeatureListOption.REMOVE) {
      project.removeFeatureList(originalFeatureList);
    }

    setStatus(TaskStatus.FINISHED);
  }

  private List<Feature> getRowFeatures(FeatureListRow row, List<RawDataFile> raws) {
    final List<Feature> list = new ArrayList<>();
    for (RawDataFile raw : raws) {
      final Feature feature = row.getFeature(raw);
      if (feature != null && feature.getFeatureStatus() != FeatureStatus.UNKNOWN) {
        list.add(feature);
      }
    }
    return list;
  }

  private double getFeatureQuantifier(Feature f, AbundanceMeasure quantType) {
    if (quantType == AbundanceMeasure.Height) {
      return f.getHeight();
    } else if (quantType == AbundanceMeasure.Area) {
      return f.getArea();
    }
    throw new RuntimeException("Unknown parameter");
  }

  private double getAbundance(List<Feature> features, AbundanceMeasure quantType,
      RatioType ratioType) {
    double intensity = 0d;
    int numDetections = 0;

    for (Feature f : features) {
      double quant = getFeatureQuantifier(f, quantType);

      // exhaustive switch
      switch (ratioType) {
        case AVERAGE -> {
          intensity += quant;
          numDetections++;
        }
        case MAXIMUM -> {
          intensity = Math.max(quant, intensity);
        }
      }
    }

    intensity = ratioType == RatioType.AVERAGE ? intensity / numDetections : intensity;
    return intensity;
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

  /**
   * Used in parameter
   */
  public enum BlankSubtractionOptions implements UniqueIdSupplier {
    MAXIMUM_FEATURE("Most abundant feature (keep/remove whole row)"), //
    EACH_FEATURE("Each feature (keep/remove individual feature)");
    // used to be this but was changed in mzmine 4.9 because these options were made redundant
    // now the removed features can be captured in 2nd feature list of removed features
    // and the actual options are to compare the maximum abundance or each feature
    // no need to map the old value to the new one, rather version bump parameters
//    KEEP("KEEP - Keep all features in that row"), REMOVE(
//        "REMOVE - Only keep features above fold change");


    private final String description;

    BlankSubtractionOptions(String description) {
      this.description = description;
    }

    @Override
    public String toString() {
      return this.description;
    }

    @Override
    public @NotNull String getUniqueID() {
      return switch (this) {
        case MAXIMUM_FEATURE -> "maximum_feature";
        case EACH_FEATURE -> "each_feature";
      };
    }
  }
}
