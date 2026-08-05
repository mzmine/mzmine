---
name: MVCI
description: Use a MVC or MVCI architecture when building complex user interface components with multiple controls or panes and if the user requests the MVC/MVCI architecture. For simpler interfaces, default to single classes.
---

- Do not use FXML.
- Split gui logic into a Model, a Controller, a ViewBuilder and potentially an Interactor

## Model

- Treat the Model as the single source of truth.
- The Model controls the state. It holds all data in JavaFX properties.
- Indicate if the value of a property can be null or not with @Nullable or @NotNull annotations in
  the `<>` brackets such as `ObjectProperty<@Nullable X>`.

## Controller

- The Controller is exposed to the outside and handles interactions of other application parts with
  the Model and contains simple logic.
- The Controller should extend io.github.mzmine.javafx.mvci.FxController or
  io.github.mzmine.javafx.mvci.FxCachedViewController.
- Use the FxCachedViewController if the same view needs to be accessed from multiple locations to
  avoid building the view multiple times.
- For threading, use the onGuiThread and onTaskThread methods from the FxController.
- onGuiThread should be used in public methods that change the state of the model.
- onTaskThread can be used for complex computations that would freeze the view.

## View builder

- The ViewBuilder is responsible for creating the UI components and layout and binds them to the
  Model.
- The ViewBuilder should extend io.github.mzmine.javafx.mvci.FxViewBuilder
- Try using existing factory classes in io.github.mzmine.javafx.components.util or
  io.github.mzmine.javafx.components.factories.
- Consider using Insets.EMPTY when wrapping multiple components from the factory to not create
  unnecessary whitespace.
- Prefer bindings from the View to the Model over listeners. Use bidirectional bindings if possible.
- The ViewBuilder is responsible for the logic that reacts to user input.
- The ViewBuilder is responsible for logic that reflects the Controller's or Interactor's changes to
  the Model in the View.
- Event handlers to react to user interactions should be put into the view builder.
- UI control listeners should update one model property (the direct user intent).
- UI control listeners should not update multiple model properties or perform derived UI
  recomputation.
- Derived updates must be triggered by model-property listeners in the ViewBuilder.
- Prefer bindings from View to Model; if control property is read-only (e.g., selectedItem), use a
  one-line listener that only writes the matching model property.
- Avoid calling `onXSelected(...)` methods from other listeners/subscribers; use model properties to
  drive flow.
-

## Interactor

- The interactor is not strictly required. In case it is used, the interactions with the outside
  world should be moved from the Controller to the Interactor.
- The Interactor is responsible for handling business logic. Reacting to user input is NOT business
  logic and belongs into the ViewBuilder.
- The Interactor gets a reference of the Model passed to it by the Controller.

## Logic placement decision

- The ViewBuilder handles UI events wiring, the Controller or Interactor handle application-facing
  commands.

## Anti-patterns (forbidden)

- `selectedItem listener -> calls onParameterSelected(...)` where `onParameterSelected` sets several
  model properties.
- Any UI listener that sets `selected*` plus `propertyX` plus `propertyY` in one callback.
- Bidirectional sync loops managed by ad-hoc cross-calls instead of model-driven updates.