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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_chromatograms;

import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.SAMPLE_TYPE_HEADER;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.FeatureDataUtils;
import io.github.mzmine.datamodel.featuredata.IonSpectrumSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.FeatureDataType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerTask;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter.OriginalFeatureListOption;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.IndexRange;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChromatogramBlankSubtractionTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      ChromatogramBlankSubtractionTask.class.getName());
  private final MZmineProject project;
  private final @NotNull FeatureList[] featureLists;
  private final MZTolerance mzTol;
  private final String suffix;
  private final OriginalFeatureListOption handleOriginal;
  private String description = "";
  private List<FeatureList> resultFeatureLists;

  /**
   * @param storage        The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                       RawDataFiles, MassLists, FeatureLists). May be null if results shall be
   *                       stored in ram. For now, one storage should be created per module call in
   * @param moduleCallDate the call date of module to order execution order
   * @param parameters
   * @param moduleClass
   * @param project
   * @param featureLists
   */
  protected ChromatogramBlankSubtractionTask(final @Nullable MemoryMapStorage storage,
      final @NotNull Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      final @NotNull MZmineProject project, final @NotNull FeatureList[] featureLists) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    this.featureLists = featureLists;
    mzTol = parameters.getValue(ChromatogramBlankSubtractionParameters.mzTol);
    suffix = parameters.getValue(ChromatogramBlankSubtractionParameters.suffix);
    handleOriginal = parameters.getValue(ChromatogramBlankSubtractionParameters.handleOriginal);
  }


  @Override
  protected void process() {
    // precondition - no resolving
    checkPreconditions();
    if (isCanceled()) {
      return;
    }

    final DataInput input = splitInputFeatureLists();
    if (input == null || isCanceled()) {
      return;
    }

    // prepare blanks - merge chromatograms across all blank samples
    final List<CommonRtAxisChromatogram> mzSortedBlanks = prepareBlanks(input.blanks);

    // use map because forEach in parallel does not wait for complete
    resultFeatureLists = input.samples.stream().parallel().map(flist -> {
      if (isCanceled()) {
        return null;
      }
      return subtractBlanks(flist, mzSortedBlanks);
    }).toList();

    // test: also apply to blanks - just to check
    var resultBlanks = input.blanks.stream().parallel().map(flist -> {
      if (isCanceled()) {
        return null;
      }
      return subtractBlanks(flist, mzSortedBlanks);
    }).toList();
    resultBlanks.forEach(project::addFeatureList);

    if (isCanceled()) {
      return;
    }

    // reflect changes
    handleOriginal.reflectChangesToProject(project, suffix, featureLists, resultFeatureLists);
  }

  /**
   * subract blanks from all features and create new feature data. Use row average mz and mzTol to
   * find matching blank chromatograms (maybe multiple). Subtract maximum intensity over all blanks
   * from sample intensity.
   *
   * @param oldFeatureList
   * @param mzSortedBlanks
   * @return
   */
  private FeatureList subtractBlanks(final FeatureList oldFeatureList,
      final List<CommonRtAxisChromatogram> mzSortedBlanks) {
    var resultFlist = handleOriginal == OriginalFeatureListOption.PROCESS_IN_PLACE ? oldFeatureList
        : FeatureListUtils.createCopy(oldFeatureList, suffix, getMemoryMapStorage(), true);

    if (resultFlist.getNumberOfRows() == 0) {
      return resultFlist;
    }

    var sortedRows = resultFlist.getRows().stream().sorted(FeatureListRowSorter.MZ_ASCENDING)
        .toList();

    int startIndex = 0;

    // flag rows to remove
    IntList rowsToRemove = new IntArrayList();

    for (int rowIndex = 0; rowIndex < sortedRows.size(); rowIndex++) {
      final FeatureListRow row = sortedRows.get(rowIndex);
      Range<Double> mzRange = mzTol.getToleranceRange(row.getAverageMZ());
      IndexRange indexRange = BinarySearch.indexRange(mzRange, mzSortedBlanks, startIndex,
          CommonRtAxisChromatogram::mz);
      if (!indexRange.isEmpty()) {
        startIndex = indexRange.min(); // helps skip some
      }

      List<CommonRtAxisChromatogram> matchingBlanks = indexRange.sublist(mzSortedBlanks);
      if (matchingBlanks.isEmpty()) {
        continue;
      }

      for (final ModularFeature feature : row.getFeatures()) {
        if (isCanceled()) {
          return resultFlist;
        }
        boolean changed = false;

        IonTimeSeries<? extends Scan> data = feature.getFeatureData();
        double[] intensities = new double[data.getNumberOfValues()];
        for (int i = 0; i < intensities.length; i++) {
          float rt = data.getRetentionTime(i);
          // index is the same for all blank chromatograms
          int blankIndex = matchingBlanks.getFirst().closestIndexOf(rt);

          double maxBlankIntensity = matchingBlanks.stream()
              .mapToDouble(blank -> blank.getMaxIntensity(blankIndex - 1, blankIndex + 1)).max()
              .orElse(0);

          // non negative
          if (maxBlankIntensity > 0) {
            intensities[i] = Math.max(data.getIntensity(i) - maxBlankIntensity, 0);
            changed = true;
          }
          // TODO remove as this is only for testing and seeing results in feature list
//          intensities[i] = maxBlankIntensity;
//          changed = true;
        }
        // only apply changes if really changed
        if (changed) {
          // all 0 then remove feature
          if (Arrays.stream(intensities).noneMatch(v -> v > 0)) {
            rowsToRemove.add(rowIndex);
          } else {
            replaceFeatureData(feature, data, intensities);
          }
        }
      }
    }

    // remove rows that are completely empty
    resultFlist.removeRows(rowsToRemove.toIntArray());
    logger.fine(
        "Blank subtraction removed %d chromatograms that are now empty from feature list: %s".formatted(
            rowsToRemove.size(), resultFlist.getName()));

    return resultFlist;
  }

  private void replaceFeatureData(final ModularFeature feature,
      final IonTimeSeries<? extends Scan> data, final double[] intensities) {
    IonSpectrumSeries<? extends Scan> blankSubtractedData = data.copyAndReplace(
        getMemoryMapStorage(), intensities);

    feature.set(FeatureDataType.class, (IonTimeSeries<? extends Scan>) blankSubtractedData);
    FeatureDataUtils.recalculateIonSeriesDependingTypes(feature);
  }

  private @Nullable ChromatogramBlankSubtractionTask.DataInput splitInputFeatureLists() {
    Set<RawDataFile> blankRaws = Set.copyOf(
        project.getProjectMetadata().getFilesOfSampleType(SampleType.BLANK));
    if (blankRaws.isEmpty()) {
      error(
          "No blank samples found in metadata. Define files as %s in metadata column %s.".formatted(
              SampleType.BLANK, SAMPLE_TYPE_HEADER));
      return null;
    }
    // split feature lists
    List<FeatureList> blanks = new ArrayList<>(blankRaws.size());
    List<FeatureList> samples = new ArrayList<>(
        Math.max(1, featureLists.length - blankRaws.size()));
    for (final FeatureList flist : featureLists) {
      if (blankRaws.contains(flist.getRawDataFile(0))) {
        blanks.add(flist);
      } else {
        samples.add(flist);
      }
    }

    if (blanks.isEmpty()) {
      error(
          "No blank feature lists found. Make sure to process blank samples by chromatogram builder and select them in the feature lists parameter.");
      return null;
    }

    // use array list to be able to sort
    return new DataInput(blanks, samples);
  }

  private @Nullable List<CommonRtAxisChromatogram> prepareBlanks(final List<FeatureList> blanks) {
    // align blank samples first
    setLogDescription("Aligning blanks");
    var alignParams = JoinAlignerParameters.create(mzTol);
    var aligner = JoinAlignerTask.createAligner(this, getMemoryMapStorage(), alignParams, blanks,
        "Blank lists aligned");

    ModularFeatureList alignedBlanks = aligner.alignFeatureLists();
    if (alignedBlanks == null || isCanceled()) {
      return null;
    }
    setLogDescription("Aligning blanks done");

    // same time scale
    var templateChrom = extractUnifiedRetentionTimes(blanks);

    setLogDescription("Creating merged blank chromatograms");
    List<CommonRtAxisChromatogram> chromatograms = alignedBlanks.getRows().stream()
        // sort by mz for binary search later
        .sorted(FeatureListRowSorter.MZ_ASCENDING)
        .map(row -> mergeChromatogramAcrossSamples(templateChrom, row)).toList();

    return chromatograms;
  }

  private void setLogDescription(final String description) {
    this.description = description;
    logger.finer(getTaskDescription());
  }

  private CommonRtAxisChromatogram mergeChromatogramAcrossSamples(
      final CommonRtAxisChromatogram templateChrom, final FeatureListRow row) {
    // create copy with intensities all 0
    CommonRtAxisChromatogram chromatogram = templateChrom.withMzAndEmpty(row.getAverageMZ());
    for (final ModularFeature feature : row.getFeatures()) {
      if (feature == null || feature.getFeatureStatus() == FeatureStatus.UNKNOWN
          || feature.getFeatureData() == null) {
        continue;
      }
      // add all intensities
      IonTimeSeries<? extends Scan> data = feature.getFeatureData();
      for (int i = 0; i < data.getNumberOfValues(); i++) {
        float rt = data.getRetentionTime(i);
        double intensity = data.getIntensity(i);

        chromatogram.setClosestMaxIntensity(rt, intensity);
      }
    }
    return chromatogram;
  }

  private CommonRtAxisChromatogram extractUnifiedRetentionTimes(final List<FeatureList> blanks) {
    setLogDescription("Unifying RT from blanks");
    var flistScans = blanks.stream().map(flist -> flist.getSeletedScans(flist.getRawDataFile(0)))
        .toList();

    // less than maximum number of data points
    float shrinkFactor = 0.75f;
    int bins = (int) (flistScans.stream().mapToInt(List::size).max().orElse(0) * shrinkFactor);
    float minRetentionTime = Float.MAX_VALUE;
    float maxRetentionTime = 0;
    for (final List<? extends Scan> scans : flistScans) {
      if (scans.isEmpty()) {
        continue;
      }
      float minRt = scans.getFirst().getRetentionTime();
      float maxRt = scans.getLast().getRetentionTime();
      if (minRt < minRetentionTime) {
        minRetentionTime = minRt;
      }
      if (maxRt > maxRetentionTime) {
        maxRetentionTime = maxRt;
      }
    }

    return CommonRtAxisChromatogram.create(0, bins, minRetentionTime, maxRetentionTime);
  }

  private void checkPreconditions() {
    for (final FeatureList flist : featureLists) {
      // only one sample
      if (flist.getNumberOfRawDataFiles() != 1) {
        error(
            "Can only run blank removal before alignment. Needs 1 raw data file per feature list. Apply blank removal after chromatogram builder and optionally smoothing.");
        return;
      }

      // cannot apply after resolving
      boolean isResolved = flist.getAppliedMethods().stream()
          .map(FeatureListAppliedMethod::getModule).filter(MZmineRunnableModule.class::isInstance)
          .map(MZmineRunnableModule.class::cast).map(MZmineRunnableModule::getModuleCategory)
          .anyMatch(Predicate.isEqual(MZmineModuleCategory.FEATURE_RESOLVING));
      if (isResolved) {
        error(
            "Apply blank removal before resolving, after chromatogram builder and optionally smoothing.");
        return;
      }

      // scans
      List<? extends Scan> scans = flist.getSeletedScans(flist.getRawDataFile(0));
      if (scans == null) {
        String steps = flist.getAppliedMethods().stream().map(FeatureListAppliedMethod::getModule)
            .map(MZmineModule::getName).collect(Collectors.joining("; "));
        logger.warning(
            "Feature list has no scans. This is the list of applied steps for this feature list:\n"
            + steps);
        error(
            "Feature list %s has no selected scans. Please report to the mzmine team with the full log.".formatted(
                flist.getName()));
        return;
      }
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return resultFeatureLists;
  }

  @Override
  public String getTaskDescription() {
    return "Feature blank subtraction: " + description;
  }

  record DataInput(List<FeatureList> blanks, List<FeatureList> samples) {

  }
}
