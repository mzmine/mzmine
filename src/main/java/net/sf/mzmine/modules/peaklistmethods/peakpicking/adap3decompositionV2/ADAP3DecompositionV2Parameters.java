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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV2;

import java.awt.Window;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;
import org.apache.commons.lang3.ArrayUtils;
import org.jfree.chart.ChartPanel;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV2Parameters extends SimpleParameterSet {
    
    
    
    public static final PeakListsParameter PEAK_LISTS = 
            new PeakListsParameter();
    
    // ------------------------------------------------------------------------
    // ----- First-phase parameters -------------------------------------------
    // ------------------------------------------------------------------------

//    public static final ParameterSetParameter PEAK_DETECTOR_PARAMETERS =
//            new ParameterSetParameter("Peak detector", "", new MsDialPeakDetectorParameters());
//
//
//    public static final DoubleParameter MIN_CLUSTER_DISTANCE =
//            new DoubleParameter("Min distance between analytes (min)",
//                    "Minimum distance between any two analytes",
//                    NumberFormat.getNumberInstance(), 0.01);
//
//    public static final IntegerParameter MIN_CLUSTER_SIZE =
//            new IntegerParameter("Min cluster size",
//                    "Minimum size of a cluster",
//                    5);
//
//    // ------------------------------------------------------------------------
//    // ----- End of First-phase parameters ------------------------------------
//    // ------------------------------------------------------------------------
//
//    // ------------------------------------------------------------------------
//    // ----- Second-phase parameters ------------------------------------------
//    // ------------------------------------------------------------------------
//
////    public static final DoubleParameter FWHM_TOLERANCE = new DoubleParameter("Full-Width at Half-Max tolerance",
////            "Model peaks found by the algorithm must have FWHM within certain range estimated from the real peaks plus/minus the specified tolerance",
////            NumberFormat.getNumberInstance(), 1.0, 0.0, 49.0);
//
//    public static final DoubleParameter PEAK_SIMILARITY = new DoubleParameter("Peak-Similarity tolerance",
//            "Each model peak must have a real peak similar to it with the specified tolerance",
//            NumberFormat.getNumberInstance(), 0.17, 0.0, 1.0);
//
//    // ------------------------------------------------------------------------
//    // ----- End of Second-phase parameters -----------------------------------
//    // ------------------------------------------------------------------------
//
//    public static final StringParameter SUFFIX = new StringParameter("Suffix",
//	    "This string is added to peak list name as suffix", "ADAP-GC Spectral Deconvolution");
//
//    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
//	    "Remove original peak list",
//	    "If checked, original chromatogram will be removed and only the deconvolved version remains");

    private static final PeakDetectionSupplier PEAK_DETECTION_SUPPLIER = new PeakDetectionSupplier();
    private static final WindowSelectionSupplier WINDOW_SELECTION_SUPPLIER = new WindowSelectionSupplier();
    private static final AlgorithmSupplier COMPONENT_CONSTRUCTION_SUPPLIER = new ComponentConstructionSupplier();

    static final AlgorithmSupplier[] SUPPLIERS = new AlgorithmSupplier[] {
            PEAK_DETECTION_SUPPLIER, WINDOW_SELECTION_SUPPLIER, COMPONENT_CONSTRUCTION_SUPPLIER};

    public ADAP3DecompositionV2Parameters() {
	    super(combineParameters());
    }

    private static Parameter[] combineParameters() {
        Parameter[] parameters = new Parameter[] {PEAK_LISTS};
        for (AlgorithmSupplier s : SUPPLIERS)
            parameters = ArrayUtils.addAll(parameters, s.getParameters());
        return parameters;
    }
    
    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired)
    {
        final ADAP3DecompositionV2SetupDialog dialog =
                new ADAP3DecompositionV2SetupDialog(
                        parent, valueCheckRequired, this);
        
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
