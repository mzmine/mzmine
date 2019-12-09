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

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult;

/**
 * This class stores the results of DataPointProcessingTasks. It offers more
 * functionality, e.g. assigning identities.
 * 
 * @author SteffenHeu steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class ProcessedDataPoint extends SimpleDataPoint {

    // this map is set in the add... methods so we don't use too much memory
    Vector<DPPResult<?>> results;

    /**
     * Generates an array of ProcessedDataPoints from DataPoints.
     * 
     * @param dp
     *            DataPoints to convert.
     * @return Array of ProcessedDataPoints from DataPoints.
     */
    public static ProcessedDataPoint[] convert(@Nonnull DataPoint[] dp) {
        ProcessedDataPoint[] pdp = new ProcessedDataPoint[dp.length];
        for (int i = 0; i < pdp.length; i++)
            pdp[i] = new ProcessedDataPoint(dp[i]);
        return pdp;
    }

    public ProcessedDataPoint(@Nonnull DataPoint dp) {
        super(dp);
    }

    public ProcessedDataPoint(@Nonnull DataPoint dp,
            @Nonnull DPPResult<?> result) {
        this(dp);
        addResult(result);
    }

    public ProcessedDataPoint(@Nonnull DataPoint dp,
            @Nonnull Collection<DPPResult<?>> results) {
        this(dp);
        addAllResults(results);
    }

    /**
     * Adds a single result to this data point.
     * 
     * @param result
     */
    public synchronized void addResult(@Nonnull DPPResult<?> result) {
        if (result == null)
            return;

        if (results == null)
            results = new Vector<DPPResult<?>>();

        results.add(result);
    }

    /**
     * Adds a collection of DPPResults to this data point.
     * 
     * @param results
     */
    public synchronized void addAllResults(
            @Nonnull Collection<DPPResult<?>> results) {
        if (results == null)
            return;

        if (this.results == null)
            this.results = new Vector<DPPResult<?>>();

        for (DPPResult<?> result : results) {
            this.results.add(result);
        }
    }

    public synchronized void addAllResults(@Nonnull DPPResult<?>[] result) {
        if (result == null)
            return;

        if (results == null)
            results = new Vector<DPPResult<?>>();

        for (DPPResult<?> r : result) {
            results.add(r);
        }
    }

    /**
     * 
     * @param i
     *            Index of the specified result
     * @return DPPResult with the given key, may be null if no result with that
     *         key exits or no result exists at all.
     */
    public @Nullable DPPResult<?> getResult(int i) {
        if (results == null)
            return null;
        return results.get(i);
    }

    /**
     * Specifies if a result with the given type exists.
     * 
     * @param type
     * @return
     */
    public boolean resultTypeExists(DPPResult.ResultType type) {
        if (results == null)
            return false;

        for (DPPResult<?> r : results)
            if (r.getResultType() == type)
                return true;
        return false;
    }

    /**
     * The number of results with the given type.
     * 
     * @param type
     * @return
     */
    public int getNumberOfResultsByType(DPPResult.ResultType type) {
        if (results == null)
            return 0;

        int i = 0;
        for (DPPResult<?> r : results)
            if (r.getResultType() == type)
                i++;

        return i;
    }

    /**
     *
     * @return Returns List of all results of the given type. Null if no result
     *         exists.
     */
    public @Nonnull List<DPPResult<?>> getAllResultsByType(
            DPPResult.ResultType type) {
        List<DPPResult<?>> list = new ArrayList<>();

        if (results == null)
            return list;

        for (DPPResult<?> r : results) {
            if (r.getResultType() == type) {
                list.add(r);
            }
        }

        return list;
    }

    /**
     * Returns the first result of the given type.
     * 
     * @param type
     * @return Instance of DPPResult<?> or null if no result of that type
     *         exists.
     */
    public @Nullable DPPResult<?> getFirstResultByType(
            DPPResult.ResultType type) {
        if (results == null)
            return null;

        for (DPPResult<?> r : results)
            if (r.getResultType() == type)
                return r;
        return null;
    }

    public synchronized void removeResult(int i) {
        if (results != null)
            results.remove(i);
    }

    public synchronized void removeResult(@Nonnull DPPResult<?> result) {
        // System.out.println(results.toString());
        if (results != null)
            results.remove(result);
        // System.out.println(results.toString());
    }

    public synchronized void removeResults(
            @Nonnull List<DPPResult<?>> results) {
        if (results != null)
            for (DPPResult<?> result : results)
                removeResult(result);
    }

    public synchronized void removeAllResultsByType(
            @Nonnull DPPResult.ResultType type) {
        List<DPPResult<?>> remove = new ArrayList<>();
        if (results != null) {
            for (DPPResult<?> result : results)
                if (result.getResultType() == type)
                    remove.add(result);
            removeResults(remove);
        }
    }

    /*
     * public boolean equals(ProcessedDataPoint p) { //TODO }
     */

    /**
     * @return The number of results
     */
    public int getNumberOfResults() {
        if (results == null)
            return 0;

        return results.size();
    }

    public boolean hasResults() {
        if (results == null || results.isEmpty())
            return false;
        return true;
    }
}
