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

package net.sf.mzmine.modules.peaklistmethods.io.gnpsexport;

import java.awt.Window;

import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.parameters.dialogs.ParameterSetupDialog;
import net.sf.mzmine.parameters.impl.SimpleParameterSet;
import net.sf.mzmine.parameters.parametertypes.MassListParameter;
import net.sf.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import net.sf.mzmine.parameters.parametertypes.selectors.PeakListsParameter;
import net.sf.mzmine.util.ExitCode;


public class GNPSExportParameters extends SimpleParameterSet {
    
    public static final PeakListsParameter PEAK_LISTS = new PeakListsParameter();
    
    public static final FileNameParameter FILENAME = new FileNameParameter(
	    "Filename",
	    "Name of the output MGF file. " +
	    "Use pattern \"{}\" in the file name to substitute with peak list name. " +
	    "(i.e. \"blah{}blah.mgf\" would become \"blahSourcePeakListNameblah.mgf\"). " +
	    "If the file already exists, it will be overwritten.",
	    "mgf");
    
    public static final MassListParameter MASS_LIST = new MassListParameter();
    
    public GNPSExportParameters() {
    	super(new Parameter[] {PEAK_LISTS, FILENAME, MASS_LIST});
    }
    
    public ExitCode showSetupDialog(Window parent, boolean valueCheckRequired) { 
    	String message = "<html>GNPS Module Disclaimer:" + 
        		"<br>    - If you use the GNPS export module for GNPS web-platform, cite MZmine2 paper and the following article:"+
        		"<br>     Wang et al., Nature Biotechnology 34.8 (2016): 828-837." +
        		"<br>    - See the documentation about MZmine2 data pre-processing for GNPS (<a href=\"\">http://gnps.ucsd.edu/</a>) molecular " +
        		"<br>     networking and MS/MS spectral library search. <a href=\"\">https://bix-lab.ucsd.edu/display/Public/GNPS+data+analysis+workflow+2.0</a></html>";
    	ParameterSetupDialog dialog = new ParameterSetupDialog(parent, valueCheckRequired, this, message);
    	dialog.setVisible(true);
    	return dialog.getExitCode();    	
    }
}