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

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.BooleanParameter;
import io.github.mzmine.parameters.parametertypes.DoubleParameter;
import io.github.mzmine.parameters.parametertypes.StringParameter;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesParameter;


public class RTtoTemperatureParameters extends SimpleParameterSet {

    /**
     * Which raw data files to process
     */
    public static final RawDataFilesParameter dataFiles =
            new RawDataFilesParameter();
    /**
     * Suffix for new files
     */
    public static final StringParameter suffix =
            new StringParameter("Suffix",
                    "This string is appended to the filename of the converted file",
                    "to temperature");
    /**
     * Initial hold time before ramp (minutes)
     */
    public static final DoubleParameter firstholdtime =
            new DoubleParameter("Initial Hold Time (min)",
                    "Time (min) to hold at the initial temperature before starting ramp",
                    MZmineCore.getConfiguration().getRTFormat(),
                    0.5);
    /**
     * Time at which ramp finishes / final hold begins (minutes)
     */
    public static final DoubleParameter finaltime =
            new DoubleParameter("Final Hold Time (min)",
                    "Time (min) at which ramp ends and final hold begins",
                    MZmineCore.getConfiguration().getRTFormat(),
                    55.5);
    /**
     * Temperature during initial hold (°C)
     */
    public static final DoubleParameter initialtemp =
            new DoubleParameter("Initial Temperature (°C)",
                    "Oven temperature during the initial hold",
                    MZmineCore.getConfiguration().getIntensityFormat(),
                    50.0);
    /**
     * Final temperature after ramp (°C)
     */
    public static final DoubleParameter finaltemp =
            new DoubleParameter("Final Temperature (°C)",
                    "Oven temperature after ramp completes",
                    MZmineCore.getConfiguration().getIntensityFormat(),
                    600.0);
    /**
     * Ramp rate (°C per minute)
     */
    public static final DoubleParameter tempramp =
            new DoubleParameter("Temperature Ramp (°C/min)",
                    "Rate at which oven warms during the ramp",
                    MZmineCore.getConfiguration().getIntensityFormat(),
                    10.0);
    public static final BooleanParameter displayKelvin =
            new BooleanParameter(
                    "Display temperature in Kelvin",
                    "If checked, temperatures will be computed and displayed in Kelvin (K) instead of Celsius (°C).",
                    false);

    public RTtoTemperatureParameters() {
        super(dataFiles,
                suffix,
                firstholdtime,
                finaltime,
                initialtemp,
                finaltemp,
                tempramp,
                displayKelvin);
    }


    public enum AxisUnit {
        TIME("Retention time (min)"),
        CELSIUS("Temperature (°C)"),
        KELVIN("Temperature (K)");

        private final String label;

        AxisUnit(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}


