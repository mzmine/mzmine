package io.github.mzmine.parameters.parametertypes;

import io.github.mzmine.datamodel.FeatureStatus;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.absoluterelative.AbsoluteNRelativeInt;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;


import java.util.HashMap;
import java.util.List;


public class MinimumFeatureFilter {

  public enum OverlapResult {
    TRUE, // all requirements met
    AntiOverlap, // Features in at least one sample were not overlapping with X% of intensity
    OutOfRTRange, // Features in at least one sample were out of RT range
    BelowMinSamples; // not enough overlapping samples
  }

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
  public MinimumFeatureFilter(MZmineProject project, RawDataFile[] raw, String groupParam,
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
  public boolean filterMinFeatures(final RawDataFile raw[], FeatureListRow row) {
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
      if (!minFInSamples.checkGreaterEqualMax(raw.length, n))
        return false;
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero())
      return true;

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<String, MutableInt>();
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
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value))
        return true;
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
   * height>minHeight)
   * 
   * @param raw
   * @param row
   * @param row2
   * @return
   */
  public OverlapResult filterMinFeaturesOverlap(final RawDataFile raw[], FeatureListRow row,
      FeatureListRow row2) {
    return filterMinFeaturesOverlap(raw, row, row2, null);
  }

  /**
   * Check for overlapping features in two rows (features in the same RawDataFile with
   * height>minHeight and within rtTolerance)
   * 
   * @param raw
   * @param row
   * @param row2
   * @param rtTolerance
   * @return
   */
  public OverlapResult filterMinFeaturesOverlap(final RawDataFile raw[], FeatureListRow row,
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
            if (checkIntensityOverlap(a, b, minIPercOverlap, minFeatureHeight))
              n++;
            else
              result = OverlapResult.AntiOverlap;
          } else {
            result = OverlapResult.OutOfRTRange;
          }
          // directly return on stric rules
          if (!result.equals(OverlapResult.TRUE) && strictRules)
            return result;
        }
      }
      // stop if <n
      if (!minFInSamples.checkGreaterEqualMax(raw.length, n))
        return OverlapResult.BelowMinSamples;
    }

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero())
      return OverlapResult.TRUE;

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<String, MutableInt>();
    for (RawDataFile file : raw) {
      Feature a = row.getFeature(file);
      Feature b = row2.getFeature(file);
      if (checkFeatureQuality(a) && checkFeatureQuality(b)) {
        if (checkRTTol(rtTolerance, a, b)) {
          if (checkIntensityOverlap(a, b, minIPercOverlap, minFeatureHeight)) {
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

        if (!result.equals(OverlapResult.TRUE) && strictRules)
          return result;
      }
    }
    // only needs a minimum number of features in at least one sample group
    // only go on if feature was present in X % of the samples
    for (MutableInt v : counter.values()) {
      // at least one
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value))
        return OverlapResult.TRUE;
    }
    // no fit
    return OverlapResult.BelowMinSamples;
  }


  private boolean checkRTTol(RTTolerance rtTolerance, Feature a, Feature b) {
    return (rtTolerance == null || rtTolerance.checkWithinTolerance(a.getRT(), b.getRT()));
  }

  private boolean checkHeight(Feature a, Feature b) {
    return a != null && a.getHeight() >= minFeatureHeight && b != null
        && b.getHeight() >= minFeatureHeight;
  }

  /**
   * Intensity overlap
   * 
   * @param a
   * @param b
   * @param minIPercOverlap
   * @param minHeight
   * @return
   */
  public boolean checkIntensityOverlap(Feature a, Feature b, double minIPercOverlap,
      double minHeight) {
    if (minIPercOverlap < 0.00001)
      return true;
    // check more
    Feature small = a;
    Feature big = b;
    if (a.getHeight() > b.getHeight()) {
      small = b;
      big = a;
    }
    if (big.getRawDataPointsRTRange().encloses(small.getRawDataPointsRTRange()))
      return true;

    double start = 0, end = 0;
    // at 5% height
    for (int sn : big.getScanNumbers()) {
      double intensity = big.getDataPoint(sn).getIntensity();
      if (intensity >= big.getHeight() * 0.05 || intensity >= minHeight) {
        if (start == 0)
          start = big.getRawDataFile().getScan(sn).getRetentionTime();
        else
          end = big.getRawDataFile().getScan(sn).getRetentionTime();
      }
    }
    // check smaller
    double overlap = 0, sum = 0;
    for (int sn : small.getScanNumbers()) {
      double rt = small.getRawDataFile().getScan(sn).getRetentionTime();
      double intensity = small.getDataPoint(sn).getIntensity();

      if (rt >= start && rt <= end) {
        overlap += intensity;
      }
      sum += intensity;
    }

    if (overlap == 0)
      return false;
    return (overlap / sum) >= minIPercOverlap;
  }

  /**
   * Checks for any other algorithm.
   * 
   * @param all the total of all raw data files
   * @param raw all positive raw data files
   * @return
   */
  public boolean filterMinFeatures(RawDataFile[] all, List<RawDataFile> raw) {
    // filter min samples in all
    if (minFInSamples.isGreaterZero()
        && !minFInSamples.checkGreaterEqualMax(all.length, raw.size()))
      return false;

    // short cut
    if (!filterGroups || sgroupSize == null || !minFInGroups.isGreaterZero())
      return true;

    // is present in X % samples of a sample set?
    // count sample in groups (no feature in a sample group->no occurrence in map)
    HashMap<String, MutableInt> counter = new HashMap<String, MutableInt>();
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
      if (minFInGroups.checkGreaterEqualMax(v.total, v.value))
        return true;
    }
    // no fit
    return false;
  }

  /**
   * gets called to initialise group variables
   * 
   */
  public void setSampleGroups(MZmineProject project, RawDataFile[] raw, String groupingParameter) {
    this.project = project;
    if (groupingParameter == null || groupingParameter.length() == 0) {
      this.sgroupSize = null;
      setGroupFilterEnabled(false);
    } else {
      sgroupSize = new HashMap<String, Integer>();
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
        if (val + 1 > max)
          max = val + 1;
      }

      setGroupFilterEnabled(!sgroupSize.isEmpty());
    }
  }

  /**
   * 
   * @param file
   * @return sample group value of raw file
   */
  public String sgroupOf(RawDataFile file) {
    return String.valueOf(project.getParameterValue(sgroupPara, file));
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


}
