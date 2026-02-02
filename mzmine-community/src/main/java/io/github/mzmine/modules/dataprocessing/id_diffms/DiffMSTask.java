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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Doubles;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrum;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.FeatureAnnotation;
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
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.FeatureUtils;
import io.github.mzmine.util.FormulaUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;

import io.github.mzmine.util.FeatureListUtils;

public class DiffMSTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiffMSTask.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();
  static {
    mapper.configure(com.fasterxml.jackson.core.JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
  }
  private static final Pattern RUNNER_PROGRESS = Pattern.compile("^MZMINE_DIFFMS_PROGRESS (\\d+)/(\\d+)$");
  private static final Pattern RUNNER_STAGE = Pattern.compile("^MZMINE_DIFFMS_STAGE (.+)$");
  private static final Pattern RUNNER_RESULT = Pattern.compile("^MZMINE_DIFFMS_RESULT_JSON (.+)$");
  private static final String RUNNER_LOG_PREFIX = "MZMINE_DIFFMS_LOG ";
  private static final double ETA_EMA_ALPHA = 0.20;
  private static final Set<String> DIFFMS_SUPPORTED_ELEMENTS = Set.of("C", "H", "N", "O", "P", "S", "Cl", "F");

  private final @NotNull FeatureList flist;
  private final @Nullable List<ModularFeatureListRow> rowsOverride;
  private final @NotNull DiffMSParameters.Device device;
  private final int topK;
  private final int maxMs2Peaks;
  private final @NotNull MZTolerance subformulaTol;
  private final @NotNull File pythonExe;
  private final @NotNull File diffmsDir;
  private final @NotNull File checkpoint;

  private final Map<Integer, ModularFeatureListRow> rowsById = new HashMap<>();
  private final Map<Integer, String> formulaByRowId = new HashMap<>();
  private final Map<Integer, IonType> adductByRowId = new HashMap<>();

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
    
    // Check if we have specific row IDs in parameters (from SelectedRowsDiffMSParameters)
    if (rowsOverride != null) {
      this.rowsOverride = List.copyOf(rowsOverride);
    } else if (parameters instanceof SelectedRowsDiffMSParameters 
        && parameters.getParameter(SelectedRowsDiffMSParameters.rowIds).getValue() != null 
        && !parameters.getParameter(SelectedRowsDiffMSParameters.rowIds).getValue().isBlank()
        && flist instanceof ModularFeatureList modularFlist) {
       this.rowsOverride = FeatureListUtils.idStringToRows(modularFlist, 
           parameters.getParameter(SelectedRowsDiffMSParameters.rowIds).getValue())
           .stream().map(ModularFeatureListRow.class::cast).toList();
    } else {
      this.rowsOverride = null;
    }

    this.pythonExe = parameters.getValue(DiffMSParameters.pythonExecutable);
    this.diffmsDir = parameters.getValue(DiffMSParameters.diffmsDir);
    this.checkpoint = parameters.getValue(DiffMSParameters.checkpoint);
    this.device = parameters.getValue(DiffMSParameters.device);
    this.topK = parameters.getValue(DiffMSParameters.topK);
    this.maxMs2Peaks = parameters.getValue(DiffMSParameters.maxMs2Peaks);
    this.subformulaTol = parameters.getValue(DiffMSParameters.subformulaTol);
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

    final List<DiffMSInputItem> input = new ArrayList<>();

    final List<? extends FeatureListRow> rows =
        rowsOverride != null ? rowsOverride : flist.getRows();
    totalRows = 0;
    doneRows = 0;
    int skippedNoFormula = 0;
    int skippedNoMs2 = 0;
    int skippedBadPolarity = 0;
    int skippedBadAdduct = 0;
    int skippedUnsupportedElements = 0;

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

      final FeatureAnnotation bestAnn = CompoundAnnotationUtils.getBestFeatureAnnotation(mrow)
          .orElse(null);
      final String formula;
      final IonType adduct;

      if (bestAnn != null && bestAnn.getFormula() != null && bestAnn.getAdductType() != null) {
        formula = bestAnn.getFormula();
        adduct = bestAnn.getAdductType();
      } else {
        formula = resolveFormula(mrow);
        if (formula == null) {
          skippedNoFormula++;
          continue;
        }

        adduct = resolveAdduct(mrow, formula);
        if (adduct == null) {
          skippedBadAdduct++;
          logger.info(() -> "DiffMS: skipping row " + row.getID()
              + " due to missing or unsupported adduct (run Ion Identity Networking / set Ion identity / ensure annotations include an adduct).");
          continue;
        }
      }

      if (formula != null) {
        final Map<String, Integer> parsedFormula = FormulaUtils.parseFormula(formula);
        final List<String> unsupportedElements = parsedFormula.keySet().stream()
            .filter(e -> !DIFFMS_SUPPORTED_ELEMENTS.contains(e)).toList();
        if (!unsupportedElements.isEmpty()) {
          skippedUnsupportedElements++;
          logger.info(() -> "DiffMS: skipping row " + row.getID() + " due to unsupported elements: "
              + unsupportedElements);
          continue;
        }
      }

      final List<Scan> ms2 = mrow.getAllFragmentScans();
      if (ms2.isEmpty()) {
        skippedNoMs2++;
        continue;
      }

      // DiffMS/MIST expects the parent formula to be neutral (no adduct / no in-source modifications
      // baked into the formula). Some pipelines store ionized/modified formulas; try to normalize.
      final String neutralFormula = normalizeParentFormulaIfNeeded(mrow, formula, adduct);

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
      final List<DataPoint> topNms2Points = Arrays.stream(dps).sorted(DataPointSorter.DEFAULT_INTENSITY)
          .limit(maxMs2Peaks).sorted(DataPointSorter.DEFAULT_MZ_ASCENDING).toList();

      final double[][] topNms2Data = DataPointUtils.getDataPointsAsDoubleArray(topNms2Points);
      final double[] subMzs = topNms2Data[0];
      final double[] subIntens = topNms2Data[1];

      final List<Double> mzOut = Doubles.asList(subMzs);
      final List<Double> intOut = Doubles.asList(subIntens);

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
      formulaByRowId.put(row.getID(), neutralFormula);
      adductByRowId.put(row.getID(), adduct);

      final var topNms2 = new SimpleMassSpectrum(subMzs, subIntens);
      final List<DiffMSSubformula> sub = resolveSubformulas(mrow, neutralFormula, adduct, topNms2);

      final Scan firstMs2 = ms2.get(0);
      final MsMsInfo msmsInfo = firstMs2.getMsMsInfo();
      final Double precursorMz = firstMs2.getPrecursorMz();
      final Integer precursorCharge = firstMs2.getPrecursorCharge();
      final Float activationEnergy = msmsInfo == null ? null : msmsInfo.getActivationEnergy();
      final var method = msmsInfo == null ? null : msmsInfo.getActivationMethod();
      final String activationMethod = method == null ? null : method.name();
      final String scanDefinition = firstMs2.getScanDefinition();
      final String instrument = resolveInstrumentString(firstMs2);

      final DiffMSInputItem item = new DiffMSInputItem(row.getID(), neutralFormula, adduct.toString(),
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
              + ", negative polarity: " + skippedBadPolarity
              + ", unsupported elements: " + skippedUnsupportedElements);
    }

    logger.info(
        "DiffMS: prepared %d rows for prediction. Skipped: %d (no formula), %d (no MS/MS), %d (bad adduct), %d (bad polarity), %d (unsupported elements)"
            .formatted(input.size(), skippedNoFormula, skippedNoMs2, skippedBadAdduct,
                skippedBadPolarity, skippedUnsupportedElements));

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

    // We do not rely on the output file anymore for results, but still pass it as argument
    // to keep the interface compatible if we wanted to read it at the end.
    // However, we now process results incrementally.
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
    cmd.add("--device");
    cmd.add(device.toArg());

    logger.info(() -> "DiffMS: running python runner with " + input.size() + " items. "
        + "Input JSON: " + inFile.getAbsolutePath() + ", output JSON: " + outFile.getAbsolutePath());
    if (logger.isLoggable(Level.FINE)) {
      logger.fine(() -> "DiffMS: runner command: " + String.join(" ", cmd));
    }

    final long startTime = System.nanoTime();
    try {
      runOrThrowWithProgress(diffmsDir, cmd);
    } catch (Exception e) {
      if (isCanceled()) {
        return;
      }
      logger.log(Level.SEVERE, "DiffMS: runner failed. Partial results might be available.", e);
    }

    final long endTime = System.nanoTime();
    final double totalSec = (endTime - startTime) / 1e9;
    final double avgSecPerRow = totalRows > 0 ? totalSec / totalRows : 0;

    logger.info(
        "DiffMS: finished in %.1f s (avg %.2f s/row) for %d rows".formatted(totalSec, avgSecPerRow,
            totalRows));

    // Results are processed incrementally via processResultItem called from runOrThrowWithProgress
    if (isCanceled()) {
      return;
    }

    setStatus(TaskStatus.FINISHED);
  }

  private void processResultItem(DiffMSResultItem res) {
    if (isCanceled()) {
      return;
    }
    final int rowId = res.rowId();
    final List<String> smiles = res.smiles();
    if (smiles == null || smiles.isEmpty()) {
      return;
    }

    final ModularFeatureListRow row = rowsById.get(rowId);
    if (row == null) {
      return;
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
            final String stage = sm.group(1);
            description = "DiffMS: " + stage + formatEtaSuffix(emaSecondsPerRow);
            if (stage.contains("inference")) {
              lastProgressNanos = System.nanoTime();
              lastProgressDone = doneRows;
              emaSecondsPerRow = 0d;
            }
            continue;
          }
          final Matcher resM = RUNNER_RESULT.matcher(line);
          if (resM.matches()) {
            try {
              final DiffMSResultItem res = mapper.readValue(resM.group(1), DiffMSResultItem.class);
              processResultItem(res);
            } catch (IOException e) {
              logger.log(Level.WARNING, "DiffMS: cannot parse result JSON: " + resM.group(1), e);
            }
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
    // Priority 1: Feature annotations (this is also handled in the main loop, but here we cover the case
    // where we only have a formula but no adduct in the annotation, and want to use that formula)
    final String bestAnnFormula = CompoundAnnotationUtils.getBestFormula(row);
    if (bestAnnFormula != null) {
      return bestAnnFormula;
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

  private static @Nullable IonType resolveAdduct(final ModularFeatureListRow row, final String formula) {
    final var ion = FeatureUtils.extractBestIonIdentity(null, row).orElse(null);
    if (ion != null) {
      if (ion.getPolarity() == PolarityType.NEGATIVE) {
        return null;
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
          return null;
        }
        return new IonType(mod);
      }
    }

    return null;
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

      // DiffMS expects neutral subformulas. The fragment formulae returned by FragmentUtils are
      // subformulas of the ionized/modified precursor formula; we therefore "unapply" the same
      // ion type again so PeakFormula can re-apply it via the "ions" field.
      final String ionStr = adduct.toString();
      return peaksWithFormulae.stream().flatMap(swf -> swf.formulae().stream().map(f -> {
        final String neutralFrag = unapplyIonTypeToNeutralFormulaString(f.formula(), adduct);
        if (neutralFrag == null || neutralFrag.isBlank()) {
          return null;
        }
        return new DiffMSSubformula(neutralFrag, swf.peak().getIntensity(), ionStr);
      })).filter(Objects::nonNull).toList();
    } catch (Exception e) {
      logger.log(Level.WARNING, "Could not resolve subformulas for row " + row.getID(), e);
      return List.of();
    }
  }

  /**
   * DiffMS/MIST expects neutral (de-adducted) formulas, with the ion/adduct provided separately.
   * This tries to detect and fix cases where the stored formula already includes the ion type's
   * adduct and/or in-source modifications.
   */
  private static String normalizeParentFormulaIfNeeded(final ModularFeatureListRow row,
      final String formula, final IonType ionType) {
    if (formula == null || formula.isBlank()) {
      return formula;
    }
    // If we cannot compute a neutral m/z for this ion type, do not attempt normalization.
    if (ionType.getAbsCharge() == 0) {
      return formula;
    }

    final double observedMz = row.getAverageMZ();
    final MZTolerance tolerance = SpectraMerging.defaultMs2MergeTol;

    final double mzA;
    try {
      mzA = ionType.getMZ(FormulaUtils.calculateExactMass(formula));
    } catch (Exception e) {
      return formula;
    }

    // If it already matches reasonably well, treat it as neutral and keep.
    if (tolerance.checkWithinTolerance(mzA, observedMz)) {
      return formula;
    }

    final String candidate = unapplyIonTypeToNeutralFormulaString(formula, ionType);
    if (candidate == null || candidate.isBlank()) {
      return formula;
    }

    final double mzB;
    try {
      mzB = ionType.getMZ(FormulaUtils.calculateExactMass(candidate));
    } catch (Exception e) {
      return formula;
    }

    final double diffA = Math.abs(mzA - observedMz);
    final double diffB = Math.abs(mzB - observedMz);

    // Prefer the neutralized candidate if it improves the m/z agreement and is within tolerance.
    if (diffB < diffA && tolerance.checkWithinTolerance(mzB, observedMz)) {
      logger.fine(() -> "DiffMS: normalized ionized parent formula for row " + row.getID() + " from "
          + formula + " to " + candidate + " for ion " + ionType);
      return candidate;
    }
    return formula;
  }

  /**
   * Inverts {@link IonType#addToFormula(IMolecularFormula)} at the elemental-composition level.
   * Returns a neutral formula string (charge cleared) or null if the operation fails.
   */
  private static @Nullable String unapplyIonTypeToNeutralFormulaString(
      final @NotNull IMolecularFormula ionFormula, final @NotNull IonType ionType) {
    try {
      final IMolecularFormula result = ionType.removeFromFormula(ionFormula);
      return MolecularFormulaManipulator.getString(result);
    } catch (Exception e) {
      return null;
    }
  }

  private static @Nullable String unapplyIonTypeToNeutralFormulaString(
      final @NotNull String maybeIonizedFormula, final @NotNull IonType ionType) {
    final IMolecularFormula f = FormulaUtils.createMajorIsotopeMolFormula(maybeIonizedFormula);
    if (f == null) {
      return null;
    }
    return unapplyIonTypeToNeutralFormulaString(f, ionType);
  }

  @JsonInclude(Include.NON_NULL)
  private record DiffMSResultItem(int rowId, List<String> smiles) {

  }

  @JsonInclude(Include.NON_NULL)
  private record DiffMSSubformula(String formula, double intensity, @Nullable String ion) {

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

