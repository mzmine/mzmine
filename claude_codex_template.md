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

# Inline Comments
- Use short comments to clarify non-obvious logic or business rules.
- Prefer comments above the line they explain.
- Mark decision points with `// decision:` and assumptions with `// assumption:`