package net.sf.mzmine.modules.peaklistmethods.IsotopePeakScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakListRow;

public class PeakListHandler {

  private TreeMap<Integer, PeakListRow> map;

  PeakListHandler() {
    map = new TreeMap<Integer, PeakListRow>();
  }
  
  PeakListHandler(PeakList pL){
    map = new TreeMap<Integer, PeakListRow>();
    setUp(pL);
  }

  /**
   * use this if you want to manage an existing PeakList
   * 
   * @param pL the peak list you want to manage
   */
  public void setUp(PeakList pL) {
    for (PeakListRow row : pL.getRows()) {
      map.put(row.getID(), row);
    }
  }

  /**
   * Manually add a PeakListRow
   * @param row row to be added
   */
  public void addRow(PeakListRow row) {
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
   * @return ArrayList<Integer> of all IDs of the peak list rows
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
  public PeakListRow getRowByID(int ID) {
    return map.get(ID);
  }

  /**
   * 
   * @param ID integer array of IDs
   * @return all rows with specified ids
   */
  public PeakListRow[] getRowsByID(int ID[]) {
    PeakListRow[] rows = new PeakListRow[ID.length];

    for (int i = 0; i < ID.length; i++)
      rows[i] = map.get(ID[i]);

    return rows;
  }
}
