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

import io.github.mzmine.javafx.mvci.FxController;
import io.github.mzmine.javafx.mvci.FxViewBuilder;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineModule;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.modules.tools.batchwizard.ParameterCustomizationModel.OverrideKey;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ApplicationScope;
import io.github.mzmine.modules.tools.batchwizard.subparameters.ParameterOverride;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.parameters.parametertypes.HiddenParameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * MVCI controller for the parameter customization pane. Owns all business logic and updates the
 * {@link ParameterCustomizationModel}. The {@link ParameterCustomizationViewBuilder} creates the UI
 * and binds it to the model.
 */
public class ParameterCustomizationController extends FxController<ParameterCustomizationModel> {

  private final ParameterCustomizationViewBuilder viewBuilder;

  public ParameterCustomizationController() {
    super(new ParameterCustomizationModel());
    viewBuilder = new ParameterCustomizationViewBuilder(model, this::onModuleSelected,
        this::onParameterSelected, this::addParameterOverride, this::updateParameterOverride,
        this::removeSelectedOverride, this::clearAllOverrides, this::changeOverrideScope,
        this::onOverrideSelected, this::onParameterListKeyPressed);
    setupModelListeners();
  }

  private void setupModelListeners() {
    // When scope changes (user or controller), reload the existing override value for the current parameter.
    model.selectedScopeProperty().subscribe(scope -> {
      if (model.getSelectedParameter() != null && scope != null) {
        loadExistingOverrideValue(model.getSelectedParameter());
      }
      updateButtonStates();
    });
  }

  @Override
  protected @NotNull FxViewBuilder<ParameterCustomizationModel> getViewBuilder() {
    return viewBuilder;
  }

  // --- Module / parameter selection ---

  void onModuleSelected(@Nullable MZmineRunnableModule module) {
    model.setSelectedModule(module);
    model.setSelectedParameter(null);
    model.setCurrentEditorComponent(null);
    model.getAvailableParameters().clear();
    updateButtonStates();

    if (module == null) {
      model.setInstructionsText("Select a module to view its parameters");
      model.setShowInstructions(true);
      return;
    }

    // Clone the parameter set so edits in the UI don't affect the global config.
    ParameterSet parameters = ConfigService.getConfiguration()
        .getModuleParameters(module.getClass()).cloneParameterSet();
    if (parameters == null) {
      model.setInstructionsText("No parameters available for this module");
      model.setShowInstructions(true);
      return;
    }

    List<UserParameter<?, ?>> params = new ArrayList<>();
    Arrays.stream(parameters.getParameters())
        .filter(p -> p instanceof UserParameter<?, ?> && !(p instanceof HiddenParameter))
        .map(p -> (UserParameter<?, ?>) p).forEach(params::add);
    model.getAvailableParameters().setAll(params);

    if (params.isEmpty()) {
      model.setInstructionsText("No user-configurable parameters for this module");
    } else {
      model.setInstructionsText("Select a parameter to edit its value");
    }
    model.setShowInstructions(true);
  }

  void onParameterSelected(@Nullable UserParameter<?, ?> parameter) {
    if (parameter != null && model.getSelectedModule() != null) {
      model.setSelectedParameter(parameter);
      Node editor = createEditorForParameter(parameter);
      model.setCurrentEditorComponent(editor);
      if (editor != null) {
        loadExistingOverrideValue(parameter);
        model.setShowInstructions(false);
      } else {
        model.setInstructionsText("Cannot create editor for this parameter type");
        model.setShowInstructions(true);
      }
    } else {
      model.setSelectedParameter(null);
      model.setCurrentEditorComponent(null);
      model.setShowInstructions(true);
    }
    updateButtonStates();
  }

  // --- Override management ---

  void addParameterOverride() {
    putParameterOverride("Parameter override added");
  }

  void updateParameterOverride() {
    putParameterOverride("Parameter override updated");
  }

