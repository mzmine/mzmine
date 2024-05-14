/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.networking.visual.enums;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.networking.MolNetClusterIdType;
import io.github.mzmine.datamodel.features.types.networking.MolNetClusterSizeType;
import io.github.mzmine.datamodel.features.types.networking.MolNetCommunityIdType;
import io.github.mzmine.datamodel.features.types.networking.MolNetCommunitySizeType;
import io.github.mzmine.datamodel.features.types.networking.NetworkStats;
import io.github.mzmine.datamodel.features.types.networking.NetworkStatsType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.Nullable;

/**
 * Graphstream node attributes
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum NodeAtt implements GraphElementAttr {

  NONE, LABEL, ROW, TYPE, RT, MZ, ID, //
  LOG10_SUM_INTENSITY, MAX_INTENSITY, SUM_INTENSITY, //
  ADDUCT, FORMULA, NEUTRAL_MASS, CHARGE, MS2_VERIFICATION, // networking
  IIN_ID, FEATURE_SHAPE_CORR_ID, CLUSTER_ID, COMMUNITY_ID, CLUSTER_SIZE, COMMUNITY_SIZE, NEIGHBOR_DISTANCE, // annotations
  ANNOTATION, ANNOTATION_SCORE, LIB_MATCH, COMPOUND_NAME, MATCHED_SIGNALS, EXPLAINED_INTENSITY, // Structure
  ADDUCT_SPECTRAL_MATCH, FORMULA_SPECTRAL_MATCH, SMILES, INCHI, INCHIKEY;


  @Override
  public String toString() {
    return super.toString().toLowerCase();
  }

  public boolean isNumber() {
    return switch (this) {
      case NONE, ROW, ANNOTATION, TYPE, FORMULA, ADDUCT, LABEL, MS2_VERIFICATION, COMPOUND_NAME, //
          LIB_MATCH, SMILES, INCHI, INCHIKEY, ADDUCT_SPECTRAL_MATCH, FORMULA_SPECTRAL_MATCH ->
          false;
      case NEIGHBOR_DISTANCE, RT, MZ, ID, MAX_INTENSITY, SUM_INTENSITY, LOG10_SUM_INTENSITY, //
          NEUTRAL_MASS, CHARGE, IIN_ID, FEATURE_SHAPE_CORR_ID, CLUSTER_ID, COMMUNITY_ID, ANNOTATION_SCORE, //
          EXPLAINED_INTENSITY, CLUSTER_SIZE, COMMUNITY_SIZE, MATCHED_SIGNALS -> true;
    };
  }

  @Override
  public boolean isReversed() {
    return this == NEIGHBOR_DISTANCE;
  }

  @Override
  public GraphObject getGraphObject() {
    return GraphObject.NODE;
  }

  @Override
  public boolean isChangingDynamically() {
    return NEIGHBOR_DISTANCE == this;
  }

  /**
   * Extract value from row by attribute
   *
   * @param row the target row
   * @return the value from row (if defined and available) or null
   */
  public Object getValue(FeatureListRow row) {
    return switch (this) {
      case NONE, MS2_VERIFICATION, NEIGHBOR_DISTANCE -> null;
      case LABEL -> Stream.of(ID.getValue(row), MZ.getFormattedValue(row, false),
              ADDUCT.getFormattedValue(row, false), COMPOUND_NAME.getFormattedValue(row, false))
          .filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(", "));
      case TYPE -> {
        IonIdentity ion = row.getBestIonIdentity();
        yield ion != null && !ion.getIonType().isUndefinedAdduct() ? NodeType.ION_FEATURE
            : NodeType.SINGLE_FEATURE;
      }
      case ROW -> row;
      case FORMULA -> {
        List<ResultFormula> formulas = row.getFormulas();
        yield formulas == null || formulas.isEmpty() ? null : formulas.get(0);
      }
      case ADDUCT -> row.getBestIonIdentity();
      case RT -> row.getAverageRT();
      case MZ -> row.getAverageMZ();
      case ID -> row.getID();
      case MAX_INTENSITY -> row.getBestFeature().getHeight();
      case SUM_INTENSITY -> row.getSumIntensity();
      case LOG10_SUM_INTENSITY -> Math.log10(row.getSumIntensity());
      case NEUTRAL_MASS -> {
        IonIdentity ion = row.getBestIonIdentity();
        yield ion == null ? null : ion.getNetwork().getNeutralMass();
      }
      case CHARGE -> row.getRowCharge();
      case IIN_ID -> {
        IonIdentity ion = row.getBestIonIdentity();
        yield ion == null ? null : ion.getNetID();
      }
      case FEATURE_SHAPE_CORR_ID -> row.getGroupID();
      case COMMUNITY_ID -> getNetworkStatsOrElse(MolNetCommunityIdType.class, row, -1);
      case CLUSTER_ID -> getNetworkStatsOrElse(MolNetClusterIdType.class, row, -1);
      case COMMUNITY_SIZE -> getNetworkStatsOrElse(MolNetCommunitySizeType.class, row, -1);
      case CLUSTER_SIZE -> getNetworkStatsOrElse(MolNetClusterSizeType.class, row, -1);
      case ANNOTATION ->
          row.streamAllFeatureAnnotations().findFirst().map(Object::toString).orElse(null);
      case COMPOUND_NAME -> row.getPreferredAnnotationName();
      case LIB_MATCH ->
          row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::toString).findFirst()
              .orElse(null);
      case SMILES -> row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::getSmiles)
          .filter(Objects::nonNull).findFirst().orElse(null);
      case INCHI -> row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::getInChI)
          .filter(Objects::nonNull).findFirst().orElse(null);
      case INCHIKEY ->
          row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::getInChIKey)
              .filter(Objects::nonNull).findFirst().orElse(null);
      case ADDUCT_SPECTRAL_MATCH ->
          row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::getAdductType)
              .filter(Objects::nonNull).findFirst().orElse(null);
      case FORMULA_SPECTRAL_MATCH ->
          row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::getFormula)
              .filter(Objects::nonNull).findFirst().orElse(null);
      case ANNOTATION_SCORE ->
          row.getSpectralLibraryMatches().stream().map(match -> match.getSimilarity().getScore())
              .findFirst().orElse(null);
      case EXPLAINED_INTENSITY -> row.getSpectralLibraryMatches().stream()
          .map(match -> match.getSimilarity().getExplainedLibraryIntensity()).findFirst()
          .orElse(null);
      case MATCHED_SIGNALS -> row.getSpectralLibraryMatches().stream()
          .map(match -> match.getSimilarity().getAlignedDataPoints().length).findFirst()
          .orElse(null);
    };
  }

  /**
   * Extract from {@link NetworkStatsType}
   *
   * @param type         sub type
   * @param row          the row
   * @param defaultValue default value if value is null
   * @return the value or defaultValue if value was not present or null
   */
  public <T> T getNetworkStatsOrElse(Class<? extends DataType<T>> type, FeatureListRow row,
      T defaultValue) {
    NetworkStats stats = row.get(NetworkStatsType.class);
    if (stats == null) {
      return defaultValue;
    }
    return stats.getOrElse(type, defaultValue);
  }

  /**
   * Extract from {@link NetworkStatsType}
   *
   * @param clazz sub type
   * @param row   the row
   * @return the value or defaultValue if value was not present or null
   */
  public <T> String getNetworkStatsString(Class<? extends DataType<T>> clazz, FeatureListRow row) {
    NetworkStats stats = row.get(NetworkStatsType.class);
    if (stats == null) {
      return null;
    }
    var type = DataTypes.get(clazz);
    T value = stats.getValue(clazz);
    return type.getFormattedString(value, false);
  }

  /**
   * Number formats for export or GUI
   *
   * @param export export or GUI
   * @return the format or null if it does not exist
   */
  @Nullable
  public NumberFormat getNumberFormat(boolean export) {
    return switch (this) {
      case NONE, ROW, ANNOTATION, TYPE, FORMULA, ADDUCT, LABEL, MS2_VERIFICATION, COMPOUND_NAME, //
          LIB_MATCH, ADDUCT_SPECTRAL_MATCH, FORMULA_SPECTRAL_MATCH, INCHI, INCHIKEY, SMILES,
          // int
          NEIGHBOR_DISTANCE, ID, CHARGE, IIN_ID, FEATURE_SHAPE_CORR_ID, CLUSTER_ID, COMMUNITY_ID, //
          CLUSTER_SIZE, COMMUNITY_SIZE, MATCHED_SIGNALS -> null;
      case RT -> MZmineCore.getConfiguration().getFormats(export).rtFormat();
      case MZ, NEUTRAL_MASS -> MZmineCore.getConfiguration().getFormats(export).mzFormat();
      case MAX_INTENSITY, SUM_INTENSITY, LOG10_SUM_INTENSITY, EXPLAINED_INTENSITY ->
          MZmineCore.getConfiguration().getFormats(export).intensityFormat();
      case ANNOTATION_SCORE -> MZmineCore.getConfiguration().getFormats(export).scoreFormat();
    };
  }

  /**
   * Numbers are formatted. Other values might still be values. This is added to nodes
   *
   * @param row the row to extract the data from
   * @return the formatted string or the value itself
   */
  public Object getFormattedValue(FeatureListRow row, boolean export) {
    final Object value = getValue(row);
    if (value == null) {
      return null;
    }
    NumberFormat numberFormat = getNumberFormat(export);
    return numberFormat == null ? value : numberFormat.format(value);
  }
}
