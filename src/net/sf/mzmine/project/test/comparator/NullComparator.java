package net.sf.mzmine.project.test.comparator;

import java.util.ArrayList;
import java.util.HashMap;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class NullComparator implements Comparator {
	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, HashMap<Object, ArrayList<Object>> doneList) {
		if (newObj.getClass()!=oldObj.getClass()) {
			return false;
		}
		return true;
	}
}
