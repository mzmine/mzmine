/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.visualization.projectmetadata.extract;

import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxTextFields;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.main.ConfigService;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Model and grid cells for a single {@link MetadataRegexMapping}. The column configuration is shown
 * as one row of the shared grid in {@link MetadataRegexExtractionComponent}; the value mappings are
 * edited in the shared {@link MetadataValueMappingEditor} whenever this row is selected and are
 * kept in this model in the meantime.
 */
public class MetadataRegexMappingRow {

  // tooltip strings also reused on the column header labels in MetadataRegexExtractionComponent
  static final String TOOLTIP_SOURCE = "The input that is searched by regex (filenames and paths).";
  static final String TOOLTIP_COLUMN = "Name of the metadata column to populate with the extracted value.";
  static final String TOOLTIP_TYPE = "Target column type. Auto detects the type from all extracted values.";
  static final String TOOLTIP_REGEX = """
      Java regex (regular expression) applied case-insensitively to the input string. \
      If the regex defines a capture group (in paratheses), the first capture group is extracted; \
      otherwise the whole match is used.""";
  static final String TOOLTIP_DEFAULT = "Value used when the regex does not match this file. Leave empty to keep the cell blank.";

  // fixed instruction block appended to the generated LLM regex prompt
  private static final String REGEX_PROMPT_INSTRUCTIONS = """
      Return exactly:
      specific: <Java regex>
      general: <Java regex>
      
      Rules:
      - Case-insensitive matching is handled externally.
      - Create Java regexes with exactly one capturing group.
      - Use non-capturing groups for any extra grouping.
      - Escape literal regex metacharacters.
      - Do not use .* or .+ unless unavoidable.
      - The returned regex must include the literal left and right separators around the captured value. \
      For underscore-delimited filename tokens, the capture should appear between literal underscores, \
      e.g. _(<capture>)_. Do not omit those separators from the regex.
      
      specific:
      Match only the listed Values in the same position shown in Examples. Capture only the value, usually as an alternation.
      
      general:
      Extract any value from that same position. Use the narrowest safe token pattern:
      - Prefer [A-Za-z0-9]+ for word/number tokens.
      - Use the shortest left and right literal qualifiers needed to uniquely identify the position.""";

  private final Label selectIcon = new Label();
  private final ComboBox<RegexInputSource> sourceCombo;
  private final TextField columnField;
  private final ComboBox<ExtractColumnType> typeCombo;
  private final TextField regexField;
  private final TextField defaultField;
  private final ButtonBase generateButton;
  private final ButtonBase removeButton;
  // generate + remove icon buttons grouped in one cell with icon-button spacing
  private final FlowPane buttonBox;
  private final Function<RegexInputSource, List<String>> inputExamples;

  // value mapping data lives here; it is edited via the shared MetadataValueMappingEditor
  private final List<MetadataValueMapping> valueMappings = new ArrayList<>();
  private DropUnmappedMode dropUnmapped;
  // whether the extracted value is mapped (value-mapping mode) or used directly (extract mode)
  private boolean useValueMappings;

  private @Nullable Consumer<MetadataRegexMappingRow> onActivate;
  private @Nullable Runnable onChange;
  private @Nullable Consumer<MetadataRegexMappingRow> onRemove;

  public MetadataRegexMappingRow(@NotNull final MetadataRegexMapping mapping,
      @NotNull final List<String> columnSuggestions,
      @NotNull final Function<RegexInputSource, List<String>> inputExamples) {
    this.inputExamples = inputExamples;

    selectIcon.setMinWidth(24);
    selectIcon.setStyle("-fx-cursor: hand;");
    selectIcon.setTooltip(
        new Tooltip("Selected mapping – shown in the preview and value-mapping editor below."));
    selectIcon.setOnMouseClicked(_ -> fireActivate());

    sourceCombo = FxComboBox.createComboBox(TOOLTIP_SOURCE, List.of(RegexInputSource.values()));
    sourceCombo.setValue(mapping.inputSource());

    columnField = new TextField(mapping.columnName());
    columnField.setPromptText("column name");
    columnField.setTooltip(new Tooltip(TOOLTIP_COLUMN));
    FxTextFields.autoGrowFitText(columnField, 10, 30);
    // suggest existing metadata columns (e.g. mzmine_sample_type) while still allowing new names
    FxTextFields.bindAutoCompletion(columnField, columnSuggestions);

    typeCombo = FxComboBox.createComboBox(TOOLTIP_TYPE, List.of(ExtractColumnType.values()));
    typeCombo.setValue(mapping.type());

    regexField = new TextField(mapping.regex());
    regexField.setPromptText("e.g., _([A-Za-z]+)_");
    regexField.setTooltip(new Tooltip(TOOLTIP_REGEX));
    FxTextFields.autoGrowFitText(regexField, 16, 60);
    // let the regex field fill the grid column that grows with the dialog width
    regexField.setMaxWidth(Double.MAX_VALUE);

    defaultField = new TextField(mapping.defaultValue());
    defaultField.setPromptText("default");
    FxTextFields.autoGrowFitText(defaultField, 6, 20);
    defaultField.setTooltip(new Tooltip(TOOLTIP_DEFAULT));

    generateButton = FxIconUtil.newIconButton(FxIcons.LIGHTBULB,
        "Copy an LLM prompt to the clipboard that asks for a regex extracting these files' values.",
        this::generateRegexQuery);
    removeButton = FxIconUtil.newIconButton(FxIcons.X_CIRCLE, "Remove this mapping",
        this::fireRemove);
    buttonBox = FxLayout.newIconPane(Orientation.HORIZONTAL, generateButton, removeButton);

    this.dropUnmapped = mapping.dropUnmapped();
    this.valueMappings.addAll(mapping.activeValueMappings());
    // start in value-mapping mode only if mappings were loaded
    this.useValueMappings = !mapping.activeValueMappings().isEmpty();

    registerListeners();
  }

