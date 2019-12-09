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

package io.github.mzmine.util.scans.similarity;

import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.SortingDirection;
import io.github.mzmine.util.SortingProperty;
import io.github.mzmine.util.scans.ScanAlignment;

/**
 * The result of a {@link SpectralSimilarityFunction}.
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class SpectralSimilarity {
    // similarity score (depends on similarity function)
    private double score;
    // aligned signals in library and query spectrum
    private int overlap;
    // similarity function name
    private String funcitonName;

    // spectral data can be nullable to save memory
    // library and query spectrum (may be filtered)
    private @Nullable DataPoint[] library;
    private @Nullable DataPoint[] query;
    // aligned data points (found in both the library[0] and the query[1]
    // sepctrum)
    // alinged[library, query][data points]
    private @Nullable DataPoint[][] aligned;

    /**
     * The result of a {@link SpectralSimilarityFunction}.
     * 
     * @param funcitonName
     *            Similarity function name
     * @param score
     *            similarity score
     * @param overlap
     *            count of aligned data points in library and query spectrum
     */
    public SpectralSimilarity(String funcitonName, double score, int overlap) {
        this.funcitonName = funcitonName;
        this.score = score;
        this.overlap = overlap;
    }

    /**
     * The result of a {@link SpectralSimilarityFunction}.
     * 
     * @param funcitonName
     *            Similarity function name
     * @param score
     *            similarity score
     * @param overlap
     *            count of aligned data points in library and query spectrum
     * @param librarySpec
     *            library spectrum (or other) which was matched to querySpec
     *            (may be filtered)
     * @param querySpec
     *            query spectrum which was matched to librarySpec (may be
     *            filtered)
     * @param alignedDP
     *            aligned data points (alignedDP.get(data point
     *            index)[library/query spectrum])
     */
    public SpectralSimilarity(String funcitonName, double score, int overlap,
            @Nullable DataPoint[] librarySpec, @Nullable DataPoint[] querySpec,
            @Nullable List<DataPoint[]> alignedDP) {
        DataPointSorter sorter = new DataPointSorter(SortingProperty.MZ,
                SortingDirection.Ascending);
        this.funcitonName = funcitonName;
        this.score = score;
        this.overlap = overlap;
        this.library = librarySpec;
        this.query = querySpec;
        if (this.library != null)
            Arrays.sort(this.library, sorter);
        if (this.query != null)
            Arrays.sort(this.query, sorter);
        if (alignedDP != null) {
            // filter unaligned
            List<DataPoint[]> filtered = ScanAlignment
                    .removeUnaligned(alignedDP);
            aligned = ScanAlignment.convertBackToMassLists(filtered);

            for (DataPoint[] dp : aligned)
                Arrays.sort(dp, sorter);
        }
    }

    /**
     * Number of overlapping signals in both spectra
     * 
     * @return
     */
    public int getOverlap() {
        return overlap;
    }

    /**
     * Cosine similarity
     * 
     * @return
     */
    public double getScore() {
        return score;
    }

    /**
     * SPectralSimilarityFunction name
     * 
     * @return
     */
    public String getFunctionName() {
        return funcitonName;
    }

    /**
     * Library spectrum (usually filtered)
     * 
     * @return
     */
    public DataPoint[] getLibrary() {
        return library;
    }

    /**
     * Query spectrum (usually filtered)
     * 
     * @return
     */
    public DataPoint[] getQuery() {
        return query;
    }

    /**
     * All aligned data points of library(0) and query(1) spectrum
     * 
     * @return DataPoint[library, query][datapoints]
     */
    public DataPoint[][] getAlignedDataPoints() {
        return aligned;
    }
}
