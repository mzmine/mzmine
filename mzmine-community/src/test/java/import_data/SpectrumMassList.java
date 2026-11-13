package import_data;

import io.github.mzmine.datamodel.MassList;
import java.util.Arrays;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * In memory copy of a mass list, stays available after the project is replaced
 *
 * @param id identifies the spectrum (scan and mobility scan number)
 */
record SpectrumMassList(@NotNull String id, double @NotNull [] mzs,
                        double @NotNull [] intensities) {

  static @NotNull SpectrumMassList of(@NotNull final String id,
      @Nullable final MassList massList) {
    Objects.requireNonNull(massList, "No mass list for " + id);
    final int numDataPoints = massList.getNumberOfDataPoints();
    // copy, mass lists may return their internal arrays
    final double[] mzs = Arrays.copyOf(massList.getMzValues(new double[numDataPoints]),
        numDataPoints);
    final double[] intensities = Arrays.copyOf(
        massList.getIntensityValues(new double[numDataPoints]), numDataPoints);
    return new SpectrumMassList(id, mzs, intensities);
  }
}
