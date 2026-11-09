/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.modules.tools.tools_autoparam;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.DataFileStatistics;
import io.github.mzmine.modules.tools.tools_autoparam.statistics.InterSampleRtStatistics;
import io.github.mzmine.gui.mainwindow.SimpleTab;
import java.util.Collection;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/** Dashboard tab carrying the raw-file identity of an already completed statistics analysis. */
public final class DataFileStatisticsDashboardTab extends SimpleTab {

  private final List<RawDataFile> rawDataFiles;

  public DataFileStatisticsDashboardTab(final @NotNull List<DataFileStatistics> statistics,
      final @NotNull InterSampleRtStatistics interSampleRtStatistics) {
    super("Data File Statistics", new DataFileStatisticsDashboardPane(statistics, interSampleRtStatistics));
    rawDataFiles = statistics.stream().map(DataFileStatistics::file).toList();
  }

  @Override
  public @NotNull Collection<? extends RawDataFile> getRawDataFiles() {
    return rawDataFiles;
  }
}
