package io.github.mzmine.modules.dataanalysis.compoundrowquality;

import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.javafx.components.factories.FxLabels;
import io.github.mzmine.javafx.components.util.FxLayout;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default text-based {@link QualityCheckResult}: bold check-type label and a wrapping summary line
 * in the main pane; one wrapping label per detail line plus an "Involves N rows" footer in the sub
 * pane. Use this for plain-text outcomes; subclass {@link QualityCheckResult} directly when a check
 * needs custom content (charts, thumbnails, links).
 */
public class DefaultQualityCheckResult extends QualityCheckResult {

  private final @NotNull String summary;
  private final @NotNull List<@NotNull String> detailLines;

  public DefaultQualityCheckResult(@NotNull QualityCheckType type,
      @NotNull QualityCheckStatus status, @NotNull String summary,
      @NotNull List<@NotNull String> detailLines,
      @NotNull List<@NotNull FeatureListRow> involvedRows) {
    super(type, status, involvedRows);
    this.summary = summary;
    this.detailLines = List.copyOf(detailLines);
  }

  public static @NotNull DefaultQualityCheckResult of(@NotNull QualityCheckType type,
      @NotNull QualityCheckStatus status, @NotNull String summary) {
    return new DefaultQualityCheckResult(type, status, summary, List.of(), List.of());
  }

  @Override
  public @NotNull Region buildMainPane() {
    final Label title = FxLabels.newBoldLabel(type.getLabel());
    final Label summaryLabel = FxLabels.newLabel(summary);
    summaryLabel.setWrapText(true);
    return FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, summaryLabel);
  }

  @Override
  public @Nullable Region buildSubPane() {
    if (detailLines.isEmpty() && involvedRows.isEmpty()) {
      return null;
    }
    final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);
    for (final String line : detailLines) {
      final Label detail = FxLabels.newLabel(line);
      detail.setWrapText(true);
      body.getChildren().add(detail);
    }
    if (!involvedRows.isEmpty()) {
      body.getChildren().add(FxLabels.newItalicLabel(
          "Involves %d row%s".formatted(involvedRows.size(),
              involvedRows.size() == 1 ? "" : "s")));
    }
    return body;
  }
}
