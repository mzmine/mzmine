package net.sf.mzmine.project.test.comparator;

import java.lang.reflect.Array;
import java.util.ArrayList;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public interface Comparator {
	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, ArrayList<Object[]> doneList)
			throws Exception;
}
