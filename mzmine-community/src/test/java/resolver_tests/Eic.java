package resolver_tests;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.featuredata.IonTimeSeriesUtils;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.project.ProjectService;
import io.github.mzmine.util.MemoryMapStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Extract an EIC from a data file.
 */
public record Eic(@NotNull String file, @NotNull Range<Double> mzRange, @Nullable String desc) {

  public static final MemoryMapStorage storage = MemoryMapStorage.forMassList();

  public IonTimeSeries<Scan> extract() {
    RawDataFile file = ProjectService.getProject().getDataFileByName(this.file);
    final IonTimeSeries<Scan> series = IonTimeSeriesUtils.extractIonTimeSeries(file,
        ScanSelection.MS1, mzRange, storage);
    return IonTimeSeriesUtils.remapRtAxis(series, ScanSelection.MS1.getMatchingScans(file.getScans()));
  }
}
