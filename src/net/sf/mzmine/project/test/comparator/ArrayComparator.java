package net.sf.mzmine.project.test.comparator;

import java.lang.reflect.Array;
import java.util.ArrayList;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class ArrayComparator implements Comparator {
	public boolean compare(Object oldArray, Object newArray,
			OmitFieldRegistory ofRegist,ArrayList<Object[]> doneList) throws Exception {
		if (Array.getLength(oldArray) != Array.getLength(newArray)) {
			return false;
		}
		boolean ok;
		Comparator factoryComparator = new FactoryComparator();
		for (int i = 0; i < Array.getLength(oldArray); i++) {
			Object oldValue = Array.get(oldArray, i);
			Object newValue = Array.get(newArray, i);
			ok = factoryComparator.compare(oldValue, newValue, ofRegist,doneList);
			if (ok == false) {
				return false;
			}
		}
		return true;
	}
}
