# Factories

- When creating graphical user interfaces, try using existing factory classes in
  io.github.mzmine.javafx.components.util or io.github.mzmine.javafx.components.factories
- When creating new charts, check if we have an appropriate chart, dataset and/or renderer in
  io.github.mzmine.gui.chartbasics.simplechart

# Code style

- When creating new methods or updating methods, add @Nullable and @NotNull annotations to method
  parameters and return values. Use the JetBrains annotations.
- Don't apply Objects.requireNonNull checks or similar to annotated variables.
- If variables do not change, make them final.
- use exhaustive switches when pattern matching sealed interfaces or enums.
- Use java.util.Logger for logging
- Prefer separate files over nested classes
- Never add the license yourself, the IDE will do that automatically on commit.
- Add javadoc, use @param and @return
- Never use static imports for static method accesses. Only import the class and qualify the call
  with the class name.

# Inline Comments

- Use short comments to clarify non-obvious logic or business rules.
- Prefer comments above the line they explain.
- Mark decision points with `// decision:` and assumptions with `// assumption:`

# Java FX

- Set a single observable list/collection to JavaFX UI components and update it via setAll instead
  of setting new lists so we can bind and add listeners and they don't get discarded.

# Scope of changes

Keep branches and PRs focused so they are easy to review and changes to mzmine internals stay
maintainable.

- A feature branch adds a single feature. Do not bundle related or unrelated changes, refactorings
  or fixes into it. Suggest them as separate branches instead.
- Branches that change mzmine internals contain only those changes and nothing else. Internals
  include:
  - batch processing and data import, e.g., `BatchTask`, `AllSpectralDataImportModule`,
    `BatchWizardTab`, WizardBatchBuilder classes
  - core data models, e.g., `ModularDataModel`, `MetadataTable`, `MZmineProject`
  - any new way of saving data other than adding a new `DataType`, e.g., new fields in project
    save/load, new files in the project archive
- If a change grows beyond its original scope, or a feature branch starts to change internals, stop
  and tell the user. Suggest moving the extra changes to a new branch.

# Verification

- Always ask the user for input from the user if uncertainties arise during implementation