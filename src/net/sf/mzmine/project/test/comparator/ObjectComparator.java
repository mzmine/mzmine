package net.sf.mzmine.project.test.comparator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.logging.Logger;

import net.sf.mzmine.project.test.OmitFieldRegistory;

public class ObjectComparator implements Comparator {
	Logger logger =Logger.getLogger(this.getClass().getName());
	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist,ArrayList<Object[]> doneList) throws Exception {
		Field[] fields;
		Object oldValue;
		Object newValue;
		FactoryComparator factoryComparator = new FactoryComparator();
		boolean ok;
		
		fields = oldObj.getClass().getDeclaredFields();
		
		for (Field oldField : fields) {
			if (ofRegist.registered(oldObj.getClass(), oldField.getName())) {
				continue;
			}
			logger.info("Comparing : Class "+oldObj.getClass().getName()+" :Field "+oldField.getName());
			if (oldObj.getClass().getName().equals("java.util.logging.LogManager")){
				int i=0;
				i++;
			}
			oldField.setAccessible(true);
			oldValue = oldField.get(oldObj);
			newValue = oldField.get(newObj);
			ok = factoryComparator.compare(oldValue, newValue, ofRegist,doneList);
			if (ok==false){
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
