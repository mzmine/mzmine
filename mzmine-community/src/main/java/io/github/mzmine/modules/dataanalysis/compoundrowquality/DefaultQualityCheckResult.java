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
 * pane. All text labels are configured to wrap to the parent container width and shrink to zero
 * minimum width so they never force the surrounding panel wider than the {@code ScrollPane}
 * viewport. Use this for plain-text outcomes; subclass {@link QualityCheckResult} directly when a
 * check needs custom content (charts, thumbnails, chip lists, links).
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
    final Label title = configureWrap(FxLabels.newBoldLabel(type.getLabel()));
    final Label summaryLabel = configureWrap(FxLabels.newLabel(summary));
    final VBox box = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true, title, summaryLabel);
    // Min 0 so the box can shrink to fit narrow parent containers; wrapping labels handle the rest.
    box.setMinWidth(0);
    return box;
  }

  @Override
  public @Nullable Region buildSubPane() {
    if (detailLines.isEmpty() && involvedRows.isEmpty()) {
      return null;
    }
    final VBox body = FxLayout.newVBox(Pos.TOP_LEFT, Insets.EMPTY, true);
    body.setMinWidth(0);
    for (final String line : detailLines) {
      body.getChildren().add(configureWrap(FxLabels.newLabel(line)));
    }
    if (!involvedRows.isEmpty()) {
      body.getChildren().add(configureWrap(FxLabels.newItalicLabel(
          "Involves %d row%s".formatted(involvedRows.size(),
              involvedRows.size() == 1 ? "" : "s"))));
    }
    return body;
  }

  /// Configure a Label so it wraps inside a width-constrained parent: zero min width lets the
  /// parent layout shrink the label below its computed text width, and {@code Double.MAX_VALUE} max
  /// width pairs with VBox {@code fillWidth=true} so the label always uses the available width.
  private static @NotNull Label configureWrap(@NotNull Label label) {
    label.setWrapText(true);
    label.setMinWidth(0);
    label.setMaxWidth(Double.MAX_VALUE);
    return label;
  }
}
