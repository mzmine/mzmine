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

package net.sf.mzmine.datamodel;

import javax.annotation.Nonnull;

import net.sf.mzmine.datamodel.impl.MZmineObjectBuilderImpl;

/**
 * Object builder
 */
public class MZmineObjectBuilder {

    public static final @Nonnull DataPoint getDataPoint(double mz,
	    double intensity) {
	return MZmineObjectBuilderImpl.getDataPoint(mz, intensity);
    }

    public static final @Nonnull DataPoint[] getDataPointArray(double mz[], double intensities[]) {
	return MZmineObjectBuilderImpl.getDataPointArray(mz, intensities);
    }

    public static final @Nonnull Feature getFeature() {
	return MZmineObjectBuilderImpl.getFeature();
    }

    public static final @Nonnull IsotopePattern getIsotopePattern() {
	return MZmineObjectBuilderImpl.getIsotopePattern();
    }

    public static final @Nonnull MassList getMassList() {
	return MZmineObjectBuilderImpl.getMassList();
    }

    public static final @Nonnull MZmineProject getMZmineProject() {
	return MZmineObjectBuilderImpl.getMZmineProject();
    }

    public static final @Nonnull PeakListRowAnnotation getPeakListRowAnnotation() {
	return MZmineObjectBuilderImpl.getPeakListRowAnnotation();
    }

    public static final @Nonnull PeakList getPeakList() {
	return MZmineObjectBuilderImpl.getPeakList();
    }

    public static final @Nonnull RawDataFile getRawDataFile() {
	return MZmineObjectBuilderImpl.getRawDataFile();
    }

    public static final @Nonnull MsScan getScan() {
	return MZmineObjectBuilderImpl.getScan();
    }

}
