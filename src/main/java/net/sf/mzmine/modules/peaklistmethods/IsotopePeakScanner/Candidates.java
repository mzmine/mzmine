package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import com.google.common.collect.Range;

import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class Candidates {
	
	private IsotopePattern pattern;
	private MZTolerance mzTolerance;
	private String massListName;
	private double minHeight;
	private double avgRating[];
	private double avgHeight[];
	
	public Candidates(int size, double minHeight, MZTolerance mzTolerance, IsotopePattern pattern, String massListName)
	{
		this.candidate = new Candidate[size];
		for(int i = 0; i < size; i++)
			candidate[i] = new Candidate();
		avgRating = new double[size];
		avgHeight = new double[size];
		this.minHeight = minHeight;
		this.mzTolerance = mzTolerance;
		this.massListName = massListName;
		this.pattern = pattern;
	}
	
	private Candidate[] candidate;
	
	public Candidate get(int index)
	{
		if(index >= candidate.length)
			throw new MSDKRuntimeException("Candidates.get(index) - index > length");
		return candidate[index];
	}
	public double getAvgRating(int index)
	{
		return avgRating[index];
	}
	public int size()
	{
		return candidate.length;
	}
	
	public Candidate[] getCandidates()
	{
		return candidate;
	}
	
	public IsotopePattern getPattern()
	{
		return pattern;
	}
	
	public void setPattern(IsotopePattern pattern)
	{
		this.pattern = pattern;
	}
	
	public double getAvgHeight(int index)
	{
		return avgHeight[index];
	}
	
	public void calcAvgRating(PeakListHandler plh)
	{
		int[] ids = new int[candidate.length];
		
		for(int i = 0; i < candidate.length; i++)
			ids[i] = candidate[i].getCandID();
		
		avgHeight = getAvgPeakHeights(plh, ids);
		
		if(avgHeight == null)
			return;
		
		for(int i = 0; i < candidate.length; i++)
		{
			avgRating[i] = candidate[i].recalcRatingWithAvgIntensities(candidate[0].getMZ(), pattern, i, avgHeight);
		}
	}
	
	private double[] getAvgPeakHeights(PeakListHandler plh, int[] ID)
	{		
		PeakListRow[] rows = plh.getRowsByID(ID);
		
		RawDataFile[] raws = rows[0].getRawDataFiles();
		
		if(raws.length< 1)
			return null;
		
		double[] mzs = new double[ID.length];  
		
		for(int i = 0; i < rows.length; i++)
			mzs[i] = rows[i].getAverageMZ();
		
		double[] avgHeights = new double[ID.length];
		int pointsAdded = 0;
		
		for(RawDataFile raw : raws)
		{
			
			if(!raw.getDataMZRange().contains(rows[0].getAverageMZ()))
				continue;
			
			int[] scanNums = raw.getScanNumbers();
			
			for(int i = 0; i < scanNums.length; i++)
			{
				Scan scan = raw.getScan(scanNums[i]);
				
				MassList list = scan.getMassList(massListName);
				
//				if(list == null)
//					throw new MSDKRuntimeException("PeakList does not contain mass list: " + massListName);
				
				if(list == null || !massListContainsEveryMZ(list, mzs, minHeight))
					continue;
				
//				if(!scanContainsEveryMZ(scan, mzs, minHeight))
//					continue;
				
				for(int j = 0; j < mzs.length; j++)
				{
					DataPoint[] points = scan.getDataPointsByMass(mzTolerance.getToleranceRange(mzs[j])); //TODO: DP from massList
					
					
					if(points.length == 0)
						continue;
					
					DataPoint dp = getClosestDataPoint(points, rows[j].getAverageMZ(), minHeight);
					avgHeights[j] += dp.getIntensity();
					pointsAdded++;
				}
			}
		}
		
		if(!(pointsAdded%mzs.length==0))
		{
			throw new MSDKRuntimeException("points added not devisable by mzs.length");
		}
		
		for(int i = 0; i < avgHeights.length; i++)
			avgHeights[i] /= (pointsAdded/mzs.length);
				
		return avgHeights;
	}
	
	private DataPoint getClosestDataPoint(DataPoint[] dp, double mz, double minHeight)
	{
		if(dp == null || dp[0] == null || dp.length == 0)
			return null;
		
		DataPoint n = dp[0];
		
		for(DataPoint p : dp)
			if(Math.abs(p.getMZ() - mz) < Math.abs(mz - n.getMZ()) && p.getIntensity() >= minHeight)
				n = p;
		
		return n;
	}
	/**
	 * 
	 * @param scan
	 * @param mz
	 * @return true if the scan contains dataPoints in every Scan
	 */
	private boolean scanContainsEveryMZ(Scan scan, double [] mz, double minHeight)
	{
		for(int i = 0; i < mz.length; i++)
		{
			DataPoint[] dps = scan.getDataPointsByMass(mzTolerance.getToleranceRange(mz[i]));
			if(dps.length < 1)
				return false;
			
			boolean aboveMinHeight = false;
			
			for(DataPoint p : dps)
				if(p.getIntensity() >= minHeight)
					aboveMinHeight = true;
			
			if(!aboveMinHeight)
				return false;
		}
		return true;
	}
	
	private boolean massListContainsEveryMZ(MassList list, double[] mz, double minHeight)
	{
		DataPoint[] dps = list.getDataPoints();
		if(dps.length < 1)
			return false;

		for(int i = 0; i < mz.length; i++)
		{
			boolean aboveMinHeight = false;
			
			for(DataPoint p : dps)
			{
				if(p.getMZ() < (mz[i] - 0.1)) //TODO: maybe make this not hard coded?
					continue;
				if(p.getMZ() > (mz[i] + 0.1))
					break;

				if(p.getIntensity() >= minHeight && mzTolerance.checkWithinTolerance(p.getMZ(), mz[i]))
					aboveMinHeight = true;
			}

			if(!aboveMinHeight)
				return false;
		}
		return true;
	}
	private DataPoint[] getMassListDataPointsByMass(MassList list, Range<Double> massRange, double minIntensity)
	{
		DataPoint[] dps = list.getDataPoints();
		int start = 0, end = 0;
		
		for(int i = 0; i < dps.length; i++)
		{
			if(massRange.contains(dps[i].getMZ()))
				//TODO
		}
	}
}
