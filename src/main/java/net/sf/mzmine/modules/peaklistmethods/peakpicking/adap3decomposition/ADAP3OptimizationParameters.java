/* 
 * Copyright (C) 2016 Du-Lab Team <dulab.binf@gmail.com>
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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decomposition;

import java.text.NumberFormat;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3OptimizationParameters extends SimpleParameterSet {
    
    static NumberFormat numberFormat = NumberFormat.getInstance();
    
    public static final DoubleParameter GRADIENT_TOLERANCE =
            new DoubleParameter("Minimum gradient norm increment",
                    "Stop optimization if the gradient norm increment is below this parameter",
                    numberFormat, 1e-5);
    
    public static final DoubleParameter COST_TOLERANCE =
            new DoubleParameter("Minimum cost-function increment",
                    "Stop optimization if the cost-function increment is below this parameter",
                    numberFormat, 1e-6);
    
    public static final DoubleParameter ALPHA =
            new DoubleParameter("Alpha-parameter",
                    "Alpha determines spe-size on each increment",
                    numberFormat, 10.0);
    
    public static final IntegerParameter MAX_ITERATION =
            new IntegerParameter("Maxmum number of iterations",
                    "Stop optimization if the number of iterations exceeds this parameter",
                    4000);
    
    public static final BooleanParameter VERBOSE =
            new BooleanParameter("Verbose Mode",
            "If selected, optimization results will be printed out", false);
    
    public ADAP3OptimizationParameters() {
        super(new Parameter[] {GRADIENT_TOLERANCE, COST_TOLERANCE, ALPHA,
                MAX_ITERATION, VERBOSE});
        
        numberFormat.setMaximumFractionDigits(24);
    }
}
