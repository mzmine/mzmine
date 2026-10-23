# QC drift correction

Choose **QC drift correction** as the **Inter-sample correction** in the intensity normalizer.
**QC correction method** selects **QC-RLSC (Dunn et al., 2011)** by default, or
**Common-scale log LOESS (fixed bandwidth)**. Both calculate one correction per feature and file,
using comparable pooled QC injections selected in **Reference samples**.

Supply acquisition timestamps through raw-file or project metadata. The optional **Batch correction
metadata column** partitions the acquisition into analytical batches; otherwise all files form one
batch. QC-RLSC uses the ordinal acquisition order of the selected files within each batch. The
common-scale method uses elapsed acquisition seconds, including fractional seconds. Missing or tied
timestamps within a batch are rejected because they do not establish an unambiguous order.

## QC-RLSC

This mode implements the fitting procedure described by
[Dunn et al. (2011), section “LOESS curve fitting” and step 33](https://doi.org/10.1038/nprot.2011.335):
tricube-weighted local linear and quadratic regression, discrete neighborhood-size selection by
leave-one-out cross-validation, and cubic-spline interpolation of the fitted QC knots. Every
candidate must predict every held-out QC; failed folds cannot improve its score by being omitted.
The held-out observation is also excluded from robust residual weighting.

The implementation fits positive finite **raw response**, after any preceding metadata or
internal-standard correction. It uses configurable bisquare residual reweighting, a natural cubic
spline, and the nearest fitted boundary value outside the usable QC interval. These are explicit
numerical conventions where the paper does not prescribe all implementation details.

Correction is multiplicative and preserves the mean QC scale within each batch:

```text
factor = arithmetic mean of usable batch QC responses / fitted QC response at injection order
```

This is the divisive, mean-QC rescaling option also provided by the
[qcrlscR package](https://github.com/wanchanglin/qcrlscR). The package is a reference, not a runtime
dependency: its optional GCV search and wrapper-level log transformation are not substituted for
Dunn's discrete LOOCV fitting procedure. This mode corrects within-batch drift; rescaling to each
batch's own QC mean does not remove between-batch offsets. No additional scalar batch correction
is applied automatically.

**Initial QC injections to exclude** removes conditioning QCs from fitting, missingness counts, and
QC diagnostics independently in each batch. The default is zero for data whose conditioning QCs
were already excluded or assigned another sample type. Dunn's GC-MS workflow excludes three initial
QC injections and the UPLC-MS workflow excludes eight. Set the count appropriate to the workflow;
excluded injections remain in the output and receive a boundary correction where applicable.

This module implements drift correction, not the entire sample-preparation and QA workflow in the
paper. It does not automatically delete features at the paper's GC-MS/UPLC-MS RSD cutoffs. Review the
diagnostics and apply the appropriate feature-quality filters separately.

## Common-scale log LOESS

This retains the previous fixed-bandwidth algorithm. Apache Commons Math fits locally linear LOESS
to natural-log QC responses in each batch. A feature's shared target is the median of natural logs
of positive finite effective abundances across **all selected QC and study files**:

```text
factor = exp(global median log abundance - fitted batch QC log abundance at acquisition time)
```

The median of logs can differ from the logarithm of the raw median. This target aligns batch curves
to one common feature scale. It is intentionally a different method from QC-RLSC. The selected
bandwidth remains fixed; its leave-one-out diagnostic does not perform automatic parameter selection.

## Settings

| Setting | Default | Meaning |
| --- | --- | --- |
| QC correction method | QC-RLSC (Dunn et al., 2011) | Automatic raw-response QC-RLSC, or fixed-bandwidth common-scale log LOESS. |
| Initial QC injections to exclude | 0 | First selected QC injections per batch omitted from fitting and QC diagnostics. |
| LOESS bandwidth | 0.75 | Used only by the common-scale method; fraction of usable QCs in each local fit. QC-RLSC selects its span automatically. |
| Robustness iterations | 2 | Bisquare residual reweighting iterations in either solver; independent of automatic span/degree selection. |
| Minimum usable QCs | 5 | Minimum positive finite QC responses per feature and batch; at least five. |
| Maximum reciprocal factor | 10 | Reject a feature/batch if any factor is outside 0.1–10 at this default; never clip factors. |
| Global missing QC fraction | 0.5 | Skip a feature globally when its missing-QC fraction exceeds this value. |

The selected raw **Area** or **Height** determines the model. The resulting factor applies to both
normalized area and normalized height when those measurements exist; an area model does not
independently validate height correction. Stored normalized values are never model inputs.
Metadata and internal-standard corrections precede QC correction and are composed once into the
final function.

UNKNOWN, missing, nonpositive, and nonfinite model inputs are excluded. Estimated features with
positive finite responses are included. The global threshold is inclusive. QC-RLSC permits exactly
50% missing QCs in a batch, subject to the minimum count, matching the paper's removal of features
detected in fewer than half of QCs. The common-scale option retains its original stricter batch rule:
strictly more than half must be usable. Global missingness counts QC injections, not an average of
batch percentages.

Unsupported features remain in the output with missing normalized values for the affected batches;
valid corrections in other batches remain. There is no invented identity-factor or raw-value
fallback. Invalid fits, insufficient support, and excessive correction factors have explicit reasons.
The configured minimum count and factor bound are mzmine safeguards, not universal values prescribed
by Dunn. A run with no successful corrections finishes with a warning.

## Diagnostics and persistence

Open **QC diagnostics** from the applied-methods pane. Diagnostics retain the method, settings,
solver, response scale, coverage, failure reasons, factor range, boundaries, and fitted QC curve.
QC-RLSC additionally records the chosen polynomial degree, neighborhood size, span, and mean-QC
correction target. Its LOOCV score is the **model-selection error**, not an independent estimate of
prediction performance. Common-scale LOESS reports error on the natural-log scale; QC-RLSC reports
error on the raw-response scale. These errors are not directly comparable.

Before/after QC RSD uses the same fitting observations and is the sample standard deviation divided
by the arithmetic mean. A low fitted RSD does not by itself establish acceptable scientific quality.
The full diagnostics are saved for inspection after project reload. The diagnostics serialization
format has its own version; scientific methods are identified by their names and settings, not a
shared development revision number.

## Subsequent processing

Active correction functions are stored directly with the feature-list preferences. Historical
processing evidence is retained in the applied method. Copying and loading preserve row identities;
filtering is supported with **Reset row IDs** disabled. IDs reserved by row-dependent outcomes cannot
be reused for unrelated new rows. A genuinely new row with an unused, unreserved ID may remain
unnormalized; the task adding it decides whether it can apply a meaningful correction.

The native API uses `inter_sample.method: "feature_qc"` and `qc_method: "dunn_qc_rlsc"` (default) or
`"common_scale_log_loess"`. It accepts `bandwidth` only for the common-scale option. GUI and API
runs use the same native implementation.

Numerical reference files are in the
[fixture directory](../mzmine-community/src/test/resources/io/github/mzmine/modules/dataprocessing/norm_intensity/feature_qc/).
Independent solver fixtures do not substitute for validation on representative analytical data.
