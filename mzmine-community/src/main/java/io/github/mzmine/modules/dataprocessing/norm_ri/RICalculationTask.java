/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.opencsv.CSVReader;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.types.annotations.RIScaleType;
import io.github.mzmine.datamodel.features.types.numbers.RIMinType;
import io.github.mzmine.datamodel.features.types.numbers.RIMaxType;
import io.github.mzmine.datamodel.features.types.numbers.RIDiffType;
import io.github.mzmine.datamodel.features.types.numbers.RIType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.visualization.projectmetadata.table.MetadataTable;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.date.LocalDateParser;
import io.github.mzmine.util.io.CsvReader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;


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
  private volatile List<RIScale> linearScales = new ArrayList<>();
  private final MetadataTable metadataTable;
  ;


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
    linearScales = new ArrayList<>(Arrays.stream(parameters.getValue(RICalculationParameters.alkaneFiles))
        .sequential()
        .map(this::processLadder)
        .filter(Objects::nonNull)
        .toList());
    linearScales.sort(Comparator.comparing(RIScale::date).reversed());

    ModularFeatureList processedList = processFeatureList(featureList);

    handleOriginal.reflectNewFeatureListToProject(suffix, project, processedList,
        featureList);
    setStatus(TaskStatus.FINISHED);

  }

  record RIScaleEntry(@JsonProperty("Carbon #") int nCarbons, @JsonProperty("RT") float rt) {
  }

  protected RIScale processLadder(File file) {
    if (file.exists() && file.canRead()) {
      try {
        String fileName = file.getAbsolutePath();
        LocalDate date = LocalDateParser.parseAnyEndingDate(FilenameUtils.removeExtension(fileName));

        if (date == null || fileName == null) {
          return null;
        }
        final List<RIScaleEntry> entries = CsvReader.readToList(file,
            RIScaleEntry.class, ',');

        PolynomialSplineFunction interpolator = new LinearInterpolator().interpolate(
            entries.stream().mapToDouble(entry -> (double) entry.rt()).toArray(),
            entries.stream().mapToDouble(entry -> (double) entry.nCarbons() * 100).toArray()
        );

        return new RIScale(date, fileName, interpolator);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  protected ModularFeatureList processFeatureList(final ModularFeatureList featureList) {
    if (featureList != null && featureList.hasFeatureType(RTType.class)) {
      ModularFeatureList outputList = featureList.createCopy(featureList.getName() + " with retention index", null, false);

      outputList.addFeatureType(new RIType());
      if (shouldAddSummary) {
        outputList.addFeatureType(new RIMaxType());
        outputList.addFeatureType(new RIMinType());
        outputList.addFeatureType(new RIDiffType());
        outputList.addFeatureType(new RIScaleType());
      }


      for (RawDataFile file : outputList.getRawDataFiles()) {
        LocalDate sampleDate = metadataTable.getValue(metadataTable.getRunDateColumn(), file).toLocalDate();
        for (RIScale riScale : linearScales) {
          if (sampleDate != null && sampleDate.isAfter(riScale.date()) || sampleDate.isEqual(riScale.date())) {
            outputList.getFeatures(file).stream().parallel().forEach(f -> processFeature(f, riScale));
            break;
          }
        }

      }
      return outputList;
    }
    return featureList;
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
      ri = (float) riScale.interpolator().getPolynomials()[riScale.interpolator().getPolynomials().length - 1].value(rt - knots[knots.length - 1]);
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