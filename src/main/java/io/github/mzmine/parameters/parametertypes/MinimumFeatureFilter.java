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

package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.CachedFeatureDataAccess;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import java.util.HashMap;
import java.util.List;
import org.jetbrains.annotations.Nullable;


public class MinimumFeatureFilter {

  /**
   * Minimum percentage of samples (in group if useGroup) that have to contain a feature
   */
  private final AbsoluteNRelativeInt minFInSamples;
  private final AbsoluteNRelativeInt minFInGroups;
  private final double minFeatureHeight;
  // sample group size
  private UserParameter<?, ?> sgroupPara;
  private HashMap<String, Integer> sgroupSize;
  private boolean filterGroups = false;
  private MZmineProject project;
  // percent of intensity of the smaller to overlap the larger feature
  private double minIPercOverlap;
  // do not accept that feature in one raw file is out of rtRange or minIPercOverlap
  private boolean strictRules = false;
  private boolean excludeEstimatedFeatures = false;

  public MinimumFeatureFilter(AbsoluteNRelativeInt minFInSamples, AbsoluteNRelativeInt minFInGroups,
      double minFeatureHeight, double minIPercOverlap, boolean excludeEstimatedFeatures) {
    this.minFInSamples = minFInSamples;
    this.minFInGroups = minFInGroups;
    this.minFeatureHeight = minFeatureHeight;
    this.minIPercOverlap = minIPercOverlap;
    this.excludeEstimatedFeatures = excludeEstimatedFeatures;
  }


  /**
   * Directly sets up the groups and group sizes
   *
   * @param project
   * @param raw
   * @param groupParam
   * @param minFInSamples
   * @param minFInGroups
   * @param minFeatureHeight
   */
  public MinimumFeatureFilter(MZmineProject project, List<RawDataFile> raw, String groupParam,
      AbsoluteNRelativeInt minFInSamples, AbsoluteNRelativeInt minFInGroups,
      double minFeatureHeight, double minIPercOverlap, boolean excludeEstimatedFeatures) {
    this(minFInSamples, minFInGroups, minFeatureHeight, minIPercOverlap, excludeEstimatedFeatures);
    this.project = project;
    setSampleGroups(project, raw, groupParam);
  }

  public void setGroupFilterEnabled(boolean state) {
    filterGroups = state;
  }

  public boolean isExcludeEstimatedFeatures() {
    return excludeEstimatedFeatures;
  }

