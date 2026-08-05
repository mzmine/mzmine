# Factories

- When creating graphical user interfaces, try using existing factory classes in
  io.github.mzmine.javafx.components.util or io.github.mzmine.javafx.components.factories
- When creating new charts, check if we have an appropriate chart, dataset and/or renderer in
  io.github.mzmine.gui.chartbasics.simplechart

# Code style

- When creating new methods or updating methods, add @Nullable and @NotNull annotations to method
  parameters and return values. Use the JetBrains annotations.
- If variables do not change, make them final.
- use exhaustive switches when pattern matching sealed interfaces or enums.
- Use java.util.Logger for logging
- Prefer separate files over nested classes
- Never add the license yourself, the IDE will do that automatically on commit.
- Set a single observable list/collection to JavaFX UI components and update it via setAll instead
  of setting new lists so we can bind and add listeners and they don't get discarded.
- Never use static imports for static method accesses. Only import the class and qualify the call
  with the class name.

# Inline Comments

- Use short comments to clarify non-obvious logic or business rules.
- Prefer comments above the line they explain.
- Mark decision points with `// decision:` and assumptions with `// assumption:`

# Java FX

- Set a single observable list/collection to JavaFX UI components and update it via setAll instead
  of setting new lists so we can bind and add listeners and they don't get discarded.

# Verification

- Always ask the user for input from the user if uncertainties arise during implementation