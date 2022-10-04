/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.impl.SimpleDataPoint;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results.DPPResult;

/**
 * This class stores the results of DataPointProcessingTasks. It offers more functionality, e.g.
 * assigning identities.
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
   * @param dp DataPoints to convert.
   * @return Array of ProcessedDataPoints from DataPoints.
   */
  public static ProcessedDataPoint[] convert(@NotNull DataPoint[] dp) {
    ProcessedDataPoint[] pdp = new ProcessedDataPoint[dp.length];
    for (int i = 0; i < pdp.length; i++)
      pdp[i] = new ProcessedDataPoint(dp[i]);
    return pdp;
  }

  /**
   * Generates an array of ProcessedDataPoints from DataPoints.
   *
   * @return Array of ProcessedDataPoints from DataPoints.
   */
  public static ProcessedDataPoint[] convert(@NotNull double[] mz, @NotNull double[] intensity) {
    ProcessedDataPoint[] pdp = new ProcessedDataPoint[mz.length];
    for (int i = 0; i < pdp.length; i++)
      pdp[i] = new ProcessedDataPoint(mz[i], intensity[i]);
    return pdp;
  }

  public ProcessedDataPoint(@NotNull DataPoint dp) {
    super(dp);
  }
  /**
   * @param mz
   * @param intensity
   */
  public ProcessedDataPoint(double mz, double intensity) {
    super(mz, intensity);
  }

  public ProcessedDataPoint(@NotNull DataPoint dp, @NotNull DPPResult<?> result) {
    this(dp);
    addResult(result);
  }

  public ProcessedDataPoint(@NotNull DataPoint dp, @NotNull Collection<DPPResult<?>> results) {
    this(dp);
    addAllResults(results);
  }

  /**
   * Adds a single result to this data point.
   * 
   * @param result
   */
  public synchronized void addResult(@NotNull DPPResult<?> result) {
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
  public synchronized void addAllResults(@NotNull Collection<DPPResult<?>> results) {
    if (results == null)
      return;

    if (this.results == null)
      this.results = new Vector<DPPResult<?>>();

    for (DPPResult<?> result : results) {
      this.results.add(result);
    }
  }

  public synchronized void addAllResults(@NotNull DPPResult<?>[] result) {
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
   * @param i Index of the specified result
   * @return DPPResult with the given key, may be null if no result with that key exits or no result
   *         exists at all.
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
   * @return Returns List of all results of the given type. Null if no result exists.
   */
  public @NotNull List<DPPResult<?>> getAllResultsByType(DPPResult.ResultType type) {
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
   * @return Instance of DPPResult<?> or null if no result of that type exists.
   */
  public @Nullable DPPResult<?> getFirstResultByType(DPPResult.ResultType type) {
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

  public synchronized void removeResult(@NotNull DPPResult<?> result) {
    // System.out.println(results.toString());
    if (results != null)
      results.remove(result);
    // System.out.println(results.toString());
  }

  public synchronized void removeResults(@NotNull List<DPPResult<?>> results) {
    if (results != null)
      for (DPPResult<?> result : results)
        removeResult(result);
  }

  public synchronized void removeAllResultsByType(@NotNull DPPResult.ResultType type) {
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
