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
import io.github.mzmine.modules.dataprocessing.id_formulaprediction.ResultFormula;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.files.FileAndPathUtil;
import io.github.mzmine.util.scans.SpectraMerging;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class DiffMSTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(DiffMSTask.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();

  private final @NotNull FeatureList flist;
  private final @NotNull DiffMSParameters.Device device;
  private final int topK;
  private final int maxMs2Peaks;
  private final double subformulaTolDa;
  private final int subformulaBeam;
  private final @NotNull File pythonExe;
  private final @NotNull File diffmsDir;
  private final @NotNull File checkpoint;

  private String description = "DiffMS structure generation";
  private int totalRows;
  private int doneRows;

  public DiffMSTask(@NotNull final MZmineProject project, @NotNull final io.github.mzmine.parameters.ParameterSet parameters,
      @NotNull final FeatureList flist, @NotNull final Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.flist = flist;
    this.pythonExe = parameters.getValue(DiffMSParameters.pythonExecutable);
    this.diffmsDir = parameters.getValue(DiffMSParameters.diffmsDir);
    this.checkpoint = parameters.getValue(DiffMSParameters.checkpoint);
    this.device = parameters.getValue(DiffMSParameters.device);
    this.topK = parameters.getValue(DiffMSParameters.topK);
    this.maxMs2Peaks = parameters.getValue(DiffMSParameters.maxMs2Peaks);
    this.subformulaTolDa = parameters.getValue(DiffMSParameters.subformulaTolDa);
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

    final List<FeatureListRow> rows = flist.getRows();
    totalRows = 0;
    doneRows = 0;
    int skippedNoFormula = 0;
    int skippedNoMs2 = 0;

    for (FeatureListRow row : rows) {
      if (isCanceled()) {
        return;
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

      totalRows++;
      final var merged = SpectraMerging.mergeSpectra(ms2, SpectraMerging.defaultMs2MergeTol,
          io.github.mzmine.datamodel.MergedMassSpectrum.MergingType.ALL_ENERGIES, null);
      final int n = merged.getNumberOfDataPoints();
      if (n == 0) {
        throw new IllegalStateException("Empty merged MS/MS spectrum for row " + row.getID());
      }

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

      rowsById.put(row.getID(), mrow);
      formulaByRowId.put(row.getID(), formula);
      input.add(Map.of("rowId", row.getID(), "formula", formula, "mzs", mzOut, "intensities", intOut,
          "polarity", polarity == PolarityType.NEGATIVE ? "NEGATIVE" : "POSITIVE"));
      doneRows++;
    }

    if (input.isEmpty()) {
      throw new IllegalStateException(
          "No rows eligible for DiffMS. Skipped rows without Formula: " + skippedNoFormula
              + ", without MS/MS: " + skippedNoMs2);
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
    cmd.add(String.valueOf(subformulaTolDa));
    cmd.add("--subformula-beam");
    cmd.add(String.valueOf(subformulaBeam));
    cmd.add("--device");
    cmd.add(device.toArg());

    runOrThrow(diffmsDir, cmd);

    final List<Map<String, Object>> outputs;
    try {
      outputs = mapper.readValue(outFile, new TypeReference<>() {});
    } catch (IOException e) {
      throw new IllegalStateException("Cannot read DiffMS output JSON.", e);
    }

    for (var out : outputs) {
      final int rowId = (Integer) out.get("rowId");
      final List<String> smiles = (List<String>) out.get("smiles");
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

  private static void runOrThrow(final File workDir, final List<String> cmd) {
    try {
      final ProcessBuilder b = new ProcessBuilder(cmd);
      b.directory(workDir);
      final Process p = b.start();
      final StringBuilder err = new StringBuilder();
      try (var outReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
          var errReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
        String line;
        while ((line = outReader.readLine()) != null) {
          logger.finest(line);
        }
        while ((line = errReader.readLine()) != null) {
          err.append(line).append('\n');
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
}

