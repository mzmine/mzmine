package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import io.github.msdk.MSDKRuntimeException;

public class Candidates {
	
	Candidates(int size)
	{
		this.candidate = new Candidate[size];
	}
	
	private Candidate[] candidate;
	
	public Candidate get(int index)
	{
		if(index >= candidate.length)
			throw new MSDKRuntimeException("Candidates.get(index) - index > length");
		return candidate[index];
	}
	
	public int size()
	{
		return candidate.length;
	}
}
