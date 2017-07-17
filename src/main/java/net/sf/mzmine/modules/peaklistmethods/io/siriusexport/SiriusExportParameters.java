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

import java.awt.Window;

import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.ComboParameter;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;


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
    	super(new Parameter[] {PEAK_LISTS, FILENAME, ROUND_MODE, MASS_LIST});
    }
    
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) { 
    	String message = "<html>SIRIUS Module Disclaimer:" +
        		"<br>    - If you use the SIRIUS export module, cite MZmine2 paper and the following articles: Dührkop et al.," +
        		"<br>     Proc Natl Acad Sci USA 112(41):12580-12585 and Boecker et al., Journal of Cheminformatics (2016) 8:5" + 
        		"<br>    - Sirius can be downloaded at the following address: <a href\"\">https://bio.informatik.uni-jena.de/software/sirius/</a>" +
        		"<br>    - Sirius results can be mapped into GNPS molecular networks. See the documentation: "+
        		"<br>     <a href=\"\">https://bix-lab.ucsd.edu/display/Public/GNPS+data+analysis+workflow+2.0</a>.</html>";
    	ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this, message);
    	dialog.setVisible(true);
    	return dialog.getExitCode();    
    }
}
