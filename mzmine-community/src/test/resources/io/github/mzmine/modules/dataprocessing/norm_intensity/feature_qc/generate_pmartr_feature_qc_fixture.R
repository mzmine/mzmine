# Independent R reference for the scale and availability rules in pinned pmartR/qcrlsc.
# This executes stats::loess, not the defective upstream normalize_qcrlsc function.
# Prerequisite: base R with stats. Rscript generate_pmartr_feature_qc_fixture.R <output-dir>
# May also be sourced with output_dir set (for webR).
if (!exists("output_dir")) {
  args <- commandArgs(trailingOnly = TRUE)
  if (length(args) != 1L) stop("Supply one output directory")
  output_dir <- args[[1]]
}
dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)
qcrlsc_commit <- "5b5ee02657c72ba0727fb3ef2a9a001d057add45"
source_sha256 <- "712eb6bcff7d8c129115505f579ef315c2068688f82d5262a4b53d20eec8b6bc"
make_case <- function(case, batch, time, qc, input, span, target = NULL) {
  global_target <- if (is.null(target)) median(log(input[input > 0 & is.finite(input)])) else target
  fit <- stats::loess(y ~ x, data = data.frame(x = time[qc], y = log(input[qc])), span = span,
                      degree = 1, family = "gaussian")
  fitted <- as.numeric(stats::predict(fit, data.frame(x = time)))
  data.frame(case, batch, time, is_qc = qc, input, global_log_target = global_target,
             r_log_fit = fitted, r_factor = exp(global_target - fitted), span)
}
time <- 0:10
qc <- time %% 2 == 0
curves <- rbind(
  make_case("affine_log", "A", time, qc, exp(4 + .08 * time), 1),
  make_case("nonlinear_log", "A", time, qc, exp(4 + .035 * time + .012 * time^2), .75))
write.csv(curves, file.path(output_dir, "feature_qc_reference.csv"), row.names = FALSE, quote = FALSE)

global_target <- median(log(c(rep(10, 9), rep(20, 9))))
flat <- rbind(
  make_case("flat", "A", 0:8, (0:8) %% 2 == 0, rep(10, 9), 1, global_target),
  make_case("flat", "B", 0:8, (0:8) %% 2 == 0, rep(20, 9), 1, global_target))
flat <- flat[c(1, 10), c("batch", "input", "global_log_target", "r_log_fit", "r_factor")]
write.csv(flat, file.path(output_dir, "multi_batch_reference.csv"), row.names = FALSE, quote = FALSE)

missing <- data.frame(case = c("global_equality", "global_above", "batch_equality", "batch_below"),
                      total_A = c(5, 5, 10, 11), usable_A = c(5, 5, 5, 6),
                      total_B = c(5, 6, 10, 5), usable_B = c(0, 0, 10, 5))
missing$global_eligible <- (1 - (missing$usable_A + missing$usable_B) /
                                (missing$total_A + missing$total_B)) <= .5
missing$batch_A_eligible <- (1 - missing$usable_A / missing$total_A) < .5
write.csv(missing, file.path(output_dir, "missingness_semantics.csv"), row.names = FALSE, quote = FALSE)

writeLines(c(
  paste0("R.version=", R.version.string),
  paste0("stats.version=", packageVersion("stats")),
  "log_base=natural", "degree=1", "family=gaussian", "robustness_iterations=0",
  "prediction_surface=interpolate (R default)",
  paste0("qcrlsc.commit=", qcrlsc_commit), paste0("qcrlsc.normalize_sha256=", source_sha256),
  "qcrlsc_source_defect=undefined edata in biomolecules_i assignment; e_data exists",
  "qcrlsc_patch_applied=false", "unmodified_qcrlsc_executed=false",
  "reference_kind=independent stats::loess with pinned qcrlsc scale and eligibility formulas"
), file.path(output_dir, "fixture_metadata.properties"))
