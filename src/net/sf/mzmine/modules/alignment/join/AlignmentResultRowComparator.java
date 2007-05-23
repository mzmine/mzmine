package net.sf.mzmine.modules.alignment.join;

import java.util.Comparator;

import net.sf.mzmine.data.AlignmentResultRow;

public class AlignmentResultRowComparator implements Comparator<AlignmentResultRow> {

	public int compare(AlignmentResultRow row1, AlignmentResultRow row2) {
		return (int)java.lang.Math.signum(row1.getAverageMZ()-row2.getAverageMZ());	
	}

}
