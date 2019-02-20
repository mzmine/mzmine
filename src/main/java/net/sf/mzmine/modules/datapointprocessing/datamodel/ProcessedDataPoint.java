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

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;
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
  TreeMap<String, DPPResult<?>> results;

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
      results = new TreeMap<String, DPPResult<?>>();

    results.put(result.getName(), result);
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
      this.results = new TreeMap<String, DPPResult<?>>();

    for (DPPResult<?> result : results) {
      if (result.getName() == null || result.getName().equals(""))
        continue;
      this.results.put(result.getName(), result);
    }
  }
  
  public void addAllResults(DPPResult[] result) {
    if (result == null)
      return;

    if (results == null)
      results = new TreeMap<String, DPPResult<?>>();
    
    for(DPPResult<?> r : result) {
      results.put(r.getName(), r);
    }
  }

  /**
   * 
   * @param name Key of the specified result
   * @return DPPResult with the given key, may be null if no result with that key exits or no result
   *         exists at all.
   */
  public DPPResult<?> getResult(String name) {
    if (results == null)
      return null;
    return results.get(name);
  }

  /**
   * Specifies if a result with the given key exists.
   * 
   * @param key
   * @return
   */
  public boolean resultKeyExists(String key) {
    if (results.containsKey(key))
      return true;
    return false;
  }

  /**
   *
   * @return Returns an array of all keys in this map.
   */
  public String[] getAllResultKeys() {
    if (results == null)
      return new String[0];

    return results.keySet().toArray(new String[0]);
  }

  public void removeResult(String key) {
    results.remove(key);
  }
  /*
   * public boolean equals(ProcessedDataPoint p) { //TODO }
   */

  /**
   * 
   * @param key
   * @return The number of entries with the given key.
   */
  public int getNumberOfKeyEntries(String key) {
    int i = 0;
    if(results == null)
      return i;
    
    for(String k : results.keySet()) {
      if(k.contains(key))
        i++;
    }
    return i;
  }
  
  public boolean hasResults() {
    if(results == null || results.isEmpty())
      return false;
    return true;
  }
}
