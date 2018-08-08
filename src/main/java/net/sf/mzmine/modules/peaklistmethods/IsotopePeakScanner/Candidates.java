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
	
	/**
	 * 
	 * @param index integer index
	 * @return Candidate with specified index
	 */
	public Candidate get(int index)
	{
		if(index >= candidate.length)
			throw new MSDKRuntimeException("Candidates.get(index) - index > length");
		return candidate[index];
	}
	
	/**
	 * 
	 * @param index
	 * @return average rating of specified peak. -1 if not set
	 */
	public double getAvgRating(int index)
	{
		if(index >= candidate.length)
			throw new MSDKRuntimeException("Candidates.get(index) - index > length");
		return avgRating[index];
	}
	/**
	 * 
	 * @return total average rating of all data points in the detected pattern
	 */
	public double getAvgAvgRatings()
	{
		if(avgRating.length == 0)
			return 0.0;
		
		double buffer = 0.0;
		for(double rating : avgRating)
			buffer += rating;
		
		return buffer / avgRating.length;
	}
	
	
	public int size()
	{
		return candidate.length;
	}
	
	/**
	 * 
	 * @return all candidate objects
	 */
	public Candidate[] getCandidates()
	{
		return candidate;
	}
	
	/**
	 * @return IsotopePattern object the pattern got initialized with 
	 */
	public IsotopePattern getPattern()
	{
		return pattern;
	}
	
	/**
	 * sets isotope pattern, should not be used when there are different numbers of data points in the pattern.
	 * @param pattern
	 */
	public void setPattern(IsotopePattern pattern)
	{
		this.pattern = pattern;
	}
	
	/**
	 * 
	 * @param indexreturns average intensity of a signle peak
	 * @return
	 */
	public double getAvgHeight(int index)
	{
		if(index > candidate.length || avgHeight == null)
			return 0.0;
		return avgHeight[index];
	}
	
	/**
	 * 
	 * @param index
	 * @param parent Row of monoisotopic mass
	 * @param cand row of candidate peak
	 * @param minRating minimum rating
	 * @param checkIntensity 
	 * @return true if better, false if worse
	 */
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
	
	/**
	 * 
	 * @param index index
	 * @return average peak intensity over all mass lists it is contained in
	 */
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
	
	/**
	 * 
	 * @return array of all avg ratings
	 */
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
	
	/**
	 * needed of calcTemporaryAvgRating
	 * @param ID
	 * @return avPeakHeight
	 */
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
	
	/**
	 * 
	 * @param ID
	 * @return avg heights of all with the ids, but only if they are contained in same scans and mass lists
	 */
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
