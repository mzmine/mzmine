/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_fraggraph;

import io.github.mzmine.datamodel.MergedMassSpectrum.MergingType;
import io.github.mzmine.datamodel.MergedMsMsSpectrum;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.identities.iontype.IonType;
import io.github.mzmine.modules.dataprocessing.group_spectral_networking.SpectralSignalFilter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.combowithinput.MsLevelFilter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractFeatureListTask;
import io.github.mzmine.taskcontrol.SimpleCalculationTask;
import io.github.mzmine.taskcontrol.TaskService;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.scans.FragmentScanSelection;
import io.github.mzmine.util.scans.FragmentScanSelection.IncludeInputSpectra;
import io.github.mzmine.util.scans.SpectraMerging.IntensityMergingType;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openscience.cdk.interfaces.IMolecularFormula;

public class FragmentGraphCalcTask extends AbstractFeatureListTask {

  private static final Logger logger = Logger.getLogger(FragmentGraphCalcTask.class.getName());

  private final FeatureList flist;
  private MZTolerance ms2MergeTol = new MZTolerance(0.005, 5);
  private FragmentScanSelection selection = new FragmentScanSelection(ms2MergeTol, true,
      IncludeInputSpectra.NONE, IntensityMergingType.MAXIMUM, MsLevelFilter.ALL_LEVELS, null);
  private MZTolerance formulaTolerance = new MZTolerance(0.005, 5);
  private final boolean checkCHONPS;
  private final boolean checkRDBE;
  private final SpectralSignalFilter defaultSignalFilter = new SpectralSignalFilter(true, 10, 50,
      200, 98);

  public FragmentGraphCalcTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      ParameterSet parameters, FeatureList flist) {
    super(storage, moduleCallDate, parameters, FragmentGraphCalcModule.class);

    checkCHONPS = true;
    checkRDBE = true;
    this.flist = flist;
  }

  public FragmentGraphCalcTask(@Nullable MemoryMapStorage storage, @NotNull Instant moduleCallDate,
      FeatureList flist, boolean checkCHONPS, boolean checkRDBE, MZTolerance formulaTolerance,
      MZTolerance ms2MergeTol) {
    super(storage, moduleCallDate, new SimpleParameterSet(), FragmentGraphCalcModule.class);

    this.flist = flist;
    this.checkCHONPS = checkCHONPS;
    this.checkRDBE = checkRDBE;
    this.formulaTolerance = formulaTolerance;
    this.ms2MergeTol = ms2MergeTol;
  }

  @Override
  public String getTaskDescription() {
    return null;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  protected void process() {

  }

  private void processRow(FeatureListRow row, @Nullable IonType ionTypeOverride) {

    if (row.getRowCharge() > 1) {
      logger.info(STR."Charge of row \{row.toString()} > 1. Skipping.");
      return;
    }

    final Scan mergedMs2 = selection.getAllFragmentSpectra(row).stream().filter(
        scan -> scan instanceof MergedMsMsSpectrum msms && msms.getMergingType()
            .equals(MergingType.ALL_ENERGIES)).findFirst().orElse(null);
    if (mergedMs2 == null) {
      return;
    }

    final var precursorFormulaTask = new FragGraphPrecursorFormulaTask(this, row, ionTypeOverride,
        formulaTolerance, checkCHONPS, checkRDBE);
    final var task = new SimpleCalculationTask<>(precursorFormulaTask);
    TaskService.getController().addTask(task);

    final var formulae = precursorFormulaTask.get();
    while (!task.isFinished()) {
      final IMolecularFormula formula = formulae.poll();
      if (formula == null) {
        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          logger.log(Level.INFO, e.getMessage(), e);
        }
      }

      final List<PeakWithFormulae> peaksWithFormulae = FragmentUtils.getPeaksWithFormulae(formula,
          mergedMs2, defaultSignalFilter, ms2MergeTol);
    }
  }

  @Override
  protected @NotNull List<FeatureList> getProcessedFeatureLists() {
    return null;
  }

}