  private void registerListeners() {
    registerEditable(sourceCombo);
    registerEditable(typeCombo);
    registerEditable(columnField);
    registerEditable(regexField);
    registerEditable(defaultField);
    sourceCombo.valueProperty().addListener((_, _, _) -> fireChangeActivated());
    typeCombo.valueProperty().addListener((_, _, _) -> fireChange());
    columnField.textProperty().addListener((_, _, _) -> fireChange());
    regexField.textProperty().addListener((_, _, _) -> fireChangeActivated());
    defaultField.textProperty().addListener((_, _, _) -> fireChange());
  }

  // focusing any control makes this the active row for the preview and value-mapping editor
  private void registerEditable(final Control control) {
    control.focusedProperty().addListener((_, _, focused) -> {
      if (focused) {
        fireActivate();
      }
    });
  }

  /**
   * @return the cells of this row in grid-column order: select icon, source, column, type, regex,
   * default, and the grouped generate-query + remove icon buttons.
   */
  public @NotNull Node[] gridCells() {
    return new Node[]{selectIcon, sourceCombo, columnField, typeCombo, regexField, defaultField,
        buttonBox};
  }

  public void setSelected(final boolean selected) {
    if (selected) {
      final FontIcon icon = FxIconUtil.getFontIcon(FxIcons.CHECK_CIRCLE, 20,
          ConfigService.getDefaultColorPalette().getPositiveColor());
      selectIcon.setGraphic(icon);
    } else {
      // no icon when not selected
      selectIcon.setGraphic(null);
    }
  }

  // copies an LLM prompt to extract the target values from the current input source of all files
  private void generateRegexQuery() {
    final MetadataRegexMapping mapping = toMapping();
    final String values = valueMappings.stream().filter(MetadataValueMapping::isActive)
        .map(MetadataValueMapping::from).collect(Collectors.joining(" "));

    final StringBuilder query = new StringBuilder("Values:\n");
    query.append(values.isBlank() ? "<your target values, space separated>" : values);
    query.append("\n\nExamples:\n");
    for (final String input : inputExamples.apply(mapping.inputSource())) {
      query.append(input).append('\n');
    }
    query.append('\n').append(REGEX_PROMPT_INSTRUCTIONS);

    final String text = query.toString();
    final ClipboardContent content = new ClipboardContent();
    content.putString(text);
    // also offer an HTML flavor: rich-text editors (e.g. ChatGPT web) prefer it and, unlike with
    // plain text, keep the blank lines between sections as paragraph breaks
    // HTML also has its issues and seems to not paste nicely into claude desktop so stick to plain text
//    content.putHtml(toHtmlParagraphs(text));
    Clipboard.getSystemClipboard().setContent(content);
    DialogLoggerUtil.showDialogForTime("Copied to clipboard",
        "LLM prompt to generate a regex was copied to the clipboard.", AlertType.INFORMATION);
  }

  // converts the plain-text prompt to HTML where blank-line-separated blocks become <p> paragraphs
  // and single line breaks become <br>. Text is HTML-escaped so tokens like <Java regex> survive.
  private static @NotNull String toHtmlParagraphs(@NotNull final String text) {
    final StringBuilder html = new StringBuilder();
    for (final String paragraph : text.split("\n{2,}")) {
      html.append("<p>").append(escapeHtml(paragraph).replace("\n", "<br>")).append("</p>");
    }
    return html.toString();
  }

  private static @NotNull String escapeHtml(@NotNull final String s) {
    return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }

  // ----------------------------------------------------------------- value mapping data accessors

  public @NotNull List<MetadataValueMapping> getValueMappings() {
    return valueMappings;
  }

  public void setValueMappings(@NotNull final List<MetadataValueMapping> mappings) {
    valueMappings.clear();
    valueMappings.addAll(mappings);
  }

  public @NotNull DropUnmappedMode getDropUnmapped() {
    return dropUnmapped;
  }

  public void setDropUnmapped(@NotNull final DropUnmappedMode dropUnmapped) {
    this.dropUnmapped = dropUnmapped;
  }

  public boolean isUseValueMappings() {
    return useValueMappings;
  }

  public void setUseValueMappings(final boolean useValueMappings) {
    this.useValueMappings = useValueMappings;
  }

  /**
   * @return the mapping represented by the current state of this row.
   */
  public @NotNull MetadataRegexMapping toMapping() {
    // value mappings only take effect in the value-mapping mode
    final boolean useMaps = useValueMappings;
    return new MetadataRegexMapping(sourceCombo.getValue(), columnField.getText().trim(),
        typeCombo.getValue(), regexField.getText(), defaultField.getText(),
        useMaps ? dropUnmapped : DropUnmappedMode.KEEP_UNMAPPED,
        useMaps ? new ArrayList<>(valueMappings) : List.of());
  }

  // --------------------------------------------------------------------------------------- callbacks

  public void setOnActivate(@Nullable final Consumer<MetadataRegexMappingRow> onActivate) {
    this.onActivate = onActivate;
  }

  public void setOnChange(@Nullable final Runnable onChange) {
    this.onChange = onChange;
  }

  public void setOnRemove(@Nullable final Consumer<MetadataRegexMappingRow> onRemove) {
    this.onRemove = onRemove;
  }

  private void fireActivate() {
    if (onActivate != null) {
      onActivate.accept(this);
    }
  }

  private void fireChange() {
    if (onChange != null) {
      onChange.run();
    }
  }

  private void fireChangeActivated() {
    fireActivate();
    fireChange();
  }

  private void fireRemove() {
    if (onRemove != null) {
      onRemove.accept(this);
    }
  }
}
