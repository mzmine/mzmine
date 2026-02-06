/*
 * Copyright (c) 2004-2026 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.chemaudit;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mzmine.datamodel.features.ModularFeatureListRow;
import io.github.mzmine.datamodel.features.compoundannotations.CompoundDBAnnotation;
import io.github.mzmine.datamodel.features.types.annotations.CompoundNameType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ChemAuditQualityIndicatorType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ChemAuditRawJsonType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.ChemAuditValidationScoreType;
import io.github.mzmine.datamodel.features.types.annotations.compounddb.DatabaseNameType;
import io.github.mzmine.datamodel.structures.MolecularStructure;
import io.github.mzmine.modules.dataprocessing.id_diffms.ChemAuditResult;
import io.github.mzmine.modules.visualization.molstructure.Structure2DComponent;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChemAuditResultPane extends BorderPane {

  private static final Logger logger = Logger.getLogger(ChemAuditResultPane.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String VALUE_UNAVAILABLE = "N/A";
  private static final int structureWidth = (int) Math.min(
      (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 3), 500);
  private static final int structureHeight = (int) Math.min(
      (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 6), 250);

  public ChemAuditResultPane(@NotNull final CompoundDBAnnotation annotation,
      @Nullable final ModularFeatureListRow row) {
    final Canvas structure = buildStructurePane(annotation);
    structure.setHeight(structureHeight);
    structure.setWidth(structureWidth);

    final VBox content = buildContent(annotation, row);
    final BorderPane card = new BorderPane();
    card.setLeft(structure);
    card.setCenter(content);
    card.setPadding(new Insets(8));
    card.setStyle("-fx-background-color: -fx-control-inner-background; "
        + "-fx-border-color: -fx-box-border;");
    BorderPane.setMargin(content, new Insets(4, 8, 4, 8));

    setCenter(card);
  }

  private static Canvas buildStructurePane(@Nullable final CompoundDBAnnotation annotation) {
    if (annotation == null) {
      return new Canvas();
    }
    MolecularStructure structure = annotation.getStructure();
    if (structure == null) {
      return new Canvas();
    }
    return new Structure2DComponent(structure.structure());
  }

  private static VBox buildContent(@NotNull final CompoundDBAnnotation annotation,
      @Nullable final ModularFeatureListRow row) {
    final VBox content = new VBox(8);
    content.setPadding(new Insets(6));

    final String json = annotation.get(ChemAuditRawJsonType.class);
    final ChemAuditResult result = parseResult(json);
    if (result == null) {
      content.getChildren().add(new Label("ChemAudit results unavailable."));
      return content;
    }

    content.getChildren().add(buildHeader(annotation, row, result));
    content.getChildren().add(buildActionRow(annotation, result));

    final List<TitledPane> sections = new ArrayList<>();
    sections.add(buildSummarySection(annotation, result));
    sections.add(buildAlertsSection(result));
    sections.add(buildValidationSection(result));
    sections.add(buildStandardizationSection(result));
    content.getChildren().addAll(sections);
    content.getChildren().add(buildExpandCollapseRow(sections));

    return content;
  }

  private static Node buildHeader(@NotNull final CompoundDBAnnotation annotation,
      @Nullable final ModularFeatureListRow row, @NotNull final ChemAuditResult result) {
    final HBox header = new HBox(10);
    final String name = Objects.requireNonNullElse(annotation.get(CompoundNameType.class),
        "Predicted structure");
    final String dbName = annotation.get(DatabaseNameType.class);
    final String title = dbName != null ? name + " (" + dbName + ")" : name;

    final Label titleLabel = new Label(title);
    titleLabel.getStyleClass().add("title-label");
    final String indicator = Objects.requireNonNullElse(
        annotation.get(ChemAuditQualityIndicatorType.class), VALUE_UNAVAILABLE);
    final Label qualityLabel = new Label(indicator);
    qualityLabel.setStyle("-fx-font-weight: bold; -fx-padding: 2 6 2 6; "
        + "-fx-border-color: #cbd5f5; -fx-background-color: #eef2ff;");

    final Label rowLabel = new Label(row != null
        ? "Row " + row.getID() + (result.rank() != null ? " | Rank " + result.rank() : "")
        : (result.rank() != null ? "Rank " + result.rank() : ""));
    rowLabel.setStyle("-fx-text-fill: -fx-text-inner-color;");

    final VBox titleBox = new VBox(2, titleLabel, rowLabel);
    HBox.setHgrow(titleBox, Priority.ALWAYS);
    header.getChildren().addAll(titleBox, qualityLabel);
    return header;
  }

  private static TitledPane buildSummarySection(@NotNull final CompoundDBAnnotation annotation,
      @NotNull final ChemAuditResult result) {
    final GridPane grid = new GridPane();
    grid.setVgap(4);
    grid.setHgap(10);

    int row = 0;
    addGridRow(grid, row++, "Valid", String.valueOf(result.valid()));
    final String validationScore = annotation.get(ChemAuditValidationScoreType.class) != null
        ? String.valueOf(annotation.get(ChemAuditValidationScoreType.class))
        : String.valueOf(result.validationScore());
    addGridRow(grid, row++, "Validation score", validationScore);
    addGridRow(grid, row++, "Quality category",
        Objects.requireNonNullElse(result.qualityCategory(), VALUE_UNAVAILABLE));
    addGridRow(grid, row++, "Quality indicator",
        Objects.requireNonNullElse(annotation.get(ChemAuditQualityIndicatorType.class),
            VALUE_UNAVAILABLE));
    addGridRow(grid, row++, "Alerts", String.valueOf(
        result.alerts() != null ? result.alerts().size() : 0));
    final String alertSummary = formatAlertSummary(result);
    if (alertSummary != null) {
      addGridRow(grid, row++, "Alert summary", alertSummary);
    }
    addGridRow(grid, row++, "Critical alerts",
        String.valueOf(result.hasCriticalAlerts()));
    addGridRow(grid, row++, "Standardized SMILES",
        Objects.requireNonNullElse(result.standardizedSmiles(), VALUE_UNAVAILABLE));

    final TitledPane pane = new TitledPane("Summary", grid);
    pane.setExpanded(true);
    return pane;
  }

  private static TitledPane buildAlertsSection(@NotNull final ChemAuditResult result) {
    final VBox alertsBox = new VBox(6);
    if (result.alerts() == null || result.alerts().isEmpty()) {
      final Label ok = new Label("No structural alerts detected.");
      ok.setStyle("-fx-text-fill: #15803d;");
      alertsBox.getChildren().add(ok);
    } else {
      for (var alert : result.alerts()) {
        alertsBox.getChildren().add(buildAlertCard(alert));
      }
    }
    final TitledPane pane = new TitledPane(
        "Structural alerts (" + (result.alerts() != null ? result.alerts().size() : 0) + ")",
        alertsBox);
    pane.setExpanded(true);
    return pane;
  }

  private static TitledPane buildValidationSection(@NotNull final ChemAuditResult result) {
    final VBox checksBox = new VBox(6);
    if (result.failedChecks() == null || result.failedChecks().isEmpty()) {
      checksBox.getChildren().add(new Label("No failed validation checks."));
    } else {
      for (var check : result.failedChecks()) {
        final String name = Objects.requireNonNullElse(check.name(), "check");
        final String severity = Objects.requireNonNullElse(check.severity(), "unknown");
        final String message = Objects.requireNonNullElse(check.message(), "");
        final Label label = new Label(name + " (" + severity + ")" + (message.isBlank()
            ? "" : ": " + message));
        label.setWrapText(true);
        checksBox.getChildren().add(label);
      }
    }
    final TitledPane pane = new TitledPane("Validation checks", checksBox);
    pane.setExpanded(false);
    return pane;
  }

  private static TitledPane buildStandardizationSection(@NotNull final ChemAuditResult result) {
    final VBox box = new VBox(4);
    if (result.standardization() == null) {
      box.getChildren().add(new Label("No standardization details available."));
    } else {
      final var std = result.standardization();
      final String status = Boolean.TRUE.equals(std.success()) ? "ok" : "failed";
      box.getChildren().add(new Label("Status: " + status));
      if (std.massChangePercent() != null) {
        box.getChildren().add(new Label(
            "Mass change: " + String.format("%.2f%%", std.massChangePercent())));
      }
      if (std.excludedFragments() != null && !std.excludedFragments().isEmpty()) {
        box.getChildren().add(new Label("Excluded fragments: " + std.excludedFragments()));
      }
      if (std.error() != null && !std.error().isBlank()) {
        box.getChildren().add(new Label("Error: " + std.error()));
      }
    }
    final TitledPane pane = new TitledPane("Standardization", box);
    pane.setExpanded(false);
    return pane;
  }

  private static Node buildAlertCard(@NotNull final ChemAuditResult.StructuralAlert alert) {
    final VBox card = new VBox(2);
    card.setPadding(new Insets(6));
    card.setStyle("-fx-background-color: -fx-control-inner-background; "
        + "-fx-border-color: -fx-box-border; -fx-border-radius: 4; -fx-background-radius: 4;");

    final String pattern = Objects.requireNonNullElse(alert.pattern(), "alert");
    final String severity = Objects.requireNonNullElse(alert.severity(), "unknown");
    final String catalog = Objects.requireNonNullElse(alert.catalog(), "unknown");
    final Label title = new Label(pattern + " [" + severity + "] (" + catalog + ")");
    title.setStyle("-fx-font-weight: bold; -fx-text-fill: " + resolveSeverityColor(severity) + ";");
    title.setWrapText(true);

    final List<String> detailParts = new ArrayList<>();
    if (alert.description() != null && !alert.description().isBlank()) {
      detailParts.add("Description: " + alert.description());
    }
    if (alert.matchedAtoms() != null && !alert.matchedAtoms().isEmpty()) {
      detailParts.add("Matched atoms: " + alert.matchedAtoms());
    }
    final Label details = new Label(String.join(" | ", detailParts));
    details.setWrapText(true);

    card.getChildren().add(title);
    if (!detailParts.isEmpty()) {
      card.getChildren().add(details);
    }
    return card;
  }

  private static @Nullable String formatAlertSummary(@NotNull final ChemAuditResult result) {
    if (result.alerts() == null || result.alerts().isEmpty()) {
      return null;
    }
    final var counts = new TreeMap<String, Integer>();
    for (var alert : result.alerts()) {
      final String severity = alert.severity() == null ? "unknown" : alert.severity().toLowerCase();
      counts.put(severity, counts.getOrDefault(severity, 0) + 1);
    }
    final List<String> parts = new ArrayList<>();
    for (var entry : counts.entrySet()) {
      parts.add(entry.getKey() + "=" + entry.getValue());
    }
    return parts.isEmpty() ? null : String.join(", ", parts);
  }

  private static String resolveSeverityColor(@NotNull final String severity) {
    return switch (severity.toLowerCase()) {
      case "critical" -> "#b91c1c";
      case "warning" -> "#b45309";
      case "info" -> "#1d4ed8";
      default -> "-fx-text-inner-color";
    };
  }

  private static void addGridRow(@NotNull final GridPane grid, final int row,
      @NotNull final String label, @NotNull final String value) {
    final Label labelNode = new Label(label);
    labelNode.getStyleClass().add("title-label");
    final Label valueNode = new Label(value);
    valueNode.setWrapText(true);
    grid.add(labelNode, 0, row);
    grid.add(valueNode, 1, row);
  }

  private static @Nullable ChemAuditResult parseResult(@Nullable final String json) {
    if (json == null || json.isBlank()) {
      return null;
    }
    try {
      return mapper.readValue(json, ChemAuditResult.class);
    } catch (Exception e) {
      logger.log(Level.WARNING, "Failed to parse ChemAudit JSON", e);
      return null;
    }
  }

  private static Node buildActionRow(@NotNull final CompoundDBAnnotation annotation,
      @NotNull final ChemAuditResult result) {
    final HBox actions = new HBox(8);
    final Button copySmiles = new Button("Copy SMILES");
    copySmiles.setOnAction(_ -> copyToClipboard(result.standardizedSmiles()));
    final Button copyJson = new Button("Copy ChemAudit JSON");
    copyJson.setOnAction(_ -> copyToClipboard(annotation.get(ChemAuditRawJsonType.class)));
    actions.getChildren().addAll(copySmiles, copyJson);
    return actions;
  }

  private static Node buildExpandCollapseRow(@NotNull final List<TitledPane> panes) {
    final HBox controls = new HBox(8);
    final Button expandAll = new Button("Expand details");
    final Button collapseAll = new Button("Collapse details");
    expandAll.setOnAction(_ -> panes.forEach(p -> p.setExpanded(true)));
    collapseAll.setOnAction(_ -> panes.forEach(p -> p.setExpanded(false)));
    controls.getChildren().addAll(expandAll, collapseAll);
    controls.setPadding(new Insets(4, 0, 0, 0));
    return controls;
  }

  private static void copyToClipboard(@Nullable final String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    final ClipboardContent content = new ClipboardContent();
    content.putString(text);
    Clipboard.getSystemClipboard().setContent(content);
  }
}
