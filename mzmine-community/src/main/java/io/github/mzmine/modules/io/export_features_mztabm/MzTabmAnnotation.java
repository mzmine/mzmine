package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.net.URI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Annotation values selected for mzTab-M export. Missing identifiers, links and scores stay absent.
 *
 * @param compoundName reported compound name
 * @param formula neutral molecular formula in Hill order
 * @param smiles isomeric SMILES when available, otherwise canonical SMILES
 * @param inchi reported InChI
 * @param uri explicit annotation URI, when available
 * @param databaseIdentifier actual database accession or internal identifier
 * @param databaseName source database or library name
 * @param method annotation method label
 * @param score original annotation score, without changing its scale
 * @param scoreName name of the score or spectral similarity function
 * @param precursorMz annotated precursor m/z
 * @param adduct annotated ion type
 * @param sourceKey annotation implementation name used for source provenance
 * @param queryScan spectrum used for a spectral match
 * @param spectralMatch whether the annotation came from spectral matching
 */
record MzTabmAnnotation(@Nullable String compoundName, @Nullable String formula,
                       @Nullable String smiles, @Nullable String inchi, @Nullable URI uri,
                       @Nullable String databaseIdentifier, @Nullable String databaseName,
                       @NotNull String method, @Nullable Double score, @Nullable String scoreName,
                       @Nullable Double precursorMz, @Nullable String adduct,
                       @NotNull String sourceKey, @Nullable Scan queryScan, boolean spectralMatch) {

  /**
   * Uses the selected modern annotation, falling back to a legacy identity only when none exists.
   *
   * @param row feature row to inspect
   * @return selected annotation values, or null for an unannotated row
   */
  static @Nullable MzTabmAnnotation fromRow(@NotNull final FeatureListRow row) {
    final FeatureAnnotation annotation = row.getPreferredAnnotation();
    if (annotation != null) {
      return fromModern(annotation);
    }
    final FeatureIdentity identity = row.getPreferredFeatureIdentity();
    return identity == null ? null : fromLegacy(identity);
  }

  /**
   * Copies modern annotation fields without treating compound names as database identifiers.
   *
   * @param annotation selected modern annotation
   * @return values ready for export
   */
  private static @NotNull MzTabmAnnotation fromModern(@NotNull final FeatureAnnotation annotation) {
    final String annotationMethod = normalize(annotation.getAnnotationMethodName());
    final String method = annotationMethod == null ? "Feature annotation" : annotationMethod;
    final String isomericSmiles = normalize(annotation.getIsomericSmiles());
    final String smiles = isomericSmiles == null ? normalize(annotation.getSmiles()) : isomericSmiles;
    final IonType ion = annotation.getAdductType();
    final String identifier;
    final Double score;
    final String scoreName;
    final Scan queryScan;
    final boolean spectralMatch;
    if (annotation instanceof SpectralDBAnnotation spectral) {
      identifier = spectralIdentifier(spectral.getEntry());
      final SpectralSimilarity similarity = spectral.getSimilarity();
      score = finiteValue(similarity.getScore());
      final String functionName = normalize(similarity.getFunctionName());
      scoreName = score == null ? null : functionName == null ? method + " score" : functionName;
      queryScan = spectral.getQueryScan();
      spectralMatch = true;
    } else {
      identifier = normalize(annotation.getInternalId());
      final Float annotationScore = annotation.getScore();
      score = annotationScore == null ? null : finiteValue(annotationScore.doubleValue());
      scoreName = score == null ? null : method + " score";
      queryScan = null;
      spectralMatch = false;
    }
    return new MzTabmAnnotation(normalize(annotation.getCompoundName()),
        neutralFormula(annotation.getFormula()), smiles, normalize(annotation.getInChI()),
        absoluteIdentifierUri(identifier), identifier, normalize(annotation.getDatabase()), method,
        score, scoreName, finiteValue(annotation.getPrecursorMZ()),
        ion == null ? null : ion.toString(), annotation.getClass().getSimpleName(), queryScan,
        spectralMatch);
  }

  /**
   * Retains the legacy identity properties without manufacturing scores or annotation links.
   *
   * @param identity legacy identity selected by the row
   * @return values ready for export
   */
  private static @NotNull MzTabmAnnotation fromLegacy(@NotNull final FeatureIdentity identity) {
    final String identityMethod = normalize(identity.getPropertyValue(FeatureIdentity.PROPERTY_METHOD));
    final String method = identityMethod == null ? "Legacy feature identity" : identityMethod;
    final String url = normalize(identity.getPropertyValue(FeatureIdentity.PROPERTY_URL));
    final String precursorMz = normalize(
        identity.getPropertyValue(FeatureIdentity.PROPERTY_PRECURSORMZ));
    return new MzTabmAnnotation(normalize(identity.getName()),
        neutralFormula(identity.getPropertyValue(FeatureIdentity.PROPERTY_FORMULA)),
        normalize(identity.getPropertyValue(FeatureIdentity.PROPERTY_SMILES)), null,
        url == null ? null : URI.create(url),
        normalize(identity.getPropertyValue(FeatureIdentity.PROPERTY_ID)), identityMethod, method,
        null, null, parseNumber(precursorMz),
        normalize(identity.getPropertyValue(FeatureIdentity.PROPERTY_ADDUCT)),
        identity.getClass().getSimpleName(), null, false);
  }

  /**
   * Uses the project's protonation rule to report the neutral formula required by mzTab-M.
   *
   * @param formula source formula, optionally including charge
   * @return neutral Hill formula, or null if parsing or neutralization is impossible
   */
  private static @Nullable String neutralFormula(@Nullable final String formula) {
    final IMolecularFormula neutral = FormulaUtils.neutralizeFormulaWithHydrogen(normalize(formula));
    return normalize(FormulaUtils.getFormulaString(neutral, false));
  }

  /**
   * Selects an actual spectral-library accession without substituting the compound's name.
   *
   * @param entry matched library entry
   * @return first available accession or internal identifier
   */
  private static @Nullable String spectralIdentifier(@NotNull final SpectralLibraryEntry entry) {
    for (final DBEntryField field : new DBEntryField[]{DBEntryField.ENTRY_ID, DBEntryField.MONA_ID,
        DBEntryField.GNPS_ID, DBEntryField.INTERNAL_ID}) {
      final String identifier = normalize(entry.getField(field).map(Object::toString).orElse(null));
      if (identifier != null) {
        return identifier;
      }
    }
    return null;
  }

  /**
   * Preserves an accession that already is an absolute URI without inventing database URLs.
   *
   * @param identifier database accession
   * @return the absolute URI, or null for a plain accession
   */
  private static @Nullable URI absoluteIdentifierUri(@Nullable final String identifier) {
    if (identifier == null) {
      return null;
    }
    try {
      final URI uri = URI.create(identifier);
      // decision: Namespace-prefixed accessions such as HMDB:123 are not assumed to be links.
      return uri.isAbsolute() && (identifier.contains("://") || identifier.startsWith("urn:")) ? uri
          : null;
    } catch (IllegalArgumentException e) {
      // decision: A plain accession need not be valid URI syntax.
      return null;
    }
  }

  /**
   * Removes control characters and normalizes absent annotation strings.
   *
   * @param value optional annotation text
   * @return cleaned text, or null for blank values
   */
  private static @Nullable String normalize(@Nullable final String value) {
    if (value == null) {
      return null;
    }
    final String cleaned = value.replaceAll("[\\p{Cntrl}]", " ").strip();
    return cleaned.isEmpty() ? null : cleaned;
  }

  /**
   * Discards numeric values that cannot be represented as finite annotation values.
   *
   * @param value optional annotation value
   * @return finite value, or null
   */
  private static @Nullable Double finiteValue(@Nullable final Double value) {
    return value != null && Double.isFinite(value) ? value : null;
  }

  /**
   * Reads an optional numeric property from a legacy identity.
   *
   * @param value property text
   * @return finite number, or null if the property is absent or invalid
   */
  private static @Nullable Double parseNumber(@Nullable final String value) {
    if (value == null) {
      return null;
    }
    try {
      return finiteValue(Double.valueOf(value));
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
