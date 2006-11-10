package net.sf.mzmine.batchmode;

import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;

public interface BatchModeController extends MZmineModule {

	public enum BatchModeStep { RAWDATAFILTERING, PEAKPICKING, PEAKLISTPROCESSING, ALIGNMENT, ALIGNMENTPROCESSING }; 
	
	public void registerMethod(BatchModeStep batchModeStep, Method method);
	
}
