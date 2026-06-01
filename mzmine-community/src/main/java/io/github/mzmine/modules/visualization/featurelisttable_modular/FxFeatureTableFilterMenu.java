/*
 * Copyright (c) 2004-2025 The mzmine Development Team
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

package io.github.mzmine.modules.visualization.featurelisttable_modular;

import static io.github.mzmine.javafx.components.factories.FxLabels.newBoldLabel;
import static io.github.mzmine.javafx.components.factories.FxTextFields.newAutoGrowTextField;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.features.compoundlist.CompoundRowSelection;
import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.javafx.components.factories.FxComboBox;
import io.github.mzmine.javafx.components.factories.FxPopOvers;
import io.github.mzmine.javafx.components.util.FxLayout;
import io.github.mzmine.javafx.properties.PropertyUtils;
import io.github.mzmine.javafx.util.FxIconUtil;
import io.github.mzmine.javafx.util.FxIcons;
import io.github.mzmine.javafx.validation.FxValidation;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterComponent;
import io.github.mzmine.parameters.parametertypes.row_type_filter.RowTypeFilterParameter;
import io.github.mzmine.parameters.parametertypes.row_type_filter.filters.RowTypeFilter;
import io.github.mzmine.util.RangeUtils;
import io.github.mzmine.util.StringUtils;
import io.github.mzmine.util.collections.IndexRange;
import java.util.List;
import java.util.logging.Logger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.PopOver.ArrowLocation;
import org.controlsfx.validation.ValidationSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FxFeatureTableFilterMenu extends BorderPane {

  private static final Logger logger = Logger.getLogger(FxFeatureTableFilterMenu.class.getName());
  private final FxFeatureTableFilterMenuModel model;
  @NotNull
  private final FxFeatureTableModel parentModel;
  @NotNull
  private final FxFeatureTableController parentController;
  private final FlowPane filterFlow;
  private final HBox rightButtonMenu;

  public FxFeatureTableFilterMenu(FxFeatureTableModel parentModel,
      @NotNull FxFeatureTableController parentController) {
    this.model = parentModel.getFilterModel();
    this.parentModel = parentModel;
    this.parentController = parentController;
    filterFlow = createFilters();
    rightButtonMenu = createRightButtonMenu();

    setCenter(filterFlow);
    setRight(rightButtonMenu);

    PropertyUtils.onChangeDelayedSubscription(this::updateFilter, Duration.millis(150),
        model.idFilterProperty(), model.mzFilterProperty(), model.rtFilterProperty(),
        model.specialRowTypeFilterProperty());
  }

  private void updateFilter() {
    final String id = model.idFilterProperty().get();
    final String mz = model.mzFilterProperty().get();
    final String rt = model.rtFilterProperty().get();
    final RowTypeFilter rowTypeFilter = model.getSpecialRowTypeFilter();

    try {
      final List<IndexRange> idRanges = parseIndexRanges(id);
      final Range<Double> mzRange = parseDoubleRange(mz);
      final Range<Double> rtRange = parseDoubleRange(rt);

      final var filter = new TableFeatureListRowFilter(idRanges, mzRange, rtRange, rowTypeFilter);
      model.combinedRowFilter.set(filter);

    } catch (Exception e) {
      // has validations on field
//      logger.log(Level.WARNING, "Could not parse filter: " + e.getMessage(), e);
      model.combinedRowFilter.set(null);
    }
  }

  private static @NotNull List<IndexRange> parseIndexRanges(String id) {
    return id.isBlank() ? List.of() : IndexRange.parseRanges(id);
  }

  private static @NotNull Range<Double> parseDoubleRange(String mz) {
    return RangeUtils.getSingleValueToCeilDecimalRangeOrRange(mz);
  }

  private FlowPane createFilters() {
    RowTypeFilterComponent rowTypeFilter = new RowTypeFilterParameter().createEditingComponent();
    model.specialRowTypeFilterProperty().bindBidirectional(rowTypeFilter.valueProperty());

    final TextField idField = newAutoGrowTextField(model.idFilterProperty(), "1,5-6",
        "Filter for feature row IDs by index ranges defined as a list of single indices and ranges,e.g, 1,5-6 or 1,5,6",
        4, 10);

    final TextField mzField = newAutoGrowTextField(model.mzFilterProperty(),
        model.mzFilterPromptProperty(), "Filter for feature row m/z", 5, 10);
    final TextField rtField = newAutoGrowTextField(model.rtFilterProperty(),
        model.rtFilterPromptProperty(), "Filter for feature row RT", 4, 10);

    initValidation(idField, mzField, rtField);

    // toggle between compound list rows and flat feature list rows; the items list is maintained
    // by the interactor so ALL_ISOTOPES is hidden when no compound row has isotope sub-rows.
    final var rowSelection = FxComboBox.createComboBox(
        "Toggle between compound-grouped rows and flat feature list rows",
        model.getAvailableCompoundRowSelections(), model.compoundRowSelectionProperty());
    rowSelection.disableProperty().bind(model.compoundListAvailableProperty().not());
    rowSelection.visibleProperty().bind(model.compoundListAvailableProperty());
    rowSelection.managedProperty().bind(model.compoundListAvailableProperty());

    // layout
    return FxLayout.newFlowPane( //
        rowSelection, //
        FxIconUtil.getFontIcon(FxIcons.SEARCH), newBoldLabel("ID="), //
        idField, newBoldLabel("m/z="), //
        mzField, newBoldLabel("RT="), //
        rtField, //
        rowTypeFilter);
  }

  private void initValidation(TextField idField, TextField mzField, TextField rtField) {
    final ValidationSupport support = FxValidation.newValidationSupport();
    // blank value is also ok
    FxValidation.registerOnException(support, idField,
        s -> StringUtils.isBlank(s) || !parseIndexRanges(s).isEmpty(),
        s -> "Cannot parse index ranges from " + s);

    FxValidation.registerOnException(support, mzField,
        s -> StringUtils.isBlank(s) || parseDoubleRange(s) != null,
        s -> "Cannot parse single value or value range from " + s);
    FxValidation.registerOnException(support, rtField,
        s -> StringUtils.isBlank(s) || parseDoubleRange(s) != null,
        s -> "Cannot parse single value or value range from " + s);
  }


  private HBox createRightButtonMenu() {
    final Runnable onOpenParametersDialog = parentModel.getOnOpenParameterDialogAction();
    assert onOpenParametersDialog
        != null : "initialization error. First initialize interactor then build view";

    final ButtonBase config = FxIconUtil.newIconButton(FxIcons.GEAR_PREFERENCES,
        "Configure the look & feel of the feature table", onOpenParametersDialog);
    final ButtonBase quick = FxIconUtil.newIconButton(FxIcons.COLUMNS_DOTS,
        "Quick configuration of table columns", parentModel.getOnQuickColumnSelectionAction());
    final ButtonBase links = createLinksButton();
    final ButtonBase docu = FxIconUtil.newIconButton(FxIcons.QUESTION_CIRCLE,
        "Open the documentation of the feature table", () -> DesktopService.getDesktop()
            .openWebPage(
                "https://mzmine.github.io/mzmine_documentation/module_docs/lc-ms_featdet/featdet_results/featdet_results.html"));

    return FxLayout.newHBox( //
        docu, links, quick, config //
    );
  }

  /**
   * Button with links to other feature tables
   */
  private ButtonBase createLinksButton() {
    final ButtonBase btn = FxIconUtil.newIconButton(FxIcons.LINK,
        "Linked feature tables — sync selection across dashboards", null);
    final FeatureTableLinksPopoverContent content = new FeatureTableLinksPopoverContent(
        parentController);
    final PopOver popover = FxPopOvers.newPopOver(content, ArrowLocation.BOTTOM_RIGHT);
    // Prune stale weak refs each time the popover opens so closed dashboards drop from the list.
    popover.setOnShowing(_ -> content.refresh());
    FxPopOvers.install(btn, popover);
    return btn;
  }

  public FlowPane getFilterFlow() {
    return filterFlow;
  }

  public HBox getRightButtonMenu() {
    return rightButtonMenu;
  }

  public FxFeatureTableFilterMenuModel getModel() {
    return model;
  }


  /**
   * inner class to hide access to rowFilter changes
   */
  public static class FxFeatureTableFilterMenuModel {

    private final StringProperty idFilter = new SimpleStringProperty("");
    private final StringProperty mzFilter = new SimpleStringProperty("");
    private final StringProperty rtFilter = new SimpleStringProperty("");
    private final StringProperty mzFilterPrompt = new SimpleStringProperty("");
    private final StringProperty rtFilterPrompt = new SimpleStringProperty("");
    // specific
    private final ObjectProperty<RowTypeFilter> specialRowTypeFilter = new SimpleObjectProperty<>();

    // null = feature list rows; non-null = compound list with that selection level
    private final ObjectProperty<@Nullable CompoundRowSelection> compoundRowSelection = new SimpleObjectProperty<>(
        null);
    // true when the current feature list has a valid compound list
    private final BooleanProperty compoundListAvailable = new SimpleBooleanProperty(false);
    // Items shown in the compound row selection ComboBox. Defaults to all values; the interactor
    // narrows this to {COMPOUNDS, ALL_MAJOR_IONS} when the current compound list has no nested
    // isotope rows, so ALL_ISOTOPES is hidden in that case.
    private final ObservableList<CompoundRowSelection> availableCompoundRowSelections = FXCollections.observableArrayList(
        CompoundRowSelection.values());

    // the actual filter
    private final ReadOnlyObjectWrapper<@Nullable TableFeatureListRowFilter> combinedRowFilter = new ReadOnlyObjectWrapper<>();

    public String getIdFilter() {
      return idFilter.get();
    }

    public StringProperty idFilterProperty() {
      return idFilter;
    }

    public void setIdFilter(String idFilter) {
      this.idFilter.set(idFilter);
    }

    public String getMzFilter() {
      return mzFilter.get();
    }

    public StringProperty mzFilterProperty() {
      return mzFilter;
    }

    public void setMzFilter(String mzFilter) {
      this.mzFilter.set(mzFilter);
    }

    public String getRtFilter() {
      return rtFilter.get();
    }

    public StringProperty rtFilterProperty() {
      return rtFilter;
    }

    public void setRtFilter(String rtFilter) {
      this.rtFilter.set(rtFilter);
    }

    public String getRtFilterPrompt() {
      return rtFilterPrompt.get();
    }

    public StringProperty rtFilterPromptProperty() {
      return rtFilterPrompt;
    }

    public String getMzFilterPrompt() {
      return mzFilterPrompt.get();
    }

    public StringProperty mzFilterPromptProperty() {
      return mzFilterPrompt;
    }

    public @Nullable TableFeatureListRowFilter getCombinedRowFilter() {
      return combinedRowFilter.get();
    }

    public ReadOnlyObjectWrapper<@Nullable TableFeatureListRowFilter> combinedRowFilterProperty() {
      return combinedRowFilter;
    }

    public RowTypeFilter getSpecialRowTypeFilter() {
      return specialRowTypeFilter.get();
    }

    public ObjectProperty<RowTypeFilter> specialRowTypeFilterProperty() {
      return specialRowTypeFilter;
    }

    public @Nullable CompoundRowSelection getCompoundRowSelection() {
      return compoundRowSelection.get();
    }

    public ObjectProperty<@Nullable CompoundRowSelection> compoundRowSelectionProperty() {
      return compoundRowSelection;
    }

    public void setCompoundRowSelection(@Nullable CompoundRowSelection selection) {
      compoundRowSelection.set(selection);
    }

    public boolean isCompoundListAvailable() {
      return compoundListAvailable.get();
    }

    public BooleanProperty compoundListAvailableProperty() {
      return compoundListAvailable;
    }

    public @NotNull ObservableList<CompoundRowSelection> getAvailableCompoundRowSelections() {
      return availableCompoundRowSelections;
    }
  }
}
