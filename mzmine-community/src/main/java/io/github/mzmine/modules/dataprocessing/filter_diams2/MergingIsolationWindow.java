package io.github.mzmine.modules.dataprocessing.filter_diams2;

import io.github.mzmine.datamodel.Scan;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public final class MergingIsolationWindow {

  private IsolationWindow window;
  private final @NotNull List<Scan> scans;

  public MergingIsolationWindow(IsolationWindow window, @NotNull List<Scan> scans) {
    this.window = window;
    this.scans = scans;
  }

  public IsolationWindow window() {
    return window;
  }

  public @NotNull List<Scan> scans() {
    return scans;
  }

  public void setWindow(IsolationWindow window) {
    this.window = window;
  }
}
