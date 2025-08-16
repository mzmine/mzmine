/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_ri;

import static io.github.mzmine.modules.visualization.projectmetadata.table.columns.MetadataColumn.DATE_HEADER;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.annotations.RIScaleType;
import io.github.mzmine.datamodel.features.types.numbers.RIDiffType;
import io.github.mzmine.datamodel.features.types.numbers.RIMaxType;
import io.github.mzmine.datamodel.features.types.numbers.RIMinType;
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.date.LocalDateTimeParser;
import io.github.mzmine.util.io.CsvReader;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
public class RICalculationTask extends AbstractFeatureListTask {


  private static final Logger logger = Logger.getLogger(RICalculationTask.class.getName());
  private final String suffix;
  private final MZmineProject project;
  private final ModularFeatureList featureList;
  private ModularFeatureList outputList;
  private final OriginalFeatureListHandlingParameter.OriginalFeatureListOption handleOriginal;
  private final boolean shouldExtrapolate;
  private final boolean shouldAddSummary;
  private final MetadataTable metadataTable;
  private List<RIScale> linearScalesDescendingDate;


  public RICalculationTask(MZmineProject project, @Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull ModularFeatureList featureList, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
    this.metadataTable = project.getProjectMetadata();
    this.featureList = featureList;
    this.shouldExtrapolate = parameters.getValue(RICalculationParameters.extrapolate);
    this.shouldAddSummary = parameters.getValue(RICalculationParameters.addSummary);
    this.handleOriginal = parameters.getValue(RICalculationParameters.handleOriginal);
    this.suffix = parameters.getValue(RICalculationParameters.suffix);
  }

  @Override
  protected void process() {
    assert featureList != null;
    if (!featureList.hasFeatureType(RTType.class)) {
      error("Feature list requires RT in features for retention index calculation");
      return;
    }

    // sorted scales - descending
    final File[] scaleFiles = parameters.getValue(RICalculationParameters.alkaneFiles);
    linearScalesDescendingDate = Arrays.stream(scaleFiles).map(this::processLadder)
        .filter(Objects::nonNull).sorted(Comparator.comparing(RIScale::date).reversed()).toList();

    if (linearScalesDescendingDate.isEmpty()) {
      error("No valid scale files found");
      return;
    } else if (scaleFiles.length != linearScalesDescendingDate.size()) {
      logger.warning(
          "Number of scale files %d and number of valid scales %d unequal. Will proceed.".formatted(
              scaleFiles.length, linearScalesDescendingDate.size()));
    }

    ModularFeatureList processedList = processFeatureList(featureList);

    if (!isCanceled()) {
      handleOriginal.reflectNewFeatureListToProject(suffix, project, processedList, featureList);
      setStatus(TaskStatus.FINISHED);
    }
  }

  record RIScaleEntry(@JsonProperty("Carbon #") int nCarbons, @JsonProperty("RT") float rt) {

  }

  protected RIScale processLadder(File file) {
    if (file.exists() && file.canRead()) {
      try {
        String fileName = file.getAbsolutePath();
        LocalDateTime date = LocalDateTimeParser.parseAnyFirstDate(
            FilenameUtils.removeExtension(fileName));

        if (date == null) {
          return null;
        }
        final List<RIScaleEntry> entries = CsvReader.readToList(file, RIScaleEntry.class, ',');

        PolynomialSplineFunction interpolator = new LinearInterpolator().interpolate(
            entries.stream().mapToDouble(entry -> (double) entry.rt()).toArray(),
            entries.stream().mapToDouble(entry -> (double) entry.nCarbons() * 100).toArray());

        return new RIScale(date, fileName, interpolator);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  protected ModularFeatureList processFeatureList(final ModularFeatureList featureList) {
    outputList = featureList.createCopy(featureList.getName() + " with retention index", null,
        false);

    outputList.addFeatureType(new RIType());
    if (shouldAddSummary) {
      // the file is set directly
      outputList.addFeatureType(new RIScaleType());

      // values below are calculated as row bindings from the RIType - so just add the row binding
      // this automatically adds the types as row types instead of feature types
      outputList.addFeatureType(new RIMinType());
      outputList.addFeatureType(new RIMaxType());
      outputList.addFeatureType(new RIDiffType());

    }

    for (RawDataFile file : outputList.getRawDataFiles()) {
      LocalDateTime sampleDate = metadataTable.getValue(metadataTable.getRunDateColumn(), file);
      if (sampleDate == null) {
        error(
            "Requires sample date to be set in project metadata in column %s. The easiest way is to define the dates in a csv file and import as metadata. Format: 2024-01-21 or with time 2024-01-21T20:35:41".formatted(
                DATE_HEADER));
        return null;
      }

      // find closest linear scale based on date comparison

      final RIScale bestScale = findScalePreviousToSample(sampleDate);

      final long days = ChronoUnit.DAYS.between(bestScale.date().toLocalDate(),
          sampleDate.toLocalDate());
      if (Math.abs(days) > 0) {
        logger.info(
            "NOTICE: The retention index scale (%s) was measured on a different day than the sample %s (%s) (%d days)".formatted(
                bestScale.date(), file.getName(), sampleDate, days));
      }

      outputList.getFeatures(file).stream().parallel().forEach(f -> processFeature(f, bestScale));
    }
    return outputList;
  }

  private RIScale findScalePreviousToSample(LocalDateTime sampleDate) {
    // find latest scale that was measured before sample date
    // newest scale first
    for (RIScale scale : linearScalesDescendingDate) {
      // same day or after
      if (!sampleDate.isBefore(scale.date())) {
        return scale;
      }
    }
    // last scale in case sample was measured before all scales
    return linearScalesDescendingDate.getLast();
  }

  private void processFeature(final ModularFeature feature, final RIScale riScale) {
    Float rt = feature.getRT();
    if (rt == null) {
      return;
    }

    Float ri = null;
    double[] knots = riScale.interpolator().getKnots();

    if (rt >= knots[0] && rt <= knots[knots.length - 1]) {
      ri = (float) riScale.interpolator().value(rt);
    } else if (shouldExtrapolate && rt > knots[knots.length - 1]) {
      ri = (float) riScale.interpolator().getPolynomials()[
          riScale.interpolator().getPolynomials().length - 1].value(rt - knots[knots.length - 1]);
    } else if (shouldExtrapolate && rt < knots[0]) {
      ri = (float) riScale.interpolator().getPolynomials()[0].value(rt - knots[0]);
    }

    if (ri != null) {
      feature.set(RIType.class, ri);
      feature.set(RIScaleType.class, FilenameUtils.getName(riScale.fileName()));
    }
  }

  @Override
  public String getTaskDescription() {
    return "Calculates Kovats retention index for each feature";
  }

  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(outputList);
  }

}