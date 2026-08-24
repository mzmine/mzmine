# Processing module order recommendations

## Intention

Help users place processing modules correctly in batch queues. Modules declare their own ordering
recommendations, which are shown in parameter dialogs and checked against the complete batch before
execution.

## Decisions

- `MZmineProcessingModule#getModuleOrderRecommendations()` returns independently evaluated order
  requirements. A passing recommendation does not suppress another recommendation's violation.
- A recommendation only stores its rationale and rule. User-facing messages obtain the declaring
  module's name directly instead of maintaining a separate use-case label.
- A rule positions the module before or after a specific module class. The anchor can either be
  required or checked only if it is present in the same pipeline.
- Relative rules own direction, severity, and whether an anchor is required or only evaluated when
  present. Anchor conditions identify matching batch steps, allowing class-, category-, and
  parameter-sensitive anchors to use the same `before`/`after` rule factories. Conditions may use
  the complete batch queue, inferred pipeline `IndexRange`, and evaluated step index to provide a
  specific validation description while retaining a context-free description for parameter
  dialogs.
- Declarations, conditions, segmentation, and validation are colocated in
  `io.github.mzmine.modules.batchmode.order`. Only declaration and condition types plus the
  validator facade are public; concrete rules and evaluation details remain package-private.
- `MUST` and `SHOULD` both produce confirmable warnings. They are grouped separately to communicate
  importance; neither blocks execution.
- Applicability is inferred from the `BatchQueue`. A conditional rule with no matching anchor is not
  applicable. If multiple applicable recommendations fail, the least severe failed rule is reported
  for that module.
- Concatenated batches are evaluated as separate pipelines. The queue is split recursively at
  repeated data-import steps, then at repeated ADAP or legacy chromatogram-builder steps within each
  resulting segment. Segments use the shared `IndexRange` abstraction. Rules cannot be satisfied by
  steps in another segment.
- Recommendations are appended to the existing parameter-dialog Message area without replacing
  module-specific notes or citations.
- The batch editor marks steps with violated recommendations using a positive-colored information
  icon. Its tooltip contains the validation message and updates after queue or parameter changes.
- GUI execution asks for confirmation when violations exist. Headless execution logs the grouped
  warnings and continues.
- Initial `MUST` declarations mirror unconditional task preconditions for mass-list consumers, GC
  alignment, compound representation, correlation-dependent ion networking, and ion-network formula
  processing. Mass-list consumers use a shared anchor condition that matches standalone mass
  detection or advanced data import with either MS1 or MSn mass detection enabled in the same
  inferred pipeline. Feature-list blank subtraction requires any earlier alignment-category
  module, while chromatogram blank subtraction must precede any `FeatureResolverModule`. Other
  parameter-dependent checks remain undeclared until the rule model can express their applicability
  without false positives.

Scientific ordering policies remain in the individual processing modules; the framework does not
define global module relationships.
