package net.sf.mzmine.modules.peaklistmethods.mymodule;

import java.util.ArrayList;

public class ResultBuffer {
	private int found;
	private ArrayList<Integer> row;
	
	
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
	public int getRow(int i) {
		return row.get(i).intValue();
	}
	public void addRow(int r) {
		row.add((Integer) r);
	}
	
	ResultBuffer()
	{
		found = 0;
		row = new ArrayList<Integer>();
	}
}
