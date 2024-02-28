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

package io.github.mzmine.modules.dataprocessing.align_path.functions;

import io.github.mzmine.datamodel.features.FeatureListRow;
import java.util.Comparator;

public class AlignmentSorterFactory {

  public static enum SORT_MODE {

    name {

      public String toString() {
        return "name";
      }
    },
    peaks {

      public String toString() {
        return "number of peaks";
      }
    },
    rt {

      public String toString() {
        return "RT";
      }
    },
    none {

      public String toString() {
        return "nothing";
      }
    };

    public abstract String toString();
  }

  public static Comparator<FeatureListRow> getComparator(final SORT_MODE mode) {
    return getComparator(mode, true);
  }

  /**
   * Return a comparator that <b>is</b> inconsistent with equals.
   * 
   * @param mode
   * @param ascending
   * @return
   */
  public static Comparator<FeatureListRow> getComparator(final SORT_MODE mode,
      final boolean ascending) {
    switch (mode) {
      case name:
        return getNameComparator(ascending);
      case peaks:
        return getPeakCountComparator(ascending);
      case rt:
        return getDoubleValComparator(ascending, mode);
      default:
        return nullComparator();
    }
  }

  private static Comparator<FeatureListRow> getNameComparator(final boolean ascending) {
    return new Comparator<FeatureListRow>() {

      public int compare(FeatureListRow o1, FeatureListRow o2) {
        int comparison = 0;
        comparison = o1.getPreferredFeatureIdentity().getName()
            .compareToIgnoreCase(o2.getPreferredFeatureIdentity().getName());

        return ascending ? comparison : -comparison;
      }
    };
  }

  private static Comparator<FeatureListRow> getPeakCountComparator(final boolean ascending) {
    return new Comparator<FeatureListRow>() {

      public int compare(FeatureListRow o1, FeatureListRow o2) {
        int comp = (Integer) o1.getNumberOfFeatures() - (Integer) o2.getNumberOfFeatures();
        return ascending ? comp : -comp;
      }
    };
  }

  private static Comparator<FeatureListRow> getDoubleValComparator(final boolean ascending,
      final SORT_MODE mode) {
    return new Comparator<FeatureListRow>() {

      public int compare(FeatureListRow o1, FeatureListRow o2) {
        int comparison = 0;
        double val1 = 0.0;
        double val2 = 0.0;
        if (mode == SORT_MODE.rt)
          val1 = (double) o1.getAverageRT();
        if (val1 < val2) {
          comparison = -1;
        }
        if (val1 > val2) {
          comparison = 1;
        }
        return ascending ? comparison : -comparison;
      }
    };
  }

  private static Comparator<FeatureListRow> nullComparator() {
    return new Comparator<FeatureListRow>() {

      public int compare(FeatureListRow o1, FeatureListRow o2) {
        return 0;
      }
    };
  }
}
