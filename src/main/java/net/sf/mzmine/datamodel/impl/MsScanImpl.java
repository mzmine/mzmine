/*
 * Copyright 2006-2014 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MsMsScan;
import net.sf.mzmine.datamodel.MsScan;
import net.sf.mzmine.datamodel.Polarity;
import net.sf.mzmine.datamodel.RawDataFile;
import net.sf.mzmine.util.ScanUtils;

import com.google.common.collect.Range;

/**
 * Simple implementation of the Scan interface.
 */
public class MsScanImpl extends SpectrumImpl implements MsScan {

    private final RawDataFile dataFile;
    private int scanNumber;
    private int msLevel;
    private double retentionTime;
    private final List<MsMsScan> fragmentScans;
    private final List<MassList> massLists;

    public MsScanImpl(RawDataFile dataFile) {
	super((RawDataFileImpl) dataFile);
	this.dataFile = dataFile;
	this.fragmentScans = new ArrayList<MsMsScan>();
	this.massLists = new ArrayList<MassList>();
    }

    /**
     * @see net.sf.mzmine.datamodel.MsScan#getScanNumber()
     */
    @Override
    public int getScanNumber() {
	return scanNumber;
    }

    /**
     * @param scanNumber
     *            The scanNumber to set.
     */
    public void setScanNumber(int scanNumber) {
	this.scanNumber = scanNumber;
    }

    @Override
    public int getMSLevel() {
	return msLevel;
    }

    @Override
    public void setMSLevel(int msLevel) {
	this.msLevel = msLevel;
    }

    @Override
    public double getRetentionTime() {
	return retentionTime;
    }

    @Override
    public void setRetentionTime(double retentionTime) {
	this.retentionTime = retentionTime;
    }

    /**
     * @see net.sf.mzmine.datamodel.MsScan#getFragmentScanNumbers()
     */
    @Override
    public Collection<MsMsScan> fragmentScans() {
	return fragmentScans;
    }

    @Override
    public String toString() {
	return ScanUtils.scanToString(this);
    }

    @Override
    public @Nonnull RawDataFile getDataFile() {
	return dataFile;
    }

    @Override
    public @Nonnull Collection<MassList> massLists() {
	return massLists;
    }

    @Override
    public MassList getMassList(@Nonnull String name) {
	throw new UnsupportedOperationException();
    }

    @Override
    public Polarity getPolarity() {
	return Polarity.UNKNOWN;
    }

    @Override
    public Range<Double> getScanRange() {
	return null;
    }

    @Override
    public double getTIC() {
	// TODO Auto-generated method stub
	return 0;
    }

}
