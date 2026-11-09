"""Regenerate AdvancedStudyStatisticsTest fixtures with Python 3.12, numpy 2.5.4,
scipy 1.16.2, and statsmodels 0.14.5. This is test-only independent reference code."""
import json
import numpy as np
from scipy.optimize import brentq
from scipy.special import digamma, polygamma
from statsmodels.regression.linear_model import OLS
from statsmodels.regression.mixed_linear_model import MixedLM


def mixed(values, time, subjects):
    design = np.column_stack((np.ones(len(values)), time))
    fit = MixedLM(np.asarray(values), design, groups=subjects).fit(reml=True, method="lbfgs", disp=False)
    return {"coefficients": fit.params[:2].tolist(), "residual_variance": fit.scale,
            "standard_errors": fit.bse[:2].tolist(), "confidence_intervals": fit.conf_int()[:2].tolist(),
            "p_values": fit.pvalues[:2].tolist(), "random_intercept_variance": float(fit.cov_re[0, 0])}


def moderation(values):
    design = np.column_stack((np.ones(len(values[0])), np.arange(len(values[0]))))
    fits = [OLS(y, design).fit() for y in values]
    variances = np.asarray([fit.mse_resid for fit in fits])
    dfs = np.asarray([fit.df_resid for fit in fits])
    adjusted_logs = np.log(variances) - (digamma(dfs / 2) - np.log(dfs / 2))
    excess = np.var(adjusted_logs, ddof=1) - np.mean(polygamma(1, dfs / 2))
    d0 = brentq(lambda df: polygamma(1, df / 2) - excess, 1e-6, 1e12)
    s0 = np.exp(np.mean(adjusted_logs) + digamma(d0 / 2) - np.log(d0 / 2))
    return {"residual_variances": variances.tolist(), "prior_df": d0, "prior_variance": s0,
            "posterior_variances": ((dfs * variances + d0 * s0) / (dfs + d0)).tolist()}


references = {
    "balanced": mixed([1, 2.2, 2, 2.8, 3, 4.1, 4, 4.9], [0, 1, 0, 1, 0, 1, 0, 1],
                      ["a", "a", "b", "b", "c", "c", "d", "d"]),
    "unbalanced": mixed([1, 2, 2.8, 3, 3.9, 5, 6.8], [0, 1, 2, 0, 1, 0, 2],
                        ["a", "a", "a", "b", "b", "c", "c"]),
    "moderation": moderation([[1, 2, 2, 4, 4, 7, 7, 10], [1, 1.5, 3, 3.2, 5, 7.1, 6.8, 9.4],
                                [2, 2.1, 3.9, 4.2, 5.8, 7.9, 8.4, 10.1],
                                [0, 2, 2.2, 4.8, 4.5, 7.5, 5.6, 11.2],
                                [3, 2, 5, 3, 8, 4, 12, 5], [1, 5, 0, 7, -1, 9, -2, 12]])}
print(json.dumps(references, indent=2))
