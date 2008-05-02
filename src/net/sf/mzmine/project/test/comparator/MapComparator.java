package net.sf.mzmine.project.test.comparator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class MapComparator implements Comparator {
	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, HashMap<Object, ArrayList<Object>> doneList)
			throws Exception {
		Map oldMap = (Map) oldObj;
		Map newMap = (Map) newObj;

		if (newMap.size() != newMap.size()) {
			return false;
		}

		Comparator factoryComparator = new FactoryComparator();
		Iterator keys = newMap.keySet().iterator();
		Object key;
		Object oldValue;
		Object newValue;
		boolean ok;
		while (keys.hasNext()) {
			key = keys.next();
			ok = (newMap.containsKey(key));
			if (ok == false) {
				return false;
			}

			oldValue = oldMap.get(key);
			newValue = newMap.get(key);
			ok = factoryComparator.compare(oldValue, newValue, ofRegist,
					doneList);
			if (ok == false) {
				return false;
			}
		}
		return true;
	}
}
