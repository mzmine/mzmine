/*
 * Copyright (c) 2004-2022 The MZmine Development Team
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

package io.github.mzmine.modules.dataprocessing.id_biotransformer;

import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

public class BioTransformerSingleRowTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(BioTransformerSingleRowTask.class.getName());

  private final ModularFeatureListRow row;
  private final String smiles;
  private final String prefix;
  private final ParameterSet parameters;
  private final File bioPath;
  private final MZTolerance mzTolerance;
  private String description;

  public BioTransformerSingleRowTask(ModularFeatureListRow row, String smiles, String prefix,
      @NotNull ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate);
    this.row = row;
    this.smiles = smiles;
    this.prefix = prefix;
    this.parameters = parameters;
    bioPath = parameters.getValue(BioTransformerParameters.bioPath);
    mzTolerance = parameters.getValue(BioTransformerParameters.mzTol);

    description = "Biotransformer task - SMILES: " + smiles;
  }

  @Override
  public String getTaskDescription() {
    return description;
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);

    if (isCanceled()) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    if (smiles == null) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    description = "Biotransformer task - SMILES: " + smiles;

    final List<CompoundDBAnnotation> bioTransformerAnnotations;
    try {
      bioTransformerAnnotations = BioTransformerTask.singleRowPrediction(
          row.getID(), smiles, prefix, bioPath, parameters);
    } catch (IOException e) {
      logger.log(Level.WARNING, e.getMessage(), e);
      setErrorMessage("Error reading/writing temporary files during BioTransformer prediciton.\n"
          + e.getMessage());
      setStatus(TaskStatus.ERROR);
      return;
    }

    if (bioTransformerAnnotations.isEmpty()) {
      setStatus(TaskStatus.FINISHED);
      return;
    }

    final ModularFeatureList flist = row.getFeatureList();
    for (CompoundDBAnnotation annotation : bioTransformerAnnotations) {
      flist.stream().forEach(r -> {
        final CompoundDBAnnotation clone = annotation.checkMatchAndCalculateDeviation(r,
            mzTolerance, null, null, null);
        if (clone != null) {
          r.addCompoundAnnotation(clone);
          row.getCompoundAnnotations()
              .sort(Comparator.comparingDouble(a -> Objects.requireNonNullElse(a.getScore(), 0f)));
        }
      });
    }

    setStatus(TaskStatus.FINISHED);
  }
}
