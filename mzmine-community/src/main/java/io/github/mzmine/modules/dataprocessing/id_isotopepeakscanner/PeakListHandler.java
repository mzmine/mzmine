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

package io.github.mzmine.modules.dataprocessing.id_isotopepeakscanner;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;


/**
 * This class can be used to handle feature lists by ID rather than row indices. Set up via the
 * constructor by passing a feature list or via setUp(PeakList) Rows can also be added manually, for
 * example if you want to create a result feature list that does not contain duplicates. Since this
 * class uses a tree map the results will be in order and duplicates will be overwritten, which is
 * why it has the method containsID to check.
 *
 * @author Steffen Heuckeroth steffen.heuckeroth@gmx.de / s_heuc03@uni-muenster.de
 *
 */
public class PeakListHandler {

  private TreeMap<Integer, FeatureListRow> map;

  public PeakListHandler() {
    map = new TreeMap<Integer, FeatureListRow>();
  }

  public PeakListHandler(FeatureList pL) {
    map = new TreeMap<Integer, FeatureListRow>();
    setUp(pL);
  }

  /**
   * use this if you want to manage an existing PeakList
   * 
   * @param pL the feature list you want to manage
   */
  public void setUp(FeatureList pL) {
    for (FeatureListRow row : pL.getRows()) {
      map.put(row.getID(), row);
    }
  }

  /**
   * Manually add a PeakListRow
   * 
   * @param row row to be added
   */
  public void addRow(FeatureListRow row) {
    map.put(row.getID(), row);
  }

  /**
   * 
   * @return number of rows handled with plh
   */
  public int size() {
    return map.size();
  }

  /**
   * 
   * @param ID ID to check for
   * @return true if contained, false if not
   */
  public boolean containsID(int ID) {
    return map.containsKey(ID);
  }

  /**
   * 
   * @return ArrayList<Integer> of all IDs of the feature list rows
   */
  public ArrayList<Integer> getAllKeys() {
    Set<Integer> set = map.keySet();
    ArrayList<Integer> list = new ArrayList<Integer>(set);

    return list;
  }

  /**
   * 
   * @param ID ID of the row you want
   * @return Row with specified ID
   */
  public FeatureListRow getRowByID(int ID) {
    return map.get(ID);
  }

  /**
   * 
   * @param ID integer array of IDs
   * @return all rows with specified ids
   */
  public FeatureListRow[] getRowsByID(int ID[]) {
    FeatureListRow[] rows = new FeatureListRow[ID.length];

    for (int i = 0; i < ID.length; i++)
      rows[i] = map.get(ID[i]);

    return rows;
  }
}
