/*
 * Copyright (c) 2004-2026 The mzmine Development Team
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

package io.github.mzmine.modules.tools.batchwizard;

import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.parameters.UserParameter;
import java.util.LinkedHashMap;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Observable model for the parameter customization MVCI component. Holds all state as JavaFX
 * properties so the view can bind and react to changes.
 */
public class ParameterCustomizationModel {

  /**
   * Type-safe composite key for the overrides map, replacing fragile string concatenation.
   */
  public record OverrideKey(String moduleClassName, String paramName, ApplicationScope scope) {

    public OverrideKey(@NotNull MZmineRunnableModule module, @NotNull String paramName,
        @NotNull ApplicationScope scope) {
      this(module.getClass().getName(), paramName, scope);
    }
  }

  // --- Selection state ---
  private final ObjectProperty<MZmineRunnableModule> selectedModule = new SimpleObjectProperty<>();
  private final ObservableList<UserParameter<?, ?>> availableParameters = FXCollections.observableArrayList();
  private final ObjectProperty<UserParameter<?, ?>> selectedParameter = new SimpleObjectProperty<>();
  private final ObjectProperty<ApplicationScope> selectedScope = new SimpleObjectProperty<>(
      ApplicationScope.ALL);
  private final ObjectProperty<ParameterOverride> selectedOverride = new SimpleObjectProperty<>();

  // --- Override data (LinkedHashMap preserves insertion order) ---
  private final ObservableMap<OverrideKey, ParameterOverride> overrides = FXCollections.observableMap(
      new LinkedHashMap<>());

  // --- Editor state ---
  private final ObjectProperty<Node> currentEditorComponent = new SimpleObjectProperty<>();
  private final StringProperty instructionsText = new SimpleStringProperty(
      "Select a module to view its parameters");

  // --- Button states ---
  private final BooleanProperty addButtonDisabled = new SimpleBooleanProperty(true);
  private final BooleanProperty overrideButtonDisabled = new SimpleBooleanProperty(true);

  // --- View command properties (controller → view) ---
  // Set by the controller to request the view to scroll the module tree to a specific module.
  private final ObjectProperty<Class<? extends MZmineRunnableModule>> scrollToModuleRequest = new SimpleObjectProperty<>();
  // Set by the controller to request the view to select a parameter by name in the list.
  private final StringProperty parameterNameToSelect = new SimpleStringProperty();

  // --- selectedModule ---

  public ObjectProperty<MZmineRunnableModule> selectedModuleProperty() {
    return selectedModule;
  }

  public MZmineRunnableModule getSelectedModule() {
    return selectedModule.get();
  }

  public void setSelectedModule(MZmineRunnableModule module) {
    selectedModule.set(module);
  }

  // --- availableParameters ---

  public ObservableList<UserParameter<?, ?>> getAvailableParameters() {
    return availableParameters;
  }

  // --- selectedParameter ---

  public ObjectProperty<UserParameter<?, ?>> selectedParameterProperty() {
    return selectedParameter;
  }

  public UserParameter<?, ?> getSelectedParameter() {
    return selectedParameter.get();
  }

  public void setSelectedParameter(UserParameter<?, ?> parameter) {
    selectedParameter.set(parameter);
  }

  // --- selectedScope ---

  public ObjectProperty<ApplicationScope> selectedScopeProperty() {
    return selectedScope;
  }

  public ApplicationScope getSelectedScope() {
    return selectedScope.get();
  }

  public void setSelectedScope(ApplicationScope scope) {
    selectedScope.set(scope);
  }

  // --- selectedOverride ---

  public ObjectProperty<ParameterOverride> selectedOverrideProperty() {
    return selectedOverride;
  }

  public ParameterOverride getSelectedOverride() {
    return selectedOverride.get();
  }

  public void setSelectedOverride(ParameterOverride override) {
    selectedOverride.set(override);
  }

  // --- overrides ---

  public ObservableMap<OverrideKey, ParameterOverride> getOverrides() {
    return overrides;
  }

  // --- currentEditorComponent ---

  public ObjectProperty<Node> currentEditorComponentProperty() {
    return currentEditorComponent;
  }

  public Node getCurrentEditorComponent() {
    return currentEditorComponent.get();
  }

  public void setCurrentEditorComponent(Node component) {
    currentEditorComponent.set(component);
  }

  // --- instructionsText ---

  public StringProperty instructionsTextProperty() {
    return instructionsText;
  }

  public @Nullable String getInstructionsText() {
    return instructionsText.get();
  }

  public void setInstructionsText(@Nullable String text) {
    instructionsText.set(text);
  }

  // --- scrollToModuleRequest ---

  public ObjectProperty<Class<? extends MZmineRunnableModule>> scrollToModuleRequestProperty() {
    return scrollToModuleRequest;
  }

  public void requestScrollToModule(Class<? extends MZmineRunnableModule> moduleClass) {
    // Reset to null first to ensure the subscribe fires even if the same class is requested again.
    scrollToModuleRequest.set(null);
    scrollToModuleRequest.set(moduleClass);
  }

  // --- parameterNameToSelect ---

  public StringProperty parameterNameToSelectProperty() {
    return parameterNameToSelect;
  }

  public void requestSelectParameterByName(String name) {
    // Reset to null first to ensure the subscribe fires even if the same name is requested again.
    parameterNameToSelect.set(null);
    parameterNameToSelect.set(name);
  }
}
