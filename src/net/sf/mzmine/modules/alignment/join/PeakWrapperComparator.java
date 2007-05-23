package net.sf.mzmine.modules.alignment.join;

import java.util.Comparator;

public class PeakWrapperComparator implements Comparator<PeakWrapper> {

	public int compare(PeakWrapper peakWrapper1, PeakWrapper peakWrapper2) {
		return (int)java.lang.Math.signum(peakWrapper1.getPeak().getMZ()-peakWrapper2.getPeak().getMZ());		
	}

}
