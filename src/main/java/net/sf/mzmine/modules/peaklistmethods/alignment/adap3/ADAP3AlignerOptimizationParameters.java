/* Copyright 2006-2019 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package net.sf.mzmine.modules.peaklistmethods.alignment.adap3;

import java.text.NumberFormat;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

/**
 * @author aleksandrsmirnov
 */
public class ADAP3AlignerOptimizationParameters extends SimpleParameterSet {

    static NumberFormat numberFormat = NumberFormat.getInstance();

    public static final DoubleParameter GRADIENT_TOLERANCE =
            new DoubleParameter("Minimum gradient norm increment",
                    "Stop optimization if the gradient norm increment is below this parameter",
                    numberFormat, 1e-4);
    
    /*public static final DoubleParameter COST_TOLERANCE =
            new DoubleParameter("Minimum cost-function increment",
                    "Stop optimization if the cost-function increment is below this parameter",
                    numberFormat, 1e-6);*/

    public static final DoubleParameter ALPHA =
            new DoubleParameter("Alpha-parameter",
                    "Alpha determines spe-size on each increment",
                    numberFormat, 1e-4);

    public static final IntegerParameter MAX_ITERATION =
            new IntegerParameter("Maxmum number of iterations",
                    "Stop optimization if the number of iterations exceeds this parameter",
                    500);

    public static final BooleanParameter VERBOSE =
            new BooleanParameter("Verbose Mode",
                    "If selected, optimization results will be printed out", false);

    public ADAP3AlignerOptimizationParameters() {
        super(new Parameter[]{GRADIENT_TOLERANCE, ALPHA, MAX_ITERATION,
                VERBOSE});

        numberFormat.setMaximumFractionDigits(24);
    }

}
