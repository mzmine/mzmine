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
          "Optional override for the Python executable. If not selected, use the 'Build Runtime' module to create and extract a local runtime pack.",
          List.of(), FileSelectionType.OPEN),
      false);

  public static final DiffMSBuildRuntimeParameter buildRuntime = new DiffMSBuildRuntimeParameter("Build Runtime",
      "Create a local Python runtime for DiffMS using the bundled scripts. Requires internet access.", "Create Python Runtime");

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
    this(new io.github.mzmine.parameters.Parameter<?>[] { flists, pythonExecutable, buildRuntime, checkpoint,
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

  @Override
  public boolean checkParameterValues(java.util.Collection<String> errorMessages) {
    boolean allOk = super.checkParameterValues(errorMessages);
    
    // Check if we have a valid Python runtime
    final File customPython = getEmbeddedParameterValueIfSelectedOrElse(pythonExecutable, null);
    
    if (customPython != null) {
      // Custom Python is selected, validate it
      if (!customPython.isFile()) {
        errorMessages.add("Custom Python executable does not exist: " + customPython.getAbsolutePath() + 
            ". Please select a valid Python executable or uncheck the option to use the bundled runtime.");
        allOk = false;
      }
    } else {
      // Check if bundled runtime is available
      final File cpuRuntime = DiffMSRuntimeManager.getUsablePython(DiffMSRuntimeManager.Variant.CPU);
      final File cudaRuntime = DiffMSRuntimeManager.getUsablePython(DiffMSRuntimeManager.Variant.CUDA);
      
      if (cpuRuntime == null && cudaRuntime == null) {
        if (DiffMSRuntimeManager.anyPackExists()) {
          errorMessages.add("A DiffMS runtime pack was found but is not yet installed. Please use the 'Install Found Runtime' button in the 'Build Runtime' parameter to initialize it.");
        } else {
          errorMessages.add("No Python runtime found. Please use the 'Build Runtime' option to create a local Python runtime, " +
              "or select a custom Python executable. The Build Runtime option requires internet access.");
        }
        allOk = false;
      }
    }
    
    return allOk;
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
