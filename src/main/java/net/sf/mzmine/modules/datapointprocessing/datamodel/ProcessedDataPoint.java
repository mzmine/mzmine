/*
 * Copyright 2006-2018 The MZmine 2 Development Team
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

package net.sf.mzmine.modules.datapointprocessing.datamodel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.datamodel.impl.SimpleDataPoint;
import net.sf.mzmine.modules.datapointprocessing.datamodel.results.DPPResult;

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
  public static ProcessedDataPoint[] convert(DataPoint[] dp) {
    if (dp == null)
      return new ProcessedDataPoint[0];

    ProcessedDataPoint[] pdp = new ProcessedDataPoint[dp.length];
    for (int i = 0; i < pdp.length; i++)
      pdp[i] = new ProcessedDataPoint(dp[i]);
    return pdp;
  }

  public ProcessedDataPoint(DataPoint dp) {
    super(dp);
  }

  public ProcessedDataPoint(DataPoint dp, DPPResult<?> result) {
    this(dp);
    addResult(result);
  }

  public ProcessedDataPoint(DataPoint dp, Collection<DPPResult<?>> results) {
    this(dp);
    addAllResults(results);
  }

  /**
   * Adds a single result to this data point.
   * 
   * @param result
   */
  public void addResult(DPPResult<?> result) {
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
  public void addAllResults(Collection<DPPResult<?>> results) {
    if (results == null)
      return;

    if (this.results == null)
      this.results = new Vector<DPPResult<?>>();

    for (DPPResult<?> result : results) {
      this.results.add(result);
    }
  }

  public void addAllResults(DPPResult<?>[] result) {
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
  public DPPResult<?> getResult(int i) {
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
  public List<DPPResult<?>> getAllResultsByType(DPPResult.ResultType type) {
    if (results == null)
      return null;

    List<DPPResult<?>> list = new ArrayList<>();

    for (DPPResult<?> r : results) {
      if (r.getResultType() == type) {
        list.add(r);
      }
    }

    if (list.isEmpty())
      return null;

    return list;
  }
  
  /**
   * Returns the first result of the given type.
   * @param type
   * @return Instance of DPPResult<?> or null if no result of that type exists.
   */
  public DPPResult<?> getFirstResultByType(DPPResult.ResultType type){
    if (results == null)
      return null;
    
    for(DPPResult<?> r : results)
      if(r.getResultType() == type)
        return r;
    return null;
  }

  public void removeResult(int i) {
    results.remove(i);
  }
  
  public void removeResult(DPPResult<?> result) {
    results.remove(result);
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
