package io.github.mzmine.modules.io.export_features_mztabm;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
import io.github.mzmine.datamodel.identities.iontype.IonTypes;
import io.github.mzmine.datamodel.impl.SimpleFeatureIdentity;
import io.github.mzmine.util.scans.similarity.SpectralSimilarity;
import io.github.mzmine.util.spectraldb.entry.DBEntryField;
import io.github.mzmine.util.spectraldb.entry.SpectralDBAnnotation;
import io.github.mzmine.util.spectraldb.entry.SpectralLibraryEntry;
import java.net.URI;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

/**
 * Checks annotation selection and preservation of real identifiers, provenance and scores.
 */
class MzTabmAnnotationTest {

  /**
   * Formula export uses neutral composition, including the charged betaine in the workshop data.
   *
   * @param sourceFormula formula supplied by the annotation
   * @param expectedFormula neutral formula, or null if the source cannot be represented
   */
  @ParameterizedTest
  @CsvSource(value = {"[C21H43N2O3]+,C21H42N2O3", "[C7H5O2]-,C7H6O2",
      "C8H10N4O2,C8H10N4O2", "N4O2C8H10,C8H10N4O2", "not a formula,NULL"}, nullValues = "NULL")
  void exportsNeutralHillFormulas(@NotNull final String sourceFormula,
      @Nullable final String expectedFormula) {
    final FeatureAnnotation modern = Mockito.mock(FeatureAnnotation.class);
    Mockito.when(modern.getFormula()).thenReturn(sourceFormula);
    Mockito.when(modern.getAnnotationMethodName()).thenReturn("Compound DB");

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(rowWith(modern));

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals(expectedFormula, annotation.formula());
  }

  /**
   * A selected modern annotation takes precedence over a legacy identity on the same row.
   */
  @Test
  void usesPreferredModernAnnotationWithoutInventingAnAccession() {
    final FeatureAnnotation modern = Mockito.mock(FeatureAnnotation.class);
    Mockito.when(modern.getCompoundName()).thenReturn("Caffeine");
    Mockito.when(modern.getFormula()).thenReturn("C8H10N4O2");
    Mockito.when(modern.getIsomericSmiles()).thenReturn("isomeric smiles");
    Mockito.when(modern.getInChI()).thenReturn("InChI=1S/example");
    Mockito.when(modern.getDatabase()).thenReturn("local compounds.csv");
    Mockito.when(modern.getAnnotationMethodName()).thenReturn("Compound DB");
    Mockito.when(modern.getScore()).thenReturn(0.96745944f);
    Mockito.when(modern.getPrecursorMZ()).thenReturn(195.08765);
    Mockito.when(modern.getAdductType()).thenReturn(IonTypes.H.asIonType());
    final FeatureListRow row = rowWith(modern);

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(row);

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals("Caffeine", annotation.compoundName());
    Assertions.assertEquals("C8H10N4O2", annotation.formula());
    Assertions.assertEquals("isomeric smiles", annotation.smiles());
    Assertions.assertEquals("InChI=1S/example", annotation.inchi());
    Assertions.assertEquals("local compounds.csv", annotation.databaseName());
    Assertions.assertEquals("Compound DB", annotation.method());
    Assertions.assertEquals((double) 0.96745944f, annotation.score());
    Assertions.assertEquals("Compound DB score", annotation.scoreName());
    Assertions.assertEquals(195.08765, annotation.precursorMz());
    Assertions.assertEquals("[M+H]+", annotation.adduct());
    Assertions.assertNull(annotation.databaseIdentifier());
    Assertions.assertNull(annotation.uri());
    Assertions.assertFalse(annotation.spectralMatch());
    Mockito.verify(row, Mockito.never()).getPreferredFeatureIdentity();
  }

  /**
   * Spectral scores retain double precision, their actual function name and the matched query scan.
   */
  @Test
  void preservesSpectralAccessionScoreAndQueryScan() {
    final SpectralLibraryEntry entry = Mockito.mock(SpectralLibraryEntry.class);
    Mockito.when(entry.getField(DBEntryField.ENTRY_ID))
        .thenReturn(Optional.of("MSBNK-CASMI_2016-SM824801"));
    Mockito.when(entry.getField(DBEntryField.MONA_ID)).thenReturn(Optional.of("UP000250"));
    final SpectralDBAnnotation modern = Mockito.mock(SpectralDBAnnotation.class);
    final Scan queryScan = Mockito.mock(Scan.class);
    final double score = 0.9782618264079744;
    Mockito.when(modern.getEntry()).thenReturn(entry);
    Mockito.when(modern.getSimilarity())
        .thenReturn(new SpectralSimilarity("Weighted cosine similarity", score, 5, 0.99));
    Mockito.when(modern.getQueryScan()).thenReturn(queryScan);
    Mockito.when(modern.getAnnotationMethodName()).thenReturn("Spectral match");
    Mockito.when(modern.getDatabase()).thenReturn("massbank_nist_for_tests.msp");

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(rowWith(modern));

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals("MSBNK-CASMI_2016-SM824801", annotation.databaseIdentifier());
    Assertions.assertEquals(score, annotation.score());
    Assertions.assertEquals("Weighted cosine similarity", annotation.scoreName());
    Assertions.assertEquals("massbank_nist_for_tests.msp", annotation.databaseName());
    Assertions.assertSame(queryScan, annotation.queryScan());
    Assertions.assertTrue(annotation.spectralMatch());
    Assertions.assertNull(annotation.uri());
  }

