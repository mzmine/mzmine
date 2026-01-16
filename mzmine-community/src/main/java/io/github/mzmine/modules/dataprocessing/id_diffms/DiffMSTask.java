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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
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
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.features.types.annotations.formula.ConsensusFormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaListType;
import io.github.mzmine.datamodel.features.types.annotations.formula.FormulaType;
import io.github.mzmine.datamodel.features.types.annotations.iin.IonTypeType;
import io.github.mzmine.datamodel.features.types.numbers.CCSRelativeErrorType;
import io.github.mzmine.datamodel.features.types.numbers.RtAbsoluteDifferenceType;
import io.github.mzmine.datamodel.identities.iontype.IonModification;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.datamodel.impl.SimpleMassSpectrum;
import io.github.mzmine.datamodel.msms.MsMsInfo;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.FragmentUtils;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.SignalWithFormulae;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.DataPointSorter;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.FormulaUtils;
import io.github.mzmine.util.FormulaWithExactMz;
import io.github.mzmine.util.RawDataFileType;
import io.github.mzmine.util.RawDataFileTypeDetector;
import io.github.mzmine.util.annotations.CompoundAnnotationUtils;
import io.github.mzmine.util.annotations.ConnectedTypeCalculation;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.ScanUtils;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class DiffMSTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiffMSTask.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();
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
    final Map<Integer, IonType> adductByRowId = new HashMap<>();
    final List<DiffMSInputItem> input = new ArrayList<>();

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

      final IonType adduct;
      try {
        adduct = resolveAdduct(mrow, formula);
      } catch (IllegalStateException e) {
        skippedBadAdduct++;
        logger.info(() -> "DiffMS: skipping row " + row.getID() + " due to adduct issue: "
            + e.getMessage());
        continue;
      }

      final var ms2WithMassList = ms2.stream().filter(s -> s.getMassList() != null).toList();
      if (ms2WithMassList.isEmpty()) {
        skippedNoMs2++;
        logger.info(() -> "DiffMS: skipping row " + row.getID()
            + " because no MS/MS scans have mass lists (apply mass detection first)");
        continue;
      }

      final var merged = SpectraMerging.mergeSpectra(ms2WithMassList, SpectraMerging.defaultMs2MergeTol,
          io.github.mzmine.datamodel.MergedMassSpectrum.MergingType.ALL_ENERGIES, null);
      final int n = merged.getNumberOfDataPoints();
      if (n == 0) {
        skippedNoMs2++;
        logger.info(() -> "DiffMS: skipping row " + row.getID() + " because merged MS/MS spectrum is empty");
        continue;
      }

      totalRows++;

      final DataPoint[] dps = ScanUtils.extractDataPoints(merged, true);
      Arrays.sort(dps, DataPointSorter.DEFAULT_INTENSITY);

      final int take = Math.min(maxMs2Peaks, dps.length);
      final List<Double> mzOut = new ArrayList<>(take);
      final List<Double> intOut = new ArrayList<>(take);
      for (int i = 0; i < take; i++) {
        mzOut.add(dps[i].getMZ());
        intOut.add(dps[i].getIntensity());
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
      adductByRowId.put(row.getID(), adduct);

      final double[] subMzs = new double[take];
      final double[] subIntens = new double[take];
      for (int i = 0; i < take; i++) {
        subMzs[i] = dps[i].getMZ();
        subIntens[i] = dps[i].getIntensity();
      }
      final var topNms2 = new SimpleMassSpectrum(subMzs, subIntens);
      final List<DiffMSSubformula> sub = resolveSubformulas(mrow, formula, adduct, topNms2);

      final Scan firstMs2 = ms2.get(0);
      final MsMsInfo msmsInfo = firstMs2.getMsMsInfo();
      final Double precursorMz = firstMs2.getPrecursorMz();
      final Integer precursorCharge = firstMs2.getPrecursorCharge();
      final Float activationEnergy = msmsInfo == null ? null : msmsInfo.getActivationEnergy();
      final var method = msmsInfo == null ? null : msmsInfo.getActivationMethod();
      final String activationMethod = method == null ? null : method.name();
      final String scanDefinition = firstMs2.getScanDefinition();
      final String instrument = resolveInstrumentString(firstMs2);

      final DiffMSInputItem item = new DiffMSInputItem(row.getID(), formula, adduct.toString(),
          mzOut, intOut, "POSITIVE", sub, instrument, scanDefinition,
          activationEnergy == null ? null : activationEnergy.doubleValue(), activationMethod,
          precursorMz != null ? precursorMz : row.getAverageMZ(), precursorCharge);

      input.add(item);
    }

    if (input.isEmpty()) {
      throw new IllegalStateException(
          "No rows eligible for DiffMS. Skipped rows without Formula: " + skippedNoFormula
              + ", without MS/MS: " + skippedNoMs2
              + ", bad adduct: " + skippedBadAdduct
              + ", negative polarity: " + skippedBadPolarity);
    }

    logger.info(
        "DiffMS: prepared %d rows for prediction. Skipped: %d (no formula), %d (no MS/MS), %d (bad adduct), %d (bad polarity)"
            .formatted(input.size(), skippedNoFormula, skippedNoMs2, skippedBadAdduct,
                skippedBadPolarity));

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

    if (isCanceled()) {
      return;
    }

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
      final IonType adduct = adductByRowId.get(rowId);
      int rank = 1;
      for (String smi : smiles) {
        final SimpleCompoundDBAnnotation ann = new SimpleCompoundDBAnnotation();
        ann.put(DatabaseNameType.class, "DiffMS");
        ann.put(CompoundNameType.class, "DiffMS #" + rank);
        ann.put(FormulaType.class, formula);
        ann.put(IonTypeType.class, adduct);
        ann.put(SmilesStructureType.class, smi);
        ann.put(CommentType.class, "DiffMS");
        // manually call calculations to avoid RT absolute error log if RT is missing
        ConnectedTypeCalculation.LIST.forEach(calc -> {
          if (calc.typeToCalculate() instanceof RtAbsoluteDifferenceType
              || calc.typeToCalculate() instanceof CCSRelativeErrorType) {
            return;
          }
          calc.calculateIfAbsent(row, ann);
        });
        row.addCompoundAnnotation(ann);
        rank++;
      }
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void runOrThrowWithProgress(final File workDir, final List<String> cmd) {
    Process p = null;
    try {
      final ProcessBuilder b = new ProcessBuilder(cmd);
      b.directory(workDir);
      b.redirectErrorStream(true);
      p = b.start();

      long lastProgressNanos = System.nanoTime();
      int lastProgressDone = 0;
      double emaSecondsPerRow = 0d;

      final StringBuilder err = new StringBuilder();
      try (var outReader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
        String line;
        while ((line = outReader.readLine()) != null) {
          if (isCanceled()) {
            return;
          }
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
      if (isCanceled()) {
        return;
      }
      throw new IllegalStateException("Failed to run DiffMS runner.", e);
    } finally {
      if (p != null && p.isAlive()) {
        p.destroyForcibly();
      }
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
    final File file = scan.getDataFile().getAbsoluteFilePath();
    final RawDataFileType type = RawDataFileTypeDetector.detectDataFileType(file);

    if (type != null) {
      switch (type) {
        case THERMO_RAW -> {
          return "Orbitrap (LCMS)";
        }
        case BRUKER_TDF, BRUKER_TSF, BRUKER_BAF, WATERS_RAW, WATERS_RAW_IMS, SCIEX_WIFF, SCIEX_WIFF2,
             SHIMADZU_LCD, AGILENT_D, AGILENT_D_IMS, MBI -> {
          return "Q-ToF (LCMS)";
        }
      }
    }

    // Fallback to scan definition
    final String def = scan.getScanDefinition().toLowerCase();
    if (def.contains("orbitrap") || def.contains("ftms")) {
      return "Orbitrap (LCMS)";
    }
    if (def.contains("qtof") || def.contains("q-tof") || def.contains("tof")) {
      return "Q-ToF (LCMS)";
    }

    return "Unknown (LCMS)";
  }

  private static String resolveFormula(final ModularFeatureListRow row) {
    final List<ResultFormula> fromAnnotations = ResultFormula.forAllAnnotations(row, true);
    if (!fromAnnotations.isEmpty()) {
      final String f = fromAnnotations.get(0).getFormulaAsString();
      if (f != null && !f.isBlank()) {
        return f;
      }
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

    return null;
  }

  private static IonType resolveAdduct(final ModularFeatureListRow row, final String formula) {
    final var ion = FeatureUtils.extractBestIonIdentity(null, row).orElse(null);
    if (ion != null) {
      if (ion.getPolarity() == PolarityType.NEGATIVE) {
        throw new IllegalStateException(
            "Negative polarity ion types are not supported by the DiffMS ion list: " + ion);
      }
      return ion;
    }

    // Try to guess from formula and precursor m/z
    if (formula != null) {
      final double neutralMass = FormulaUtils.calculateExactMass(formula);
      final double mz = row.getAverageMZ();
      final IonModification mod = IonModification.getBestIonModification(neutralMass, mz,
          SpectraMerging.defaultMs2MergeTol, PolarityType.POSITIVE);
      if (mod != null) {
        if (mod.getPolarity() == PolarityType.NEGATIVE) {
          throw new IllegalStateException(
              "Guessed negative polarity ion modification is not supported by the DiffMS ion list: "
                  + mod);
        }
        return new IonType(mod);
      }
    }

    throw new IllegalStateException(
        "Missing adduct/ion type for row " + row.getID()
            + " (run Ion Identity Networking / set Ion identity / ensure annotations include an adduct).");
  }

  private List<DiffMSSubformula> resolveSubformulas(final ModularFeatureListRow row,
      final String parentFormulaStr, final IonType adduct, final MassSpectrum mergedMs2) {
    try {
      final IMolecularFormula formula = FormulaUtils.createMajorIsotopeMolFormula(parentFormulaStr);
      if (formula == null) {
        return List.of();
      }
      final IMolecularFormula ionFormula = adduct.addToFormula(formula);
      final List<SignalWithFormulae> peaksWithFormulae = FragmentUtils.getPeaksWithFormulae(
          ionFormula, mergedMs2, SpectralSignalFilter.DEFAULT_NO_PRECURSOR, subformulaTol);

      return peaksWithFormulae.stream().flatMap(swf -> swf.formulae().stream().map(
          f -> new DiffMSSubformula(f.formulaString(), swf.peak().getIntensity()))).toList();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not resolve subformulas for row " + row.getID(), e);
      return List.of();
    }
  }

  @JsonInclude(Include.NON_NULL)
  private record DiffMSSubformula(String formula, double intensity) {

  }

  @JsonInclude(Include.NON_NULL)
  private record DiffMSInputItem(int rowId, String formula, String adduct, List<Double> mzs,
                                 List<Double> intensities, String polarity,
                                 List<DiffMSSubformula> subformulas, String instrument,
                                 String scanDefinition, @Nullable Double collisionEnergy,
                                 @Nullable String activationMethod, double precursorMz,
                                 @Nullable Integer precursorCharge) {

  }
}

