package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.Set;
import java.util.TreeMap;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;

public class PeakListHandler {
	
	TreeMap<Integer, PeakListRow> map;
	
	PeakListHandler()
	{
		map = new TreeMap<Integer, PeakListRow>();
	}
	/**
	 * use this if you want to manage an existing PeakList
	 * @param pL 
	 */
	public void setUp(PeakList pL)
	{
		for(PeakListRow row : pL.getRows())
		{
			map.put(row.getID(), row);
		}
	}
	
	public void addRow(PeakListRow row)
	{
		map.put(row.getID(), row);
	}
	
	public int size()
	{
		return map.size();
	}
	
	public Integer[] getAllKeys()
	{
		Set<Integer> set = map.keySet();
		Integer[] keys = (Integer[]) set.toArray();
		
		return keys;
	}
	
	/**
	 * 
	 * @param ID
	 * @return
	 */
	public PeakListRow getRowByID(int ID)
	{
		return map.get(ID);
	}
}
