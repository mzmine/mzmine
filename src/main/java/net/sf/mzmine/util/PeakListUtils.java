package net.sf.mzmine.util;

import net.sf.mzmine.datamodel.PeakList;
import net.sf.mzmine.datamodel.PeakList.PeakListAppliedMethod;

public class PeakListUtils {
  /**
   * Copies the PeakListAppliedMethods from <b>source</b> to <b>target</b>
   * @param source The source peak list.
   * @param target the target peak list.
   */
  public static void copyPeakListAppliedMethods(PeakList source, PeakList target) {
    for (PeakListAppliedMethod proc : source.getAppliedMethods()) {
      target.addDescriptionOfAppliedTask(proc);
    }
  }
}
