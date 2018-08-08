/*
 * Copyright 2006-2015 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;

public class PeakListHandler {
	
	private TreeMap<Integer, PeakListRow> map;
	
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
	
	public boolean containsID(int ID)
	{
		return map.containsKey(ID);
	}
	
	public ArrayList<Integer> getAllKeys()
	{
		Set<Integer> set = map.keySet();
		ArrayList<Integer> list = new ArrayList<Integer>(set);
		
		return list;
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
	public PeakListRow[] getRowsByID(int ID[])
	{
		PeakListRow[] rows = new PeakListRow[ID.length];
		
		for(int i = 0; i < ID.length; i++)
			rows[i] = map.get(ID[i]);
		
		return rows;
	}
}
