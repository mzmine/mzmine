package net.sf.mzmine.project.test.comparator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import net.sf.mzmine.data.Scan;
import net.sf.mzmine.project.test.OmitFieldRegistory;

public class RawDataFileComparator implements Comparator {
	Logger logger = Logger.getLogger(this.getClass().getName());
	FactoryComparator factoryComparator;

	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, HashMap<Object, ArrayList<Object>> doneList)
			throws Exception {

		Object oldValue;
		Object newValue;
		factoryComparator = new FactoryComparator();

		String[] fieldNames = { "fileName", "dataMinMZ", "dataMaxMZ",
				"dataMinRT", "dataMaxRT", "dataMaxBasePeakIntensity",
				"dataMaxTIC", "scanDataFileName", "writingScanDataFileName",
				"scans", "writingScans", "preloadLevel" };

		Field oldField;
		boolean ok;
		for (String fieldName : fieldNames) {
			oldField = oldObj.getClass().getDeclaredField(fieldName);
			if (ofRegist.registered(oldObj.getClass(), oldField.getName())) {
				continue;
			}
			logger.info("Comparing : Class " + oldObj.getClass().getName()
					+ " :Field " + oldField.getName());

			oldField.setAccessible(true);
			oldValue = oldField.get(oldObj);
			newValue = oldField.get(newObj);
			if (fieldName.equals("scans") || fieldName.equals("writingScans")) {
				if (oldValue != null) {
					ok = this.compareScans(oldValue, newValue, ofRegist,
							doneList);
				} else {
					ok = factoryComparator.compare(oldValue, newValue,
							ofRegist, doneList);
				}
			} else {
				ok = factoryComparator.compare(oldValue, newValue, ofRegist,
						doneList);
			}
			if (ok == false) {
				logger.info("Not consistent : Class "
						+ oldObj.getClass().getName() + " :Field "
						+ oldField.getName() + "\nold value: " + oldValue
						+ "\nnew value " + newValue);
				return false;
			}
		}
		return true;
	}

	private boolean compareScans(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, HashMap<Object,ArrayList<Object>> doneList)
			throws Exception {

		Hashtable<Integer, Scan> oldHash = (Hashtable<Integer, Scan>) oldObj;
		Hashtable<Integer, Scan> newHash = (Hashtable<Integer, Scan>) newObj;
		if (oldHash.size() != newHash.size()) {
			return false;
		} else {
			List<Integer> keys = new ArrayList<Integer>(oldHash.keySet());

			ArrayList<Integer> subKeyList = new ArrayList<Integer>(0);
			int size = Math.min(10, keys.size());
			for (int m = 0; m < size; m++) {
				subKeyList.add(keys.get(m));
			}
			Integer key;
			for (int i = 0; i < subKeyList.size(); i++) {
				key = subKeyList.get(i);
				logger.info(String.format("Checking %1$d of %2$d scans", i + 1,
						subKeyList.size()));
				if (newHash.containsKey(key) == false) {
					return false;
				}
				Object oldFieldValue = oldHash.get(key);
				Object newFieldValue = newHash.get(key);

				boolean ok = factoryComparator.compare(oldFieldValue,
						newFieldValue, ofRegist, doneList);
				if (ok== false) {
					return false;
				}
			}
		}
		return true;
	}
}
