# Feature specific QC drift correction

Choose **Feature specific QC drift correction (QC-RLSC)** as the **Inter-sample correction** in the intensity normalizer.
The **Smoothing** option selects **QC-RLSC** (default, automatic) or **Manual LOESS**. Both
calculate one correction per feature and file, using comparable pooled QC injections selected in
**Reference samples**.

Supply acquisition timestamps through raw-file or project metadata. The optional **Batch correction
metadata column** partitions the acquisition into analytical batches; otherwise all files form one
batch. The drift is modeled against the ordinal acquisition order of the selected files within each
batch. Missing or tied timestamps within a batch are rejected because they do not establish an
unambiguous order.

## QC-RLSC

**QC-RLSC** implements the fitting procedure described by
[Dunn et al. (2011), section “LOESS curve fitting” and step 33](https://doi.org/10.1038/nprot.2011.335):
tricube-weighted local linear and quadratic regression, discrete neighborhood-size selection by
leave-one-out cross-validation, and cubic-spline interpolation of the fitted QC knots. Every
candidate must predict every held-out QC; failed folds cannot improve its score by being omitted.
The held-out observation is also excluded from robust residual weighting. QC-RLSC has no
parameters.

The implementation fits positive finite **raw response**, after any preceding metadata or
internal-standard correction. It uses bisquare residual reweighting, a natural cubic spline, and the
nearest fitted boundary value outside the usable QC interval. These are explicit numerical
conventions where the paper does not prescribe all implementation details. Dunn does not state the
number of robustness iterations; QC-RLSC uses two, as in the robust LOESS by Cleveland (1979) that
Dunn cites.

Correction is multiplicative. Every batch of a feature is rescaled to one common target:

```text
factor = median of usable QC responses across all batches / fitted QC response at injection order
```

Dunn divides by the fitted QC curve, which brings the QCs of every batch to the same level and so
integrates the batches without a separate between-batch step. Multiplying all batches by the same
per-feature median keeps this alignment and the abundance scale. A per-batch target would restore
the batch offsets. The [qcrlscR package](https://github.com/wanchanglin/qcrlscR) is a reference, not
a runtime dependency: its optional GCV search and wrapper-level log transformation are not
substituted for Dunn's discrete LOOCV fitting procedure. No additional scalar batch correction is
applied.

Dunn excludes the first QC injections of each batch that condition the system (three for GC-MS,
eight for UPLC-MS). mzmine has no setting for this: assign these injections a sample type other than
the reference samples, or remove them from the feature list.

This module implements drift correction, not the entire sample-preparation and QA workflow in the
paper. It does not automatically delete features at the paper's GC-MS/UPLC-MS RSD cutoffs. Review the
diagnostics and apply the appropriate feature-quality filters separately.

## Manual LOESS

**Manual LOESS** uses the supplied **LOESS bandwidth**, **Polynomial degree** and **Robustness
iterations** for all features and batches. This skips the entire cross-validation search. The same
raw-response fitter, QC selection, injection-order coordinates, spline interpolation, boundary
handling, and common QC-median rescaling still apply.

The neighborhood contains `ceil(bandwidth * usable QC count)` observations. It must contain at least
`polynomial degree + 2` neighbors, and each local fit still needs enough positively weighted,
independent observations. Unsupported fits are reported as unavailable; the algorithm does not
silently increase the span or change the degree. Diagnostics record the actual neighborhood/span
and the supplied degree. No cross-validation score is reported for manual LOESS.

Manual LOESS changes only parameter selection. It does not switch to log intensities or
elapsed-time coordinates.

## Settings

| Setting | Default | Meaning |
| --- | --- | --- |
| Reference samples | QC | Comparable pooled QC sample types used for each feature's drift fit. |
| Smoothing | QC-RLSC | QC-RLSC selects span and degree by complete leave-one-out cross-validation; Manual LOESS uses the settings below. |
| LOESS bandwidth | 0.75 | Manual LOESS only: fraction of usable QCs per local neighborhood, rounded up. |
| Polynomial degree | 1 | Manual LOESS only: 1 for linear or 2 for quadratic fitting. |
| Robustness iterations | 2 | Manual LOESS only: bisquare residual reweighting iterations. QC-RLSC always uses two. |
| Minimum usable QCs | 5 | Minimum positive finite QC responses per feature and batch; at least five. |
| Maximum correction factor | 10 | Largest allowed change up or down (factors 0.1–10 at this default). A feature/batch that needs more gets no normalized values; factors are never capped. mzmine safeguard, not part of Dunn. |
| Global missing QC fraction | 0.5 | Skip a feature globally when its missing-QC fraction exceeds this value. |

The selected raw **Area** or **Height** determines the model. The resulting factor applies to both
normalized area and normalized height when those measurements exist; an area model does not
independently validate height correction. Stored normalized values are never model inputs.
Metadata and internal-standard corrections precede QC correction and are composed once into the
final function.

UNKNOWN, missing, nonpositive, and nonfinite model inputs are excluded. Estimated features with
positive finite responses are included. The global threshold is inclusive. QC-RLSC permits exactly
50% missing QCs in a batch, subject to the minimum count, matching the paper's removal of features
detected in fewer than half of QCs. The same rule applies with manual LOESS. Global missingness
counts QC injections, not an average of batch percentages.

Unsupported features remain in the output with missing normalized values for the affected batches;
valid corrections in other batches remain. There is no invented identity-factor or raw-value
fallback. Invalid fits, insufficient support, and excessive correction factors have explicit reasons.
The configured minimum count and factor bound are mzmine safeguards, not universal values prescribed
by Dunn. A run with no successful corrections finishes with a warning.

## Diagnostics and persistence

Open **QC diagnostics** from the applied-methods pane. Diagnostics retain the method, settings,
solver, response scale, coverage, failure reasons, factor range, boundaries, and fitted QC curve.
Diagnostics also record the polynomial degree, neighborhood size, effective span, and the common
QC-median correction target. With QC-RLSC, the raw-response LOOCV score is the **model-selection
error**, not an independent estimate of prediction performance. Manual LOESS records that
cross-validation was not performed, with no score and zero validation predictions or failures.

Before/after QC RSD uses the same fitting observations and is the sample standard deviation divided
by the arithmetic mean. A low fitted RSD does not by itself establish acceptable scientific quality.
The full diagnostics are saved for inspection after project reload. The diagnostics serialization
format has its own version; scientific methods are identified by their names and settings, not a
shared development revision number.

## Subsequent processing

Active correction functions are stored once in the feature-list settings. The applied method keeps
only the processing evidence (messages and feature-QC diagnostics). Row-specific factors are keyed
by row ID: copying, filtering and **Reset row IDs** remap them to the new IDs and drop the factors
of removed rows, so a later row that reuses a removed ID stays unnormalized. Merging feature lists
with row-specific factors is rejected because their row IDs collide. Gap filling applies the
row-specific factors once to all new features at the end of the task; any other task that adds new
rows or features decides whether it can apply a meaningful correction.

The native API uses `inter_sample.method: "feature_qc"`. `smoothing` defaults to `"qc_rlsc"`.
Set it to `"manual_loess"` to supply `bandwidth`, `polynomial_degree` and `robustness_iterations`;
omitted manual settings default to 0.75, 1 and 2. Manual settings are rejected with QC-RLSC. For
example:

```json
{"inter_sample": {"method": "feature_qc", "smoothing": "manual_loess",
                  "bandwidth": 0.75, "polynomial_degree": 1, "robustness_iterations": 2}}
```

GUI and API runs use the same native implementation.

Numerical reference files are in the
[fixture directory](../mzmine-community/src/test/resources/io/github/mzmine/modules/dataprocessing/norm_intensity/feature_qc/).
Independent solver fixtures do not substitute for validation on representative analytical data.
