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

package io.github.mzmine.parameters.dialogs;

import io.github.mzmine.gui.DesktopService;
import io.github.mzmine.gui.helpwindow.HelpWindow;
import io.github.mzmine.javafx.dialogs.DialogLoggerUtil;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.presets.ModulePreset;
import io.github.mzmine.modules.presets.ModulePresetStore;
import io.github.mzmine.parameters.EmbeddedParameterComponentProvider;
import io.github.mzmine.parameters.Parameter;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.ParameterUtils;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import io.github.mzmine.parameters.parametertypes.selectors.FeatureListsComponent;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesComponent;
import io.github.mzmine.parameters.parametertypes.selectors.SpectralLibrarySelectionComponent;
import io.github.mzmine.parameters.parametertypes.submodules.ModuleOptionsEnumComponent;
import io.github.mzmine.util.presets.PresetsButton;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.controlsfx.control.CheckComboBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class represents a basic pane for parameter setup when the {@link ParameterSetupDialog} is
 * not used. All added parameters and their components in {@link #parametersAndComponents}
 * <p>
 */
@SuppressWarnings("rawtypes")
public class ParameterSetupPane extends BorderPane implements EmbeddedParameterComponentProvider {

  public static final Logger logger = Logger.getLogger(ParameterSetupPane.class.getName());
  protected final URL helpURL;
  // Parameters and their representation in the dialog
  protected final ParameterSet parameterSet;
  protected final Map<String, Node> parametersAndComponents = new HashMap<>();
  protected final Button btnHelp;
  // Button panel - added here so it is possible to move buttons as a whole,
  // if needed.
  protected final ButtonBar pnlButtons;
  // Footer message
  @Nullable
  protected final Region footerMessage;
  // the centerPane is empty and used as the main container for all parameter components
  protected final BorderPane mainPane;
  protected final BorderPane centerPane;
  // If true, the dialog won't allow the OK button to proceed, unless all
  // parameters pass the value check. This is undesirable in the BatchMode
  // setup dialog, where some parameters need to be set in advance according
  // to values that are not yet imported etc.
  private final boolean valueCheckRequired;
  protected Button btnCancel;
  // Buttons
  protected Button btnOK;
  /**
   * Help window for this setup dialog. Initially null, until the user clicks the Help button.
   */
  protected HelpWindow helpWindow = null;
  private GridPane paramsPane;
  /**
   * Usually opens a dialog to ask for overwrite. BatchModeDialog for example changes that behavior.
   * Is called on selected presets
   */
  private final ObjectProperty<@NotNull Consumer<ParameterSet>> askApplyParameterSet = new SimpleObjectProperty<>(
      (params) -> {
        if (DialogLoggerUtil.showDialogYesNo("Overwrite parameters?",
            "Do you want to overwrite the current parameters?")) {
          setParametersDirect(params);
        }
      });
  // null if no presets can be stored
  private @Nullable ModulePresetStore modulePresetStore;

  public ParameterSetupPane(boolean valueCheckRequired, boolean addOkButton,
      ParameterSet parameters) {
    this(valueCheckRequired, parameters, addOkButton, false, null, true, true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, @Nullable Region message) {
    this(valueCheckRequired, parameters, addOkButton, false, message, true, true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   *
   * @param message: html-formatted text
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, @Nullable Region message,
      boolean addParamComponents) {
    this(valueCheckRequired, parameters, addOkButton, addCancelButton, message, addParamComponents,
        true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, @Nullable Region message,
      boolean addParamComponents, boolean addHelp) {
    this(valueCheckRequired, parameters, addOkButton, addCancelButton, message, addParamComponents,
        addHelp, true);
  }

  /**
   * Method to display setup dialog with a html-formatted footer message at the bottom.
   */
  public ParameterSetupPane(boolean valueCheckRequired, ParameterSet parameters,
      boolean addOkButton, boolean addCancelButton, @Nullable Region message,
      boolean addParamComponents, boolean addHelp, boolean addScrollPane) {
    this.valueCheckRequired = valueCheckRequired;
    this.parameterSet = parameters;
    this.helpURL = parameters.getClass().getResource("help/help.html");
    this.footerMessage = message;

    // Main panel which holds all the components in a grid
    mainPane = this;

    // Use main CSS
    if (DesktopService.isGUI()) {
      // may be called in headless mode for graphics export
      getStylesheets().addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());
    }

    centerPane = new BorderPane();

    if (addScrollPane) {
      ScrollPane mainScrollPane = new ScrollPane(centerPane);
      // mainScrollPane.setStyle("-fx-border-color: red;");
      mainScrollPane.setFitToWidth(true);
      mainScrollPane.setFitToHeight(true);
      mainScrollPane.setPadding(new Insets(10.0));
      mainPane.setCenter(mainScrollPane);
    } else {
      mainPane.setCenter(centerPane);
    }

    // Add buttons to the ButtonBar
    pnlButtons = new ButtonBar();
    pnlButtons.setPadding(new Insets(1));
    if (addOkButton) {
      btnOK = new Button("OK");
      btnOK.setOnAction(e -> callOkButton());
      pnlButtons.getButtons().addAll(btnOK);
      ButtonBar.setButtonData(btnOK, ButtonData.OK_DONE);

      // add presets button if available for this parameterset
      MZmineCore.getModuleForParameterSetIfUnique(parameterSet).ifPresent(module -> {
        modulePresetStore = new ModulePresetStore(module, parameterSet);
        final PresetsButton<ModulePreset> presetButton = new PresetsButton<>(true,
            modulePresetStore, this::createPreset,
            activePreset -> askApplyParameterSet.get().accept(activePreset.parameters()));
        ButtonBar.setButtonData(presetButton, ButtonData.OK_DONE);
        pnlButtons.getButtons().add(presetButton);
      });
    }
    if (addCancelButton) {
      btnCancel = new Button("Cancel");
      btnCancel.setOnAction(e -> callCancelButton());
      pnlButtons.getButtons().addAll(btnCancel);
      ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);
    }

    if (addHelp) {
      if (parameters.getOnlineHelpUrl() != null) { // if we have online docs, use those
        btnHelp = new Button("Help");
        btnHelp.setOnAction(
            e -> MZmineCore.getDesktop().openWebPage(parameters.getOnlineHelpUrl()));

        ButtonBar.setButtonData(btnHelp, ButtonData.HELP);
        pnlButtons.getButtons().add(btnHelp);
      } else if (helpURL != null) { // otherwise use old help url
        btnHelp = new Button("Help");
        btnHelp.setOnAction(e -> {
          if (helpWindow != null) {
            helpWindow.show();
            helpWindow.toFront();
          } else {
            helpWindow = new HelpWindow(helpURL.toString());
            helpWindow.show();
          }
        });

        ButtonBar.setButtonData(btnHelp, ButtonData.HELP);
        pnlButtons.getButtons().add(btnHelp);
      } else {
        btnHelp = null;
      }
    } else {
      btnHelp = null;
    }
    mainPane.setBottom(pnlButtons);

    if (addParamComponents) {
      paramsPane = createParameterPane(parameterSet.getParameters());
      centerPane.setCenter(paramsPane);
    }

    if (footerMessage != null) {
      final VBox pane = new VBox(footerMessage);
      footerMessage.prefWidthProperty().bind(pane.widthProperty());
      pane.setFillWidth(true);
      mainPane.setTop(pane);
    }

    // do not set min height - this works badly with {@link ModuleOptionsEnumComponent}
    // which then does not scale well or when too small crunches multiple components above each other
    setMinWidth(300.0);
  }

  private ModulePreset createPreset(String name) {
    if (modulePresetStore == null) {
      return null;
    }
    ParameterSet clone = parameterSet.cloneParameterSet();
    clone = updateParameterSetFromComponents(clone);
    return modulePresetStore.createPreset(name, clone);
  }

  public void setParametersDirect(ParameterSet parameters) {
    // first set parameters to components - otherwise components tend to auto trigger updates on change
    setParameterValuesToComponents(parameters);
    // then set parameters to the parameterset
    ParameterUtils.copyParameters(parameters, parameterSet);
  }

  /**
   * Defines the behavior when a new parameter set is applied like from presets. Default is to ask
   * user and copy parameter values to actual parameterset. Batch dialog for example redefines
   * this.
   */
  public void setAskApplyParameterSet(@NotNull Consumer<ParameterSet> askApplyParameterSet) {
    this.askApplyParameterSet.set(askApplyParameterSet);
  }

  /**
   * Embedded parameter setup pane without scroll pane and with all components initialized
   *
   * @param onParametersChanged run this method if parameters change through their components
   */
  @NotNull
  public static ParameterSetupPane createEmbedded(final boolean valueCheckRequired,
      final ParameterSet parameters, Runnable onParametersChanged) {
    return new ParameterSetupPane(valueCheckRequired, parameters, false, false, null, true, false,
        false) {
      @Override
      protected void parametersChanged() {
        if (onParametersChanged != null) {
          onParametersChanged.run();
        }
      }
    };
  }

  public BorderPane getCenterPane() {
    return centerPane;
  }

  @Override
  @NotNull
  public Map<String, Node> getParametersAndComponents() {
    return parametersAndComponents;
  }

  public ButtonBar getButtonBar() {
    return pnlButtons;
  }

  /**
   * ok button was clicked
   */
  protected void callOkButton() {
  }

  /**
   * check parameters button was clicked
   */
  protected void callCheckParametersButton() {
  }

  /**
   * cancel button was clicked
   */
  protected void callCancelButton() {
  }

  /**
   * Creating a grid pane with all the parameters and labels
   *
   * @param parameters parameters to fill the grid pane
   * @return a grid pane
   */
  @NotNull
  public ParameterGridLayout createParameterPane(List<? extends Parameter<?>> parameters) {
    return createParameterPane(parameters.toArray(Parameter[]::new));
  }

  /**
   * Creating a grid pane with all the parameters and labels
   *
   * @param parameters parameters to fill the grid pane
   * @return a grid pane
   */
  @NotNull
  public ParameterGridLayout createParameterPane(@NotNull Parameter<?>[] parameters) {
    ParameterGridLayout paramsPane = new ParameterGridLayout(parameters);

    for (Parameter<?> p : parameters) {
      // components only for user parameters so may be null
      final ParameterAndComponent comp = paramsPane.getParameterAndComponent(p);
      if (comp == null) {
        continue;
      }

      // Add listeners so we are notified about any change in the values
      addListenersToNode(comp.component());

      // add to map to reflect changes
      parametersAndComponents.put(p.getName(), comp.component());
    }

    return paramsPane;
  }

  @SuppressWarnings("unchecked")
  public <ComponentType extends Node> ComponentType getComponentForParameter(
      UserParameter<?, ComponentType> p) {
    return (ComponentType) parametersAndComponents.get(p.getName());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public ParameterSet updateParameterSetFromComponents() {
    return updateParameterSetFromComponents(parameterSet);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  public ParameterSet updateParameterSetFromComponents(ParameterSet parameterSet) {
    for (Parameter<?> p : parameterSet.getParameters()) {
      if (!(p instanceof UserParameter) && !(p instanceof HiddenParameter)) {
        continue;
      }
      UserParameter up;
      if (p instanceof UserParameter) {
        up = (UserParameter) p;
      } else {
        up = (UserParameter) ((HiddenParameter) p).getEmbeddedParameter();
      }

      Node component = parametersAndComponents.get(p.getName());

      // if a parameter is a HiddenParameter it does not necessarily have
      // component
      if (component != null) {
        up.setValueFromComponent(component);
      }
    }
    return parameterSet;
  }

  public void setParameterValuesToComponents() {
    setParameterValuesToComponents(this.parameterSet);
  }

  /**
   * May want to apply other parameters to components, similar to other parameters this means that
   * the embedded parameterset in this pane is not updated or changed by this. Still needs to call
   * {@link #updateParameterSetFromComponents()} for this.
   */
  public void setParameterValuesToComponents(ParameterSet parameterSet) {
    for (Parameter<?> p : parameterSet.getParameters()) {
      if (!(p instanceof UserParameter) && !(p instanceof HiddenParameter)) {
        continue;
      }
      UserParameter up;
      if (p instanceof UserParameter) {
        up = (UserParameter) p;
      } else {
        up = (UserParameter) ((HiddenParameter) p).getEmbeddedParameter();
      }

      Node component = parametersAndComponents.get(p.getName());

      // if a parameter is a HiddenParameter it does not necessarily have
      // component
      if (component != null) {
        up.setValueToComponent(component, up.getValue());
      }
    }
  }

  protected int getNumberOfParameters() {
    return parameterSet.getParameters().length;
  }

  /**
   * This method does nothing, but it is called whenever user changes the parameters. It can be
   * overridden in extending classes to update the preview components, for example.
   */
  protected void parametersChanged() {
  }

  public GridPane getParamsPane() {
    return paramsPane;
  }

  public ParameterSet getParameterSet() {
    return parameterSet;
  }

  protected void addListenersToNode(Node node) {
    if (node instanceof TextField textField) {
      textField.textProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof FeatureListsComponent fselect) {
      fselect.currentlySelectedProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof RawDataFilesComponent rselect) {
      rselect.currentlySelectedProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof SpectralLibrarySelectionComponent rselect) {
      rselect.currentlySelectedProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof ComboBox<?> comboComp) {
      comboComp.valueProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof ChoiceBox) {
      ChoiceBox<?> choiceBox = (ChoiceBox) node;
      choiceBox.valueProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof CheckBox checkBox) {
      checkBox.selectedProperty().addListener(((_, _, _) -> parametersChanged()));
    } else if (node instanceof ListView listview) {
      listview.getItems().addListener((ListChangeListener) _ -> parametersChanged());
    } else if (node instanceof CheckComboBox<?> checkCombo) {
      checkCombo.getCheckModel().getCheckedIndices()
          .addListener((ListChangeListener<? super Integer>) _ -> parametersChanged());
    } else if (node instanceof ModuleOptionsEnumComponent<?> options) {
      options.addSubParameterChangedListener(this::parametersChanged);
    } else if (node instanceof EmbeddedParameterComponentProvider prov) {
      for (final Node child : prov.getComponents()) {
        addListenersToNode(child);
      }
    } else if (node instanceof Region panelComp) {
      for (final Node child : panelComp.getChildrenUnmodifiable()) {
        addListenersToNode(child);
      }
    }
  }

  public boolean isValueCheckRequired() {
    return valueCheckRequired;
  }

  protected void addToolTipToControls(Node node, String toolTipText) {
    if (node instanceof Control) {
      ((Control) node).setTooltip(new Tooltip(toolTipText));
    }
    if (node instanceof Region panelComp) {
      for (int i = 0; i < panelComp.getChildrenUnmodifiable().size(); i++) {
        Node child = panelComp.getChildrenUnmodifiable().get(i);
        addToolTipToControls(child, toolTipText);
      }
    }
  }

  /**
   * Adds a button to check the parameter
   */
  public void addCheckParametersButton() {
    Button btnCheck = new Button("Check");
    btnCheck.setOnAction(e -> callCheckParametersButton());
    pnlButtons.getButtons().addAll(btnCheck);
    // this way its always right next to OK button
    ButtonBar.setButtonData(btnCheck, ButtonData.OK_DONE);
  }

}
