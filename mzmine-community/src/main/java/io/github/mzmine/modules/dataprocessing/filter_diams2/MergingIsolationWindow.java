package io.github.mzmine.modules.dataprocessing.filter_diams2;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.Scan;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jetbrains.annotations.Nullable;

public final class MergingIsolationWindow {

  private IsolationWindow window;
  private final @Nullable List<Scan> scans;

  public MergingIsolationWindow(IsolationWindow window, @Nullable List<Scan> scans) {
    this.window = window;
    this.scans = scans;
  }

  public IsolationWindow window() {
    return window;
  }

  public @Nullable List<Scan> scans() {
    return scans;
  }

  public void setWindow(IsolationWindow window) {
    this.window = window;
  }
}