  @SuppressWarnings("unchecked")
  private void putParameterOverride(String actionLabel) {
    UserParameter<?, ?> selectedParameter = model.getSelectedParameter();
    MZmineRunnableModule selectedModule = model.getSelectedModule();
    Node currentEditorComponent = model.getCurrentEditorComponent();
    if (selectedParameter == null || selectedModule == null || currentEditorComponent == null) {
      return;
    }
    try {
      UserParameter<Object, Node> param = (UserParameter<Object, Node>) selectedParameter;
      param.setValueFromComponent(currentEditorComponent);

      String moduleClassName = selectedModule.getClass().getName();
      String moduleUniqueId = getModuleUniqueId(selectedModule.getClass());
      ApplicationScope scope = model.getSelectedScope();
      final ParameterOverride added = new ParameterOverride(moduleClassName, moduleUniqueId,
          param.cloneParameter(), scope);
      model.getOverrides().put(new OverrideKey(selectedModule, param.getName(), scope), added);
      model.setSelectedOverride(added);
      model.setInstructionsText(
          actionLabel + ": " + param.getName() + " (apply to: " + scope.getLabel() + ")");
      model.setShowInstructions(true);
    } catch (Exception e) {
      model.setInstructionsText("Error: " + e.getMessage());
      model.setShowInstructions(true);
    }
  }

  void removeSelectedOverride() {
    ParameterOverride selected = model.getSelectedOverride();
    if (selected == null) {
      return;
    }
    model.getOverrides().remove(
        new OverrideKey(selected.moduleClassName(), selected.parameterWithValue().getName(),
            selected.scope()));
  }

  void clearAllOverrides() {
    model.getOverrides().clear();
    model.setInstructionsText("All parameter overrides cleared");
    model.setShowInstructions(true);
  }

  void changeOverrideScope(@NotNull ParameterOverride override,
      @NotNull ApplicationScope newScope) {
    String moduleClass = override.moduleClassName();
    String paramName = override.parameterWithValue().getName();
    model.getOverrides().remove(new OverrideKey(moduleClass, paramName, override.scope()));
    model.getOverrides().put(new OverrideKey(moduleClass, paramName, newScope),
        new ParameterOverride(moduleClass, override.moduleUniqueId(), override.parameterWithValue(),
            newScope));
  }

  /**
   * Called when the user selects an override in the overrides table. Synchronizes the module tree,
   * parameter list, and scope selector to reflect the selected override.
   */
  @SuppressWarnings("unchecked")
  void onOverrideSelected(@Nullable ParameterOverride override) {
    if (override == null) {
      model.setSelectedOverride(null);
      updateButtonStates();
      return;
    }
    model.setSelectedOverride(override);

    // Early-exit: the UI is already in sync with this override (e.g. after putParameterOverride).
    MZmineRunnableModule currentModule = model.getSelectedModule();
    if (currentModule != null && currentModule.getClass().getName()
        .equals(override.moduleClassName()) && model.getSelectedParameter() != null
        && model.getSelectedParameter().getName().equals(override.parameterWithValue().getName())
        && override.scope().equals(model.getSelectedScope())) {
      updateButtonStates();
      return;
    }

    MZmineRunnableModule targetModule;
    Class<? extends MZmineRunnableModule> targetModuleType;
    try {
      Class<?> moduleClass = Class.forName(override.moduleClassName());
      if (!MZmineRunnableModule.class.isAssignableFrom(moduleClass)) {
        updateButtonStates();
        return;
      }
      targetModuleType = (Class<? extends MZmineRunnableModule>) moduleClass;
      targetModule = MZmineCore.getModuleInstance(targetModuleType);
    } catch (Exception e) {
      updateButtonStates();
      return;
    }

    if (targetModule == null) {
      updateButtonStates();
      return;
    }

    // Set scope first so the scopeComboBox updates before the parameter editor reloads.
    model.setSelectedScope(override.scope());

    // Request the view to scroll the module tree. This will trigger onModuleSelected, which
    // populates availableParameters synchronously before requestSelectParameterByName fires.
    model.requestScrollToModule(targetModuleType);

    // Request the view to select the parameter by name in the list. By the time this fires
    // (synchronously on the FX thread), availableParameters is already populated.
    model.requestSelectParameterByName(override.parameterWithValue().getName());

    updateButtonStates();
  }

