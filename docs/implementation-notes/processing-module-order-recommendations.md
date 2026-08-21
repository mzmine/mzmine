# Processing module order recommendations

## Intention

Help users place processing modules correctly in batch queues. Modules declare their own ordering
recommendations, which are shown in parameter dialogs and checked against the complete batch before
execution.

## Decisions

- `MZmineProcessingModule#getModuleOrderRecommendations()` returns alternative valid placements.
  If any applicable recommendation passes, the module placement is accepted.
- A recommendation only stores its rationale and rule. User-facing messages obtain the declaring
  module's name directly instead of maintaining a separate use-case label.
- A rule positions the module before or after a specific module class. The anchor can either be
  required or checked only if it is present in the same pipeline.
- Rules are sealed into relative-module and custom-condition variants. Custom conditions receive
  the complete batch queue, the inferred pipeline `IndexRange`, and the evaluated step index.
- Declarations, conditions, segmentation, and validation are colocated in
  `io.github.mzmine.modules.batchmode.order`. Only declaration and condition types plus the
  validator facade are public; concrete rules and evaluation details remain package-private.
- `MUST` and `SHOULD` both produce confirmable warnings. They are grouped separately to communicate
  importance; neither blocks execution.
- Applicability is inferred from the `BatchQueue`. A conditional rule with no matching anchor is not
  applicable. If every applicable alternative fails, the least severe violation is reported.
- Concatenated batches are evaluated as separate pipelines. The queue is split recursively at
  repeated data-import steps, then at repeated ADAP or legacy chromatogram-builder steps within each
  resulting segment. Segments use the shared `IndexRange` abstraction. Rules cannot be satisfied by
  steps in another segment.
- Recommendations are appended to the existing parameter-dialog Message area without replacing
  module-specific notes or citations.
- GUI execution asks for confirmation when violations exist. Headless execution logs the grouped
  warnings and continues.
- Initial `MUST` declarations mirror unconditional task preconditions for mass-list consumers, GC
  alignment, compound representation, correlation-dependent ion networking, and ion-network formula
  processing. Mass-list consumers use a shared custom condition that accepts earlier standalone mass
  detection or an earlier advanced data import with either MS1 or MSn mass detection enabled in the
  same inferred pipeline. Feature-list blank subtraction requires any earlier alignment-category
  module, while chromatogram blank subtraction must precede any `FeatureResolverModule`. Other
  parameter-dependent checks remain undeclared until the rule model can express their applicability
  without false positives.

Scientific ordering policies remain in the individual processing modules; the framework does not
define global module relationships.