  /**
   * A real MoNA accession is retained when the aggregate library has no primary entry identifier.
   */
  @Test
  void retainsAlternateSpectralAccessionsWithoutGuessingTheirUrls() {
    final SpectralLibraryEntry entry = Mockito.mock(SpectralLibraryEntry.class);
    Mockito.when(entry.getField(DBEntryField.ENTRY_ID)).thenReturn(Optional.of(" "));
    Mockito.when(entry.getField(DBEntryField.MONA_ID)).thenReturn(Optional.of("UP000250"));
    final SpectralDBAnnotation modern = Mockito.mock(SpectralDBAnnotation.class);
    Mockito.when(modern.getEntry()).thenReturn(entry);
    Mockito.when(modern.getSimilarity()).thenReturn(new SpectralSimilarity("Cosine", 0.9, 5, 0.9));
    Mockito.when(modern.getAnnotationMethodName()).thenReturn("Spectral match");

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(rowWith(modern));

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals("UP000250", annotation.databaseIdentifier());
    Assertions.assertNull(annotation.uri());
  }

  /**
   * An identifier that already is a URI can be retained as a link without database-specific guesses.
   */
  @Test
  void preservesExplicitModernIdentifierUrisAndAbsentScores() {
    final FeatureAnnotation modern = Mockito.mock(FeatureAnnotation.class);
    Mockito.when(modern.getInternalId()).thenReturn("https://example.org/compound/1");
    Mockito.when(modern.getSmiles()).thenReturn("canonical smiles");
    Mockito.when(modern.getAnnotationMethodName()).thenReturn("Manual annotation");
    Mockito.when(modern.getScore()).thenReturn(null);

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(rowWith(modern));

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals(URI.create("https://example.org/compound/1"), annotation.uri());
    Assertions.assertEquals("canonical smiles", annotation.smiles());
    Assertions.assertNull(annotation.score());
    Assertions.assertNull(annotation.scoreName());
  }

  /**
   * Namespace-prefixed accessions are retained as identifiers without being guessed as URI schemes.
   */
  @Test
  void doesNotInterpretNamespacedAccessionsAsLinks() {
    final FeatureAnnotation modern = Mockito.mock(FeatureAnnotation.class);
    Mockito.when(modern.getInternalId()).thenReturn("HMDB:HMDB0000122");
    Mockito.when(modern.getAnnotationMethodName()).thenReturn("Compound DB");

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(rowWith(modern));

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals("HMDB:HMDB0000122", annotation.databaseIdentifier());
    Assertions.assertNull(annotation.uri());
  }

  /**
   * The legacy path retains explicit fields while leaving unavailable evidence values absent.
   */
  @Test
  void retainsLegacyIdentityFallback() {
    final SimpleFeatureIdentity identity = new SimpleFeatureIdentity("Caffeine", "C8H10N4O2",
        "Legacy database", "TEST:1", "https://example.org/compound/1");
    identity.setPropertyValue(FeatureIdentity.PROPERTY_PRECURSORMZ, "195.08765");
    identity.setPropertyValue(FeatureIdentity.PROPERTY_SMILES, "legacy smiles");
    final FeatureListRow row = Mockito.mock(FeatureListRow.class);
    Mockito.when(row.getPreferredFeatureIdentity()).thenReturn(identity);

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(row);

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals("Caffeine", annotation.compoundName());
    Assertions.assertEquals("C8H10N4O2", annotation.formula());
    Assertions.assertEquals("TEST:1", annotation.databaseIdentifier());
    Assertions.assertEquals("Legacy database", annotation.method());
    Assertions.assertEquals(URI.create("https://example.org/compound/1"), annotation.uri());
    Assertions.assertEquals(195.08765, annotation.precursorMz());
    Assertions.assertEquals("legacy smiles", annotation.smiles());
    Assertions.assertEquals("SimpleFeatureIdentity", annotation.sourceKey());
    Assertions.assertNull(annotation.score());
    Assertions.assertNull(annotation.queryScan());
    Assertions.assertFalse(annotation.spectralMatch());
  }

  /**
   * Incomplete legacy identities receive a method label without inventing IDs or zero scores.
   */
  @Test
  void handlesMissingLegacyProperties() {
    final FeatureListRow row = Mockito.mock(FeatureListRow.class);
    Mockito.when(row.getPreferredFeatureIdentity()).thenReturn(new SimpleFeatureIdentity("Known"));

    final MzTabmAnnotation annotation = MzTabmAnnotation.fromRow(row);

    Assertions.assertNotNull(annotation);
    Assertions.assertEquals("Legacy feature identity", annotation.method());
    Assertions.assertNull(annotation.databaseIdentifier());
    Assertions.assertNull(annotation.databaseName());
    Assertions.assertNull(annotation.uri());
    Assertions.assertNull(annotation.score());
  }

  /**
   * Malformed explicit legacy URLs retain their previous error behavior.
   */
  @Test
  void rejectsMalformedLegacyUrls() {
    final FeatureListRow row = Mockito.mock(FeatureListRow.class);
    Mockito.when(row.getPreferredFeatureIdentity()).thenReturn(
        new SimpleFeatureIdentity("Known", null, null, null, "https://invalid url"));

    Assertions.assertThrows(IllegalArgumentException.class, () -> MzTabmAnnotation.fromRow(row));
  }

  /**
   * Rows with neither annotation model remain unannotated.
   */
  @Test
  void returnsNullForUnannotatedRows() {
    Assertions.assertNull(MzTabmAnnotation.fromRow(Mockito.mock(FeatureListRow.class)));
  }

  /**
   * Supplies a selected annotation without modifying project state or annotation priority settings.
   *
   * @param annotation selected annotation
   * @return isolated row exposing the annotation
   */
  private static @NotNull FeatureListRow rowWith(@NotNull final FeatureAnnotation annotation) {
    final FeatureListRow row = Mockito.mock(FeatureListRow.class);
    Mockito.when(row.getPreferredAnnotation()).thenReturn(annotation);
    return row;
  }
}
