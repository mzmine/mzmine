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
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3TICWindowDetectionParameters extends SimpleParameterSet {
    
    public static final IntegerParameter PEAK_SPAN =
            new IntegerParameter("Peak Span", "Peak Detection Window Size", 11);
    
    public static final IntegerParameter VALLEY_SPAN =
            new IntegerParameter("Valley Span", 
                    "Boundary Detection Window Size", 9);
    
    public static final DoubleParameter EDGE_TO_HEIGHT_RATIO =
            new DoubleParameter("Minimum Edge-to-Height Ratio", 
                    "A peak is considered shared if its edge-to-height ratio is below this parameter",
                    NumberFormat.getInstance(), 0.4, 0.0, 1.0);
    
    public static final DoubleParameter DELTA_TO_HEIGHT_RATIO =
            new DoubleParameter("Minimum Delta-to-Height Ratio",
                    "A peak is considered shared if its delta (difference between the edges) -to-height ratio is below this parameter",
                    NumberFormat.getInstance(), 0.2, 0.0, 1.0);
    
    public static final IntegerParameter MAX_WINDOW_SIZE =
            new IntegerParameter("Maximum Window Size (# of scans)", 
                    "Peaks with the combined length exceeding this parameter, cannot be combined", 
                    450);
    
    public static final IntegerParameter MAX_PEAK_WIDTH =
            new IntegerParameter("Maximum Peak Width (# of scans)",
                    "Only peaks with the width below this parameter, will be analyzed", 200);
    
    public static final DoubleParameter NOISE_WINDOW_SIZE =
            new DoubleParameter("Noise Window Size (fraction of total chromatogram length)",
                    "Window of this size is used for noise estimation", 
                    NumberFormat.getInstance(), 0.1, 0.0, 1.0);
    
    public static final DoubleParameter SIGNAL_TO_NOISE_THRESHOLD =
            new DoubleParameter("Signal-to-Noise Threshold", "",
                    NumberFormat.getInstance(), 0.0);
    
    public ADAP3TICWindowDetectionParameters() {
        super(new Parameter[] {PEAK_SPAN, VALLEY_SPAN, EDGE_TO_HEIGHT_RATIO,
                DELTA_TO_HEIGHT_RATIO, MAX_WINDOW_SIZE, MAX_PEAK_WIDTH,
                NOISE_WINDOW_SIZE, SIGNAL_TO_NOISE_THRESHOLD});
    }
}
