/*
 * Copyright 2006-2007 The MZmine Development Team
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

package net.sf.mzmine.data.impl;

import java.text.Format;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.Peak;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.io.OpenedRawDataFile;
import net.sf.mzmine.userinterface.mainwindow.MainWindow;

/**
 * 
 */
public class SimplePeakListRow implements PeakListRow {

    private Hashtable<OpenedRawDataFile, Peak> peaks;
    private Hashtable<OpenedRawDataFile, Peak> originalPeaks;
    private HashSet<CompoundIdentity> identities;
    private CompoundIdentity preferredIdentity;
    private String comment;
    private int myID;
    private double maxDataPointIntensity = 0;

    public SimplePeakListRow(int myID) {
        this.myID = myID;
        peaks = new Hashtable<OpenedRawDataFile, Peak>();
        originalPeaks = new Hashtable<OpenedRawDataFile, Peak>();
        identities = new HashSet<CompoundIdentity>();
        preferredIdentity = CompoundIdentity.UNKNOWN_IDENTITY;
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#getID()
     */
    public int getID() {
        return myID;
    }

    /*
     * Return peaks assigned to this row
     */
    public Peak[] getPeaks() {
        return peaks.values().toArray(new Peak[0]);
    }

    /*
     * Return opened raw data files with a peak on this row
     */
    public OpenedRawDataFile[] getOpenedRawDataFiles() {
        return peaks.keySet().toArray(new OpenedRawDataFile[0]);
    }

    /*
     * Returns peak for given raw data file
     */
    public Peak getPeak(OpenedRawDataFile rawData) {
        return peaks.get(rawData);
    }

    public void setPeak(OpenedRawDataFile rawData, Peak p) {
        peaks.put(rawData, p);
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#getOriginalPeakListEntry(net.sf.mzmine.io.OpenedRawDataFile)
     */
    public Peak getOriginalPeakListEntry(OpenedRawDataFile rawData) {
        return originalPeaks.get(rawData);
    }

    public void addPeak(OpenedRawDataFile rawData, Peak original, Peak current) {
        peaks.put(rawData, current);
        originalPeaks.put(rawData, original);
        if (current.getDataPointMaxIntensity() > maxDataPointIntensity)
            maxDataPointIntensity = current.getDataPointMaxIntensity();
    }

    /*
     * Returns average normalized M/Z for peaks on this row
     */
    public double getAverageMZ() {
        double mzSum = 0.0;
        Enumeration<Peak> peakEnum = peaks.elements();
        while (peakEnum.hasMoreElements()) {
            Peak p = peakEnum.nextElement();
            mzSum += p.getMZ();
        }
        return mzSum / peaks.size();
    }

    /*
     * Returns average normalized RT for peaks on this row
     */
    public double getAverageRT() {
        double rtSum = 0.0;
        Enumeration<Peak> peakEnum = peaks.elements();
        while (peakEnum.hasMoreElements()) {
            Peak p = peakEnum.nextElement();
            rtSum += p.getRT();
        }
        return rtSum / peaks.size();
    }

    /*
     * Returns number of peaks assigned to this row
     */
    public int getNumberOfPeaks() {
        return peaks.size();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        Format mzFormat = MainWindow.getInstance().getMZFormat();
        Format timeFormat = MainWindow.getInstance().getRTFormat();
        buf.append(mzFormat.format(getAverageMZ()));
        buf.append(" m/z @");
        buf.append(timeFormat.format(getAverageRT()));
        if (preferredIdentity != null)
            buf.append(" " + preferredIdentity.getCompoundName());
        if ((comment != null) && (comment.length() > 0))
            buf.append(" (" + comment + ")");
        return buf.toString();
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#getComment()
     */
    public String getComment() {
        return comment;
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#setComment(java.lang.String)
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#addCompoundIdentity(net.sf.mzmine.data.CompoundIdentity)
     */
    public void addCompoundIdentity(CompoundIdentity identity) {
        identities.add(identity);
        if (preferredIdentity == null)
            setPreferredCompoundIdentity(identity);
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#getCompoundIdentities()
     */
    public CompoundIdentity[] getCompoundIdentities() {
        return identities.toArray(new CompoundIdentity[0]);
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#getPreferredCompoundIdentity()
     */
    public CompoundIdentity getPreferredCompoundIdentity() {
        return preferredIdentity;
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#setPreferredCompoundIdentity(net.sf.mzmine.data.CompoundIdentity)
     */
    public void setPreferredCompoundIdentity(CompoundIdentity identity) {
        preferredIdentity = identity;
    }

    /**
     * @see net.sf.mzmine.data.PeakListRow#getDataPointMaxIntensity()
     */
    public double getDataPointMaxIntensity() {
        return maxDataPointIntensity;
    }

}
