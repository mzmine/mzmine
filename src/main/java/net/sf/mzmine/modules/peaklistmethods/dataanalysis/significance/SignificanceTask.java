/*
 * Copyright (C) 2018 Du-Lab Team <dulab.binf@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.significance;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.util.stream.IntStream;
import net.sf.mzmine.datamodel.*;
import net.sf.mzmine.datamodel.impl.SimplePeakInformation;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.UserParameter;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.FDistribution;
import org.apache.commons.math.distribution.FDistributionImpl;
import smile.stat.hypothesis.TTest;

public class SignificanceTask extends AbstractTask {

  private static final double LOG_OF_2 = Math.log(2.0);

  private static final String FOLD_CHANGE_KEY = "LOG2_FOLD_CHANGE";
  private static final String P_VALUE_KEY = "STUDENT_P_VALUE";
  private static final String T_VALUE_KEY = "STUDENT_T_VALUE";

  private Logger logger = Logger.getLogger(this.getClass().getName());
  private double finishedPercentage = 0.0;

  private final PeakListRow[] peakListRows;
  private final UserParameter userParameter;
//    private String controlGroupName;
//    private String experimentalGroupName;

  public SignificanceTask(PeakListRow[] peakListRows, ParameterSet parameters) {
    this.peakListRows = peakListRows;
    this.userParameter = parameters.getParameter(SignificanceParameters.selectionData).getValue();
//        this.controlGroupName = parameters.getParameter(SignificanceParameters.controlGroupName).getValue();
//        this.experimentalGroupName = parameters.getParameter(SignificanceParameters.experimentalGroupName).getValue();
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

    if (peakListRows.length == 0) {
      return;
    }

    List<Set<RawDataFile>> groups = getGroups(userParameter);

    finishedPercentage = 0.0;
    final double finishedStep = 1.0 / peakListRows.length;

    for (PeakListRow row : peakListRows) {

      if (isCanceled()) {
        break;
      }

      finishedPercentage += finishedStep;

      double[][] intensityGroups = new double[groups.size()][];
      for (int i = 0; i < groups.size(); ++i) {
        Set<RawDataFile> groupFiles = groups.get(i);
        intensityGroups[i] = Arrays.stream(row.getPeaks())
            .filter(peak -> groupFiles.contains(peak.getDataFile()))
            .mapToDouble(Feature::getHeight)
            .toArray();
      }

      oneWayAnova(intensityGroups);

      // Calculating log2 fold change

//      double controlAverageIntensity = Arrays.stream(controlGroupIntensities)
//          .average()
//          .orElse(0.0);
//
//      double experimentalAverageIntensity = Arrays.stream(experimentalGroupIntensities)
//          .average()
//          .orElse(0.0);
//
//      double log2FoldChange = 0.0;
//      if (controlAverageIntensity > 0.0 && experimentalAverageIntensity > 0.0) {
//        log2FoldChange = log2(experimentalAverageIntensity / controlAverageIntensity);
//      } else if (experimentalAverageIntensity > 0.0) {
//        log2FoldChange = Double.POSITIVE_INFINITY;
//      } else if (controlAverageIntensity > 0.0) {
//        log2FoldChange = Double.NEGATIVE_INFINITY;
//      }
//
//      // Calculating Student's t-test
//
//      TTest tTest = null;
//      try {
//        tTest = TTest.test(controlGroupIntensities, experimentalGroupIntensities, false);
//      } catch (IllegalArgumentException e) {
//        logger.warning(e.getMessage());
//      }

      // Saving results

//      PeakInformation peakInformation = row.getPeakInformation();
//      if (peakInformation == null) {
//        peakInformation = new SimplePeakInformation();
//      }
//
//      if (tTest != null) {
//        peakInformation.getAllProperties().put(P_VALUE_KEY, Double.toString(tTest.pvalue));
//        peakInformation.getAllProperties().put(T_VALUE_KEY, Double.toString(tTest.t));
//      }
//
//      peakInformation.getAllProperties().put(FOLD_CHANGE_KEY, Double.toString(log2FoldChange));
//
//      row.setPeakInformation(peakInformation);
    }
  }

  private static double log2(double x) {
    return Math.log(x) / LOG_OF_2;
  }

//  private Set<RawDataFile> getGroup(String template) {
//
//    Set<RawDataFile> groupFiles = new HashSet<>();
//    for (PeakListRow row : peakListRows) {
//      for (RawDataFile file : row.getRawDataFiles()) {
//        if (file.getName().contains(template)) {
//          groupFiles.add(file);
//        }
//      }
//    }
//
//    return groupFiles;
//  }

  private List<Set<RawDataFile>> getGroups(UserParameter factor) {

    MZmineProject project = MZmineCore.getProjectManager().getCurrentProject();

    // Find the parameter value of each data file
    Map<RawDataFile, Object> paramMap = new HashMap<>();
    for (PeakListRow row : peakListRows) {
      for (RawDataFile file : row.getRawDataFiles()) {
        Object paramValue = project.getParameterValue(factor, file);
        if (paramValue != null) {
          paramMap.put(file, paramValue);
        }
      }
    }

    // Find unique parameter values
    Object[] paramValues = paramMap.values()
        .stream()
        .distinct()
        .toArray();

    // Form groups of files for each parameter value
    List<Set<RawDataFile>> groups = new ArrayList<>(paramValues.length);
    for (Object paramValue : paramValues) {
      groups.add(paramMap.entrySet()
          .stream()
          .filter(paramValue::equals)
          .map(Entry::getKey)
          .collect(Collectors.toSet()));
    }

    return groups;
  }

  private void oneWayAnova(double[][] intensityGroups) {

    int numGroups = intensityGroups.length;
    long numIntensities = Arrays.stream(intensityGroups)
        .flatMapToDouble(Arrays::stream)
        .count();

    double[] groupMeans = Arrays.stream(intensityGroups)
        .mapToDouble(intensities -> Arrays.stream(intensities).average().orElse(0.0))
        .toArray();

    double overallMean = Arrays.stream(intensityGroups)
        .flatMapToDouble(Arrays::stream)
        .average()
        .orElse(0.0);

    double sumOfSquaresOfError = IntStream.range(0, intensityGroups.length)
        .mapToDouble(i -> Arrays
            .stream(intensityGroups[i])
            .map(x -> x - groupMeans[i])
            .map(x -> x * x)
            .sum())
        .sum();

    double sumOfSquaresOfTreatment = (numGroups - 1) * Arrays.stream(groupMeans)
        .map(x -> x - overallMean)
        .map(x -> x * x)
        .sum();

    long degreesOfFreedomOfTreatment = numGroups - 1;
    long degreesOfFreedomOfError = numIntensities - numGroups;

    // TODO: if degrees == 0

    double meanSquareOfTreatment = sumOfSquaresOfTreatment / degreesOfFreedomOfTreatment;
    double meanSquareOfError = sumOfSquaresOfError / degreesOfFreedomOfError;

    // TODO: if MSE == 0

    double anovaStatistics = meanSquareOfTreatment / meanSquareOfError;

    FDistribution distribution = new FDistributionImpl(degreesOfFreedomOfTreatment, degreesOfFreedomOfError);
    double pValue;
    try {
      pValue = 1.0 - distribution.cumulativeProbability(anovaStatistics);
    }
    catch (MathException e) {
      pValue = 1.0;
    }


  }
}
