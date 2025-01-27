/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.tools.debug_compare_featurelists;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeature;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.FeatureGroupType;
import io.github.mzmine.datamodel.features.types.FeaturesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.networking.NetworkStatsType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractSimpleToolTask;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.maths.Precision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DebugCompareFeatureListsTask extends AbstractSimpleToolTask {

  private static final Logger logger = Logger.getLogger(
      DebugCompareFeatureListsTask.class.getName());
  private final FeatureList list1;
  private final FeatureList list2;
  private final FeatureList[] featureLists;

  public DebugCompareFeatureListsTask(final MZmineProject project, final ParameterSet parameters,
      final Instant moduleCallDate, final MemoryMapStorage storage,
      final FeatureList[] featureLists) {
    super(storage, moduleCallDate, parameters);
    this.featureLists = featureLists;
    list1 = featureLists[0];
    list2 = featureLists[1];
  }


  @Override
  protected void process() {
    logger.info(
        "Starting to compare feature lists %s and %s".formatted(list1.getName(), list2.getName()));

    compareTypes(list1, list2);
    compareRowFeatures(list1, list2);
  }

  private void compareRowFeatures(final FeatureList list1, final FeatureList list2) {
    Set<DataType> ignoredTypes = Set.copyOf(
        DataTypes.getAll(FeaturesType.class, ManualAnnotationType.class,
            SpectralLibraryMatchesType.class, CompoundDatabaseMatchesType.class,
            // different community ID is not stable
            NetworkStatsType.class,
            // check separately
            IonIdentityListType.class,
            // references to other rows do not work
            FeatureGroupType.class,
            // different order
            FragmentScanNumbersType.class));

    // number of raw data files
    if (list1.getNumberOfRawDataFiles() != list2.getNumberOfRawDataFiles()) {
      logger.info("Number of raw data files is different between the two lists: %d : %d".formatted(
          list1.getNumberOfRawDataFiles(), list2.getNumberOfRawDataFiles()));
    } else {
      logger.info("Same number of raw data files: %d".formatted(list1.getNumberOfRawDataFiles()));
    }

    if (list1.getNumberOfRows() != list2.getNumberOfRows()) {
      logger.info("Number of rows is different between the two lists: %d : %d".formatted(
          list1.getNumberOfRows(), list2.getNumberOfRows()));
    } else {
      logger.info("Same number of rows: %d".formatted(list1.getNumberOfRows()));
      // sorting of rows the same?
      List<String> errors = new ArrayList<>();
      for (int i = 0; i < list1.getRows().size(); i++) {
        // n features
        var row1 = list1.getRow(i);
        var row2 = list2.getRow(i);
        if (row1.getNumberOfFeatures() != row2.getNumberOfFeatures()) {
          errors.add("Number of features is different for row i=%d. %d : %d".formatted(i,
              row1.getNumberOfFeatures(), row2.getNumberOfFeatures()));
        }
        var scans1 = row1.getAllFragmentScans();
        var scans2 = row2.getAllFragmentScans();
        if (scans1.size() != scans2.size()) {
          errors.add("Number of fragment scans is different for row i=%d. %d : %d".formatted(i,
              scans1.size(), scans2.size()));
        }

        // ion identities
        var ions1 = row1.getIonIdentities();
        var ions2 = row2.getIonIdentities();
        if (ions1 == null ^ ions2 == null) {
          errors.add(
              "Ions in one list were null and the other not null for row i=%d. %s : %s".formatted(i,
                  ions1, ions2));
        } else if (ions1 != null && ions2 != null) {
          if (ions1.size() != ions2.size()) {
            errors.add("Different number of ions in row i=%d. %d : %d".formatted(i, ions1.size(),
                ions2.size()));
          } else {
            for (int ion = 0; ion < ions1.size(); ion++) {
              var adduct1 = ions1.get(ion);
              var adduct2 = ions2.get(ion);
              if (!adduct1.equalsAdduct(adduct2.getIonType())) {
                errors.add("Ion types were different for row i=%d. %s : %s".formatted(i, adduct1,
                    adduct2));
              }
            }
          }
        }

        var matches1 = row1.getSpectralLibraryMatches();
        var matches2 = row1.getSpectralLibraryMatches();
        if (matches1.size() != matches2.size()) {
          errors.add(
              "Different number of spectral library matches in row i=%d. %d : %d".formatted(i,
                  matches1.size(), matches2.size()));
        } else {
          for (int match = 0; match < matches1.size(); match++) {
            var id1 = matches1.get(match).getEntry();
            var id2 = matches2.get(match).getEntry();
            if (!id1.equals(id2)) {
              errors.add(
                  "Spectral library matches were different for row i=%d. %s : %s".formatted(i, id1,
                      id2));
            }
          }
        }

        // row types
        for (final DataType rowType : list1.getRowTypes()) {
          if (ignoredTypes.contains(rowType)) {
            continue;
          }

          var a = row1.get(rowType);
          var b = row2.get(rowType);
          if (!isEquals(a, b)) {
            errors.add("""
                Row i=%d had error for type: %s of class %s. Values %s != %s""".formatted(i,
                rowType, rowType.getClass(), a, b));
          }
        }

        // feature types
        for (final DataType featureType : list1.getFeatureTypes()) {
          if (ignoredTypes.contains(featureType)) {
            continue;
          }
          var raws = row1.getRawDataFiles();
          for (final RawDataFile raw : raws) {
            var f1 = (ModularFeature) row1.getFeature(raw);
            var f2 = (ModularFeature) row2.getFeature(raw);
            if (f1 == null && f2 == null) {
              continue;
            } else if (f1 == null || f2 == null) { // one is null
              errors.add("One feature is null but the other is not in row i=%d".formatted(i));
              continue;
            }

            var a = f1.get(featureType);
            var b = f2.get(featureType);
            if (!isEquals(a, b)) {
              errors.add("""
                  Row i=%d raw=%s had error for feature type: %s of class %s. Values %s != %s""".formatted(
                  i, raw.getName(), featureType, featureType.getClass(), a, b));
            }
          }
        }

        if (errors.size() > 50) {
          break;
        }
      }
      if (!errors.isEmpty()) {
        logger.info("There were errors comparing rows\n" + String.join("\n", errors));
      }
    }
  }

  private static boolean isEquals(final Object a, final Object b) {
    if (a == b) {
      return true;
    } else if (a == null ^ b == null) { // one is null the other not
      return false;
    }
    return switch (a) {
      case Double va -> Precision.equalDoubleSignificance(va, (Double) b);
      case Float va -> Precision.equalFloatSignificance(va, (Float) b);
      default -> Objects.equals(a, b);
    };
  }

  private void compareTypes(final FeatureList list1, final FeatureList list2) {
    var a = new HashSet<DataType>(list1.getRowTypes());
    var b = new HashSet<DataType>(list2.getRowTypes());
    var inCommon = new HashSet<DataType>(a);
    inCommon.retainAll(b);
    a.removeAll(inCommon);
    b.removeAll(inCommon);

    logger.info("""
        \nRow types in common: %d: %s
        Unique in List1: %d: %s
        Unique in List2: %d: %s""".formatted(inCommon.size(), joinNames(inCommon), a.size(),
        joinNames(a), b.size(), joinNames(b)));

    a = new HashSet<DataType>(list1.getFeatureTypes());
    b = new HashSet<DataType>(list2.getFeatureTypes());
    inCommon = new HashSet<DataType>(a);
    inCommon.retainAll(b);
    a.removeAll(inCommon);
    b.removeAll(inCommon);

    logger.info("""
        \nFeature types in common: %d: %s
        Unique in List1: %d: %s
        Unique in List2: %d: %s""".formatted(inCommon.size(), joinNames(inCommon), a.size(),
        joinNames(a), b.size(), joinNames(b)));
  }

  private static String joinNames(final HashSet<DataType> inCommon) {
    return inCommon.stream().map(DataType::getUniqueID).collect(Collectors.joining(", "));
  }


  @Override
  public String getTaskDescription() {
    return "Debug comparing 2 feature lists";
  }
}
