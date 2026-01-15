/*
 * Copyright (c) 2004-2026 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_diffms;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.SimpleCompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.DataTypes;
import io.github.mzmine.datamodel.features.types.annotations.CommentType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundDatabaseMatchesType;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.SmilesStructureType;
import io.github.mzmine.datamodel.features.types.annotations.formula.ConsensusFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DiffMSTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiffMSTask.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final Pattern MSMS_ANN_LOSS = Pattern.compile("^\\[M-(.+)]$");
  private static final Pattern MSMS_ANN_FORMULA = Pattern.compile("^\\[(.+)]$");
  private static final Pattern RUNNER_PROGRESS = Pattern.compile("^MZMINE_DIFFMS_PROGRESS (\\d+)/(\\d+)$");
  private static final Pattern RUNNER_STAGE = Pattern.compile("^MZMINE_DIFFMS_STAGE (.+)$");
  private static final String RUNNER_LOG_PREFIX = "MZMINE_DIFFMS_LOG ";
  private static final double ETA_EMA_ALPHA = 0.20; // smoother ETA, less jumpy

  private final @NotNull FeatureList flist;
  private final @Nullable List<ModularFeatureListRow> rowsOverride;
  private final @NotNull DiffMSParameters.Device device;
  private final int topK;
  private final int maxMs2Peaks;
  private final @NotNull MZTolerance subformulaTol;
  private final int subformulaBeam;
  private final @NotNull File pythonExe;
  private final @NotNull File diffmsDir;
  private final @NotNull File checkpoint;

  private String description = "DiffMS structure generation";
  private int totalRows;
  private int doneRows;

  public DiffMSTask(@NotNull final MZmineProject project,
      @NotNull final io.github.mzmine.parameters.ParameterSet parameters,
      @NotNull final FeatureList flist, @NotNull final Instant moduleCallDate) {
    this(project, parameters, flist, null, moduleCallDate);
  }

  public DiffMSTask(@NotNull final MZmineProject project,
      @NotNull final io.github.mzmine.parameters.ParameterSet parameters,
      @NotNull final FeatureList flist, @Nullable final List<ModularFeatureListRow> rowsOverride,
      @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.flist = flist;
    this.rowsOverride = rowsOverride == null ? null : List.copyOf(rowsOverride);
    this.pythonExe = parameters.getValue(DiffMSParameters.pythonExecutable);
    this.diffmsDir = parameters.getValue(DiffMSParameters.diffmsDir);
    this.checkpoint = parameters.getValue(DiffMSParameters.checkpoint);
    this.device = parameters.getValue(DiffMSParameters.device);
    this.topK = parameters.getValue(DiffMSParameters.topK);
    this.maxMs2Peaks = parameters.getValue(DiffMSParameters.maxMs2Peaks);
    this.subformulaTol = parameters.getValue(DiffMSParameters.subformulaTol);
    this.subformulaBeam = parameters.getValue(DiffMSParameters.subformulaBeam);
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return totalRows == 0 ? 0d : doneRows / (double) totalRows;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (!(flist instanceof ModularFeatureList mlist)) {
      throw new IllegalStateException("DiffMS requires a ModularFeatureList.");
    }
    mlist.addRowType(DataTypes.get(CompoundDatabaseMatchesType.class));

    if (pythonExe == null || !pythonExe.isFile()) {
      throw new IllegalStateException("Python executable not found.");
    }
    if (diffmsDir == null || !diffmsDir.isDirectory()) {
      throw new IllegalStateException("DiffMS directory not found.");
    }
    if (checkpoint == null || !checkpoint.isFile()) {
      throw new IllegalStateException("DiffMS checkpoint not found.");
    }

    final File runnerDir = FileAndPathUtil.resolveInExternalToolsDir("diffms/");
    final File runner = new File(runnerDir, "mzmine_diffms_predict.py");
    if (!runner.isFile()) {
      throw new IllegalStateException(
          "DiffMS runner not found. Expected external_tools/diffms/mzmine_diffms_predict.py");
    }

    final Map<Integer, ModularFeatureListRow> rowsById = new HashMap<>();
    final Map<Integer, String> formulaByRowId = new HashMap<>();
    final List<Map<String, Object>> input = new ArrayList<>();

    final List<? extends FeatureListRow> rows =
        rowsOverride != null ? rowsOverride : flist.getRows();
    totalRows = 0;
    doneRows = 0;
    int skippedNoFormula = 0;
    int skippedNoMs2 = 0;
    int skippedBadPolarity = 0;
    int skippedBadAdduct = 0;

    logger.info(() -> "DiffMS: preparing inputs for " + rows.size() + " rows from feature list '"
        + flist.getName() + "'" + (rowsOverride != null ? " (selected rows)" : ""));

    for (FeatureListRow row : rows) {
      if (isCanceled()) {
        return;
      }
      if (!Objects.equals(row.getFeatureList(), flist)) {
        throw new IllegalStateException("Selected rows must be from the same feature list.");
      }
      if (!(row instanceof ModularFeatureListRow mrow)) {
        throw new IllegalStateException("Unexpected row type.");
      }

      final String formula = resolveFormula(mrow);
      final List<Scan> ms2 = mrow.getAllFragmentScans();
      if (formula == null) {
        skippedNoFormula++;
        continue;
      }
      if (ms2.isEmpty()) {
        skippedNoMs2++;
        continue;
      }

      final String adduct;
      try {
        adduct = resolveAdduct(mrow);
      } catch (IllegalStateException e) {
        skippedBadAdduct++;
        logger.info(() -> "DiffMS: skipping row " + row.getID() + " due to adduct issue: "
            + e.getMessage());
        continue;
      }

      final var ms2MassLists = ms2.stream().map(Scan::getMassList).filter(Objects::nonNull).toList();
      if (ms2MassLists.isEmpty()) {
        skippedNoMs2++;
        logger.info(() -> "DiffMS: skipping row " + row.getID()
            + " because no MS/MS scans have mass lists (apply mass detection first)");
        continue;
      }

      final var merged = SpectraMerging.mergeSpectra(ms2MassLists, SpectraMerging.defaultMs2MergeTol,
          io.github.mzmine.datamodel.MergedMassSpectrum.MergingType.ALL_ENERGIES, null);
      final int n = merged.getNumberOfDataPoints();
      if (n == 0) {
        skippedNoMs2++;
        logger.info(() -> "DiffMS: skipping row " + row.getID() + " because merged MS/MS spectrum is empty");
        continue;
      }

      totalRows++;

      final double[] mzs = new double[n];
      final double[] ints = new double[n];
      merged.getMzValues(mzs);
      merged.getIntensityValues(ints);

      final List<Integer> idx = new ArrayList<>(n);
      for (int i = 0; i < n; i++) {
        idx.add(i);
      }
      idx.sort(Comparator.comparingDouble((Integer i) -> ints[i]).reversed());

      final int take = Math.min(maxMs2Peaks, n);
      final List<Double> mzOut = new ArrayList<>(take);
      final List<Double> intOut = new ArrayList<>(take);
      for (int k = 0; k < take; k++) {
        final int i = idx.get(k);
        mzOut.add(mzs[i]);
        intOut.add(ints[i]);
      }

      final PolarityType polarity = ms2.get(0).getPolarity();
      if (polarity == null) {
        throw new IllegalStateException("Missing polarity for row " + row.getID());
      }
      if (polarity == PolarityType.NEGATIVE) {
        skippedBadPolarity++;
        logger.info(() -> "DiffMS: skipping row " + row.getID()
            + " because negative polarity is not supported by DiffMS ion list");
        // do NOT count this row as eligible for DiffMS
        totalRows--;
        continue;
      }

      rowsById.put(row.getID(), mrow);
      formulaByRowId.put(row.getID(), formula);
      final List<Map<String, Object>> sub = resolveSubformulas(mrow, formula);

      final Scan firstMs2 = ms2.get(0);
      final MsMsInfo msmsInfo = firstMs2.getMsMsInfo();
      final Double precursorMz = firstMs2.getPrecursorMz();
      final Integer precursorCharge = firstMs2.getPrecursorCharge();
      final Float activationEnergy = msmsInfo == null ? null : msmsInfo.getActivationEnergy();
      final var method = msmsInfo == null ? null : msmsInfo.getActivationMethod();
      final String activationMethod = method == null ? null : method.name();
      final String scanDefinition = firstMs2.getScanDefinition();
      final String instrument = resolveInstrumentString(firstMs2);

      final Map<String, Object> item = new HashMap<>();
      item.put("rowId", row.getID());
      item.put("formula", formula);
      item.put("adduct", adduct);
      item.put("mzs", mzOut);
      item.put("intensities", intOut);
      item.put("polarity", "POSITIVE");
      item.put("subformulas", sub);
      // additional optional conditioning/debug info
      item.put("instrument", instrument);
      item.put("scanDefinition", scanDefinition);
      if (activationEnergy != null) {
        item.put("collisionEnergy", activationEnergy.doubleValue());
      }
      if (activationMethod != null) {
        item.put("activationMethod", activationMethod);
      }
      if (precursorMz != null) {
        item.put("precursorMz", precursorMz);
      } else {
        item.put("precursorMz", row.getAverageMZ());
      }
      if (precursorCharge != null) {
        item.put("precursorCharge", precursorCharge);
      }

      input.add(item);

      final int subCount = sub == null ? 0 : sub.size();
      final String subExample = sub == null || sub.isEmpty() ? ""
          : sub.stream().limit(5)
              .map(m -> Objects.toString(m.get("formula"), "?"))
              .collect(Collectors.joining(", "));
      final String msg = "DiffMS input rowId=" + row.getID()
          + " formula=" + formula
          + " adduct=" + adduct
          + " ms2Scans=" + ms2.size()
          + " mergedPeaks=" + n
          + " topPeaksSent=" + take
          + " subformulas=" + subCount
          + (subExample.isBlank() ? "" : " subformulasExample=[" + subExample + "]")
          + " instrument='" + instrument + "'"
          + " collisionEnergy=" + (activationEnergy == null ? "null" : activationEnergy)
          + " activationMethod=" + (activationMethod == null ? "null" : activationMethod)
          + " precursorMz=" + (precursorMz == null ? "null" : precursorMz)
          + " precursorCharge=" + (precursorCharge == null ? "null" : precursorCharge);
      logger.info(msg);
    }

    if (input.isEmpty()) {
      throw new IllegalStateException(
          "No rows eligible for DiffMS. Skipped rows without Formula: " + skippedNoFormula
              + ", without MS/MS: " + skippedNoMs2
              + ", bad adduct: " + skippedBadAdduct
              + ", negative polarity: " + skippedBadPolarity);
    }

    final File inFile;
    final File outFile;
    try {
      inFile = FileAndPathUtil.createTempFile("mzmine_diffms_input_", ".json");
      outFile = FileAndPathUtil.createTempFile("mzmine_diffms_output_", ".json");
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create DiffMS temp files.", e);
    }
    inFile.deleteOnExit();
    outFile.deleteOnExit();

    try {
      mapper.writeValue(inFile, input);
    } catch (IOException e) {
      throw new IllegalStateException("Cannot write DiffMS input JSON.", e);
    }

    doneRows = 0;
    description = "DiffMS: loading model";

    final List<String> cmd = new ArrayList<>();
    cmd.add(pythonExe.getAbsolutePath());
    cmd.add(runner.getAbsolutePath());
    cmd.add("--diffms-dir");
    cmd.add(diffmsDir.getAbsolutePath());
    cmd.add("--checkpoint");
    cmd.add(checkpoint.getAbsolutePath());
    cmd.add("--input");
    cmd.add(inFile.getAbsolutePath());
    cmd.add("--output");
    cmd.add(outFile.getAbsolutePath());
    cmd.add("--top-k");
    cmd.add(String.valueOf(topK));
    cmd.add("--max-ms2-peaks");
    cmd.add(String.valueOf(maxMs2Peaks));
    cmd.add("--subformula-tol");
    cmd.add(String.valueOf(subformulaTol.getMzTolerance()));
    cmd.add("--subformula-beam");
    cmd.add(String.valueOf(subformulaBeam));
    cmd.add("--device");
    cmd.add(device.toArg());

    logger.info(() -> "DiffMS: running python runner with " + input.size() + " items. "
        + "Input JSON: " + inFile.getAbsolutePath() + ", output JSON: " + outFile.getAbsolutePath());
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(() -> "DiffMS: runner command: " + String.join(" ", cmd));
    }

    runOrThrowWithProgress(diffmsDir, cmd);

    final List<Map<String, Object>> outputs;
    try {
      outputs = mapper.readValue(outFile, new TypeReference<>() {});
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read DiffMS output JSON.", e);
    }

    for (var out : outputs) {
      final int rowId = (Integer) out.get("rowId");
      final Object rawSmiles = out.get("smiles");
      if (!(rawSmiles instanceof List<?> rawList)) {
        continue;
      }
      final List<String> smiles = rawList.stream().map(Object::toString).toList();
      final ModularFeatureListRow row = rowsById.get(rowId);
      if (row == null) {
        continue;
      }

      final String formula = formulaByRowId.get(rowId);
      int rank = 1;
      for (String smi : smiles) {
        final SimpleCompoundDBAnnotation ann = new SimpleCompoundDBAnnotation();
        ann.put(DatabaseNameType.class, "DiffMS");
        ann.put(CompoundNameType.class, "DiffMS #" + rank);
        ann.put(FormulaType.class, formula);
        ann.put(SmilesStructureType.class, smi);
        ann.put(CommentType.class, "DiffMS");
        row.addCompoundAnnotation(ann);
        rank++;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void runOrThrowWithProgress(final File workDir, final List<String> cmd) {
    try {
      final ProcessBuilder b = new ProcessBuilder(cmd);
      b.directory(workDir);
      b.redirectErrorStream(true);
      final Process p = b.start();

      long lastProgressNanos = System.nanoTime();
      int lastProgressDone = 0;
      double emaSecondsPerRow = 0d;

      final StringBuilder err = new StringBuilder();
      try (var outReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = outReader.readLine()) != null) {
          final Matcher pm = RUNNER_PROGRESS.matcher(line);
          if (pm.matches()) {
            final int newDone = Integer.parseInt(pm.group(1));
            final int newTotal = Integer.parseInt(pm.group(2));

            totalRows = newTotal;
            doneRows = newDone;

            // Update ETA estimate only when progress advances.
            if (newDone > lastProgressDone) {
              final long now = System.nanoTime();
              final int delta = newDone - lastProgressDone;
              final long dtNanos = now - lastProgressNanos;
              final double sec = dtNanos / 1e9;
              if (sec > 0d && delta > 0) {
                final double secPerRow = sec / delta;
                emaSecondsPerRow =
                    emaSecondsPerRow <= 0 ? secPerRow
                        : (ETA_EMA_ALPHA * secPerRow + (1d - ETA_EMA_ALPHA) * emaSecondsPerRow);
              }
              lastProgressNanos = now;
              lastProgressDone = newDone;
            }

            description = formatProgressDescription(emaSecondsPerRow);
            continue;
          }
          final Matcher sm = RUNNER_STAGE.matcher(line);
          if (sm.matches()) {
            description = "DiffMS: " + sm.group(1) + formatEtaSuffix(emaSecondsPerRow);
            continue;
          }
          if (line.startsWith(RUNNER_LOG_PREFIX)) {
            final String msg = line.substring(RUNNER_LOG_PREFIX.length());
            logger.info(() -> "DiffMS runner: " + msg);
          } else {
            logger.fine("DiffMS runner: " + line);
          }
          // keep some output for error messages
          if (err.length() < 50_000) {
            err.append(line).append('\n');
          }
        }
      }
      final int code = p.waitFor();
      if (code != 0) {
        throw new IllegalStateException("DiffMS runner failed (" + code + "):\n" + err);
      }
    } catch (IOException | InterruptedException e) {
      throw new IllegalStateException("Failed to run DiffMS runner.", e);
    }
  }

  private String formatProgressDescription(final double emaSecondsPerRow) {
    // Prefer a stable "i/N" plus ETA once we have enough timing signal.
    final String base = "DiffMS: " + doneRows + "/" + totalRows;
    return base + formatEtaSuffix(emaSecondsPerRow);
  }

  private String formatEtaSuffix(final double emaSecondsPerRow) {
    if (totalRows <= 0 || doneRows <= 0) {
      return "";
    }
    if (emaSecondsPerRow <= 0d || Double.isNaN(emaSecondsPerRow) || Double.isInfinite(
        emaSecondsPerRow)) {
      return "";
    }
    final int remaining = Math.max(0, totalRows - doneRows);
    final long etaSec = Math.round(remaining * emaSecondsPerRow);
    final double rowsPerMin = 60d / emaSecondsPerRow;
    return " (%.1f rows/min, ETA %s)".formatted(rowsPerMin, formatDurationSeconds(etaSec));
  }

  private static String formatDurationSeconds(final long sec) {
    final long s = Math.max(0L, sec);
    final long h = s / 3600;
    final long m = (s % 3600) / 60;
    final long r = s % 60;
    if (h > 0) {
      return "%d:%02d:%02d".formatted(h, m, r);
    }
    return "%d:%02d".formatted(m, r);
  }

  private static String resolveInstrumentString(final Scan scan) {
    // MZmine does not expose a unified "instrument name" across all importers; scan definition is
    // the most widely available field. Keep it short to avoid breaking downstream instrument mapping.
    final String def = scan.getScanDefinition();
    if (def != null && !def.isBlank()) {
      // truncate extremely long scan definitions
      final String s = def.trim();
      return s.length() > 120 ? s.substring(0, 120) : s;
    }
    return "Unknown (LCMS)";
  }

  private static String resolveFormula(final ModularFeatureListRow row) {
    final String direct = row.get(FormulaType.class);
    if (direct != null && !direct.isBlank()) {
      return direct;
    }

    final List<ResultFormula> consensus = row.get(ConsensusFormulaListType.class);
    if (consensus != null && !consensus.isEmpty()) {
      final String f = consensus.get(0).getFormulaAsString();
      if (f != null && !f.isBlank()) {
        return f;
      }
    }

    final List<ResultFormula> formulas = row.get(FormulaListType.class);
    if (formulas != null && !formulas.isEmpty()) {
      final String f = formulas.get(0).getFormulaAsString();
      if (f != null && !f.isBlank()) {
        return f;
      }
    }

    final List<ResultFormula> fromAnnotations = ResultFormula.forAllAnnotations(row, true);
    if (!fromAnnotations.isEmpty()) {
      final String f = fromAnnotations.get(0).getFormulaAsString();
      if (f != null && !f.isBlank()) {
        return f;
      }
    }

    return null;
  }

  private static String resolveAdduct(final ModularFeatureListRow row) {
    final var ion = FeatureUtils.extractBestIonIdentity(null, row).orElse(null);
    if (ion == null) {
      throw new IllegalStateException(
          "Missing adduct/ion type for row " + row.getID()
              + " (run Ion Identity Networking / set Ion identity / ensure annotations include an adduct).");
    }
    if (ion.getPolarity() == PolarityType.NEGATIVE) {
      throw new IllegalStateException(
          "Negative polarity ion types are not supported by the DiffMS ion list: " + ion);
    }
    return ion.toString();
  }

  private static List<Map<String, Object>> resolveSubformulas(final ModularFeatureListRow row,
      final String parentFormula) {
    final List<ResultFormula> formulas = row.getFormulas();
    if (formulas.isEmpty()) {
      return List.of();
    }
    final ResultFormula rf = formulas.stream()
        .filter(f -> Objects.equals(f.getFormulaAsString(), parentFormula))
        .findFirst()
        .orElse(formulas.getFirst());
    final var ann = rf.getMSMSannotation();
    if (ann == null || ann.isEmpty()) {
      return List.of();
    }

    return ann.entrySet().stream()
        .sorted(Comparator.comparingDouble((Map.Entry<DataPoint, String> e) -> e.getKey()
            .getIntensity()).reversed())
        .limit(200)
        .map(e -> {
          final String v = e.getValue();
          if (v == null || v.isBlank()) {
            return null;
          }
          final String fragment = toFragmentFormula(parentFormula, v.trim());
          if (fragment == null || fragment.isBlank()) {
            return null;
          }
          return Map.<String, Object>of("formula", fragment, "intensity", e.getKey().getIntensity());
        })
        .filter(Objects::nonNull)
        .toList();
  }

  private static @Nullable String toFragmentFormula(final String parentFormula,
      final String msmsAnnotation) {
    final Matcher mLoss = MSMS_ANN_LOSS.matcher(msmsAnnotation);
    if (mLoss.matches()) {
      final String loss = mLoss.group(1);
      return subtractFormula(parentFormula, loss);
    }
    final Matcher mF = MSMS_ANN_FORMULA.matcher(msmsAnnotation);
    if (mF.matches()) {
      final String f = mF.group(1);
      return f == null ? null : f.trim();
    }
    return null;
  }

  private static @Nullable String subtractFormula(final String parent, final String loss) {
    final Map<String, Integer> p = parseFormulaCounts(parent);
    final Map<String, Integer> l = parseFormulaCounts(loss);
    if (p.isEmpty() || l.isEmpty()) {
      return null;
    }
    for (var e : l.entrySet()) {
      final String el = e.getKey();
      final int v = e.getValue() == null ? 0 : e.getValue();
      p.put(el, p.getOrDefault(el, 0) - v);
    }
    final StringBuilder sb = new StringBuilder();
    for (var e : p.entrySet()) {
      final int n = e.getValue();
      if (n < 0) {
        return null;
      }
      if (n == 0) {
        continue;
      }
      sb.append(e.getKey());
      if (n != 1) {
        sb.append(n);
      }
    }
    return sb.isEmpty() ? null : sb.toString();
  }

  private static Map<String, Integer> parseFormulaCounts(final String formula) {
    if (formula == null) {
      return Map.of();
    }
    final var s = formula.trim();
    if (s.isEmpty()) {
      return Map.of();
    }
    final Map<String, Integer> out = new HashMap<>();
    final Matcher m = Pattern.compile("([A-Z][a-z]*)(\\d*)").matcher(s);
    while (m.find()) {
      final String el = m.group(1);
      final String n = m.group(2);
      final int v = n == null || n.isEmpty() ? 1 : Integer.parseInt(n);
      out.put(el, out.getOrDefault(el, 0) + v);
    }
    return out;
  }
}

