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

package io.github.mzmine.modules.dataprocessing.filter_blanksubtraction_using_eic;

import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.SAMPLE_TYPE_HEADER;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerParameters;
import io.github.mzmine.modules.dataprocessing.align_join.JoinAlignerTask;
import io.github.mzmine.modules.visualization.projectmetadata.SampleType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.util.FeatureListRowSorter;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.collections.BinarySearch;
import io.github.mzmine.util.collections.BinarySearch.DefaultTo;
import io.github.mzmine.util.collections.IndexRange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FeatureBlankSubtractionByChromatogramTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(
      FeatureBlankSubtractionByChromatogramTask.class.getName());
  private final MZmineProject project;
  private final @NotNull FeatureList[] featureLists;
  private final MZTolerance mzTol;
  private String description = "";

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
  protected FeatureBlankSubtractionByChromatogramTask(final @Nullable MemoryMapStorage storage,
      final @NotNull Instant moduleCallDate, @NotNull final ParameterSet parameters,
      @NotNull final Class<? extends MZmineModule> moduleClass,
      final @NotNull MZmineProject project, final @NotNull FeatureList[] featureLists) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    this.featureLists = featureLists;
    mzTol = parameters.getValue(FeatureBlankSubtractionByChromatogramParameters.mzTol);
  }


  @Override
  protected void process() {
    // precondition - no resolving
    checkPreconditions();
    if (isCanceled()) {
      return;
    }

    PreparedInput input = splitInputFeatureListsPrepareBlanks();
    if (isCanceled()) {
      return;
    }

    // use map because forEach in parallel does not wait for complete
    int success = input.samples.stream().parallel().mapToInt(flist -> {
      if (isCanceled()) {
        return 0;
      }
      subtractBlanks(flist, input.mzSortedBlanks);
      return 1;
    }).sum();

  }

  /**
   * subract blanks from all features and create new feature data.
   * Use row average mz and mzTol to find matching blank chromatograms (maybe multiple).
   * Subtract maximum intensity over all blanks from sample intensity.
   * @param flist
   * @param mzSortedBlanks
   */
  private void subtractBlanks(final FeatureList flist,
      final List<CommonRtAxisChromatogram> mzSortedBlanks) {
    if (flist.getNumberOfRows() == 0) {
      return;
    }

    var sortedRows = flist.getRows().stream().sorted(FeatureListRowSorter.MZ_ASCENDING).toList();

    int startIndex = 0;

    for (final FeatureListRow row : sortedRows) {
      Range<Double> mzRange = mzTol.getToleranceRange(row.getAverageMZ());
      IndexRange indexRange = BinarySearch.indexRange(mzRange, mzSortedBlanks, startIndex,
          CommonRtAxisChromatogram::mz);
      startIndex = indexRange.min(); // helps skip some

      List<CommonRtAxisChromatogram> matchingBlanks = indexRange.sublist(mzSortedBlanks);
      if (matchingBlanks.isEmpty()) {
        continue;
      }

      for (final ModularFeature feature : row.getFeatures()) {
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
          intensities[i] = Math.max(data.getIntensity(i)- maxBlankIntensity, 0);
        }

        data.copyAndReplace
      }
    }

  }

  private @Nullable PreparedInput splitInputFeatureListsPrepareBlanks() {
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
      if (blankRaws.contains(flist.getRawDataFile(1))) {
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

    // prepare blanks
    List<CommonRtAxisChromatogram> mzSortedBlanks = prepareBlanks(blanks);

    // use array list to be able to sort
    return new PreparedInput(blanks, samples, mzSortedBlanks);
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
      float rt = scans.getFirst().getRetentionTime();
      if (rt < minRetentionTime) {
        minRetentionTime = rt;
      }
      if (rt > maxRetentionTime) {
        maxRetentionTime = rt;
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
    return List.of();
  }

  @Override
  public String getTaskDescription() {
    return "Feature blank subtraction: " + description;
  }

  record PreparedInput(List<FeatureList> blanks, List<FeatureList> samples,
                       List<CommonRtAxisChromatogram> mzSortedBlanks) {

  }
}
