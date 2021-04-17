package io.github.mzmine.datamodel.features.correlation;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import com.google.common.util.concurrent.AtomicDouble;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.RowGroupList;
import io.github.mzmine.parameters.parametertypes.MinimumFeatureFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import javafx.collections.ObservableList;


/**
 * Correlation of row 2 row
 * 
 * @author Robin Schmid
 *
 */
public class R2RCorrMap extends R2RMap<R2RCorrelationData> {
  private static final Logger LOG = Logger.getLogger(R2RCorrMap.class.getName());
  private static final long serialVersionUID = 1L;

  private RTTolerance rtTolerance;
  private MinimumFeatureFilter minFFilter;

  public R2RCorrMap(RTTolerance rtTolerance, MinimumFeatureFilter minFFilter) {
    this.rtTolerance = rtTolerance;
    this.minFFilter = minFFilter;
  }

  public RTTolerance getRtTolerance() {
    return rtTolerance;
  }

  public MinimumFeatureFilter getMinFeatureFilter() {
    return minFFilter;
  }

  public Stream<R2RFullCorrelationData> streamCorrData() {
    return values().stream().filter(R2RFullCorrelationData.class::isInstance)
        .map(R2RFullCorrelationData.class::cast);
  }

  public Stream<java.util.Map.Entry<Integer, R2RCorrelationData>> streamCorrDataEntries() {
    return entrySet().stream().filter(e -> e.getValue() instanceof R2RFullCorrelationData);
  }


  /**
   * Stream of all elements sorted by their correlation score
   * 
   * @return
   */
  public Stream<Entry<Integer, R2RCorrelationData>> streamAllSortedByR() {
    return this.entrySet().stream().sorted((ea, eb) -> {
      R2RCorrelationData a = ea.getValue();
      R2RCorrelationData b = eb.getValue();
      return Double.compare(a.getAvgShapeR(), b.getAvgShapeR());
    });
  }

  /**
   * Create list of correlated rows TODO delete
   * 
   * @param flist feature list
   * @param stageProgress can be null. points to the progress
   * 
   * @return
   */
  public RowGroupList createCorrGroups(FeatureList flist, AtomicDouble stageProgress) {
    LOG.info("Corr: Creating correlation groups");

    try {
      RowGroupList groups = new RowGroupList();
      HashMap<FeatureListRow, CorrelationRowGroup> used = new HashMap<>();

      ObservableList<RawDataFile> raw = flist.getRawDataFiles();
      // add all connections
      Iterator<Entry<Integer, R2RCorrelationData>> entries = this.entrySet().iterator();
      int c = 0;
      while (entries.hasNext()) {
        Entry<Integer, R2RCorrelationData> e = entries.next();

        R2RCorrelationData r2r = e.getValue();
        FeatureListRow rowA = r2r.getRowA();
        FeatureListRow rowB = r2r.getRowB();
        if (r2r instanceof R2RFullCorrelationData data) {
          // already added?
          CorrelationRowGroup group = used.get(rowA);
          CorrelationRowGroup group2 = used.get(rowB);
          // merge groups if both present
          if (group != null && group2 != null && group.getGroupID() != group2.getGroupID()) {
            // copy all to group1 and remove g2
            for(FeatureListRow r : group2.getRows()) {
              group.add(r);
              used.put(r, group);
            }
            groups.remove(group2);
          } else if (group == null && group2 == null) {
            // create new group with both rows
            group = new CorrelationRowGroup(raw, groups.size());
            group.addAll(rowA, rowB);
            groups.add(group);
            // mark as used
            used.put(rowA, group);
            used.put(rowB, group);
          } else if (group2 == null) {
            group.add(rowB);
            used.put(rowB, group);
          } else if (group == null) {
            group2.add(rowA);
            used.put(rowA, group2);
          }
        }
        // report back progress
        c++;
        if (stageProgress != null)
          stageProgress.addAndGet(1d / this.size());
      }
      // sort by retention time
      groups.sortByRT();

      // reset index
      for (int i = 0; i < groups.size(); i++)
        groups.get(i).setGroupID(i);

      LOG.info("Corr: DONE: Creating correlation groups");
      return groups;
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Error while creating groups", e);
      return null;
    }
  }


  /**
   * The two row IDs the first is always the lower one
   *
   * @param key
   * @return
   */
//  public static int[] toKeyIDs(String key) {
//    return Arrays.stream(key.split(",")).mapToInt(Integer::parseInt).toArray();
//  }
}
