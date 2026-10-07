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

package io.github.mzmine.modules.io.spectraldbsubmit.batch;

import static io.github.mzmine.javafx.components.factories.FxTexts.linebreak;
import static io.github.mzmine.javafx.components.factories.FxTexts.text;

import io.github.mzmine.javafx.components.factories.ArticleReferences;
import io.github.mzmine.javafx.components.factories.FxTextFlows;
import io.github.mzmine.javafx.components.factories.FxTexts;
import io.github.mzmine.modules.io.export_merge_libraries.MergeLibrariesModule;
import io.github.mzmine.parameters.dialogs.ParameterSetupDialog;
import io.github.mzmine.parameters.impl.IonMobilitySupport;
import io.github.mzmine.parameters.impl.SimpleParameterSet;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelection;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionParameter;
import io.github.mzmine.util.ExitCode;
import java.util.List;
import javafx.scene.layout.Region;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A subset of {@link LibraryBatchGenerationParameters} that excludes the feature list selection,
 * output file, and export format parameters. Used when generating spectral library entries for
 * selected rows and adding them to an existing or new in-memory library (rather than exporting to
 * file directly).
 */
public class LibraryBatchGenerationSubParameters extends SimpleParameterSet {

  /**
   * Keeps last selected library to open dialog with correct library
   */
  public static final HiddenParameter<SpectralLibrarySelection> lastLibrarySelection = new HiddenParameter<>(
      new SpectralLibrarySelectionParameter(false, new SpectralLibrarySelection(List.of())));

  public LibraryBatchGenerationSubParameters() {
    super(lastLibrarySelection, LibraryBatchGenerationParameters.postMergingMsLevelFilter,
        LibraryBatchGenerationParameters.metadata, LibraryBatchGenerationParameters.normalizer,
        LibraryBatchGenerationParameters.merging, LibraryBatchGenerationParameters.handleChimerics,
        LibraryBatchGenerationParameters.quality, LibraryBatchGenerationParameters.advanced);
  }

  @Override
  public ExitCode showSetupDialog(final boolean valueCheckRequired) {
    final Region message = FxTextFlows.newTextFlowInAccordion("How to cite",
        text("When using the spectral library generation module please cite:"), linebreak(),
        ArticleReferences.SPECLIBGENERATION.hyperlinkText());
    ParameterSetupDialog dialog = new ParameterSetupDialog(valueCheckRequired, this, message);
    dialog.showAndWait();
    return dialog.getExitCode();
  }

  @Override
  public @NotNull IonMobilitySupport getIonMobilitySupport() {
    return IonMobilitySupport.SUPPORTED;
  }

  @Override
  public @Nullable Region getMessage() {
    return FxTextFlows.newTextFlowInAccordion("Information", true, FxTexts.text("This will only "),
        FxTexts.boldText("add"), FxTexts.text(" entries to the library you selected. It will "),
        FxTexts.boldText("NOT"), FxTexts.text("""
            export the created library automatically. You can export the new/appended library \
            using the %s module or directly from the context menu of the spectral library in the \
            libraries tab.""".formatted(MergeLibrariesModule.NAME)));
  }
}
