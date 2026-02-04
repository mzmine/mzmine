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

import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.ComboParameter;
import io.github.mzmine.parameters.parametertypes.IntegerParameter;
import io.github.mzmine.parameters.parametertypes.OptionalParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileNameParameter;
import io.github.mzmine.parameters.parametertypes.filenames.FileSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsParameter;
import io.github.mzmine.parameters.parametertypes.tolerances.MZToleranceParameter;
import java.io.File;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;

public class DiffMSParameters extends SimpleParameterSet {

  public static final FeatureListsParameter flists = new FeatureListsParameter();

  public static final OptionalParameter<FileNameParameter> pythonExecutable = new OptionalParameter<>(
      new FileNameParameter("Custom Python executable",
          "Optional override for the Python executable. If not selected, MZmine will use the bundled DiffMS runtime pack "
              + "(external_tools/diffms/runtime-packs) and extract it into the user directory on first use.",
          List.of(), FileSelectionType.OPEN),
      false);

  public static final FileNameParameter checkpoint = new DiffMSCheckpointParameter("Checkpoint",
      "Pretrained DiffMS checkpoint (.ckpt). Use the download button to fetch the DiffMS checkpoint archive from Zenodo and automatically extract the required .ckpt.",
      List.of(new ExtensionFilter("Checkpoint", "*.ckpt")), FileSelectionType.OPEN);

  public static final ComboParameter<Device> device = new ComboParameter<>("Device",
      "Execution device for inference.", Device.values(), Device.CPU);

  public static final IntegerParameter topK = new IntegerParameter("Top-K structures",
      "Number of structures to sample per feature.", 10, 1, 100);

  public static final IntegerParameter maxMs2Peaks = new IntegerParameter("Max MS/MS peaks",
      "Top-N peaks (by intensity) exported per feature (merged MS/MS).", 50, 5, 500);

  public static final MZToleranceParameter subformulaTol = new MZToleranceParameter("Subformula tolerance",
      "Absolute tolerance for matching a subformula mass to an MS/MS peak m/z.", 0.02, 10.0);

  public DiffMSParameters() {
    this(new io.github.mzmine.parameters.Parameter<?>[] { flists, pythonExecutable, checkpoint,
        device, topK, maxMs2Peaks, subformulaTol });

    // Auto-select checkpoint if it was downloaded previously or if the current one
    // is missing.
    final File defaultCkpt = DiffMSCheckpointFiles.getDefaultCheckpointFile();
    final File currentCkpt = getValue(checkpoint);
    if (defaultCkpt.isFile() && (currentCkpt == null || !currentCkpt.isFile())) {
      getParameter(checkpoint).setValue(defaultCkpt);
    }
  }

  protected DiffMSParameters(final io.github.mzmine.parameters.Parameter<?>[] parameters) {
    super(parameters);
  }

  public enum Device {
    CPU, CUDA;

    @Override
    public String toString() {
      return switch (this) {
        case CPU -> "CPU";
        case CUDA -> "CUDA";
      };
    }

    public String toArg() {
      return switch (this) {
        case CPU -> "cpu";
        case CUDA -> "cuda";
      };
    }
  }
}
