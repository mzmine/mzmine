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
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.OriginalFeatureListHandlingParameter;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The task will be scheduled by the TaskController. Progress is calculated from the
 * finishedItems/totalItems
 */
public class RICalculationTask extends AbstractFeatureListTask {

  record RIScale(Date date, String fileName, PolynomialSplineFunction interpolator){
    public RIScale(Date date, String fileName, PolynomialSplineFunction interpolator){
      this.date = date;
      this.fileName = fileName;
      this.interpolator = interpolator;
    }
  }
  private static final Logger logger = Logger.getLogger(RICalculationTask.class.getName());
  private final String suffix;
  private final MZmineProject project;
  private final ModularFeatureList featureList;
  private ModularFeatureList outputList;
  private final OriginalFeatureListHandlingParameter.OriginalFeatureListOption handleOriginal;
  private final boolean shouldExtrapolate;
  private final boolean shouldAddSummary;
  private volatile List <RIScale> linearScales = new ArrayList<>();


  public RICalculationTask(MZmineProject project, @Nullable MemoryMapStorage storage,
                           @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
                           @NotNull ModularFeatureList featureList, @NotNull Class<? extends MZmineModule> moduleClass) {
    super(storage, moduleCallDate, parameters, moduleClass);
    this.project = project;
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
            .map(this::processScale)
            .filter(Objects::nonNull)
            .toList());

    linearScales.sort(new Comparator<RIScale>() {
        @Override
        public int compare(RIScale o1, RIScale o2) {
            return o1.date.compareTo(o2.date);
        }
    });

    Collections.reverse(linearScales);
    ModularFeatureList processedList = processFeatureList(featureList);

    handleOriginal.reflectNewFeatureListToProject(suffix, project, processedList,
            featureList);
    setStatus(TaskStatus.FINISHED);

  }

  private RIScale processScale(File file) {
    if (file.exists() && file.canRead()) {
      try {
        Date date = extractDate(file.getName());
        String fileName = file.getAbsolutePath();

        if (date == null || fileName == null) {
          return null;
        }

        CSVReader reader = new CSVReader(new BufferedReader(new FileReader(file)));
        List<String[]> data = reader.readAll();
        String[] header = data.getFirst();
        if (header.length != 2 || !header[0].equalsIgnoreCase("carbon #") || !header[1].equalsIgnoreCase("rt")) {
          return null;
        }
        DoubleArrayList x = new DoubleArrayList();
        DoubleArrayList y = new DoubleArrayList();
        for (String[] pair : data.subList(1, data.size())) {
          y.add(Double.parseDouble(pair[0]) * 100);
          x.add(Double.parseDouble(pair[1]));
        }
        PolynomialSplineFunction interpolator = new LinearInterpolator().interpolate(
                x.toDoubleArray(),
                y.toDoubleArray()
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
      outputList = featureList.createCopy(featureList.getName() + " with retention index", null, false);

      outputList.addFeatureType(new RIType());
      if (shouldAddSummary) {
        outputList.addFeatureType(new RIMaxType());
        outputList.addFeatureType(new RIMinType());
        outputList.addFeatureType(new RIDiffType());
        outputList.addFeatureType(new RIScaleType());
      }


      for (RawDataFile file : outputList.getRawDataFiles()) {
        Date sampleDate = extractDate(file.getAbsolutePath());
        for (RIScale _riScale : linearScales) {
          if (sampleDate != null && sampleDate.after(_riScale.date) || sampleDate.equals(_riScale.date)) {
            final RIScale riScale = _riScale;
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
    double[] knots = riScale.interpolator.getKnots();

    if (rt >= knots[0] && rt <= knots[knots.length - 1]) {
      ri = (float) riScale.interpolator.value(rt);
    }

    else if (shouldExtrapolate && rt > knots[knots.length - 1]) {
      ri = (float) riScale.interpolator.getPolynomials()[riScale.interpolator.getPolynomials().length - 1].value(rt - knots[knots.length - 1]);
    }

    else if (shouldExtrapolate && rt < knots[0]) {
      ri = (float) riScale.interpolator.getPolynomials()[0].value(rt - knots[0]);
    }

    if (ri != null) {
      feature.set(RIType.class, ri);
      feature.set(RIScaleType.class, FilenameUtils.getName(riScale.fileName));
    }
  }

  private Date extractDate(String fileName) {
    // Extract date from file name
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Matcher matcher = Pattern.compile(".*(\\d{4}([-/])\\d{2}([-/])\\d{2}).*").matcher(fileName);
    if (matcher.find()) {
      try {
        return dateFormat.parse(matcher.group(1));
      }
      catch (Exception e) {
        logger.warning("Could not parse date from file name: " + fileName);
        return null;
      }
    }
    return null;
  }

  @Override
  public String getTaskDescription() {
    return "Calculates Kovats retention index for each feature";
  }

  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return List.of(outputList);
  }

}