  /**
   * only keep rows which contain features in at least X % samples in a set called before starting
   * row processing
   *
   * @param raw
   * @param row
   * @return
   */
  public boolean filterMinFeatures(final List<RawDataFile> raw, FeatureListRow row) {
    // filter min samples in all
    if (minFInSamples.isGreaterZero()) {
      int n = 0;
      for (RawDataFile file : raw) {
        Feature f = row.getFeature(file);
        if (checkFeatureQuality(f)) {
          n++;
        }
      }
      // stop if <n
      if (!minFInSamples.checkGreaterEqualMax(raw.size(), n)) {
        return false;
      }
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero()) {
      return true;
    }

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<>();
    for (RawDataFile file : raw) {
      Feature f = row.getFeature(file);
      if (checkFeatureQuality(f)) {
        String sgroup = sgroupOf(file);

        MutableInt count = counter.get(sgroup);
        if (count == null) {
          // new counter with total N for group size
          counter.put(sgroup, new MutableInt(sgroupSize.get(sgroup)));
        } else {
          count.increment();
        }
      }
    }
    // only needs a minimum number of features in at least one sample group
    // only go on if feature was present in X % of the samples
    for (MutableInt v : counter.values()) {
      // at least one
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value)) {
        return true;
      }
    }
    // no fit
    return false;
  }

  private boolean checkFeatureQuality(Feature f) {
    return f != null && f.getHeight() >= minFeatureHeight && filterEstimated(f);
  }

  private boolean filterEstimated(Feature f) {
    return f != null
           && (!excludeEstimatedFeatures || !f.getFeatureStatus().equals(FeatureStatus.ESTIMATED));
  }

  /**
   * Check for overlapping features in two rows (features in the same RawDataFile with
   * height>minHeight and within rtTolerance)
   *
   * @param data        Optional preloaded feature data for large scale comparison of features
   * @param raw
   * @param row
   * @param row2
   * @param rtTolerance
   * @return
   */
  public OverlapResult filterMinFeaturesOverlap(@Nullable CachedFeatureDataAccess data,
      final List<RawDataFile> raw, FeatureListRow row,
      FeatureListRow row2, RTTolerance rtTolerance) {
    OverlapResult result = OverlapResult.TRUE;
    // filter min samples in all
    if (minFInSamples.isGreaterZero()) {
      int n = 0;
      for (RawDataFile file : raw) {
        Feature a = row.getFeature(file);
        Feature b = row2.getFeature(file);
        if (checkFeatureQuality(a) && checkFeatureQuality(b)) {
          if (checkRTTol(rtTolerance, a, b)) {
            if (checkIntensityOverlap(data, a, b, minIPercOverlap, minFeatureHeight)) {
              n++;
            } else {
              result = OverlapResult.AntiOverlap;
            }
          } else {
            result = OverlapResult.OutOfRTRange;
          }
          // directly return on stric rules
          if (!result.equals(OverlapResult.TRUE) && strictRules) {
            return result;
          }
        }
      }
      // stop if <n
      if (!minFInSamples.checkGreaterEqualMax(raw.size(), n)) {
        return OverlapResult.BelowMinSamples;
      }
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero()) {
      return OverlapResult.TRUE;
    }

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<>();
    for (RawDataFile file : raw) {
      Feature a = row.getFeature(file);
      Feature b = row2.getFeature(file);
      if (checkFeatureQuality(a) && checkFeatureQuality(b)) {
        if (checkRTTol(rtTolerance, a, b)) {
          if (checkIntensityOverlap(data, a, b, minIPercOverlap, minFeatureHeight)) {
            String sgroup = sgroupOf(file);

            MutableInt count = counter.get(sgroup);
            if (count == null) {
              // new counter with total N for group size
              counter.put(sgroup, new MutableInt(sgroupSize.get(sgroup)));
            } else {
              count.increment();
            }
          } else {
            result = OverlapResult.AntiOverlap;
          }
        } else {
          result = OverlapResult.OutOfRTRange;
        }

        if (!result.equals(OverlapResult.TRUE) && strictRules) {
          return result;
        }
      }
    }
    // only needs a minimum number of features in at least one sample group
    // only go on if feature was present in X % of the samples
    for (MutableInt v : counter.values()) {
      // at least one
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value)) {
        return OverlapResult.TRUE;
      }
    }
    // no fit
    return OverlapResult.BelowMinSamples;
  }

  private boolean checkRTTol(RTTolerance rtTolerance, Feature a, Feature b) {
    return (rtTolerance == null || rtTolerance.checkWithinTolerance(a.getRT(), b.getRT()));
  }

  /**
   * Intensity overlap
   *
   * @param data            optional preloaded feature data for large scale data access. null to
   *                        load data directly from features
   * @param a
   * @param b
   * @param minIPercOverlap
   * @param minHeight
   * @return
   */
  public boolean checkIntensityOverlap(
      CachedFeatureDataAccess data, Feature a, Feature b,
      double minIPercOverlap,
      double minHeight) {
    if (minIPercOverlap < 0.00001) {
      return true;
    }
    // check more
    Feature small = a;
    Feature big = b;
    if (a.getHeight() > b.getHeight()) {
      small = b;
      big = a;
    }
    if (big.getRawDataPointsRTRange().encloses(small.getRawDataPointsRTRange())) {
      return true;
    }

    double start = -1;
    double end = 0;

    List<Scan> bigScans = big.getScanNumbers();
    List<Scan> smallScans = small.getScanNumbers();
    double[] bigIntensities;
    double[] smallIntensities;
    if (data != null) {
      // get preloaded
      bigIntensities = data.getIntensityValues(big);
      smallIntensities = data.getIntensityValues(small);
    } else {
      // load from features
      bigIntensities = big.getFeatureData()
          .getIntensityValues(new double[big.getNumberOfDataPoints()]);
      smallIntensities = small.getFeatureData()
          .getIntensityValues(new double[small.getNumberOfDataPoints()]);
    }
    // at 5% height
    for (int i = 0; i < big.getNumberOfDataPoints(); i++) {
      double intensity = bigIntensities[i];
      Scan sn = bigScans.get(i);
      if (intensity >= big.getHeight() * 0.05 || intensity >= minHeight) {
        if (start <= 0) {
          start = sn.getRetentionTime();
        } else {
          end = sn.getRetentionTime();
        }
      }
    }
    // check smaller
    double overlap = 0, sum = 0;
    for (int i = 0; i < small.getNumberOfDataPoints(); i++) {
      double intensity = smallIntensities[i];
      Scan sn = smallScans.get(i);
      double rt = sn.getRetentionTime();

      if (rt >= start && rt <= end) {
        overlap += intensity;
      }
      sum += intensity;
    }

    if (overlap == 0) {
      return false;
    }
    return (overlap / sum) >= minIPercOverlap;
  }

  /**
   * Checks for any other algorithm.
   *
   * @param all the total of all raw data files
   * @param raw all positive raw data files (with feature)
   * @return true if all criteria are met, false otherwise
   */
  public boolean filterMinFeatures(List<RawDataFile> all, List<RawDataFile> raw) {
    // filter min samples in all
    if (minFInSamples.isGreaterZero()
        && !minFInSamples.checkGreaterEqualMax(all.size(), raw.size())) {
      return false;
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero()) {
      return true;
    }

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<>();
    for (RawDataFile file : raw) {
      String sgroup = sgroupOf(file);

      MutableInt count = counter.get(sgroup);
      if (count == null) {
        // new counter with total N for group size
        counter.put(sgroup, new MutableInt(sgroupSize.get(sgroup)));
      } else {
        count.increment();
      }
    }
    // only needs a minimum number of features in at least one sample group
    // only go on if feature was present in X % of the samples
    for (MutableInt v : counter.values()) {
      // at least one
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value)) {
        return true;
      }
    }
    // no fit
    return false;
  }

  /**
   * gets called to initialise group variables
   */
  public void setSampleGroups(MZmineProject project, List<RawDataFile> raw,
      String groupingParameter) {
    this.project = project;
    if (groupingParameter == null || groupingParameter.length() == 0) {
      this.sgroupSize = null;
      setGroupFilterEnabled(false);
    } else {
      sgroupSize = new HashMap<>();
      UserParameter<?, ?> params[] = project.getParameters();
      for (UserParameter<?, ?> p : params) {
        if (groupingParameter.equals(p.getName())) {
          // save parameter for sample groups
          sgroupPara = p;
          break;
        }
      }
      int max = 0;
      // calc size of sample groups
      for (RawDataFile file : raw) {
        String parameterValue = sgroupOf(file);

        Integer v = sgroupSize.get(parameterValue);
        int val = v == null ? 0 : v;
        sgroupSize.put(parameterValue, val + 1);
        if (val + 1 > max) {
          max = val + 1;
        }
      }

      setGroupFilterEnabled(!sgroupSize.isEmpty());
    }
  }

  /**
   * @param file
   * @return sample group value of raw file
   */
  public String sgroupOf(RawDataFile file) {
    return String.valueOf(project.getParameterValue(sgroupPara, file));
  }

  /**
   * Size map
   *
   * @return
   */
  public HashMap<String, Integer> getGroupSizeMap() {
    return sgroupSize;
  }

  /**
   * Group parameter
   *
   * @return
   */
  public UserParameter<?, ?> getGroupParam() {
    return sgroupPara;
  }

  public enum OverlapResult {
    TRUE, // all requirements met
    AntiOverlap, // Features in at least one sample were not overlapping with X% of intensity
    OutOfRTRange, // Features in at least one sample were out of RT range
    BelowMinSamples; // not enough overlapping samples
  }

  class MutableInt {

    int total;
    int value = 1; // note that we start at 1 since we're counting

    MutableInt(int total) {
      this.total = total;
    }

    public void increment() {
      ++value;
    }

    public int get() {
      return value;
    }
  }


}
