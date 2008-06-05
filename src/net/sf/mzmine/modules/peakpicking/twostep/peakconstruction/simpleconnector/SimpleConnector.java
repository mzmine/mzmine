/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.simpleconnector;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.modules.peakpicking.twostep.massdetection.MzPeak;
import net.sf.mzmine.modules.peakpicking.twostep.peakconstruction.PeakBuilder;

/**
 * 
 */
public class SimpleConnector implements PeakBuilder {

    private float intTolerance, mzTolerance;
    private float minimumPeakHeight, minimumPeakDuration;
    
    public SimpleConnector(SimpleConnectorParameters parameters) {
        intTolerance = (Float) parameters.getParameterValue(SimpleConnectorParameters.intTolerance);
        minimumPeakDuration = (Float) parameters.getParameterValue(SimpleConnectorParameters.minimumPeakDuration);
        minimumPeakHeight = (Float) parameters.getParameterValue(SimpleConnectorParameters.minimumPeakHeight);
        mzTolerance = (Float) parameters.getParameterValue(SimpleConnectorParameters.mzTolerance);
    }

    public Peak[] addScan(Scan scan, MzPeak[] mzValues, Vector<ConnectedPeak> underConstructionPeaks, RawDataFile dataFile) {

    	Vector<Peak> finishedPeaks = new Vector<Peak>();
    	Vector<ConnectedMzPeak> cMzPeaks = new Vector<ConnectedMzPeak>();
        // Calculate scores between under-construction scores and 1d-peaks
        TreeSet<MatchScore> scores = new TreeSet<MatchScore>();

        for (MzPeak mzPeak : mzValues) 
        	cMzPeaks.add(new ConnectedMzPeak(mzPeak));

        for (ConnectedPeak ucPeak : underConstructionPeaks) {
            for (ConnectedMzPeak mzPeak : cMzPeaks) {
            	MatchScore score = new MatchScore(ucPeak, mzPeak,
                        mzTolerance, intTolerance);
                if (score.getScore() < Float.MAX_VALUE) {
                    scores.add(score);
                }
            }
        }

        // Connect the best scoring pairs of under-construction and 1d peaks

        Iterator<MatchScore> scoreIterator = scores.iterator();
        while (scoreIterator.hasNext()) {
            MatchScore score = scoreIterator.next();

            // If 1d peak is already connected, then move to next score
            ConnectedMzPeak cMzPeak = score.getMzPeak();
            if (cMzPeak.isConnected()) {
                continue;
            }

            // If uc peak is already connected, then move on to next score
            ConnectedPeak ucPeak = score.getPeak();
            if (ucPeak.isGrowing()) {
                continue;
            }

            // Connect 1d to uc
            ucPeak.addDatapoint(scan.getScanNumber(), cMzPeak.mzPeak.getMZ(),
                    scan.getRetentionTime(), cMzPeak.mzPeak.getIntensity());
            cMzPeak.setConnected();

        }

        
        // Check if there are any under-construction peaks that were not
        // connected (finished)
        for (ConnectedPeak ucPeak : underConstructionPeaks) {

            // If nothing was added,
            if (!ucPeak.isGrowing()) {
            	
            	// Finalize peak
            	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
            	
                // Check length
                float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
                float ucHeight = ucPeak.getHeight();
                if ((ucLength >= minimumPeakDuration)
                        && (ucHeight >= minimumPeakHeight)) {
                		finishedPeaks.add(ucPeak);
                }

                // Remove the peak from under construction peaks
                int ucInd = underConstructionPeaks.indexOf(ucPeak);
                underConstructionPeaks.set(ucInd, null);
            }

        }
        
        
        // Clean-up empty slots under-construction peaks collection and
        // reset growing statuses for remaining under construction peaks
        for (int ucInd = 0; ucInd < underConstructionPeaks.size(); ucInd++) {
        	ConnectedPeak ucPeak = underConstructionPeaks.get(ucInd);
            if (ucPeak == null) {
                underConstructionPeaks.remove(ucInd);
                ucInd--;
            } else {
                ucPeak.resetGrowingState();
            }
        }

        
        // If there are some unconnected 1d-peaks, then start a new
        // under-construction peak for each of them
        for (ConnectedMzPeak cMzPeak : cMzPeaks) {
            if (!cMzPeak.isConnected()) {
            	ConnectedPeak ucPeak = new ConnectedPeak(dataFile);
                ucPeak.addDatapoint(scan.getScanNumber(), cMzPeak.mzPeak.getMZ(),
                        scan.getRetentionTime(), cMzPeak.mzPeak.getIntensity());
                ucPeak.resetGrowingState();
                underConstructionPeaks.add(ucPeak);
            }

        }

        return finishedPeaks.toArray(new Peak[0]);
    }
    
    public Peak[] finishPeaks(Vector<ConnectedPeak> underConstructionPeaks){
    	Vector<Peak> finishedPeaks = new Vector<Peak>();    	
        for (ConnectedPeak ucPeak : underConstructionPeaks) {
         	// Finalize peak
         	ucPeak.finalizedAddingDatapoints(PeakStatus.DETECTED);
         	
             // Check length & height
             float ucLength = ucPeak.getRawDataPointsRTRange().getSize();
             float ucHeight = ucPeak.getHeight();
             
             if ((ucLength >= minimumPeakDuration)
                     && (ucHeight >= minimumPeakHeight)) {
         		finishedPeaks.add(ucPeak);
             }
         }    	
        return finishedPeaks.toArray(new Peak[0]);   	
    }


}
