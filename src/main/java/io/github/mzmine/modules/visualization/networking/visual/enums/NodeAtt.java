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

package io.github.mzmine.modules.visualization.networking.visual.enums;

import static java.util.Objects.requireNonNullElse;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.types.DataType;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.networking.MolNetCommunityIdType;
import io.github.mzmine.datamodel.features.types.networking.MolNetCommunitySizeType;
import io.github.mzmine.datamodel.features.types.networking.MolNetIdType;
import io.github.mzmine.datamodel.features.types.networking.MolNetSizeType;
import io.github.mzmine.datamodel.features.types.networking.NetworkStats;
import io.github.mzmine.datamodel.features.types.networking.NetworkStatsType;
import io.github.mzmine.datamodel.identities.iontype.IonIdentity;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import java.util.List;

/**
 * Graphstream node attributes
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
public enum NodeAtt implements GraphElementAttr {

  NONE, LABEL, ROW, TYPE, RT, MZ, ID, //
  LOG10_SUM_INTENSITY, MAX_INTENSITY, SUM_INTENSITY, //
  ION_TYPE, FORMULA, NEUTRAL_MASS, CHARGE, MS2_VERIFICATION, // networking
  IIN_ID, CORR_ID, CLUSTER_ID, COMMUNITY_ID, CLUSTER_SIZE, COMMUNITY_SIZE, NEIGHBOR_DISTANCE, // annotations
  ANNOTATION, ANNOTATION_SCORE, LIB_MATCH, COMPOUND_NAME, MATCHED_SIGNALS, EXPLAINED_INTENSITY;


  @Override
  public String toString() {
    return super.toString().replaceAll("_", " ");
  }

  public boolean isNumber() {
    return switch (this) {
      case NONE, ROW, ANNOTATION, TYPE, FORMULA, ION_TYPE, LABEL, MS2_VERIFICATION, COMPOUND_NAME, //
          LIB_MATCH -> false;
      case NEIGHBOR_DISTANCE, RT, MZ, ID, MAX_INTENSITY, SUM_INTENSITY, LOG10_SUM_INTENSITY, //
          NEUTRAL_MASS, CHARGE, IIN_ID, CORR_ID, CLUSTER_ID, COMMUNITY_ID, ANNOTATION_SCORE, //
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
      case ROW -> row;
      case NONE, TYPE, LABEL, MS2_VERIFICATION, NEIGHBOR_DISTANCE -> null;
      case FORMULA -> {
        List<ResultFormula> formulas = row.getFormulas();
        yield formulas == null || formulas.isEmpty() ? null : formulas.get(0);
      }
      case ION_TYPE -> row.getBestIonIdentity();
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
      case CORR_ID -> row.getGroupID();
      case COMMUNITY_ID -> getNetworkStatsOrElse(MolNetCommunityIdType.class, row, -1);
      case CLUSTER_ID -> getNetworkStatsOrElse(MolNetIdType.class, row, -1);
      case COMMUNITY_SIZE -> getNetworkStatsOrElse(MolNetCommunitySizeType.class, row, -1);
      case CLUSTER_SIZE -> getNetworkStatsOrElse(MolNetSizeType.class, row, -1);
      case ANNOTATION ->
          row.getAllFeatureAnnotations().stream().findFirst().map(Object::toString).orElse(null);
      case LIB_MATCH ->
          row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::toString).findFirst()
              .orElse(null);
      case COMPOUND_NAME -> row.getSpectralLibraryMatches().stream()
          .map(match -> match.getEntry().getOrElse(DBEntryField.NAME, (String) null)).findFirst()
          .orElse(null);
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
   * Formatted string
   *
   * @param row the row to extract the data from
   * @return the formatted string or null
   */
  public String getValueString(FeatureListRow row) {
    return switch (this) {
      case NONE, ROW, TYPE, LABEL, MS2_VERIFICATION, NEIGHBOR_DISTANCE -> "";
      case FORMULA -> {
        List<ResultFormula> formulas = row.getFormulas();
        yield formulas == null || formulas.isEmpty() ? "" : formulas.get(0).toString();
      }
      case ION_TYPE -> {
        IonIdentity ion = row.getBestIonIdentity();
        yield ion == null ? "" : ion.toString();
      }
      case RT -> MZmineCore.getConfiguration().getRTFormat().format(row.getAverageRT());
      case MZ -> MZmineCore.getConfiguration().getMZFormat().format(getValue(row));
      case ID -> String.valueOf(row.getID());
      case MAX_INTENSITY, SUM_INTENSITY, LOG10_SUM_INTENSITY ->
          MZmineCore.getConfiguration().getIntensityFormat().format(getValue(row));
      case NEUTRAL_MASS -> {
        IonIdentity ion = row.getBestIonIdentity();
        yield ion == null ? ""
            : MZmineCore.getConfiguration().getMZFormat().format(ion.getNetwork().getNeutralMass());
      }
      case CHARGE -> String.valueOf(row.getRowCharge());
      case IIN_ID -> {
        IonIdentity ion = row.getBestIonIdentity();
        yield ion == null ? "" : String.valueOf(ion.getNetID());
      }
      case CORR_ID -> {
        int i = row.getGroupID();
        yield i > -1 ? String.valueOf(i) : "";
      }
      case CLUSTER_ID -> getNetworkStatsString(MolNetIdType.class, row);
      case COMMUNITY_ID -> getNetworkStatsString(MolNetCommunityIdType.class, row);
      case COMMUNITY_SIZE -> getNetworkStatsString(MolNetCommunitySizeType.class, row);
      case CLUSTER_SIZE -> getNetworkStatsString(MolNetSizeType.class, row);
      case LIB_MATCH ->
          row.getSpectralLibraryMatches().stream().map(SpectralDBAnnotation::getCompoundName)
              .findFirst().orElse("");
      case COMPOUND_NAME -> row.getSpectralLibraryMatches().stream()
          .map(match -> match.getEntry().getOrElse(DBEntryField.NAME, "")).findFirst().orElse("");
      case ANNOTATION -> requireNonNullElse(getValue(row), "").toString();
      case ANNOTATION_SCORE -> row.getSpectralLibraryMatches().stream()
          .map(match -> String.format("%1.2G", match.getSimilarity().getScore())).findFirst()
          .orElse("");
      case EXPLAINED_INTENSITY -> row.getSpectralLibraryMatches().stream().map(
              match -> String.format("%1.2G", match.getSimilarity().getExplainedLibraryIntensity()))
          .findFirst().orElse("");
      case MATCHED_SIGNALS -> row.getSpectralLibraryMatches().stream()
          .map(match -> String.valueOf(match.getSimilarity().getAlignedDataPoints().length)).findFirst()
          .orElse("");
    };

  }
}
