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

package io.github.mzmine.util.spectraldb.entry;

import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.Arrays;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.impl.SimplePeakIdentity;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;

public class SpectralDBPeakIdentity extends SimplePeakIdentity {
    private static final DecimalFormat COS_FORM = new DecimalFormat("0.000");

    private final SpectralDBEntry entry;
    private final SpectralSimilarity similarity;

    private Scan queryScan;
    private String massListName;

    public SpectralDBPeakIdentity(Scan queryScan, String massListName,
            SpectralDBEntry entry, SpectralSimilarity similarity,
            String method) {
        super(MessageFormat.format("{0} as {3} ({1}) {2} cos={4}",
                entry.getField(DBEntryField.NAME).orElse("NONAME"), // Name
                entry.getField(DBEntryField.MZ).orElse(""), // precursor m/z
                entry.getField(DBEntryField.FORMULA).orElse(""), // molecular
                                                                 // formula
                entry.getField(DBEntryField.ION_TYPE).orElse(""), // Ion type
                COS_FORM.format(similarity.getScore())), // cosine similarity
                entry.getField(DBEntryField.FORMULA).orElse("").toString(),
                method, "", "");
        this.entry = entry;
        this.similarity = similarity;
        this.queryScan = queryScan;
        this.massListName = massListName;
    }

    public SpectralDBEntry getEntry() {
        return entry;
    }

    public SpectralSimilarity getSimilarity() {
        return similarity;
    }

    public Scan getQueryScan() {
        return queryScan;
    }

    public String getMassListName() {
        return massListName;
    }

    public DataPoint[] getQueryDataPoints() {
        if (massListName == null || queryScan == null
                || queryScan.getMassList(massListName) == null)
            return null;
        return queryScan.getMassList(massListName).getDataPoints();
    }

    public DataPoint[] getLibraryDataPoints(DataPointsTag tag) {
        switch (tag) {
        case ORIGINAL:
            return entry.getDataPoints();
        case FILTERED:
            return similarity.getLibrary();
        case ALIGNED:
            return similarity.getAlignedDataPoints()[0];
        case MERGED:
            return new DataPoint[0];
        }
        return new DataPoint[0];
    }

    public DataPoint[] getQueryDataPoints(DataPointsTag tag) {
        switch (tag) {
        case ORIGINAL:
            DataPoint[] dp = getQueryDataPoints();
            if (dp == null)
                return new DataPoint[0];
            Arrays.sort(dp, new DataPointSorter(SortingProperty.MZ,
                    SortingDirection.Ascending));
            return dp;
        case FILTERED:
            return similarity.getQuery();
        case ALIGNED:
            return similarity.getAlignedDataPoints()[1];
        case MERGED:
            return new DataPoint[0];
        }
        return new DataPoint[0];
    }

}
