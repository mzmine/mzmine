package net.sf.mzmine.batchmode;

import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.methods.Method;
import net.sf.mzmine.methods.MethodListener;

public interface BatchModeController extends MZmineModule, MethodListener {

	public enum BatchModeStep { RAWDATAFILTERING, PEAKPICKING, PEAKLISTPROCESSING, ALIGNMENT, ALIGNMENTPROCESSING };
	
	public void registerMethod(BatchModeStep batchModeStep, Method method);

}
