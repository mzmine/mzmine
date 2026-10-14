/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.gui;

import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.dataanalysis.compounddashboard.CompoundDashboardTab;
import io.github.mzmine.modules.dataanalysis.statsdashboard.StatsDashboardTab;
import io.github.mzmine.modules.tools.tools_autoparam.DataFileStatisticsDashboardTab;
import io.github.mzmine.modules.tools.fraggraphdashboard.FragDashboardTab;
import io.github.mzmine.modules.visualization.dash_integration.IntegrationDashboardTab;
import io.github.mzmine.modules.visualization.dash_lipidqc.LipidAnnotationQCDashboardTab;
import io.github.mzmine.modules.visualization.lipidannotationsummary.LipidAnnotationSummaryModule;
import io.github.mzmine.modules.visualization.lipidannotationsummary.LipidAnnotationSummaryTab;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Opens supported dashboard tabs for a known local feature list. This intentionally does not run
 * processing modules when a dashboard prerequisite is absent.
 * <p>
 * All calls must be made on the JavaFX application thread. Integrations can register additional
 * destinations, for example licensed dashboard modules, through {@link #register(Destination)}.
 */
public final class DashboardNavigation {

  public static final String COMPOUND = "compound";
  public static final String LIPID_QC = "lipid-qc";
  public static final String STATISTICS = "statistics";
  public static final String INTEGRATION = "integration";
  public static final String FRAGMENTATION = "fragmentation";
  public static final String LIPID_SUMMARY = "lipid-summary";
  public static final String DATA_FILE_STATISTICS = "data-file-statistics";

  private static final Map<String, Destination> destinations = new LinkedHashMap<>();

  static {
    register(new CompoundDestination());
    register(new LipidQcDestination());
    register(new StatisticsDestination());
    register(new IntegrationDestination());
    register(new FragmentationDestination());
    register(new LipidSummaryDestination());
    register(new DataFileStatisticsDestination());
  }

  private DashboardNavigation() {
  }

  /** Adds an application-specific dashboard destination. Duplicate IDs are rejected. */
  public static void register(final @NotNull Destination destination) {
    final Destination previous = destinations.putIfAbsent(destination.descriptor().id(), destination);
    if (previous != null) {
      throw new IllegalArgumentException("A dashboard destination is already registered for ID "
          + destination.descriptor().id());
    }
  }

  /** Returns the stable dashboard definitions without making a selected table available. */
  public static @NotNull List<Descriptor> descriptors() {
    return destinations.values().stream().map(Destination::descriptor).toList();
  }

  /** Returns dashboard availability for a table and optional selected row. */
  public static @NotNull Availability availability(final @NotNull String id,
      final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
    final Destination destination = destinations.get(id);
    if (destination == null) {
      return new Availability(id, id, false, "Unknown dashboard destination.");
    }
    return destination.availability(featureList, row);
  }

  /** Returns availability for every registered dashboard in display order. */
  public static @NotNull List<Availability> describe(final @Nullable FeatureList featureList,
      final @Nullable FeatureListRow row) {
    return destinations.values().stream().map(destination -> destination.availability(featureList, row))
        .toList();
  }

  /**
   * Opens or focuses a dashboard for the supplied native objects.
   *
   * @throws UnavailableDashboardException if the ID is unknown or a prerequisite is missing
   */
  public static @NotNull Descriptor open(final @NotNull String id,
      final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
    requireFxThread();
    final Destination destination = destinations.get(id);
    if (destination == null) {
      throw new UnavailableDashboardException(id, "Unknown dashboard destination.");
    }
    final Availability availability = destination.availability(featureList, row);
    if (!availability.available()) {
      throw new UnavailableDashboardException(id, availability.reason());
    }
    destination.open(featureList, row);
    return destination.descriptor();
  }

  /** A stable, agent-facing dashboard definition. */
  public record Descriptor(@NotNull String id, @NotNull String label, @NotNull String prerequisite) {
  }

  /** Availability of a dashboard for the current native selection. */
  public record Availability(@NotNull String id, @NotNull String label, boolean available,
                             @NotNull String reason) {
  }

  /** Extension point for destinations supplied by mzmine editions. */
  public interface Destination {

    @NotNull Descriptor descriptor();

    @NotNull Availability availability(@Nullable FeatureList featureList,
        @Nullable FeatureListRow row);

    void open(@Nullable FeatureList featureList, @Nullable FeatureListRow row);
  }

  /** A typed, stable failure for callers that need to preserve their response contract. */
  public static final class UnavailableDashboardException extends IllegalStateException {

    private final String dashboardId;

    public UnavailableDashboardException(final @NotNull String dashboardId,
        final @NotNull String reason) {
      super(reason);
      this.dashboardId = dashboardId;
    }

    public @NotNull String getDashboardId() {
      return dashboardId;
    }
  }

  private abstract static class FeatureListDestination implements Destination {

    @Override
    public @NotNull Availability availability(final @Nullable FeatureList featureList,
        final @Nullable FeatureListRow row) {
      if (!(featureList instanceof ModularFeatureList modularFeatureList)) {
        return unavailable("Select a modular feature list.");
      }
      if (row != null && row.getFeatureList() != modularFeatureList) {
        return unavailable("The selected row does not belong to the selected feature list.");
      }
      return availability(modularFeatureList, row);
    }

    abstract @NotNull Availability availability(@NotNull ModularFeatureList featureList,
        @Nullable FeatureListRow row);

    final @NotNull Availability available() {
      final Descriptor descriptor = descriptor();
      return new Availability(descriptor.id(), descriptor.label(), true, "Available.");
    }

    final @NotNull Availability unavailable(final @NotNull String reason) {
      final Descriptor descriptor = descriptor();
      return new Availability(descriptor.id(), descriptor.label(), false, reason);
    }
  }

  private static final class CompoundDestination extends FeatureListDestination {

    private static final Descriptor DESCRIPTOR = new Descriptor(COMPOUND, "Compound dashboard",
        "The selected feature list must have compound grouping results.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    @NotNull Availability availability(final @NotNull ModularFeatureList featureList,
        final @Nullable FeatureListRow row) {
      return featureList.getCompoundList() == null
          ? unavailable("Run compound grouping for the selected feature list first.") : available();
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final CompoundDashboardTab tab = findTab(CompoundDashboardTab.class, featureList);
      if (tab != null) {
        selectRow(tab, row);
        ApplicationNavigation.focus(tab);
        return;
      }
      final CompoundDashboardTab newTab = new CompoundDashboardTab(featureList);
      MZmineCore.getDesktop().addTab(newTab);
      selectRow(newTab, row);
      ApplicationNavigation.focus(newTab);
    }

    private static void selectRow(final @NotNull CompoundDashboardTab tab,
        final @Nullable FeatureListRow row) {
      if (row != null) {
        tab.selectRow(row);
      }
    }
  }

  private static final class LipidQcDestination extends FeatureListDestination {

    private static final Descriptor DESCRIPTOR = new Descriptor(LIPID_QC, "Lipid dashboard",
        "Select a modular feature list.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    @NotNull Availability availability(final @NotNull ModularFeatureList featureList,
        final @Nullable FeatureListRow row) {
      return available();
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final LipidAnnotationQCDashboardTab tab = findTab(LipidAnnotationQCDashboardTab.class,
          featureList);
      if (tab != null) {
        selectRow(tab, row);
        ApplicationNavigation.focus(tab);
        return;
      }
      final LipidAnnotationQCDashboardTab newTab = new LipidAnnotationQCDashboardTab();
      newTab.onFeatureListSelectionChanged(List.of(featureList));
      MZmineCore.getDesktop().addTab(newTab);
      selectRow(newTab, row);
      ApplicationNavigation.focus(newTab);
    }

    private static void selectRow(final @NotNull LipidAnnotationQCDashboardTab tab,
        final @Nullable FeatureListRow row) {
      if (row != null) {
        tab.selectRow(row);
      }
    }
  }

  private static final class StatisticsDestination extends FeatureListDestination {

    private static final Descriptor DESCRIPTOR = new Descriptor(STATISTICS, "Statistics dashboard",
        "Select an aligned feature list.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    @NotNull Availability availability(final @NotNull ModularFeatureList featureList,
        final @Nullable FeatureListRow row) {
      return featureList.isAligned() ? available()
          : unavailable("Select an aligned feature list for statistics.");
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final MZmineTab tab = findTab(StatsDashboardTab.class, featureList);
      if (tab != null) {
        ApplicationNavigation.focus(tab);
        return;
      }
      final StatsDashboardTab newTab = new StatsDashboardTab();
      newTab.onFeatureListSelectionChanged(List.of(featureList));
      MZmineCore.getDesktop().addTab(newTab);
      ApplicationNavigation.focus(newTab);
    }
  }

  private static final class IntegrationDestination extends FeatureListDestination {

    private static final Descriptor DESCRIPTOR = new Descriptor(INTEGRATION, "Integration dashboard",
        "Select a modular feature list.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    @NotNull Availability availability(final @NotNull ModularFeatureList featureList,
        final @Nullable FeatureListRow row) {
      return available();
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final IntegrationDashboardTab tab = findTab(IntegrationDashboardTab.class, featureList);
      if (tab != null) {
        selectRow(tab, row);
        ApplicationNavigation.focus(tab);
        return;
      }
      final IntegrationDashboardTab newTab = new IntegrationDashboardTab();
      newTab.onFeatureListSelectionChanged(List.of(featureList));
      MZmineCore.getDesktop().addTab(newTab);
      selectRow(newTab, row);
      ApplicationNavigation.focus(newTab);
    }

    private static void selectRow(final @NotNull IntegrationDashboardTab tab,
        final @Nullable FeatureListRow row) {
      if (row != null) {
        tab.selectRow(row);
      }
    }
  }

  private static final class FragmentationDestination extends FeatureListDestination {

    private static final Descriptor DESCRIPTOR = new Descriptor(FRAGMENTATION,
        "Fragment formula dashboard", "Select a feature-list row with an MS/MS spectrum.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    @NotNull Availability availability(final @NotNull ModularFeatureList featureList,
        final @Nullable FeatureListRow row) {
      if (row == null) {
        return unavailable("Select a feature-list row with an MS/MS spectrum.");
      }
      return row.getMostIntenseFragmentScan() == null
          ? unavailable("The selected row has no MS/MS spectrum.") : available();
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final MZmineTab tab = MZmineCore.getDesktop().getAllTabs().stream()
          .filter(FragDashboardTab.class::isInstance).map(FragDashboardTab.class::cast)
          .filter(candidate -> candidate.getFeatureListRow() == row).findFirst().orElse(null);
      if (tab != null) {
        ApplicationNavigation.focus(tab);
        return;
      }
      final FragDashboardTab newTab = new FragDashboardTab(row, null, null);
      MZmineCore.getDesktop().addTab(newTab);
      ApplicationNavigation.focus(newTab);
    }
  }

  private static final class LipidSummaryDestination extends FeatureListDestination {

    private static final Descriptor DESCRIPTOR = new Descriptor(LIPID_SUMMARY,
        "Lipid annotation summary", "The selected feature list must contain lipid annotations.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    @NotNull Availability availability(final @NotNull ModularFeatureList featureList,
        final @Nullable FeatureListRow row) {
      final boolean hasLipidAnnotation = featureList.getRows().stream()
          .anyMatch(candidate -> !candidate.getLipidMatches().isEmpty());
      return hasLipidAnnotation ? available()
          : unavailable("The selected feature list has no lipid annotations.");
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final MZmineTab tab = findTab(LipidAnnotationSummaryTab.class, featureList);
      if (tab != null) {
        ApplicationNavigation.focus(tab);
        return;
      }
      final LipidAnnotationSummaryTab newTab = new LipidAnnotationSummaryTab(
          "Lipid Annotation summary",
          MZmineCore.getConfiguration().getModuleParameters(LipidAnnotationSummaryModule.class),
          featureList);
      MZmineCore.getDesktop().addTab(newTab);
      ApplicationNavigation.focus(newTab);
    }
  }

  private static final class DataFileStatisticsDestination implements Destination {

    private static final Descriptor DESCRIPTOR = new Descriptor(DATA_FILE_STATISTICS,
        "Data file statistics", "Run the data-file statistics analysis through its native workflow.");

    @Override
    public @NotNull Descriptor descriptor() {
      return DESCRIPTOR;
    }

    @Override
    public @NotNull Availability availability(final @Nullable FeatureList featureList,
        final @Nullable FeatureListRow row) {
      if (featureList == null) {
        return unavailable("Select a feature list to find matching data-file statistics.");
      }
      return findTab(featureList) == null
          ? unavailable("Requires data-file statistics analysis; use the native analysis workflow.")
          : available();
    }

    @Override
    public void open(final @Nullable FeatureList featureList, final @Nullable FeatureListRow row) {
      final DataFileStatisticsDashboardTab tab = featureList == null ? null : findTab(featureList);
      if (tab == null) {
        throw new UnavailableDashboardException(DESCRIPTOR.id(),
            "Requires data-file statistics analysis; use the native analysis workflow.");
      }
      ApplicationNavigation.focus(tab);
    }

    private @NotNull Availability available() {
      return new Availability(DESCRIPTOR.id(), DESCRIPTOR.label(), true, "Available.");
    }

    private @NotNull Availability unavailable(final @NotNull String reason) {
      return new Availability(DESCRIPTOR.id(), DESCRIPTOR.label(), false, reason);
    }

    private static @Nullable DataFileStatisticsDashboardTab findTab(
        final @NotNull FeatureList featureList) {
      return MZmineCore.getDesktop().getAllTabs().stream()
          .filter(DataFileStatisticsDashboardTab.class::isInstance)
          .map(DataFileStatisticsDashboardTab.class::cast)
          .filter(tab -> sameRawDataFiles(tab.getRawDataFiles(), featureList.getRawDataFiles()))
          .findFirst()
          .orElse(null);
    }
  }

  static boolean sameRawDataFiles(final @NotNull java.util.Collection<? extends RawDataFile> first,
      final @NotNull java.util.Collection<? extends RawDataFile> second) {
    return first.size() == second.size() && new HashSet<>(first).equals(new HashSet<>(second));
  }

  private static <T extends MZmineTab> @Nullable T findTab(final @NotNull Class<T> tabType,
      final @NotNull FeatureList featureList) {
    return MZmineCore.getDesktop().getAllTabs().stream().filter(tabType::isInstance)
        .map(tabType::cast).filter(tab -> ownsFeatureList(tab, featureList)).findFirst().orElse(null);
  }

  private static boolean ownsFeatureList(final @NotNull MZmineTab tab,
      final @NotNull FeatureList featureList) {
    return tab.getFeatureLists().contains(featureList)
        || tab.getAlignedFeatureLists().contains(featureList);
  }

  private static void requireFxThread() {
    if (!javafx.application.Platform.isFxApplicationThread()) {
      throw new IllegalStateException(
          "DashboardNavigation must be called on the JavaFX application thread; use FxThread.runLater");
    }
  }
}
