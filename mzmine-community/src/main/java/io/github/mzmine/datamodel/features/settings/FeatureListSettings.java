package io.github.mzmine.datamodel.features.settings;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.preferences.FeatureListPreferences;
import io.github.mzmine.modules.dataprocessing.norm_intensity.IntensityNormalizationSummary;
import io.github.mzmine.modules.dataprocessing.norm_intensity.NormalizationFunctionsParameter;
import io.github.mzmine.util.XMLUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

/**
 * Settings of a {@link FeatureList} that are written by processing modules and never edited by the
 * user, in contrast to the user defined {@link FeatureListPreferences}. Transferred to new feature
 * lists with {@link #copy()}.
 *
 * @param normalization the functions of the latest intensity normalization or null if the feature
 *                      list was never normalized. Messages and feature-QC provenance stay in the
 *                      applied method of the normalizer.
 */
public record FeatureListSettings(@Nullable IntensityNormalizationSummary normalization) {

  public static final @NotNull FeatureListSettings DEFAULT = new FeatureListSettings(null);

  private static final String XML_NORMALIZATION_ELEMENT = "intensity_normalization";

  public FeatureListSettings {
    // decision: an empty summary is the same as no normalization, keep a single representation
    if (normalization != null && normalization.isEmpty()) {
      normalization = null;
    }
  }

  /**
   * Used when the settings are transferred to a new feature list. All current fields are
   * immutable, new mutable fields must be copied here.
   *
   * @return a copy of these settings
   */
  public @NotNull FeatureListSettings copy() {
    return new FeatureListSettings(normalization);
  }

  public @NotNull FeatureListSettings withNormalization(
      @Nullable final IntensityNormalizationSummary normalization) {
    return new FeatureListSettings(normalization);
  }

  /**
   * Each field writes its own child element. Null fields are not written.
   */
  public void saveToXML(@NotNull final Element element) {
    if (normalization != null) {
      final Element normElement = element.getOwnerDocument()
          .createElement(XML_NORMALIZATION_ELEMENT);
      final NormalizationFunctionsParameter param = new NormalizationFunctionsParameter();
      param.setValue(normalization);
      param.saveValueToXML(normElement);
      element.appendChild(normElement);
    }
  }

  /**
   * @param element the settings element or null if the project was saved before settings were
   *                introduced
   * @return the loaded settings or null if the element is missing
   */
  public static @Nullable FeatureListSettings loadFromXML(@Nullable final Element element) {
    if (element == null) {
      return null;
    }
    final Element normElement = XMLUtils.streamChildElementsByTagName(element,
        XML_NORMALIZATION_ELEMENT).findFirst().orElse(null);
    if (normElement == null) {
      return DEFAULT;
    }
    final NormalizationFunctionsParameter param = new NormalizationFunctionsParameter();
    param.loadValueFromXML(normElement);
    return new FeatureListSettings(param.getValue());
  }
}
