package io.github.mzmine.modules.dataprocessing.featdet_chromatogramdeconvolution.wavelet;

import io.github.mzmine.datamodel.utils.UniqueIdSupplier;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public enum EdgeDetectors implements UniqueIdSupplier {
  LOCAL_MIN, ABS_MIN, SLOPE;

  public EdgeDetector create(int tol) {
    return switch (this) {
      case LOCAL_MIN -> new LocalMinimumEdgeDetector(tol);
      case ABS_MIN -> new AbsoluteMinimumEdgeDetector(tol);
      case SLOPE -> new SlopeEdgeDetector(tol + 2);
    };
  }

  @Override
  public @NotNull String getUniqueID() {
    return switch (this) {
      case LOCAL_MIN -> "local_minimum";
      case ABS_MIN -> "absolute_minimum";
      case SLOPE -> "slope";
    };
  }

  @NotNull
  public String getDescription() {
    return switch (this) {
      case LOCAL_MIN ->
          "Continues the search for a minimum as long as the next point (within a tolerance depending on peak width) is lower than the previous.";
      case ABS_MIN ->
          "Continues the search as long as the next point (within a tolerance depending on peak width) is lower than the previous absolute minimum.";
      case SLOPE ->
          "Continues the search until an increasing slope is reached. Uses varying window width depending on peak width.";
    };
  }

  public static String getDescriptions() {
    return Arrays.stream(EdgeDetectors.values())
        .map(e -> "%s: %s".formatted(e.toString(), e.getDescription()))
        .collect(Collectors.joining("\n"));
  }
}
