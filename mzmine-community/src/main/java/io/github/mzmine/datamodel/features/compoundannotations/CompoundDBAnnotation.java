/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.features.compoundannotations;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.abstr.UrlShortName;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseMatchInfoType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.Structure2dUrlType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.Structure3dUrlType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.CCSType;
import io.github.mzmine.datamodel.features.types.numbers.MobilityType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.RTType;
import io.github.mzmine.datamodel.features.types.numbers.RtRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.scores.CompoundAnnotationScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_ion_identity_networking.ionidnetworking.IonNetworkLibrary;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.PercentTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.RTTolerance;
import io.github.mzmine.parameters.parametertypes.tolerances.mobilitytolerance.MobilityTolerance;
import io.github.mzmine.util.FeatureListUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.MathUtils;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

public interface CompoundDBAnnotation extends Cloneable, FeatureAnnotation,
    Comparable<CompoundDBAnnotation> {

  Logger logger = Logger.getLogger(CompoundDBAnnotation.class.getName());

  String XML_ELEMENT_OLD = "compound_db_annotation";
  String XML_TYPE_ATTRIBUTE_OLD = "annotationtype";
  String XML_NUM_ENTRIES_ATTR = "entries";

  @NotNull
  static List<CompoundDBAnnotation> buildCompoundsWithAdducts(
      CompoundDBAnnotation neutralAnnotation, IonNetworkLibrary library) {
    final List<CompoundDBAnnotation> annotations = new ArrayList<>();
    for (IonType adduct : library.getAllAdducts()) {
      if (adduct.isUndefinedAdduct() || adduct.isUndefinedAdductParent() || adduct.getName()
          .contains("?")) {
        continue;
      }
      try {
        annotations.add(neutralAnnotation.ionize(adduct));
      } catch (IllegalStateException e) {
        // do not log the full stack trace as this is expected in many cases
        logger.log(Level.WARNING, e.getMessage());
      }
    }

    return annotations;
  }

  /**
   * @param baseAnnotation The annotation to check.
   * @param useIonLibrary  true if an ion library shall be used later on to ionise the
   *                       formula/smiles/neutral mass.
   * @return True if the baseAnnotation contains a precursor m/z and useIonLibrary is false. Also
   * true if useIonLibrary is true and the annotation contains a smiles, a formula or a neutral
   * mass.
   */
  static boolean isBaseAnnotationValid(CompoundDBAnnotation baseAnnotation, boolean useIonLibrary) {
    if (baseAnnotation.getPrecursorMZ() != null && !useIonLibrary) {
      return true;
    } else {
      return useIonLibrary && (baseAnnotation.get(NeutralMassType.class) != null
                               || baseAnnotation.getFormula() != null
                               || baseAnnotation.getSmiles() != null);
    }
  }

  /**
   * Calculates the m/z for a given adduct.
   *
   * @param annotation
   * @param adduct
   * @return
   * @throws CannotDetermineMassException
   */
  static double calcMzForAdduct(@NotNull CompoundDBAnnotation annotation, @NotNull IonType adduct)
      throws CannotDetermineMassException {

    Double neutralMass = annotation.get(NeutralMassType.class);
    if (neutralMass == null) {
      // try to calc the neutral mass and keep it for subsequent calls.
      neutralMass = CompoundDBAnnotation.calcNeutralMass(annotation);
      annotation.put(NeutralMassType.class, neutralMass);
    }

    if (neutralMass != null) {
      return adduct.getMZ(neutralMass);
    }

    throw new CannotDetermineMassException(annotation);
  }

  /**
   * Calculates the neutral mass of the given annotation from adduct information, smiles, or
   * formula.
   *
   * @return The neutral mass or null.
   */
  static Double calcNeutralMass(CompoundDBAnnotation annotation) {
    final IonType currentAdduct = annotation.get(IonTypeType.class);
    if (currentAdduct != null && annotation.getPrecursorMZ() != null) {
      return currentAdduct.getMass(annotation.getPrecursorMZ());
    }

    final String formulaString = annotation.getFormula();
    final String smiles = annotation.getSmiles();
    final IMolecularFormula neutralFormula =
        formulaString != null ? FormulaUtils.neutralizeFormulaWithHydrogen(formulaString)
            : FormulaUtils.neutralizeFormulaWithHydrogen(FormulaUtils.getFormulaFromSmiles(smiles));

    if (neutralFormula != null) {
      return MolecularFormulaManipulator.getMass(neutralFormula,
          MolecularFormulaManipulator.MonoIsotopic);
    }
    return null;
  }

  /**
   * @param adduct The adduct.
   * @return A new {@link CompoundDBAnnotation} with the given adduct.
   * {@link CompoundDBAnnotation#getPrecursorMZ()} is adjusted.
   * @throws CannotDetermineMassException In case the original compound does not contain enough
   *                                      information to calculate the ionized compound.
   */
  default CompoundDBAnnotation ionize(IonType adduct) throws CannotDetermineMassException {
    final CompoundDBAnnotation clone = clone();
    final double mz = clone.calcMzForAdduct(adduct);
    clone.put(PrecursorMZType.class, mz);
    clone.put(IonTypeType.class, adduct);
    // TODO add ion formula
    return clone;
  }

  default double calcMzForAdduct(final IonType adduct) throws CannotDetermineMassException {
    return calcMzForAdduct(this, adduct);
  }


  <T> T get(@NotNull DataType<T> key);

  <T> T get(Class<? extends DataType<T>> key);

  <T> T put(@NotNull DataType<T> key, T value);

  /**
   * Stores the given value to this annotation if the value is not equal to null.
   *
   * @param key   The key.
   * @param value The value.
   * @return The previously mapped value. Also returns the currently mapped value if the parameter
   * was null.
   */
  default <T> T putIfNotNull(@NotNull DataType<T> key, @Nullable T value) {
    if (value != null) {
      return put(key, value);
    }
    return get(key);
  }

  <T> T put(@NotNull Class<? extends DataType<T>> key, T value);

  /**
   * Stores the given value to this annotation if the value is not equal to null.
   *
   * @param key   The key.
   * @param value The value.
   * @return The previously mapped value. Also returns the currently mapped value if the parameter
   * was null.
   */
  default <T> T putIfNotNull(@NotNull Class<? extends DataType<T>> key, @Nullable T value) {
    if (value != null) {
      return put(key, value);
    }
    return get(key);
  }

  Set<DataType> getTypes();

  void saveToXML(@NotNull XMLStreamWriter writer, ModularFeatureList flist,
      ModularFeatureListRow row) throws XMLStreamException;

  @Nullable
  default DatabaseMatchInfo getDatabaseMatchInfo() {
    return get(DatabaseMatchInfoType.class);
  }

  @Nullable
  default String getDatabaseUrl() {
    final DatabaseMatchInfo databaseMatchInfo = getDatabaseMatchInfo();
    return databaseMatchInfo == null ? null : databaseMatchInfo.url();
  }

  @Override
  @Nullable
  default Double getPrecursorMZ() {
    return get(PrecursorMZType.class);
  }

  @Nullable
  default String getSmiles() {
    return get(SmilesStructureType.class);
  }

  @Nullable
  default String getInChI() {
    return get(InChIStructureType.class);
  }

  @Nullable
  default String getInChIKey() {
    return get(InChIKeyStructureType.class);
  }

  @Override
  @Nullable
  default String getCompoundName() {
    return get(CompoundNameType.class);
  }

  @Override
  @Nullable
  default String getFormula() {
    return get(FormulaType.class);
  }

  @Override
  @Nullable
  default IonType getAdductType() {
    return get(IonTypeType.class);
  }

  @Override
  @Nullable
  default Float getMobility() {
    return get(MobilityType.class);
  }

  @Override
  @Nullable
  default Float getCCS() {
    return get(CCSType.class);
  }

  @Override
  @Nullable
  default Float getRT() {
    return get(RTType.class);
  }

  @Override
  @Nullable
  default Float getScore() {
    return get(CompoundAnnotationScoreType.class);
  }

  default void setScore(Float score) {
    put(CompoundAnnotationScoreType.class, score);
  }

  @Override
  @Nullable
  default String getDatabase() {
    return get(DatabaseNameType.class);
  }

  boolean matches(FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobilityTolerance,
      @Nullable Double percentCCSTolerance);

  /**
   * @param row                 tested row
   * @param mzTolerance         matching tolerance
   * @param rtTolerance         matching tolerance
   * @param mobilityTolerance   matching tolerance
   * @param percentCCSTolerance matching tolerance
   * @return
   */
  @Nullable
  default Float calculateScore(@NotNull FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobilityTolerance,
      @Nullable Double percentCCSTolerance) {
    if (!matches(row, mzTolerance, rtTolerance, mobilityTolerance, percentCCSTolerance)) {
      return null;
    }
    // setup ranges around the annotation and test for row average values
    Double mz = getPrecursorMZ();
    final Float rt = getRT();
    final Float mobility = getMobility();
    final Float ccs = getCCS();
    var mzRange = mzTolerance != null && mz != null ? mzTolerance.getToleranceRange(mz) : null;
    var rtRange = rtTolerance != null && rt != null ? rtTolerance.getToleranceRange(rt) : null;
    var mobilityRange =
        mobilityTolerance != null && mobility != null ? mobilityTolerance.getToleranceRange(
            mobility) : null;

    Range<Float> ccsRange = null;
    if (percentCCSTolerance != null && ccs != null && row.getAverageCCS() != null) {
      float tol = (float) (ccs * percentCCSTolerance);
      ccsRange = Range.closed(ccs - tol, ccs + tol);
    }
    return (float) FeatureListUtils.getAlignmentScore(row.getAverageMZ(), row.getAverageRT(),
        row.getAverageMobility(), row.getAverageCCS(), mzRange, rtRange, mobilityRange, ccsRange, 1,
        1, 1, 1);
  }

  /**
   * @param row              The row
   * @param mzTolerance      MZ tolerance for matching or null
   * @param rtTolerance      RT tolerance for matching or null
   * @param mobTolerance     mobility tolerance for matching or null
   * @param percCcsTolerance ccs tolerance for matching or null
   * @return A <b>clone</b> of the original annotation with {@link MzPpmDifferenceType},
   * .{@link CCSRelativeErrorType} and {@link RtRelativeErrorType} set.
   */
  default @Nullable CompoundDBAnnotation checkMatchAndCalculateDeviation(
      @NotNull FeatureListRow row, @Nullable MZTolerance mzTolerance,
      @Nullable RTTolerance rtTolerance, @Nullable MobilityTolerance mobTolerance,
      @Nullable Double percCcsTolerance) {
    final Float score = calculateScore(row, mzTolerance, rtTolerance, mobTolerance,
        percCcsTolerance);
    if (score == null || score <= 0) {
      return null;
    }

    final CompoundDBAnnotation clone = clone();
    clone.put(CompoundAnnotationScoreType.class, score);
    clone.put(MzPpmDifferenceType.class,
        (float) MathUtils.getPpmDiff(Objects.requireNonNullElse(clone.getPrecursorMZ(), 0d),
            row.getAverageMZ()));

    // if the compound entry contained <=0 for RT or mobility
    // do not check. This is defined as wildcard in the documentation and outside valid values
    var compCcs = get(CCSType.class);
    if (compCcs != null && compCcs > 0 && row.getAverageCCS() != null) {
      clone.put(CCSRelativeErrorType.class,
          PercentTolerance.getPercentError(compCcs, row.getAverageCCS()));
    }
    var compRt = get(RTType.class);
    if (compRt != null && compRt > 0 && row.getAverageRT() != null) {
      clone.put(RtRelativeErrorType.class,
          PercentTolerance.getPercentError(compRt, row.getAverageRT()));
    }

    return clone;
  }

  /**
   * @return Returns the 2D structure URL.
   */
  default URL get2DStructureURL() {
    final UrlShortName url = get(Structure2dUrlType.class);
    try {
      return url != null ? new URL(url.longUrl()) : null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * @return Returns the 3D structure URL.
   */
  default URL get3DStructureURL() {
    final UrlShortName url = get(Structure3dUrlType.class);
    try {
      return url != null ? new URL(url.longUrl()) : null;
    } catch (MalformedURLException e) {
      return null;
    }
  }

  /**
   * Returns the isotope pattern score or null if the score was not calculated.
   *
   * @return isotope pattern score.
   */
  default Float getIsotopePatternScore() {
    return get(IsotopePatternScoreType.class);
  }

  /**
   * Returns the isotope pattern (predicted) of this compound.
   *
   * @return the isotope pattern
   */
  default IsotopePattern getIsotopePattern() {
    return get(IsotopePatternType.class);
  }

  Map<DataType, Object> getReadOnlyMap();

  CompoundDBAnnotation clone();

  default String toFullString() {
    return getReadOnlyMap().keySet().stream()
        .map(key -> key.getUniqueID() + ": " + getFormattedString(key))
        .collect(Collectors.joining("; "));
  }

  /**
   * A formatted string representation of the value - internally in MZmine GUI
   *
   * @return the formatted representation of the value (or an empty String)
   */
  @NotNull
  default String getFormattedString(DataType key) {
    return key.getFormattedString(get(key));
  }

  /**
   * A formatted string representation of the value either for export or internally in MZmine GUI
   *
   * @return the formatted representation of the value, or the {@link DataType#getDefaultValue()}
   * for null values, (or an empty String if default is also null)
   */
  @NotNull
  default String getFormattedString(DataType key, boolean export) {
    return key.getFormattedString(get(key), export);
  }


  /**
   * highest score first
   *
   * @param o the object to be compared.
   */
  @Override
  default int compareTo(@NotNull CompoundDBAnnotation o) {
    var sc = this.getScore();
    var sc2 = o.getScore();
    if (sc == null && sc2 == null) {
      return 0;
    } else if (sc == null) {
      return -1;
    } else if (sc2 == null) {
      return 1;
    }
    return -Float.compare(sc, sc2);
  }
}
