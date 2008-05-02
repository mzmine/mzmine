package net.sf.mzmine.project.test.comparator;

import java.beans.beancontext.BeanContextServicesSupport;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.main.RawDataFileImpl;
import net.sf.mzmine.main.StorableScan;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.MZmineProjectImpl;
import net.sf.mzmine.project.test.OmitFieldRegistory;

public class FactoryComparator implements Comparator {
	private HashMap<Class, Comparator> comparators;
	private Logger logger;

	public FactoryComparator() {
		logger = Logger.getLogger(this.getClass().getName());

		comparators = new HashMap<Class, Comparator>();

		comparators.put(Array.class, new ArrayComparator());
		comparators.put(NullType.class, new NullComparator());
		comparators.put(GenericType.class, new ObjectComparator());
		
		Comparator collectionComparator = new CollectionComparator();
		comparators.put(BeanContextServicesSupport.class, collectionComparator);
		comparators.put(HashSet.class, collectionComparator);
		comparators.put(LinkedHashSet.class, collectionComparator);
		comparators.put(ArrayList.class, collectionComparator);
		comparators.put(LinkedList.class, collectionComparator);
		comparators.put(TreeSet.class, collectionComparator);
		comparators.put(Vector.class, collectionComparator);

		Comparator mapComparator = new MapComparator();
		comparators.put(ConcurrentHashMap.class, mapComparator);
		comparators.put(HashMap.class, mapComparator);
		comparators.put(Hashtable.class, mapComparator);
		comparators.put(IdentityHashMap.class, mapComparator);
		comparators.put(TreeMap.class, mapComparator);
		comparators.put(WeakHashMap.class, mapComparator);

		Comparator simpleComparator = new SimpleComparator();
		comparators.put(Integer.class, simpleComparator);
		comparators.put(Float.class, simpleComparator);
		comparators.put(Character.class, simpleComparator);
		comparators.put(Byte.class, simpleComparator);
		comparators.put(Short.class, simpleComparator);
		comparators.put(Long.class, simpleComparator);
		comparators.put(Float.class, simpleComparator);
		comparators.put(Double.class, simpleComparator);
		comparators.put(Boolean.class, simpleComparator);
		comparators.put(Enum.class, simpleComparator);
		comparators.put(String.class, simpleComparator);

		Comparator rawDataFileComparator = new RawDataFileComparator();
		comparators.put(RawDataFile.class, rawDataFileComparator);
		comparators.put(RawDataFileImpl.class, rawDataFileComparator);

		Comparator projectComparator = new MZmineProjectComparator();
		comparators.put(MZmineProject.class, projectComparator);
		comparators.put(MZmineProjectImpl.class, projectComparator);
		
		comparators.put(StorableScan.class,new StorableScanComparator());
	}

	private class GenericType {
		// marker type
	}

	private class NullType {
		public String toString() {
			return "Null value";
		}
	}

	public boolean compare(Object oldObj, Object newObj,
			HashMap<Object, ArrayList<Object>> doneList) throws Exception {
		return this.compare(oldObj, newObj, new OmitFieldRegistory(), doneList);
	}

	public boolean compare(Object oldObj, Object newObj,
			OmitFieldRegistory ofRegist,
			HashMap<Object, ArrayList<Object>> doneList) throws Exception {

		Comparator comparator = null;
		Class cls;

		if (oldObj == null) {
			oldObj = new NullType();
			newObj = new NullType();
		}

		cls = oldObj.getClass();
		if (cls.isArray()) {
			comparator = comparators.get(Array.class);

		} else if (cls.isEnum()) {
			comparator = comparators.get(Enum.class);

		} else if (comparators.containsKey(cls)) {
			comparator = comparators.get(cls);

		} else {
			cls = GenericType.class;
			comparator = comparators.get(GenericType.class);
		}

		if (comparator == null) {
			throw new Exception("Comparator not found for "
					+ oldObj.getClass().getName());
		}

		if (doneList.containsKey(oldObj)) {
			ArrayList<Object> list = doneList.get(oldObj);
			if (list.contains(newObj)) {
				logger.info("Found in already done list");
				return true;
			}
		}
		boolean ok = comparator.compare(oldObj, newObj, ofRegist, doneList);
		if (ok == true) {
			logger.info("Consistent :\noldObj:" + oldObj.toString()
					+ " \nnewObj:" + newObj.toString());

			if (doneList.containsKey(oldObj)) {
				doneList.get(oldObj).add(newObj);
			} else {
				ArrayList<Object> list = new ArrayList<Object>(0);
				list.add(newObj);
				doneList.put(oldObj, list);
			}
			return true;
		} else {
			logger.info("Not consistent :\noldObj:" + oldObj.toString()
					+ " \nnewObj:" + newObj.toString());
			return false;
		}
	}
}
