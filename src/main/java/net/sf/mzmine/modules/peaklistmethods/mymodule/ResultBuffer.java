package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.ArrayList;

public class ResultBuffer {
	private int found;
	private ArrayList<Integer> row;
	
	
	public int getFoundCount() {
		return found;
	}
	public void addFound() {
		this.found += 1;
	}
	public int getSize()
	{
		return row.size();
	}
	public int getRow(int i) {
		return row.get(i).intValue();
	}
	public void addRow(int r) {
		row.add(new Integer(r));
	}
	
	ResultBuffer()
	{
		found = 0;
		row = new ArrayList<Integer>(1);
	}
}
