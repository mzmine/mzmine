package io.github.mzmine.modules.tools.timstofmaldiacq.precursorselection;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public record PrecursorOverlap(MaldiTimsPrecursor precursor,
                               List<MaldiTimsPrecursor> overlaps) implements
    Comparable<PrecursorOverlap> {

  @Override
  public int compareTo(@NotNull PrecursorOverlap o) {
    return Integer.compare(this.overlaps.size(), o.overlaps.size());
  }
}
