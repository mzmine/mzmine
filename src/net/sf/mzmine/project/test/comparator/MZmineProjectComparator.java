package net.sf.mzmine.project.test.comparator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.swing.DefaultListModel;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class MZmineProjectComparator implements Comparator {
	Logger logger = Logger.getLogger(this.getClass().getName());

	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist, HashMap<Object, ArrayList<Object>> doneList)
			throws Exception {
		Field[] fields;
		Object oldValue;
		Object newValue;
		FactoryComparator factoryComparator = new FactoryComparator();
		boolean ok;

		String[] fieldNames = { "projectParametersAndValues", "rawDataList",
				"peakListsList", "isTemporal" };

		fields = oldObj.getClass().getDeclaredFields();
		Field oldField;
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
			boolean okField = true;
			if (fieldName == "rawDataList" || fieldName == "peakListsList") {

				DefaultListModel oldModel = (DefaultListModel) oldValue;
				DefaultListModel newModel = (DefaultListModel) newValue;
				okField = factoryComparator.compare(oldModel.toArray(),
						newModel.toArray(), ofRegist, doneList);

				ok = okField;
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
}
