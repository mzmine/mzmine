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

package io.github.mzmine.modules.dataprocessing.featdet_manualintegration;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Minimal read-only editor for {@link ManualIntegrationEntriesParameter}. Manual integration
 * entries are produced by the integration dashboard, not hand-authored in a setup dialog, so this
 * component only summarizes how many entries are stored while preserving the value across a dialog
 * round-trip.
 */
public class ManualIntegrationEntriesComponent extends Label {

  // decision: hold the value so a setup-dialog round-trip returns it unchanged (component is read-only)
  private @NotNull List<ManualIntegrationEntry> value = new ArrayList<>();

  public ManualIntegrationEntriesComponent() {
    updateText();
  }

  public @NotNull List<ManualIntegrationEntry> getValue() {
    return value;
  }

  public void setValue(@Nullable List<ManualIntegrationEntry> newValue) {
    this.value = newValue != null ? new ArrayList<>(newValue) : new ArrayList<>();
    updateText();
  }

  private void updateText() {
    setText("%d manual integration%s".formatted(value.size(), value.size() == 1 ? "" : "s"));
  }
}
