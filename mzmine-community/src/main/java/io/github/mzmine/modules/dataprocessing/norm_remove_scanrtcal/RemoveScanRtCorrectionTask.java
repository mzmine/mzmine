/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.dataprocessing.norm_remove_scanrtcal;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.features.FeatureList.FeatureListAppliedMethod;
import io.github.mzmine.datamodel.features.SimpleFeatureListAppliedMethod;
import io.github.mzmine.datamodel.impl.SimpleScan;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.methods.AbstractRtCorrectionFunction;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.rawfilemethod.ApplyRtCorrectionToRawFileModule;
import io.github.mzmine.modules.dataprocessing.norm_rtcalibration2.rawfilemethod.ApplyRtCorrectionToRawFileParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractRawDataFileTask;
import io.github.mzmine.util.MemoryMapStorage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RemoveScanRtCorrectionTask extends AbstractRawDataFileTask {

  private final RawDataFile[] files;
  @Nullable
  private final String callerDescription;
  private final Set<RawDataFile> clearedFiles = new LinkedHashSet<>();

  /**
   * @param storage           The {@link MemoryMapStorage} used to store results of this task (e.g.
   *                          RawDataFiles, MassLists, FeatureLists). May be null if results shall
   *                          be stored in ram. For now, one storage should be created per module
   *                          call in
   * @param moduleCallDate    the call date of module to order execution order
   * @param callerDescription
   */
  public RemoveScanRtCorrectionTask(@Nullable MemoryMapStorage storage,
      @NotNull Instant moduleCallDate, @NotNull ParameterSet parameters,
      @NotNull Class<? extends MZmineModule> moduleClass, @Nullable String callerDescription) {
    super(storage, moduleCallDate, parameters, moduleClass);

    files = parameters.getValue(RemoveScanRtCorrectionParameters.files).getMatchingRawDataFiles();
    this.callerDescription = callerDescription;
  }

  @Override
  protected @NotNull List<RawDataFile> getProcessedDataFiles() {
    return List.of(files);
  }

  @Override
  protected void addAppliedMethod() {
    if (!clearedFiles.isEmpty()) {
      // Retain the complete correction history unless it can be removed atomically. Project
      // loading then reproduces the final state by applying all corrections before this clear.
      final ParameterSet clearParameters = RemoveScanRtCorrectionParameters.create(
          clearedFiles.toArray(RawDataFile[]::new));
      clearedFiles.forEach(file -> file.getAppliedMethods().add(
          new SimpleFeatureListAppliedMethod(Objects.requireNonNullElse(callerDescription, ""),
              getModuleClass(), clearParameters, getModuleCallDate())));
    }

    // Also compact an existing Apply -> Clear suffix. This matters when individual chromatogram
    // builder tasks already cleared all scans and a later manual clear is therefore a physical
    // no-op.
    removeRevertedRtCorrectionsIfAllFilesCleared(getProcessedDataFiles());
  }

  @Override
  protected void process() {
    for (RawDataFile file : files) {
      boolean fileWasCorrected = false;
      for (Scan scan : file.getScans()) {
        if (scan instanceof SimpleScan ss) {
          fileWasCorrected |= ss.getCorrectedRetentionTime() != null;
          ss.setCorrectedRetentionTime(null);
        }
      }
      if (fileWasCorrected) {
        clearedFiles.add(file);
      }
    }
  }

  /**
   * Removes a trailing sequence of RT correction applications and the clear operation that reverted
   * them. Every raw file affected by every correction must be selected, and removing the complete
   * sequence must leave every file uncorrected during project replay. The removal is planned
   * completely before any applied method is changed.
   *
   * @return {@code true} if at least one correction application was safely removed
   */
  static boolean removeRevertedRtCorrectionsIfAllFilesCleared(
      @NotNull Collection<RawDataFile> selectedFiles) {
    if (selectedFiles.isEmpty()) {
      return false;
    }

    final Set<RawDataFile> selectedFileSet = new LinkedHashSet<>(selectedFiles);
    final Map<RawDataFile, List<CorrectionCall>> correctionCallsByFile = new LinkedHashMap<>();
    final Map<RawDataFile, Integer> removals = new LinkedHashMap<>();
    final Set<CorrectionCall> allCorrectionCalls = new LinkedHashSet<>();

    for (RawDataFile file : selectedFileSet) {
      final List<FeatureListAppliedMethod> methods = file.getAppliedMethods();
      int index = methods.size() - 1;
      if (index < 0 || !isRtCorrectionClear(methods.get(index))) {
        continue;
      }

      final List<CorrectionCall> fileCorrectionCalls = new ArrayList<>();
      index--;
      while (index >= 0 && isRtCorrection(methods.get(index))) {
        final CorrectionCall correctionCall = getCorrectionCall(methods.get(index));
        if (correctionCall == null) {
          return false;
        }
        fileCorrectionCalls.add(correctionCall);
        allCorrectionCalls.add(correctionCall);
        index--;
      }

      if (fileCorrectionCalls.isEmpty()) {
        continue;
      }

      // A processing method may have observed an older correction. Preserve the complete history
      // rather than removing only the newest correction epoch and exposing that older state.
      if (replayIsCorrected(file, index)) {
        return false;
      }

      correctionCallsByFile.put(file, fileCorrectionCalls);
      removals.put(file, fileCorrectionCalls.size() + 1); // corrections and their final clear
    }

    if (allCorrectionCalls.isEmpty()) {
      return false;
    }

    for (CorrectionCall correctionCall : allCorrectionCalls) {
      if (!selectedFileSet.containsAll(correctionCall.files())) {
        return false;
      }
      for (RawDataFile correctionFile : correctionCall.files()) {
        if (!correctionCallsByFile.getOrDefault(correctionFile, List.of()).contains(
            correctionCall)) {
          return false;
        }
      }
    }

    removals.forEach((file, count) -> {
      for (int i = 0; i < count; i++) {
        file.getAppliedMethods().removeLast();
      }
    });
    return true;
  }

  private static boolean isRtCorrection(@Nullable FeatureListAppliedMethod method) {
    return method != null
        && method.getModule() instanceof ApplyRtCorrectionToRawFileModule;
  }

  private static boolean isRtCorrectionClear(@Nullable FeatureListAppliedMethod method) {
    return method != null && method.getModule() instanceof RemoveScanRtCorrectionModule;
  }

  private static @Nullable CorrectionCall getCorrectionCall(
      FeatureListAppliedMethod appliedMethod) {
    final List<AbstractRtCorrectionFunction> corrections;
    try {
      corrections = appliedMethod.getParameters().getValue(
          ApplyRtCorrectionToRawFileParameters.calis);
    } catch (RuntimeException e) {
      return null;
    }

    if (corrections == null) {
      return null;
    }

    final Set<RawDataFile> correctionFiles = new LinkedHashSet<>();
    for (AbstractRtCorrectionFunction correction : corrections) {
      final RawDataFile file = correction.getRawDataFile();
      if (file == null) {
        return null;
      }
      correctionFiles.add(file);
    }
    return correctionFiles.isEmpty() ? null
        : new CorrectionCall(appliedMethod.getModuleCallDate(), Set.copyOf(correctionFiles));
  }

  private static boolean replayIsCorrected(RawDataFile file, int simulatedLastIndex) {
    final List<FeatureListAppliedMethod> methods = file.getAppliedMethods();
    for (int i = simulatedLastIndex; i >= 0; i--) {
      final MZmineModule module = methods.get(i).getModule();
      if (module instanceof ApplyRtCorrectionToRawFileModule) {
        return true;
      }
      if (module instanceof RemoveScanRtCorrectionModule) {
        return false;
      }
    }
    return false;
  }

  private record CorrectionCall(@NotNull Instant moduleCallDate,
                                @NotNull Set<RawDataFile> files) {

  }

  @Override
  public String getTaskDescription() {
    return "Removing corrected retention times from %d data files".formatted(files.length);
  }
}
