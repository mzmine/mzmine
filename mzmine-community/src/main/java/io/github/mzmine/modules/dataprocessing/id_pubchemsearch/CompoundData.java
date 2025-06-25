package io.github.mzmine.modules.dataprocessing.id_pubchemsearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty; // Import Jackson annotation

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.IsotopePatternType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.InChIKeyStructureType;
import io.github.mzmine.datamodel.features.types.annotations.InChIStructureType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.PubChemIdType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.ChargeType;
import io.github.mzmine.datamodel.features.types.numbers.MzAbsoluteDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.MzPpmDifferenceType;
import io.github.mzmine.datamodel.features.types.numbers.NeutralMassType;
import io.github.mzmine.datamodel.features.types.numbers.PrecursorMZType;
import io.github.mzmine.datamodel.features.types.numbers.scores.IsotopePatternScoreType;
import io.github.mzmine.datamodel.features.types.numbers.scores.MsMsScoreType;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.util.FormulaUtils;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

/**
 * Represents the parsed data for a single compound retrieved from PubChem. Uses Optional for fields
 * that might be missing in the PubChem response. Annotated for Jackson JSON deserialization.
 */
// Ignore properties in JSON not defined here (e.g., if PubChem adds new ones)
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompoundData(
    // Map JSON "CID" to the 'cid' component
    @JsonProperty("CID") int cid, // Assuming CID is always present and an int

    // Map JSON property names to Optional record components
    @JsonProperty("MolecularFormula") String molecularFormula,

    @JsonProperty("SMILES") String smiles,

    @JsonProperty("InChI") String inchi,

    @JsonProperty("InChIKey") String inchiKey,

    @JsonProperty("IUPACName") String iupacName,

    @JsonProperty("Title") String title,

    @JsonProperty("Charge") Integer charge,

    @JsonProperty("MonoisotopicMass") Double monoisotopicMass) {

  public @NotNull CompoundDBAnnotation convert(@Nullable IonType ionType) {
    final CompoundDBAnnotation annotation = new SimpleCompoundDBAnnotation();

    annotation.put(PubChemIdType.class, String.valueOf(cid()));
    annotation.putIfNotNull(FormulaType.class, molecularFormula);
    annotation.putIfNotNull(ChargeType.class, charge);
    annotation.putIfNotNull(InChIStructureType.class, inchi);
    annotation.putIfNotNull(InChIKeyStructureType.class, inchiKey);
    annotation.putIfNotNull(SmilesStructureType.class, smiles);
    annotation.putIfNotNull(CompoundNameType.class, title);

    if (ionType != null) {
      annotation.putIfNotNull(IonTypeType.class, ionType);
      if(monoisotopicMass != null) {
        annotation.put(PrecursorMZType.class, ionType.getMZ(monoisotopicMass));
      }
    }

    return annotation;
  }

  /**
   * Converts the provided {@link IonType} into a {@link CompoundDBAnnotation} and scores it based
   * on properties extracted from the provided {@link FeatureListRow}.
   *
   * @param row     the feature list row containing data for scoring; may be null, in which case no
   *                scoring will be performed.
   * @param ionType the ion type used for conversion and scoring; may be null.
   * @return a {@link CompoundDBAnnotation} that is the result of the conversion and optional
   * scoring process. Guaranteed to be non-null.
   */
  public @NotNull CompoundDBAnnotation convertAndScore(@Nullable FeatureListRow row,
      @Nullable IonType ionType) {
    final CompoundDBAnnotation annotation = convert(ionType);

    if (row == null) {
      // no scoring if there is no row
      return annotation;
    }

    IMolecularFormula formula;
    if (annotation.getFormula() == null) {
      if (annotation.getStructure() == null) {
        return annotation;
      } else {
        formula = annotation.getStructure().formula();
      }
    } else {
      formula = FormulaUtils.createMajorIsotopeMolFormula(annotation.getFormula());
    }

    if (ionType != null && formula != null) {
      try {
        formula = ionType.addToFormula(formula, true);
      } catch (CloneNotSupportedException e) {
        //
      }
    }

    if (formula == null) {
      return annotation;
    }
    annotation.putIfNotNull(IonTypeType.class, ionType);

    final ResultFormula f = new ResultFormula(formula, row);
    annotation.put(IsotopePatternScoreType.class, f.getIsotopeScore());
    annotation.put(MsMsScoreType.class, f.getMSMSScore());
    annotation.put(MzAbsoluteDifferenceType.class, f.getAbsoluteMzDiff());
    annotation.put(MzPpmDifferenceType.class, f.getPpmDiff());
    annotation.put(IsotopePatternType.class, f.getPredictedIsotopes());
    return annotation;
  }
}
