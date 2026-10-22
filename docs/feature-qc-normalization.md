# Feature-specific QC LOESS normalization

Choose **Feature-specific QC LOESS** as the **Inter-sample correction** in the intensity
normalizer. Select comparable pooled QC injections in **Reference samples** and supply acquisition
timestamps through raw-file or project metadata. The method creates a new feature list and preserves
the source, including when the general original-list option is set to process in place.

Use the optional **Batch correction metadata column** when the acquisition contains multiple
analytical batches. Without it, all files form one batch. Each feature gets its own QC drift curve
within each batch. The batch curves share one feature target; the scalar batch-alignment step is
not applied afterwards.

## Settings

| Setting | Default | Meaning |
| --- | ---: | --- |
| LOESS bandwidth | 0.75 | Fraction of usable QC observations in each local fit; greater than 0 and at most 1. |
| Robustness iterations | 2 | Nonnegative number of robust reweighting iterations. |
| Minimum usable QCs | 5 | Minimum positive finite QC responses per feature and batch; cannot be less than 5. |
| Maximum reciprocal factor | 10 | Reject a feature/batch when any factor is outside 0.1–10 at this default. The value must be finite and greater than 1. Factors are not clipped. |
| Global missing QC fraction | 0.5 | Skip a feature globally when its missing QC fraction exceeds this value. |

The selected raw **Area** or **Height** determines the model. The resulting factor is applied to both
normalized area and normalized height when those measurements exist. A model based on area therefore
does not independently validate the height correction. Previously stored normalized columns are
never model inputs.

Optional metadata and internal-standard corrections run first. Their effective abundances determine
both the QC fits and the common feature target. The final function composes these stages once.

## Output scale and missing values

For each feature, mzmine calculates the median of the natural logarithms of positive finite
abundances across **all selected files, including QC and study samples**, after preceding correction
stages. It fits the logarithms of usable QC abundances separately in each batch. At acquisition time
`t`, the factor is:

```text
factor = exp(global median log abundance - fitted batch QC log abundance at t)
```

The median of logs is intentional; it can differ from the log of the raw median. Estimated features
with positive finite abundances are included. Missing, UNKNOWN, nonpositive and nonfinite model
inputs are excluded.

A global missing-QC fraction equal to the configured threshold is allowed. Within a batch,
**strictly more than half** of its selected QC responses must be usable, in addition to the minimum
count. Exactly 50% missing in a batch is unsupported. Global missingness is counted across all QC
injections, not by averaging batch percentages.

Unsupported features remain in the output. Global failures leave all normalized values missing;
batch failures leave only the affected batch missing. Valid corrections in other batches remain.
Reasons include missing QC support, insufficient usable QCs, failed or invalid fits, and factor-bound
violations. A run with no successful corrections finishes with an explicit warning. There is no raw
value or identity-factor fallback. Existing normalized columns in the output are cleared first, and
missing raw measurements remain missing even when a factor exists. A result that overflows the
stored measurement range also remains missing; the other abundance measure can still be corrected.

Acquisition time retains fractional seconds. Outside a feature's usable QC time interval, mzmine
uses the nearest fitted boundary value and records a boundary flag. Duplicate coordinates within a
batch, missing timestamps, ambiguous sample mappings and invalid settings are errors.

## Reviewing the correction

Open **QC diagnostics** from the applied-methods pane. The saved diagnostics show settings, solver,
source revision, modeled abundance and overall availability. Select a feature row to inspect:

- Usable and total QC counts and the usable acquisition interval per batch.
- Before/after QC RSD using the same usable observations.
- Leave-one-out prediction error on the log scale, successful prediction count and failures.
- Factor extrema, boundary counts, fit points and explicit unavailable reasons.

RSD is the sample standard deviation divided by the arithmetic mean, expressed as a fraction.
An unavailable validation score is not zero. Low RSD or prediction error is evidence to review;
neither certifies batch acceptance. The method requires the selected QCs to be scientifically
comparable.

## Subsequent processing

Factors and unavailable reasons are saved exactly with the applied method. Feature updates and gap
filling use the latest normalization summary. Filtering remains supported with **Reset row IDs**
disabled. Renumbering and transformations that change row identity are rejected while row-dependent
normalization is active. Removed rows' IDs remain reserved; a new row receives an unprotected ID
and has no invented QC factor.

MCP previews and native processing use the same calculation for matching inputs and settings. The
MCP method is version 2 and rejects the old `correct_between_batches` option for feature-QC. The
sample-wide `qc_batch` method retains that option and its established timestamp interpolation.
Reviewed application uses the saved factors without refitting. Re-execution verifies the saved
source fingerprint before creating a normalized copy.

## Reference interpretation

The scale and missingness rules follow the
[pmartR/qcrlsc normalization design](https://github.com/pmartR/qcrlsc/tree/5b5ee02657c72ba0727fb3ef2a9a001d057add45),
an implementation based on [Dunn et al.](https://doi.org/10.1038/nprot.2011.335).
The solver is Apache Commons Math 3 local linear LOESS with bisquare reweighting and cubic-spline
interpolation of fitted knots. Its configured span, robustness, minimum of five QCs and boundary
behavior differ from R's parameter search and prediction policies. Numerical R fixtures and their
limitations are recorded in the
[reference fixture notes](../mzmine-community/src/test/resources/io/github/mzmine/modules/dataprocessing/norm_intensity/feature_qc/README.md).

See the [persistence benchmark](qc-normalization-persistence.md) for measured storage and memory use.
