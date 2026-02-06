/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_diffms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Result of ChemAudit validation for a single chemical structure.
 * 
 * ChemAudit validates and scores structures predicted by DiffMS to ensure high quality annotations.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChemAuditResult(
    @JsonProperty("row_id") @Nullable Integer rowId,
    @JsonProperty("rank") @Nullable Integer rank,
    @JsonProperty("smiles") String smiles,
    @JsonProperty("valid") boolean valid,
    @JsonProperty("validation_score") int validationScore,
    @JsonProperty("ml_readiness_score") int mlReadinessScore,
    @JsonProperty("ml_interpretation") @Nullable String mlInterpretation,
    @JsonProperty("alerts") List<StructuralAlert> alerts,
    @JsonProperty("standardized_smiles") String standardizedSmiles,
    @JsonProperty("failed_checks") List<FailedCheck> failedChecks,
    @JsonProperty("quality_category") String qualityCategory,
    @JsonProperty("error") @Nullable String error,
    @JsonProperty("ml_breakdown") @Nullable MlBreakdown mlBreakdown,
    @JsonProperty("standardization") @Nullable StandardizationResult standardization,
    @JsonProperty("traceback") @Nullable String traceback
) {

  /**
   * Structural alert found during screening.
   */
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StructuralAlert(
      @JsonProperty("pattern") String pattern,
      @JsonProperty("severity") String severity,
      @JsonProperty("catalog") String catalog,
      @JsonProperty("description") @Nullable String description,
      @JsonProperty("matched_atoms") @Nullable List<Integer> matchedAtoms
  ) {}

  /**
   * Failed validation check.
   */
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record FailedCheck(
      @JsonProperty("name") String name,
      @JsonProperty("severity") String severity,
      @JsonProperty("message") String message
  ) {}

  /**
   * ML readiness breakdown details.
   */
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record MlBreakdown(
      @JsonProperty("descriptors_score") @Nullable Integer descriptorsScore,
      @JsonProperty("descriptors_successful") @Nullable Integer descriptorsSuccessful,
      @JsonProperty("descriptors_total") @Nullable Integer descriptorsTotal,
      @JsonProperty("fingerprints_score") @Nullable Integer fingerprintsScore,
      @JsonProperty("fingerprints_successful") @Nullable JsonNode fingerprintsSuccessful,
      @JsonProperty("size_score") @Nullable Integer sizeScore,
      @JsonProperty("molecular_weight") @Nullable Double molecularWeight,
      @JsonProperty("num_atoms") @Nullable Integer numAtoms
  ) {}

  /**
   * Standardization result details.
   */
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record StandardizationResult(
      @JsonProperty("success") @Nullable Boolean success,
      @JsonProperty("excluded_fragments") @Nullable List<String> excludedFragments,
      @JsonProperty("mass_change_percent") @Nullable Double massChangePercent,
      @JsonProperty("error") @Nullable String error
  ) {}

  /**
   * Check if this structure meets quality thresholds.
   * 
   * @return true if the structure is valid and has no errors
   */
  public boolean meetsQualityThreshold() {
    return valid && error == null;
  }

  /**
   * Check if there are no errors during validation.
   * 
   * @return true if no errors occurred
   */
  public boolean hasNoErrors() {
    return error == null || error.isEmpty();
  }

  /**
   * Check if there are any critical structural alerts.
   * 
   * @return true if critical alerts are present
   */
  public boolean hasCriticalAlerts() {
    return alerts != null && alerts.stream().anyMatch(a -> "critical".equals(a.severity()));
  }

  /**
   * Get a human-readable summary of the validation results.
   * 
   * @return summary string
   */
  public String getSummary() {
    if (error != null) {
      return "Error: " + error;
    }
    return String.format("Validation: %d/100 | ML: %d/100 | Quality: %s%s",
        validationScore, mlReadinessScore, qualityCategory,
        alerts != null && !alerts.isEmpty() ? " | Alerts: " + alerts.size() : "");
  }
}
