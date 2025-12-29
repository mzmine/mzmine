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
package io.github.mzmine.modules.dataprocessing.transform_rttotemperature;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

public class RTtoTemperatureTask extends AbstractTask {

    private static final Logger logger =
            Logger.getLogger(RTtoTemperatureTask.class.getName());

    private final RawDataFile dataFile;
    private final ParameterSet parameters;

    // progress tracking
    private int processedScans = 0;
    private int totalScans = 0;

    public RTtoTemperatureTask(@NotNull MZmineProject project,
                               @NotNull RawDataFile dataFile,
                               @NotNull ParameterSet parameters,
                               @Nullable MemoryMapStorage storage,
                               @NotNull Instant moduleCallDate) {
        super(storage, moduleCallDate);
        this.dataFile = dataFile;
        this.parameters = parameters;
    }

    @Override
    public void run() {
        setStatus(TaskStatus.PROCESSING);
        logger.info("Starting in-place RT→Temperature conversion for " + dataFile.getName());

        // 1) Read oven parameters
        double Teq = parameters.getValue(RTtoTemperatureParameters.firstholdtime);
        double FinalHoldTime = parameters.getValue(RTtoTemperatureParameters.finaltime);
        double T0 = parameters.getValue(RTtoTemperatureParameters.initialtemp);
        double FinalTemp = parameters.getValue(RTtoTemperatureParameters.finaltemp);
        double rate = parameters.getValue(RTtoTemperatureParameters.tempramp);

        // 2) Read the Celsius vs. Kelvin flag
        boolean useKelvin = parameters
                .getParameter(RTtoTemperatureParameters.displayKelvin)
                .getValue();

        // 3) Fetch scans & precompute temperatures
        List<Scan> scans = dataFile.getScanNumbers(1);
        totalScans = scans.size();
        double[] temps = new double[totalScans];

        for (int i = 0; i < totalScans; i++) {
            Scan s = scans.get(i);
            if (s == null) {
                temps[i] = Double.NaN;
            } else {
                double rt = s.getRetentionTime();
                temps[i] = computeTemperature(
                        rt, Teq, T0, rate, FinalHoldTime, FinalTemp, useKelvin);
            }
        }

        // 4) Overwrite each scan’s RT with its computed temperature
        processedScans = 0;
        for (int i = 0; i < totalScans; i++) {
            if (isCanceled()) {
                setStatus(TaskStatus.CANCELED);
                logger.info("Conversion canceled after "
                        + processedScans + " of " + totalScans + " scans");
                return;
            }
            Scan s = scans.get(i);
            if (s instanceof SimpleScan) {
                ((SimpleScan) s).setRetentionTime((float) temps[i]);
            }
            processedScans++;
        }

        // 5) Finish
        setStatus(TaskStatus.FINISHED);
        logger.info("Finished in-place RT→Temperature conversion for " + dataFile.getName());
    }

    @Override
    public double getFinishedPercentage() {
        return totalScans == 0
                ? 0.0
                : (double) processedScans / totalScans;
    }

    @Override
    public @NotNull String getTaskDescription() {
        return "Overwrites each scan’s retention time with the calculated oven temperature.";
    }

    /**
     * Map t (minutes) → oven temperature (°C), then optionally convert to Kelvin.
     *
     * @param t                retention time in minutes
     * @param Teq              initial hold time (minutes)
     * @param T0               initial temperature (°C)
     * @param rate             ramp rate (°C per minute)
     * @param FinalHoldTime    end of ramp time (minutes)
     * @param FinalTemperature temperature after final hold (°C)
     * @param useKelvin        if true, converts °C → K by adding 273.15
     * @return computed oven temperature
     */
    private double computeTemperature(double t,
                                      double Teq,
                                      double T0,
                                      double rate,
                                      double FinalHoldTime,
                                      double FinalTemperature,
                                      boolean useKelvin) {
        // first compute in °C
        double tempC;
        if (t < Teq) {
            tempC = T0;
        } else if (t > FinalHoldTime) {
            tempC = FinalTemperature;
        } else {
            tempC = (t - Teq) * rate + T0;
        }
        // then convert if requested
        return useKelvin ? tempC + 273.15 : tempC;
    }
}

