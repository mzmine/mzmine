package net.sf.mzmine.project.test.comparator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Logger;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.project.test.OmitFieldRegistory;
import net.sf.mzmine.util.Range;

public class StorableScanComparator implements Comparator {
	Logger logger = Logger.getLogger(this.getClass().getName());
	FactoryComparator factoryComparator;

	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist,
			HashMap<Object, ArrayList<Object>> doneList) throws Exception {

		Object oldValue;
		Object newValue;
		factoryComparator = new FactoryComparator();

		String[] fieldNames = { "scanNumber", "msLevel", "parentScan",
				"fragmentScans", "precursorMZ", "precursorCharge",
				"retentionTime", "mzRange", "basePeak", "totalIonCurrent",
				"centroided", "storageFileOffset", "storageArrayByteLength",
				"numberOfDataPoints", "rawDataFile" };

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
			if (fieldName.equals("rawDataFile")) {
				if (oldValue != null) {
					ok = this.compareRawDataFiles(oldValue, newValue, ofRegist,
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

	private boolean compareRawDataFiles(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist,
			HashMap<Object, ArrayList<Object>> doneList) throws Exception {

		RawDataFile oldFile=(RawDataFile)oldObj;
		RawDataFile newFile=(RawDataFile)newObj;
		
		if (!oldFile.getFileName().equals(newFile.getFileName())) {
			return false;
		}
		if (oldFile.getNumOfScans()!=newFile.getNumOfScans()){
			return false;
		}
		if (!oldFile.getPreloadLevel().equals(newFile.getPreloadLevel())){
			return false;
		}
		return true;
	}
}
