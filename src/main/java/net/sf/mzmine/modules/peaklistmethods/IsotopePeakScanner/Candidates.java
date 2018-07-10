package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.Arrays;

import com.google.common.collect.Range;

import io.github.msdk.MSDKRuntimeException;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.PeakListRow;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.datamodel.Scan;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner.IsotopePeakScannerTask.RatingType;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;

public class Candidates {
	
	private IsotopePattern pattern;
	private MZTolerance mzTolerance;
	private String massListName;
	private double minHeight;
	private double avgRating[];
	private double avgHeight[];
	private Candidate[] candidate;
	RatingType ratingType;
	PeakListHandler plh;
	
	
	public Candidates(int size, double minHeight, MZTolerance mzTolerance, IsotopePattern pattern, String massListName, PeakListHandler plh, RatingType ratingType)
	{
		this.candidate = new Candidate[size];
		for(int i = 0; i < size; i++)
			candidate[i] = new Candidate();
		avgRating = new double[size];
		Arrays.fill(avgRating, -1.0);
		avgHeight = new double[size];
		this.minHeight = minHeight;
		this.mzTolerance = mzTolerance;
		this.massListName = massListName;
		this.pattern = pattern;
		this.plh = plh;
		this.ratingType = ratingType;
	}
		
	public Candidate get(int index)
	{
		if(index >= candidate.length)
			throw new MSDKRuntimeException("Candidates.get(index) - index > length");
		return candidate[index];
	}
	public double getAvgRating(int index)
	{
		if(index >= candidate.length)
			throw new MSDKRuntimeException("Candidates.get(index) - index > length");
		return avgRating[index];
	}
	public double getAvgAvgRatings()
	{
		double buffer = 0.0;
		for(double rating : avgRating)
			buffer += rating;
		
		return buffer / avgRating.length;
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
		if(index > candidate.length || avgHeight == null)
			return 0.0;
		return avgHeight[index];
	}
	
	public boolean checkForBetterRating(int index, PeakListRow parent, PeakListRow cand, double minRating, boolean checkIntensity)
	{
		if(ratingType == RatingType.HIGHEST)
			return candidate[index].checkForBetterRating(parent, cand, pattern, index, minRating, checkIntensity);
		else if(ratingType == RatingType.TEMPAVG)
		{
			DataPoint dpParent = new SimpleDataPoint(parent.getAverageMZ(), calcAvgPeakHeight(parent.getID()));
			double candidateIntensity = calcAvgPeakHeight(cand.getID());
			return candidate[index].checkForBetterRating(dpParent, candidateIntensity, cand, pattern, index, minRating, checkIntensity);
		}
		else
			throw new MSDKRuntimeException("Error: Invalid RatingType.");
	}
	
	public double calcTemporaryAvgRating(int index)
	{
		if(index > candidate.length)
			return 0.0;
		
		double parentHeight = calcAvgPeakHeight(candidate[0].getCandID());
		double childHeight = calcAvgPeakHeight(candidate[index].getCandID());
		
		double[] avg = new double[candidate.length];
		avg[0] = parentHeight;
		avg[index] = childHeight;
		
		return candidate[index].recalcRatingWithAvgIntensities(candidate[0].getMZ(), pattern, index, avg);
	}
	
	public double[] calcAvgRatings()
	{
		int[] ids = new int[candidate.length];
		
		for(int i = 0; i < candidate.length; i++)
			ids[i] = candidate[i].getCandID();
		
		avgHeight = getAvgPeakHeights(ids);
		
		if(avgHeight == null || avgHeight[0] == 0.0)
			return avgRating;
		
		for(int i = 0; i < candidate.length; i++)
		{
			avgRating[i] = candidate[i].recalcRatingWithAvgIntensities(candidate[0].getMZ(), pattern, i, avgHeight);
		}
		return avgRating;
	}
	
	private double calcAvgPeakHeight(int ID)
	{
		PeakListRow row = plh.getRowByID(ID);
		
		RawDataFile[] raws = row.getRawDataFiles();
		
		if(raws.length< 1)
			return 0.0;
		
		double mz = row.getAverageMZ();
		double avgIntensity = 0.0;
		int pointsAdded = 0;
		
		for(RawDataFile raw : raws)
		{
			if(!raw.getDataMZRange().contains(mz))
				continue;
			
			int[] scanNums = raw.getScanNumbers();
			
			for(int i = 0; i < scanNums.length; i++)
			{
				Scan scan = raw.getScan(scanNums[i]);
				
				MassList list = scan.getMassList(massListName);
				
				if(list == null)
					continue;
				
				DataPoint [] points = getMassListDataPointsByMass(list, mzTolerance.getToleranceRange(mz));
				
				if(points.length == 0)
					continue;
				
				DataPoint dp = getClosestDataPoint(points, mz, minHeight);
				
				if(dp != null)
				{
					avgIntensity += dp.getIntensity();
					pointsAdded++;
				}
			}
		}
		return avgIntensity/pointsAdded;
	}
	
