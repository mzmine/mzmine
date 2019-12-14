/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
package io.github.mzmine.parameters.parametertypes.selectors;

import io.github.mzmine.datamodel.Feature;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.PeakListRow;
import io.github.mzmine.datamodel.RawDataFile;

/**
 * @author akshaj This class contains the set of Feature, PeakList, PeakListRow
 *         and RawDataFile selected by the user.
 */
public class FeatureSelection implements Cloneable {

    private Feature feature;
    private PeakListRow peakListRow;
    private PeakList peakList;
    private RawDataFile rawDataFile;

    public FeatureSelection(PeakList peakList, Feature feature,
            PeakListRow peakListRow, RawDataFile rawDataFile) {
        this.peakList = peakList;
        this.feature = feature;
        this.peakListRow = peakListRow;
        this.rawDataFile = rawDataFile;
    }

    public Feature getFeature() {
        return feature;
    }

    public PeakListRow getPeakListRow() {
        return peakListRow;
    }

    public PeakList getPeakList() {
        return peakList;
    }

    public RawDataFile getRawDataFile() {
        return rawDataFile;
    }

    /*
     * @see java.lang.Object#clone()
     */
    public FeatureSelection clone() {
        FeatureSelection newSelection = new FeatureSelection(peakList, feature,
                peakListRow, rawDataFile);
        return newSelection;
    }

}
