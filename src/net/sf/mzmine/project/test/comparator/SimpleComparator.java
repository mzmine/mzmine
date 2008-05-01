package net.sf.mzmine.project.test.comparator;

import java.util.ArrayList;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class SimpleComparator implements Comparator {
	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, ArrayList<Object[]> doneList) {

		return oldObj.equals(newObj);
	}
}
