# QC drift correction

Choose **QC drift correction** as the **Inter-sample correction** in the intensity normalizer.
The same QC-RLSC pipeline supports **Automatic smoothing** (enabled by default) or fixed smoothing.
Both calculate one correction per feature and file, using comparable pooled QC injections selected
in **Reference samples**.

Supply acquisition timestamps through raw-file or project metadata. The optional **Batch correction
metadata column** partitions the acquisition into analytical batches; otherwise all files form one
batch. QC-RLSC uses the ordinal acquisition order of the selected files within each batch. Missing
or tied timestamps within a batch are rejected because they do not establish an unambiguous order.

## QC-RLSC

With automatic smoothing enabled, the pipeline implements the fitting procedure described by
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

## Fixed smoothing

Disable **Automatic smoothing** to supply the **LOESS bandwidth** and **Polynomial degree** directly.
This skips the entire cross-validation search. The same raw-response fitter, robust reweighting,
QC selection, injection-order coordinates, spline interpolation, boundary handling, and batch-QC
mean rescaling still apply.

The neighborhood contains `ceil(bandwidth * usable QC count)` observations. It must contain at least
`polynomial degree + 2` neighbors, and each local fit still needs enough positively weighted,
independent observations. Unsupported fits are reported as unavailable; the algorithm does not
silently increase the span or change the degree. Diagnostics record the actual neighborhood/span
and the supplied degree. No cross-validation score is reported for fixed smoothing.

Fixed smoothing changes only parameter selection. It does not switch to log intensities, elapsed-time
coordinates, or a global scaling target.

## Settings

| Setting | Default | Meaning |
| --- | --- | --- |
| Automatic smoothing | Enabled | Select span and degree by complete leave-one-out cross-validation. Disable to use the fixed settings below. |
| Initial QC injections to exclude | 0 | First selected QC injections per batch omitted from fitting and QC diagnostics. |
| LOESS bandwidth | 0.75 | Fixed smoothing only: fraction of usable QCs per local neighborhood, rounded up. |
| Polynomial degree | 1 | Fixed smoothing only: 1 for linear or 2 for quadratic fitting. |
| Robustness iterations | 2 | Bisquare residual reweighting iterations; independent of automatic span/degree selection. |
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
detected in fewer than half of QCs. The same rule applies with fixed smoothing. Global missingness
counts QC injections, not an average of batch percentages.

Unsupported features remain in the output with missing normalized values for the affected batches;
valid corrections in other batches remain. There is no invented identity-factor or raw-value
fallback. Invalid fits, insufficient support, and excessive correction factors have explicit reasons.
The configured minimum count and factor bound are mzmine safeguards, not universal values prescribed
by Dunn. A run with no successful corrections finishes with a warning.

## Diagnostics and persistence

Open **QC diagnostics** from the applied-methods pane. Diagnostics retain the method, settings,
solver, response scale, coverage, failure reasons, factor range, boundaries, and fitted QC curve.
Diagnostics also record the polynomial degree, neighborhood size, effective span, and mean-QC
correction target. With automatic smoothing, the raw-response LOOCV score is the **model-selection
error**, not an independent estimate of prediction performance. Fixed smoothing records that
cross-validation was not performed, with no score and zero validation predictions or failures.

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

The native API uses `inter_sample.method: "feature_qc"`. `automatic_smoothing` defaults to `true`.
Set it to `false` to supply `bandwidth` and `polynomial_degree`; omitted fixed settings default to
0.75 and 1. Manual fitting settings are rejected when automatic smoothing is enabled. For example:

```json
{"inter_sample": {"method": "feature_qc", "automatic_smoothing": false,
                  "bandwidth": 0.75, "polynomial_degree": 1}}
```

GUI and API runs use the same native implementation.

Numerical reference files are in the
[fixture directory](../mzmine-community/src/test/resources/io/github/mzmine/modules/dataprocessing/norm_intensity/feature_qc/).
Independent solver fixtures do not substitute for validation on representative analytical data.
