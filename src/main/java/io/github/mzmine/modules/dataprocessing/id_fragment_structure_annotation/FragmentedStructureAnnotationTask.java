/*
 * Copyright (c) 2004-2023 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_fragment_structure_annotation;

import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import org.jetbrains.annotations.NotNull;

public class FragmentedStructureAnnotationTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      FragmentedStructureAnnotationTask.class.getName());

  private final MZTolerance mzTol;
  private final double[] mzs;
  private final String inchikey;
  private final DoubleProperty progress = new SimpleDoubleProperty(0);
  private Map<Integer, MolecularStructure> results;

  public FragmentedStructureAnnotationTask(ParameterSet parameters, @NotNull Instant moduleCallDate,
      double[] mzs) {
    super(null, moduleCallDate); // no new data stored -> null
    mzTol = parameters.getValue(FragmentedStructureAnnotationParameters.mzTol);
    this.mzs = mzs;
    this.inchikey = null;
  }

  public FragmentedStructureAnnotationTask(MZTolerance mzTol, double[] mzs, String inchikey) {
    super(null, Instant.now()); // no new data stored -> null
    this.mzTol = mzTol;
    this.mzs = mzs;
    this.inchikey = inchikey;
  }


  @Override
  public double getFinishedPercentage() {
    return progress.get();
  }

  @Override
  public String getTaskDescription() {
    return "Annotating fragments on spectrum";
  }

  /**
   * @see Runnable#run()
   */
  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    SpectraFragmentStructureAnnotator annotator = new SpectraFragmentStructureAnnotator(mzTol);
    results = annotator.annotateSpectralSignals(MZmineCore.getFragmentedStructureMap(), mzs,
        inchikey, progress);

    setStatus(TaskStatus.FINISHED);
  }

  public Map<Integer, MolecularStructure> getResults() {
    return results;
  }
}
