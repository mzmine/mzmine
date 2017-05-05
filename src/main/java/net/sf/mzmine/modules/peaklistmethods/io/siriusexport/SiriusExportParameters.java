/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang 
 * at the Dorrestein Lab (University of California, San Diego). 
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.siriusexport;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
//import net.sf.mzmine.parameters.parametertypes.BooleanParameter;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;

public class SiriusExportParameters extends SimpleParameterSet 
{
    public static final String ROUND_MODE_MAX = "Maximum";
    public static final String ROUND_MODE_SUM = "Sum";
  
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final FileNameParameter FILENAME = new FileNameParameter(
	    "Filename",
	    "Name of the output MGF file. " +
	    "Use pattern \"{}\" in the file name to substitute with peak list name. " +
	    "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). " +
	    "If the file already exists, it will be overwritten.",
	    "mgf");
    
//    public static final BooleanParameter FRACTIONAL_MZ = new BooleanParameter(
//            "Fractional m/z values", "If checked, write fractional m/z values", 
//            true);
   
    public static final ComboParameter <String> ROUND_MODE = 
            new ComboParameter <> (
                    "Merging Mode", 
                    "Determines how to merge intensities with the same m/z values",
                    new String[] {ROUND_MODE_MAX, ROUND_MODE_SUM},
                    ROUND_MODE_MAX);
    
    public static final MassListParameter MASS_LIST = new MassListParameter();
    
    public SiriusExportParameters() {
	super(new Parameter[] {PEAK_LISTS, FILENAME, /*FRACTIONAL_MZ,*/ ROUND_MODE, MASS_LIST});
    }
}
