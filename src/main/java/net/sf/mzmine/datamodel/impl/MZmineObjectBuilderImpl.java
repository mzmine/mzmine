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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.datamodel.impl;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.Feature;
import net.sf.mzmine.datamodel.IsotopePattern;
import net.sf.mzmine.datamodel.MZmineProject;
import net.sf.mzmine.datamodel.MassList;
import net.sf.mzmine.datamodel.MsScan;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRowAnnotation;
import net.sf.mzmine.datamodel.RawDataFile;

/**
 * Object builder
 */
public class MZmineObjectBuilderImpl {

    public static final DataPoint getDataPoint(double mz, double intensity) {
	return new DataPointImpl(mz, intensity);
    }

    public static final @Nonnull DataPoint[] getDataPointArray(
	    final double mz[], final double intensities[]) {
	assert mz.length == intensities.length;
	final DataPoint dpArray[] = new DataPoint[mz.length];
	for (int i = 0; i < mz.length; i++)
	    dpArray[i] = new DataPointImpl(mz[i], intensities[i]);
	return dpArray;
    }

    public static final @Nonnull Feature getFeature() {
	return new FeatureImpl();
    }

    public static final @Nonnull IsotopePattern getIsotopePattern() {
	return new IsotopePatternImpl();
    }

    public static final @Nonnull MassList getMassList() {
	return new MassListImpl();
    }

    public static final @Nonnull MZmineProject getMZmineProject() {
	return new MZmineProjectImpl();
    }

    public static final @Nonnull PeakListRowAnnotation getPeakListRowAnnotation() {
	return new PeakListRowAnnotationImpl();
    }

    public static final @Nonnull PeakList getPeakList() {
	return new PeakListImpl();
    }

    public static final @Nonnull RawDataFile getRawDataFile() {
	return new RawDataFileImpl();
    }

    public static final @Nonnull MsScan getScan() {
	return new MsScanImpl();
    }

}
