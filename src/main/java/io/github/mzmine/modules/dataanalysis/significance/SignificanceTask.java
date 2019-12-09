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

package io.github.mzmine.modules.dataanalysis.significance;

import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import java.util.stream.IntStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.math3.distribution.FDistribution;
import org.apache.commons.math3.exception.MathIllegalArgumentException;

import io.github.mzmine.datamodel.*;
import io.github.mzmine.datamodel.impl.SimplePeakInformation;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;

public class SignificanceTask extends AbstractTask {

    private static final String EMPTY_STRING = "";

    private static final String P_VALUE_KEY = "ANOVA_P_VALUE";

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private double finishedPercentage = 0.0;

    private final PeakListRow[] peakListRows;
    private final UserParameter userParameter;

    public SignificanceTask(PeakListRow[] peakListRows,
            ParameterSet parameters) {
        this.peakListRows = peakListRows;
        this.userParameter = parameters
                .getParameter(SignificanceParameters.selectionData).getValue();
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
            errorMsg = "'Unknown Error' during significance calculation: "
                    + e.getMessage();
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
                        .mapToDouble(Feature::getHeight).toArray();
            }

            Double pValue = oneWayAnova(intensityGroups);

            // Save results
            PeakInformation peakInformation = row.getPeakInformation();
            if (peakInformation == null) {
                peakInformation = new SimplePeakInformation();
            }
            peakInformation.getAllProperties().put(P_VALUE_KEY,
                    pValue == null ? EMPTY_STRING : pValue.toString());
            row.setPeakInformation(peakInformation);
        }
    }

    private List<Set<RawDataFile>> getGroups(UserParameter factor) {

        MZmineProject project = MZmineCore.getProjectManager()
                .getCurrentProject();

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
        Object[] paramValues = paramMap.values().stream().distinct().toArray();

        // Form groups of files for each parameter value
        List<Set<RawDataFile>> groups = new ArrayList<>(paramValues.length);
        for (Object paramValue : paramValues) {
            groups.add(paramMap.entrySet().stream()
                    .filter(e -> paramValue.equals(e.getValue()))
                    .map(Entry::getKey).collect(Collectors.toSet()));
        }

        return groups;
    }

    @Nullable
    private Double oneWayAnova(@Nonnull double[][] intensityGroups) {

        int numGroups = intensityGroups.length;
        long numIntensities = Arrays.stream(intensityGroups)
                .flatMapToDouble(Arrays::stream).count();

        double[] groupMeans = Arrays.stream(intensityGroups).mapToDouble(
                intensities -> Arrays.stream(intensities).average().orElse(0.0))
                .toArray();

        double overallMean = Arrays.stream(intensityGroups)
                .flatMapToDouble(Arrays::stream).average().orElse(0.0);

        double sumOfSquaresOfError = IntStream.range(0, intensityGroups.length)
                .mapToDouble(i -> Arrays.stream(intensityGroups[i])
                        .map(x -> x - groupMeans[i]).map(x -> x * x).sum())
                .sum();

        double sumOfSquaresOfTreatment = (numGroups - 1)
                * Arrays.stream(groupMeans).map(x -> x - overallMean)
                        .map(x -> x * x).sum();

        long degreesOfFreedomOfTreatment = numGroups - 1;
        long degreesOfFreedomOfError = numIntensities - numGroups;

        if (degreesOfFreedomOfTreatment <= 0 || degreesOfFreedomOfError <= 0) {
            return null;
        }

        double meanSquareOfTreatment = sumOfSquaresOfTreatment
                / degreesOfFreedomOfTreatment;
        double meanSquareOfError = sumOfSquaresOfError
                / degreesOfFreedomOfError;

        if (meanSquareOfError == 0.0) {
            return null;
        }

        double anovaStatistics = meanSquareOfTreatment / meanSquareOfError;

        Double pValue = null;
        try {
            FDistribution distribution = new FDistribution(
                    degreesOfFreedomOfTreatment, degreesOfFreedomOfError);
            pValue = 1.0 - distribution.cumulativeProbability(anovaStatistics);
        } catch (MathIllegalArgumentException ex) {
            logger.warning("Error during F-distribution calculation: "
                    + ex.getMessage());
        }

        return pValue;
    }
}