	private double[] getAvgPeakHeights(int[] ID)
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
				
				if(list == null || !massListContainsEveryMZ(list, mzs, minHeight))
					continue;

				double[] avgBuffer = new double[mzs.length];
				boolean allFound = true;
				
				for(int j = 0; j < mzs.length; j++)
				{
					//DataPoint[] points = scan.getDataPointsByMass(mzTolerance.getToleranceRange(mzs[j])); //TODO: DP from massList
					DataPoint[] points = getMassListDataPointsByMass(list, mzTolerance.getToleranceRange(mzs[j]));
					
					if(points.length == 0)
						continue;
					
					DataPoint dp = getClosestDataPoint(points, rows[j].getAverageMZ(), minHeight);
					
					if(dp == null)	//yes the list contained something close to every datapoint that was over minHeight, BUT
					{				// the closest might not have been. Check is done inside getClosestDataPoint();
						allFound = false;
						break;
					}
					avgBuffer[j] = dp.getIntensity();
				}
				
				if(allFound)
				{
					pointsAdded++;
					for(int j = 0; j < mzs.length; j++)
						avgHeights[j] += avgBuffer[j];
				}
			}
		}
		
//		if(!(pointsAdded%mzs.length==0))
//			throw new MSDKRuntimeException("points added not devideable by mzs.length");
		
		if(pointsAdded == 0)
		{
			System.out.println("Error: Peaks with ids: " + ID.toString() + "were not in same scans at all");
			return null;
		}
		for(int i = 0; i < avgHeights.length; i++)
			avgHeights[i] /= (pointsAdded/*/mzs.length*/);
				
		return avgHeights;
	}
	/**
	 * 
	 * @param dp
	 * @param mz
	 * @param minHeight
	 * @return closest data point to given mz above minimum intensity in a given set of data points; null if no DataPoint over given intensity
	 */
	private DataPoint getClosestDataPoint(DataPoint[] dp, double mz, double minHeight)	//TODO: this should have a check and return null instead of dp[0] by default
	{
		if(dp == null || dp[0] == null || dp.length == 0)
			return null;
		
		DataPoint n = new SimpleDataPoint(0.0, 0.0);
		
		for(DataPoint p : dp)
			if(Math.abs(p.getMZ() - mz) < Math.abs(mz - n.getMZ()) && p.getIntensity() >= minHeight)
				n = p;
		
		if(n.getIntensity() == 0.0)
		{
//			System.out.println("Info: Closest data point not above min intensity. m/z: " + mz);
			return null;
		}
		return n;
	}
		
	/**
	 * 
	 * @param list MassList to check
	 * @param mz array of mzs that need to be contained
	 * @param minHeight minimum peak intensity
	 * @return true or false
	 */
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
				if(p.getMZ() < (mz[i] - mzTolerance.getMzTolerance()))
					continue;
				
				if(p.getMZ() > (mz[i] + mzTolerance.getMzTolerance()))
					break;

				if(p.getIntensity() >= minHeight && mzTolerance.checkWithinTolerance(p.getMZ(), mz[i]))
					aboveMinHeight = true;
			}

			if(!aboveMinHeight)
			{
//				System.out.println("Info: Mass list " + list.getName() + " does not contain every mz: " + mz.toString());
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 
	 * @param list
	 * @param massRange
	 * @return dataPoints within given massRange contained in mass list
	 */
	private DataPoint[] getMassListDataPointsByMass(MassList list, Range<Double> massRange)
	{
		DataPoint[] dps = list.getDataPoints();
		int start = 0, end = 0;
		
		for(start = 0; start < dps.length; start++)
			if(massRange.lowerEndpoint() >= dps[start].getMZ())
				break;
			
		for(end = start; end < dps.length; end++)
			if(massRange.upperEndpoint() < dps[end].getMZ())
				break;
		
		DataPoint[] dpReturn = new DataPoint[end-start];
		
		System.arraycopy( dps, start, dpReturn, 0, end - start);
		
		return dpReturn;
	}
}
