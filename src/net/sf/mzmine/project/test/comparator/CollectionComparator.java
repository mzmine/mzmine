package net.sf.mzmine.project.test.comparator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class CollectionComparator implements Comparator {
	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist,
			HashMap<Object, ArrayList<Object>> doneList) throws Exception {
		Collection oldCol = (Collection) oldObj;
		Collection newCol = (Collection) newObj;
		if (oldCol.size() != newCol.size()) {
			return false;
		}
		Iterator oldIt = oldCol.iterator();
		Iterator newIt = newCol.iterator();
		boolean ok;
		Comparator factoryComparator = new FactoryComparator();
		while (oldIt.hasNext()) {
			Object oldValue = oldIt.next();
			Object newValue = newIt.next();
			ok = factoryComparator.compare(oldValue, newValue, ofRegist,
					doneList);
			if (ok == false) {
				return false;
			}
		}
		return true;
	}
}
