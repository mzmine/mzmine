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

package io.github.mzmine.modules.dataanalysis.anova;

import io.github.mzmine.datamodel.FeatureInformation;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.impl.SimpleFeatureInformation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnovaTask extends AbstractTask {

  private static final String EMPTY_STRING = "";

  private static final String P_VALUE_KEY = "ANOVA_P_VALUE";

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedPercentage = 0.0;

  private final FeatureListRow[] featureListRows;
  private final UserParameter userParameter;

  public AnovaTask(FeatureListRow[] featureListRows, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.featureListRows = featureListRows;
    this.userParameter = parameters.getParameter(AnovaParameters.selectionData).getValue();
  }

  public String getTaskDescription() {
    return "Calculating significance... ";
  }

  public double getFinishedPercentage() {
    return finishedPercentage;
  }

  public void run() {

    if (isCanceled()) {
      return;
    }

    String errorMsg = null;

    setStatus(TaskStatus.PROCESSING);
    logger.info("Started calculating significance values");

    try {
      calculateSignificance();

      setStatus(TaskStatus.FINISHED);
      logger.info("Calculating significance is completed");
    } catch (IllegalStateException e) {
      errorMsg = e.getMessage();
    } catch (Exception e) {
      errorMsg = "'Unknown Error' during significance calculation: " + e.getMessage();
    } catch (Throwable t) {
      setStatus(TaskStatus.ERROR);
      setErrorMessage(t.getMessage());
      logger.log(Level.SEVERE, "Significance calculation error", t);
    }

    if (errorMsg != null) {
      setErrorMessage(errorMsg);
      setStatus(TaskStatus.ERROR);
    }
  }

  private void calculateSignificance() throws IllegalStateException {

    if (featureListRows.length == 0) {
      return;
    }

    List<Set<RawDataFile>> groups = getGroups(userParameter);

    finishedPercentage = 0.0;
    final double finishedStep = 1.0 / featureListRows.length;

    for (FeatureListRow row : featureListRows) {

      if (isCanceled()) {
        break;
      }

      finishedPercentage += finishedStep;

      double[][] intensityGroups = new double[groups.size()][];
      for (int i = 0; i < groups.size(); ++i) {
        Set<RawDataFile> groupFiles = groups.get(i);
        intensityGroups[i] =
            row.getFeatures().stream().filter(feature -> groupFiles.contains(feature.getRawDataFile()))
                .mapToDouble(Feature::getHeight).toArray();
      }

      Double pValue = oneWayAnova(intensityGroups);

      // Save results
      FeatureInformation featureInformation = row.getFeatureInformation();
      if (featureInformation == null) {
        featureInformation = new SimpleFeatureInformation();
      }
      featureInformation.getAllProperties().put(P_VALUE_KEY,
          pValue == null ? EMPTY_STRING : pValue.toString());
      row.setFeatureInformation(featureInformation);
    }
  }

  private List<Set<RawDataFile>> getGroups(UserParameter factor) {

    MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

    // Find the parameter value of each data file
    Map<RawDataFile, Object> paramMap = new HashMap<>();
    for (FeatureListRow row : featureListRows) {
      for (RawDataFile file : row.getRawDataFiles()) {
        Object paramValue = project.getParameterValue(factor, file);
        if (paramValue != null) {
          paramMap.put(file, paramValue);
        }
      }
    }

    // Find unique parameter values
    Object[] paramValues = paramMap.values().stream().distinct().toArray();

    // Form groups of files for each parameter value
    List<Set<RawDataFile>> groups = new ArrayList<>(paramValues.length);
    for (Object paramValue : paramValues) {
      groups.add(paramMap.entrySet().stream().filter(e -> paramValue.equals(e.getValue()))
          .map(Entry::getKey).collect(Collectors.toSet()));
    }

    return groups;
  }

  @Nullable
  private Double oneWayAnova(@NotNull double[][] intensityGroups) {

    int numGroups = intensityGroups.length;
    long numIntensities = Arrays.stream(intensityGroups).flatMapToDouble(Arrays::stream).count();

    double[] groupMeans = Arrays.stream(intensityGroups)
        .mapToDouble(intensities -> Arrays.stream(intensities).average().orElse(0.0)).toArray();

    double overallMean =
        Arrays.stream(intensityGroups).flatMapToDouble(Arrays::stream).average().orElse(0.0);

    double sumOfSquaresOfError = IntStream.range(0, intensityGroups.length).mapToDouble(
        i -> Arrays.stream(intensityGroups[i]).map(x -> x - groupMeans[i]).map(x -> x * x).sum())
        .sum();

    double sumOfSquaresOfTreatment =
        (numGroups - 1) * Arrays.stream(groupMeans).map(x -> x - overallMean).map(x -> x * x).sum();

    long degreesOfFreedomOfTreatment = numGroups - 1;
    long degreesOfFreedomOfError = numIntensities - numGroups;

    if (degreesOfFreedomOfTreatment <= 0 || degreesOfFreedomOfError <= 0) {
      return null;
    }

    double meanSquareOfTreatment = sumOfSquaresOfTreatment / degreesOfFreedomOfTreatment;
    double meanSquareOfError = sumOfSquaresOfError / degreesOfFreedomOfError;

    if (meanSquareOfError == 0.0) {
      return null;
    }

    double anovaStatistics = meanSquareOfTreatment / meanSquareOfError;

    Double pValue = null;
    try {
      FDistribution distribution =
          new FDistribution(degreesOfFreedomOfTreatment, degreesOfFreedomOfError);
      pValue = 1.0 - distribution.cumulativeProbability(anovaStatistics);
    } catch (MathIllegalArgumentException ex) {
      logger.warning("Error during F-distribution calculation: " + ex.getMessage());
    }

    return pValue;
  }
}
