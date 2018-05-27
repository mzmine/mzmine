package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.ArrayList;

public class ResultBuffer {
	private int found;
	private ArrayList<Integer> row;
	private ArrayList<Integer> ID;
	
	
	public int getFoundCount() {
		return found;
	}
	public void addFound() {
		this.found++;
	}
	public int getSize()
	{
		return row.size();
	}
	public void addRow(int r) {
		row.add((Integer) r);
	}
	public void addID(int id)
	{
		ID.add((Integer) id);
	}
	public int getRow(int i) {
		return row.get(i).intValue();
	}
	public int getID(int i)
	{
		return ID.get(i).intValue();
	}
	
	ResultBuffer()
	{
		found = 0;
		row = new ArrayList<Integer>();
		ID = new ArrayList<Integer>();
	}
}
