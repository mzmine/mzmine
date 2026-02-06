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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.util.files.FileAndPathUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Validates chemical structures using ChemAudit.
 * 
 * ChemAudit provides:
 * - Structure validation (15+ checks)
 * - ML-readiness scoring (451 descriptors + 7 fingerprints)
 * - Structural alert screening (PAINS, BRENK)
 * - ChEMBL-compatible standardization
 */
public class ChemAuditValidator {

  private static final Logger logger = Logger.getLogger(ChemAuditValidator.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();

  private final File pythonExe;
  private final File validatorScript;
  private final int minValidationScore;
  private final int minMLReadinessScore;
  private final boolean enableAlerts;

  /**
   * Create a ChemAudit validator.
   *
   * @param pythonExe Python executable
   * @param minValidationScore Minimum validation score (0-100) to accept structures
   * @param minMLReadinessScore Minimum ML-readiness score (0-100) to accept structures
   * @param enableAlerts Whether to screen for structural alerts
   */
  public ChemAuditValidator(@NotNull File pythonExe, int minValidationScore,
      int minMLReadinessScore, boolean enableAlerts) {
    this.pythonExe = pythonExe;
    this.minValidationScore = minValidationScore;
    this.minMLReadinessScore = minMLReadinessScore;
    this.enableAlerts = enableAlerts;

    this.validatorScript = FileAndPathUtil.resolveInExternalToolsDir(
        "chemaudit/src/mzmine_chemaudit_validate.py");

    if (validatorScript == null || !validatorScript.isFile()) {
      throw new IllegalStateException(
          "ChemAudit validator script not found: expected at external_tools/chemaudit/src/mzmine_chemaudit_validate.py");
    }
  }

  /**
   * Validate a batch of SMILES structures.
   *
   * @param structures List of structures to validate, each with smiles, row_id, rank
   * @return List of validation results
   * @throws IOException if validation fails
   */
  public List<ChemAuditResult> validateBatch(@NotNull List<Map<String, Object>> structures)
      throws IOException {
    if (structures.isEmpty()) {
      return List.of();
    }

    // Write input JSON
    final File inputFile = FileAndPathUtil.createTempFile("mzmine_chemaudit_input_", ".json");
    inputFile.deleteOnExit();
    mapper.writeValue(inputFile, structures);

    // Build command
    final List<String> cmd = new ArrayList<>();
    cmd.add(pythonExe.getAbsolutePath());
    cmd.add(validatorScript.getAbsolutePath());
    cmd.add("--input");
    cmd.add(inputFile.getAbsolutePath());
    cmd.add("--min-validation-score");
    cmd.add(String.valueOf(minValidationScore));
    cmd.add("--min-ml-score");
    cmd.add(String.valueOf(minMLReadinessScore));
    if (!enableAlerts) {
      cmd.add("--disable-alerts");
    }

    // Resolve ChemAudit directory
    final File chemauditDir = FileAndPathUtil.resolveInExternalToolsDir("chemaudit/vendor/ChemAudit");
    if (chemauditDir != null && chemauditDir.isDirectory()) {
      cmd.add("--chemaudit-dir");
      cmd.add(chemauditDir.getAbsolutePath());
    }

    logger.fine(() -> "ChemAudit: validating " + structures.size() + " structures with command: "
        + String.join(" ", cmd));

    // Run validator
    try {
      final ProcessBuilder pb = new ProcessBuilder(cmd);
      // Don't merge stderr into stdout - keep them separate so JSON output is clean
      pb.redirectErrorStream(false);
      final Process p = pb.start();

      // Read stderr in a separate thread for logging
      final Thread stderrThread = new Thread(() -> {
        try (var reader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
          String line;
          while ((line = reader.readLine()) != null) {
            if (line.startsWith("MZMINE_CHEMAUDIT_LOG")) {
              final String logLine = line;
              logger.fine(() -> "ChemAudit: " + logLine.substring("MZMINE_CHEMAUDIT_LOG ".length()));
            } else if (line.startsWith("MZMINE_CHEMAUDIT_ERROR")) {
              final String errorLine = line;
              logger.warning(() -> "ChemAudit: " + errorLine.substring("MZMINE_CHEMAUDIT_ERROR ".length()));
            } else if (line.startsWith("MZMINE_CHEMAUDIT_PROGRESS")) {
              final String progressLine = line;
              logger.finest(() -> "ChemAudit: " + progressLine.substring("MZMINE_CHEMAUDIT_PROGRESS ".length()));
            } else {
              // Any other stderr output (warnings, errors, etc.)
              logger.warning("ChemAudit stderr: " + line);
            }
          }
        } catch (IOException e) {
          logger.log(Level.WARNING, "Error reading ChemAudit stderr", e);
        }
      });
      stderrThread.setDaemon(true);
      stderrThread.start();

      // Read stdout - this should contain ONLY the JSON output
      final StringBuilder output = new StringBuilder();
      try (var reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append('\n');
        }
      }

      final int exitCode = p.waitFor();
      stderrThread.join(5000); // Wait up to 5 seconds for stderr thread to finish
      
      final String outputStr = output.toString().trim();
      
      if (exitCode != 0) {
        // Log the actual output to help diagnose the issue
        logger.severe("ChemAudit validation failed with exit code " + exitCode + ". Output:\n" + outputStr);
        throw new IOException(
            "ChemAudit validation failed with exit code " + exitCode + ". Check logs for details.");
      }

      if (outputStr.isEmpty()) {
        logger.severe("ChemAudit validation produced no output");
        throw new IOException("ChemAudit validation produced no output. Check logs for details.");
      }

      // Parse results - if this fails, show what we actually received
      try {
        return mapper.readValue(outputStr, new TypeReference<List<ChemAuditResult>>() {});
      } catch (Exception e) {
        logger.severe("ChemAudit validation produced invalid JSON. First 500 chars of output:\n" 
            + outputStr.substring(0, Math.min(500, outputStr.length())));
        throw new IOException(
            "ChemAudit validation produced invalid JSON output. Check logs for details.", e);
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("ChemAudit validation interrupted", e);
    }
  }

  /**
   * Validate a single SMILES structure.
   *
   * @param smiles SMILES string
   * @param rowId Feature row ID
   * @param rank Rank of this structure
   * @return Validation result, or null if validation failed
   */
  public @Nullable ChemAuditResult validateSingle(@NotNull String smiles, int rowId, int rank) {
    try {
      final Map<String, Object> input = new HashMap<>();
      input.put("smiles", smiles);
      input.put("row_id", rowId);
      input.put("rank", rank);

      final List<ChemAuditResult> results = validateBatch(List.of(input));
      return results.isEmpty() ? null : results.get(0);

    } catch (IOException e) {
      logger.log(Level.WARNING, "ChemAudit validation failed for SMILES: " + smiles, e);
      return null;
    }
  }

  /**
   * Create a passthrough result that bypasses validation.
   * Used when validation is disabled.
   *
   * @param smiles SMILES string
   * @param rowId Feature row ID
   * @param rank Rank
   * @return Result that passes validation
   */
  public static ChemAuditResult createPassthroughResult(@NotNull String smiles, int rowId,
      int rank) {
    return new ChemAuditResult(rowId, rank, smiles, true, 100, 100,
        "Validation disabled", List.of(), smiles, List.of(), "unvalidated", null, null, null, null);
  }
}
