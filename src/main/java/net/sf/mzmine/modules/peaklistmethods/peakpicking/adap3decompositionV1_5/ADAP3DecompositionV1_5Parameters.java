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
package net.sf.mzmine.modules.peaklistmethods.peakpicking.adap3decompositionV1_5;

import dulab.adap.workflow.TwoStepDecompositionParameters;
import java.awt.Window;

import java.text.NumberFormat;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.DoubleParameter;
import net.sf.mzmine.parameters.parametertypes.IntegerParameter;
import net.sf.mzmine.parameters.parametertypes.StringParameter;
import net.sf.mzmine.parameters.parametertypes.ranges.ListDoubleRangeParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;

/**
 *
 * @author aleksandrsmirnov
 */
public class ADAP3DecompositionV1_5Parameters extends SimpleParameterSet {
    
    
    
    public static final PeakListsParameter PEAK_LISTS = 
            new PeakListsParameter();
    
    // ------------------------------------------------------------------------
    // ----- First-phase parameters -------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final DoubleParameter MIN_CLUSTER_DISTANCE =
            new DoubleParameter("Min cluster distance (min)",
                    "Minimum distance between any two clusters",
                    NumberFormat.getNumberInstance(), 0.01
            );
    
    public static final IntegerParameter MIN_CLUSTER_SIZE =
            new IntegerParameter("Min cluster size",
                    "Minimum size of a cluster",
                    2
            );
    
    public static final DoubleParameter MIN_CLUSTER_INTENSITY =
            new DoubleParameter(
                    "Min cluster intensity",
                    "If the highest peak in a cluster has the intensity below Minimum Cluster Intensity, the cluster is removed",
                    NumberFormat.getNumberInstance(), 500.0
            );
    
    // ------------------------------------------------------------------------
    // ----- End of First-phase parameters ------------------------------------
    // ------------------------------------------------------------------------
    
    // ------------------------------------------------------------------------
    // ----- Second-phase parameters ------------------------------------------
    // ------------------------------------------------------------------------
    
    public static final DoubleParameter EDGE_TO_HEIGHT_RATIO =
            new DoubleParameter("Min edge-to-height ratio", 
                    "A peak is considered shared if its edge-to-height ratio is below this parameter",
                    NumberFormat.getInstance(), 0.3, 0.0, 1.0);
    
    public static final DoubleParameter DELTA_TO_HEIGHT_RATIO =
            new DoubleParameter("Min delta-to-height ratio",
                    "A peak is considered shared if its delta (difference between the edges)-to-height ratio is below this parameter",
                    NumberFormat.getInstance(), 0.2, 0.0, 1.0);
    
    public static final BooleanParameter USE_ISSHARED =
            new BooleanParameter("Find shared peaks",
                    "If selected, peaks are marked as Shared if they are composed of two or more peaks", false);
    
    public static final DoubleParameter MIN_MODEL_SHARPNESS =
            new DoubleParameter("Min sharpness",
                    "Minimum sharpness that the model peak can have",
                    NumberFormat.getNumberInstance(), 10.0);
    
    public static final DoubleParameter SHAPE_SIM_THRESHOLD =
            new DoubleParameter("Shape-similarity tolerance (0..90)",
                    "Shape-similarity threshold is used to find similar peaks",
                    NumberFormat.getNumberInstance(), 18.0);
    
    public static final ComboParameter <String> MODEL_PEAK_CHOICE = 
            new ComboParameter <> ("Choice of Model Peak based on", 
                    "Criterion to choose a model peak in a cluster: either peak with the highest m/z-value or with the highest sharpness",
                    new String[] {
                        TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_MZ, 
                        TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_SHARPNESS
                    },
                    TwoStepDecompositionParameters.MODEL_PEAK_CHOICE_MZ);
    
    public static final ListDoubleRangeParameter MZ_VALUES =
            new ListDoubleRangeParameter("Exclude m/z-values", 
                    "M/z-values to exclude while selecting model peak", 
                    false, null);
    
    // ------------------------------------------------------------------------
    // ----- End of Second-phase parameters -----------------------------------
    // ------------------------------------------------------------------------
    
    public static final StringParameter SUFFIX = new StringParameter("Suffix",
	    "This string is added to peak list name as suffix", "ADAP-GC 3 Peak Decomposition");
    
    public static final BooleanParameter AUTO_REMOVE = new BooleanParameter(
	    "Remove original peak list",
	    "If checked, original chromatogram will be removed and only the deconvolved version remains");
    
    public ADAP3DecompositionV1_5Parameters() {
	super(new Parameter[] {PEAK_LISTS, MIN_CLUSTER_DISTANCE, 
            MIN_CLUSTER_SIZE, MIN_CLUSTER_INTENSITY, 
            USE_ISSHARED, EDGE_TO_HEIGHT_RATIO, DELTA_TO_HEIGHT_RATIO,
            MIN_MODEL_SHARPNESS, SHAPE_SIM_THRESHOLD, MODEL_PEAK_CHOICE, 
            MZ_VALUES, SUFFIX, AUTO_REMOVE});
    }
    
    @Override
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired)
    {
        final ADAP3DecompositionV1_5SetupDialog dialog = 
                new ADAP3DecompositionV1_5SetupDialog(
                        parent, valueCheckRequired, this);
        
        dialog.setVisible(true);
        return dialog.getExitCode();
    }
}