  // --- Keyboard handling ---

  void onParameterListKeyPressed(KeyEvent e) {
    if (e.getCode() != KeyCode.ENTER) {
      return;
    }
    Node currentEditor = model.getCurrentEditorComponent();
    Control c = switch (currentEditor) {
      case Control control -> control;
      case Parent p -> findFirstControlNode(p);
      case null, default -> null;
    };
    if (c != null) {
      e.consume();
      c.requestFocus();
      c.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
        if (ev.getCode() == KeyCode.ENTER) {
          addParameterOverride();
        }
      });
    }
  }

  // --- Private helpers ---

  /**
   * Checks if an override already exists for the current module+parameter+scope and loads its value
   * into the editor component if so.
   */
  @SuppressWarnings("unchecked")
  private void loadExistingOverrideValue(UserParameter<?, ?> parameter) {
    MZmineRunnableModule selectedModule = model.getSelectedModule();
    Node currentEditorComponent = model.getCurrentEditorComponent();
    if (selectedModule == null || currentEditorComponent == null) {
      return;
    }
    ApplicationScope currentScope = model.getSelectedScope();
    ParameterOverride temp = model.getOverrides()
        .get(new OverrideKey(selectedModule, parameter.getName(), currentScope));
    if (temp != null) {
      try {
        ((UserParameter<Object, Node>) parameter).setValueToComponent(currentEditorComponent,
            temp.parameterWithValue().getValue());
      } catch (Exception e) {
        // Ignore type mismatch — the override value may not be compatible.
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T, C extends Node> @Nullable Node createEditorForParameter(
      UserParameter<?, ?> parameter) {
    try {
      UserParameter<T, C> typedParam = (UserParameter<T, C>) parameter;
      C component = typedParam.createEditingComponent();
      typedParam.setValueToComponent(component, typedParam.getValue());
      return component;
    } catch (Exception e) {
      return null;
    }
  }

  private void updateButtonStates() {
    UserParameter<?, ?> selectedParameter = model.getSelectedParameter();
    MZmineRunnableModule selectedModule = model.getSelectedModule();
    Node currentEditorComponent = model.getCurrentEditorComponent();

    final boolean hasParameterSelected =
        selectedParameter != null && selectedModule != null && currentEditorComponent != null;
    model.setAddButtonDisabled(!hasParameterSelected);

    final boolean overrideExists = hasParameterSelected && model.getOverrides().containsKey(
        new OverrideKey(selectedModule, selectedParameter.getName(), model.getSelectedScope()));
    model.setOverrideButtonDisabled(!overrideExists);
  }

  private String getModuleUniqueId(Class<? extends MZmineRunnableModule> moduleClass) {
    try {
      MZmineModule module = MZmineCore.getModuleInstance(moduleClass);
      return module != null ? module.getUniqueID() : moduleClass.getSimpleName();
    } catch (Exception e) {
      return moduleClass.getSimpleName();
    }
  }

  private @Nullable Control findFirstControlNode(@NotNull Parent p) {
    ObservableList<Node> children = p.getChildrenUnmodifiable();
    for (Node child : children) {
      if (child instanceof Control c) {
        return c;
      }
    }
    for (Node child : children) {
      if (child instanceof Parent cp) {
        Control c = findFirstControlNode(cp);
        if (c != null) {
          return c;
        }
      }
    }
    return null;
  }

  // --- Public API (used by ParameterCustomizationPane adapter) ---

  public List<ParameterOverride> getParameterOverrides() {
    return new ArrayList<>(model.getOverrides().values());
  }

  public void setParameterOverrides(List<ParameterOverride> overrides) {
    model.getOverrides().clear();
    for (ParameterOverride override : overrides) {
      model.getOverrides().put(
          new OverrideKey(override.moduleClassName(), override.parameterWithValue().getName(),
              override.scope()), override);
    }
  }
}
