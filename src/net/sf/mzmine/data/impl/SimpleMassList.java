/*
 * Copyright 2006-2011 The MZmine 2 Development Team
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

package net.sf.mzmine.data.impl;

import net.sf.mzmine.data.MassList;
import net.sf.mzmine.data.MzPeak;
import net.sf.mzmine.data.Scan;

/**
 * This class represent detected masses (ions) in one mass spectrum
 */
public class SimpleMassList implements MassList {

	private String name;
	private Scan scan;
	private MzPeak mzPeaks[];

	public SimpleMassList(String name, Scan scan, MzPeak mzPeaks[]) {
		this.name = name;
		this.scan = scan;
		this.mzPeaks = mzPeaks;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Scan getScan() {
		return scan;
	}

	@Override
	public MzPeak[] getMzPeaks() {
		return mzPeaks;
	}

	@Override
	public void setMzPeaks(MzPeak[] mzPeaks) {
		this.mzPeaks = mzPeaks;
	}

	@Override
	public String toString() {
		return name;
	}

}
