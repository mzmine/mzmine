/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardTab;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class DashboardNavigationTest {

  @Test
  void exposesStableNativeDashboardIds() {
    final Set<String> ids = DashboardNavigation.descriptors().stream()
        .map(DashboardNavigation.Descriptor::id).collect(java.util.stream.Collectors.toSet());

    assertTrue(ids.containsAll(Set.of(DashboardNavigation.COMPOUND, DashboardNavigation.LIPID_QC,
        DashboardNavigation.STATISTICS, DashboardNavigation.INTEGRATION,
        DashboardNavigation.FRAGMENTATION, DashboardNavigation.LIPID_SUMMARY,
        DashboardNavigation.DATA_FILE_STATISTICS)));
  }

  @Test
  void dataFileStatisticsRequiresASelectedTableWhenNoAnalysisIsAvailable() {
    final DashboardNavigation.Availability availability = DashboardNavigation.availability(
        DashboardNavigation.DATA_FILE_STATISTICS, null, null);

    assertFalse(availability.available());
    assertEquals("Select a feature list to find matching data-file statistics.",
        availability.reason());
  }

  @Test
  void dataFileStatisticsFocusesOnlyMatchingCompletedAnalysis() {
    final RawDataFile rawFile = mock(RawDataFile.class);
    final RawDataFile secondRawFile = mock(RawDataFile.class);
    final FeatureList featureList = mock(FeatureList.class);
    final DataFileStatisticsDashboardTab dashboard = mock(DataFileStatisticsDashboardTab.class);
    final MZmineDesktop desktop = mock(MZmineDesktop.class);
    when(featureList.getRawDataFiles()).thenReturn(java.util.List.of(rawFile, secondRawFile));
    doReturn(java.util.List.of(secondRawFile, rawFile)).when(dashboard).getRawDataFiles();
    when(desktop.getAllTabs()).thenReturn(java.util.List.of(dashboard));

    try (MockedStatic<MZmineCore> core = Mockito.mockStatic(MZmineCore.class)) {
      core.when(MZmineCore::getDesktop).thenReturn(desktop);

      final DashboardNavigation.Availability availability = DashboardNavigation.availability(
          DashboardNavigation.DATA_FILE_STATISTICS, featureList, null);

      assertTrue(availability.available());
      assertEquals("Available.", availability.reason());
    }
  }

  @Test
  void rejectsARequestedRowFromAnotherFeatureList() {
    final ModularFeatureList selected = mock(ModularFeatureList.class);
    final FeatureList other = mock(FeatureList.class);
    final FeatureListRow row = mock(FeatureListRow.class);
    when(row.getFeatureList()).thenReturn(other);

    final DashboardNavigation.Availability availability = DashboardNavigation.availability(
        DashboardNavigation.LIPID_QC, selected, row);

    assertFalse(availability.available());
    assertEquals("The selected row does not belong to the selected feature list.",
        availability.reason());
  }
}
