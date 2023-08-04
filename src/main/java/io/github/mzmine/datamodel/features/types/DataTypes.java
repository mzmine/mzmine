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

package io.github.mzmine.datamodel.features.types;

import com.google.common.reflect.ClassPath;
import io.github.mzmine.datamodel.MobilityType;
import io.github.mzmine.datamodel.features.types.alignment.AlignmentMainType;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.DatasetIdType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSClusterUrlType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSLibraryUrlType;
import io.github.mzmine.datamodel.features.types.annotations.GNPSNetworkUrlType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.LipidMatchListType;
import io.github.mzmine.datamodel.features.types.annotations.ManualAnnotationType;
import io.github.mzmine.datamodel.features.types.annotations.MasstUrlType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SpectralLibraryMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.SplashType;
import io.github.mzmine.datamodel.features.types.annotations.UsiType;
import io.github.mzmine.datamodel.features.types.annotations.formula.ConsensusFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.formula.SimpleFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonIdentityListType;
import io.github.mzmine.datamodel.features.types.modifiers.AnnotationType;
import io.github.mzmine.datamodel.features.types.modifiers.SubColumnsFactory;
import io.github.mzmine.datamodel.features.types.networking.NetworkStatsType;
import io.github.mzmine.datamodel.features.types.numbers.AreaType;
import io.github.mzmine.datamodel.features.types.numbers.AsymmetryFactorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.FragmentScanNumbersType;
import io.github.mzmine.datamodel.features.types.numbers.FwhmType;
import io.github.mzmine.datamodel.features.types.numbers.HeightType;
import io.github.mzmine.datamodel.features.types.numbers.IDType;
import io.github.mzmine.datamodel.features.types.numbers.IntensityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZRangeType;
import io.github.mzmine.datamodel.features.types.numbers.MZType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityRangeType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTRangeType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.TailingFactorType;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Hold single instance of all data types
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@SuppressWarnings("rawtypes")
public class DataTypes {

  private static final Logger logger = Logger.getLogger(DataTypes.class.getName());

  // map class to instance
  private static final HashMap<Class<? extends DataType>, DataType> TYPES = new HashMap<>();
  // map unique ID to instance
  private static final HashMap<String, DataType<?>> map = new HashMap<>();

  static {
    try {
      ClassPath classPath = ClassPath.from(DataType.class.getClassLoader());
      classPath.getTopLevelClassesRecursive("io.github.mzmine.datamodel.features.types")
          .forEach(classInfo -> {
            try {
              Object o = classInfo.load().getDeclaredConstructor().newInstance();
              if (o instanceof DataType dt) {
                var value = map.put(dt.getUniqueID(), dt);
                if (value != null) {
                  throw new IllegalStateException(
                      "FATAL: Multiple data types with unique ID " + dt.getUniqueID() + "\n"
                      + value.getClass().getName() + "\n" + dt.getClass().getName());
                }
                TYPES.put(dt.getClass(), dt);
              }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
              //               can go silent
              //              logger.log(Level.INFO, e.getMessage(), e);
            }
          });
    } catch (IOException e) {
      logger.severe("Cannot instantiate classPath for DataType.class. Cannot load projects.");
    }
  }

  private DataTypes() {
  }

  @Nullable
  public static DataType<?> getTypeForId(String uniqueId) {
    return map.get(uniqueId);
  }

  public static <T> DataType<T> get(DataType<T> instance) {
    return get((Class) instance.getClass());
  }

  public static <T> DataType<T> get(Class<? extends DataType<T>> clazz) {
    return TYPES.get(clazz);
  }

  /**
   * @return A collection of all data type instances.
   */
  public static Collection<DataType> getInstances() {
    return TYPES.values();
  }

  public static Collection<Class<? extends DataType>> getClasses() {
    return TYPES.keySet();
  }

  public static boolean isMainAnnotation(DataType<?> dataType) {
    return dataType instanceof AnnotationType && dataType instanceof SubColumnsFactory;
  }


  /**
   * @return order of the most important data types
   */
  @NotNull
  public static Map<DataType, Integer> getDataTypeOrderFeatureTable() {
    List<Class> priority = List.of(IDType.class, DetectionType.class, MZType.class,
        PrecursorMZType.class, NeutralMassType.class, MZRangeType.class, RTType.class,
        RTRangeType.class, FwhmType.class, MobilityType.class, MobilityRangeType.class,
        CCSType.class, CCSRelativeErrorType.class, MobilityUnitType.class, AreaType.class,
        HeightType.class, IntensityRangeType.class, ChargeType.class, FragmentScanNumbersType.class,
        IsotopePatternType.class, TailingFactorType.class, AsymmetryFactorType.class,
        // annotation specific
        CompoundNameType.class, DatasetIdType.class, FormulaType.class, SmilesStructureType.class,
        InChIStructureType.class, InChIKeyStructureType.class, SplashType.class, UsiType.class,
        MasstUrlType.class, GNPSLibraryUrlType.class, GNPSNetworkUrlType.class,
        GNPSClusterUrlType.class, CompoundDatabaseMatchesType.class, CommentType.class,
        // combined types with sub columns
        AlignmentMainType.class, NetworkStatsType.class, IonIdentityListType.class,
        SpectralLibraryMatchesType.class, CompoundDatabaseMatchesType.class,
        LipidMatchListType.class, ConsensusFormulaListType.class, SimpleFormulaListType.class,
        FormulaListType.class, ManualAnnotationType.class,
        // graphical columns
        FeatureShapeType.class, FeatureShapeMobilogramType.class,
        FeatureShapeIonMobilityRetentionTimeHeatMapType.class, ImageType.class);

    Map<DataType, Integer> prioMap = new HashMap<>(priority.size());
    int i = 0;
    for (final Class aClass : priority) {
      prioMap.put(DataTypes.get(aClass), i++);
    }
    return prioMap;
  }

  public static List<DataType> getList(final Class... classes) {
    return Arrays.stream(classes).map(c -> TYPES.get(c)).toList();
  }
}
