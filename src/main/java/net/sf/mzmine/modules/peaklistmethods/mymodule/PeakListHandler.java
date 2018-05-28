package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.TreeMap;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;

public class PeakListHandler {
	
	TreeMap<Integer, PeakListRow> map;
	
	PeakListHandler()
	{
		map = new TreeMap<Integer, PeakListRow>();
	}
	
	void setUp(PeakList pL)
	{
		for(PeakListRow row : pL.getRows())
		{
			map.put(row.getID(), row);
		}
	}
	
	PeakListRow getRowByID(int ID)
	{
		return map.get(ID);
	}
}
