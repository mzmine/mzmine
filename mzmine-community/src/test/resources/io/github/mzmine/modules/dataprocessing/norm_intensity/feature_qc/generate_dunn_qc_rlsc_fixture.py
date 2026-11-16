#!/usr/bin/env python3
"""Independent standard-library QC-RLSC fixture generator.

This is deliberately not a Java wrapper and does not claim R/qcrlscR equivalence. It implements
the Dunn-style integer-neighborhood LOOCV, tricube local polynomial fits, bisquare reweighting,
and a natural cubic spline. NumPy/SciPy were unavailable in the development environment, so the
small QR and spline routines below make the fixture reproducible without a runtime dependency.
"""

from math import sqrt


QC_X = [float(value) for value in range(9)]
QC_Y = [14.0, 15.6, 18.4, 18.0, 20.2, 23.9, 24.1, 28.5, 30.4]
PREDICTION_X = [-1.0, 0.0, 0.5, 2.5, 4.5, 7.5, 8.0, 9.0]
ROBUSTNESS_ITERATIONS = 2
EPSILON = 1e-14


def median(values):
    values = sorted(values)
    middle = len(values) // 2
    return (values[middle - 1] + values[middle]) / 2 if len(values) % 2 == 0 else values[middle]


def solve_qr(matrix, response):
    """Modified Gram-Schmidt QR for this small, independently generated fixture."""
    rows, columns = len(matrix), len(matrix[0])
    q = [[0.0] * columns for _ in range(rows)]
    r = [[0.0] * columns for _ in range(columns)]
    for column in range(columns):
        v = [matrix[row][column] for row in range(rows)]
        for previous in range(column):
            r[previous][column] = sum(q[row][previous] * v[row] for row in range(rows))
            for row in range(rows):
                v[row] -= r[previous][column] * q[row][previous]
        r[column][column] = sqrt(sum(value * value for value in v))
        if r[column][column] <= 1e-12:
            raise ValueError("singular local fit")
        for row in range(rows):
            q[row][column] = v[row] / r[column][column]
    transformed = [sum(q[row][column] * response[row] for row in range(rows))
                   for column in range(columns)]
    result = [0.0] * columns
    for row in range(columns - 1, -1, -1):
        result[row] = (transformed[row] - sum(r[row][column] * result[column]
                                               for column in range(row + 1, columns))) / r[row][row]
    return result


def neighbors(x, query, count, excluded):
    return sorted((index for index in range(len(x)) if index != excluded),
                  key=lambda index: (abs(x[index] - query), index))[:count]


def local_prediction(x, y, query, degree, count, excluded, robust):
    nearby = neighbors(x, query, count, excluded)
    maximum = max(abs(x[index] - query) for index in nearby)
    if maximum <= EPSILON:
        raise ValueError("zero neighborhood")
    matrix, response, positive = [], [], 0
    for index in nearby:
        ratio = abs(x[index] - query) / maximum
        tricube = 0.0 if ratio >= 1.0 else (1.0 - ratio ** 3) ** 3
        weight = tricube * robust[index]
        positive += weight > EPSILON
        normalized = (x[index] - query) / maximum
        root = sqrt(weight)
        matrix.append([root * normalized ** column for column in range(degree + 1)])
        response.append(root * y[index])
    if positive < degree + 1:
        raise ValueError("insufficient positive weights")
    return solve_qr(matrix, response)[0]


def robust_weights(x, y, degree, count, excluded):
    weights = [0.0 if index == excluded else 1.0 for index in range(len(x))]
    response_scale = median([abs(y[index]) for index in range(len(y)) if index != excluded])
    for _ in range(ROBUSTNESS_ITERATIONS):
        fitted = [None if index == excluded else local_prediction(x, y, x[index], degree, count,
                                                                    excluded, weights)
                  for index in range(len(x))]
        residuals = [abs(y[index] - fitted[index]) for index in range(len(x))
                     if index != excluded]
        center = median(residuals)
        if center <= EPSILON * max(1.0, response_scale):
            break
        scale = 6.0 * center
        for index in range(len(x)):
            if index != excluded:
                u = abs(y[index] - fitted[index]) / scale
                weights[index] = 0.0 if u >= 1.0 else (1.0 - u * u) ** 2
    return weights


def loocv(x, y, degree, count):
    errors = []
    for excluded in range(len(x)):
        weights = robust_weights(x, y, degree, count, excluded)
        predicted = local_prediction(x, y, x[excluded], degree, count, excluded, weights)
        errors.append((y[excluded] - predicted) ** 2)
    return sum(errors) / len(errors)


def natural_spline(x, values):
    n = len(x)
    widths = [x[index + 1] - x[index] for index in range(n - 1)]
    diagonal = [0.0] * n
    lower = [0.0] * n
    upper = [0.0] * n
    rhs = [0.0] * n
    diagonal[0] = diagonal[-1] = 1.0
    for index in range(1, n - 1):
        lower[index] = widths[index - 1]
        diagonal[index] = 2.0 * (widths[index - 1] + widths[index])
        upper[index] = widths[index]
        rhs[index] = 6.0 * ((values[index + 1] - values[index]) / widths[index]
                            - (values[index] - values[index - 1]) / widths[index - 1])
    for index in range(1, n):
        multiplier = lower[index] / diagonal[index - 1]
        diagonal[index] -= multiplier * upper[index - 1]
        rhs[index] -= multiplier * rhs[index - 1]
    second = [0.0] * n
    second[-1] = rhs[-1] / diagonal[-1]
    for index in range(n - 2, -1, -1):
        second[index] = (rhs[index] - upper[index] * second[index + 1]) / diagonal[index]

    def predict(query):
        query = min(max(query, x[0]), x[-1])
        index = next((i for i in range(n - 1) if query <= x[i + 1]), n - 2)
        h = widths[index]
        left, right = (x[index + 1] - query) / h, (query - x[index]) / h
        return (left * values[index] + right * values[index + 1]
                + ((left ** 3 - left) * second[index] + (right ** 3 - right)
                   * second[index + 1]) * h * h / 6.0)
    return predict


def main():
    raw_scale = median(QC_Y)
    normalized_y = [value / raw_scale for value in QC_Y]
    mse_tolerance = 4096.0 * (2.220446049250313e-16 ** 2)
    candidates = []
    for degree in (1, 2):
        for count in range(degree + 2, len(QC_X) + 1):
            try:
                candidates.append((loocv(QC_X, normalized_y, degree, count), degree, count))
            except ValueError:
                pass
    lowest_mse = min(score for score, _, _ in candidates)
    tied = [candidate for candidate in candidates
            if candidate[0] <= lowest_mse + max(mse_tolerance,
                                                64.0 * 2.220446049250313e-16
                                                * max(candidate[0], lowest_mse))]
    normalized_mse, degree, count = min(tied, key=lambda item: (-item[2], item[1]))
    mse = normalized_mse * raw_scale * raw_scale
    weights = robust_weights(QC_X, normalized_y, degree, count, -1)
    fitted = [raw_scale * local_prediction(QC_X, normalized_y, query, degree, count, -1, weights)
              for query in QC_X]
    predict = natural_spline(QC_X, fitted)
    target = sum(QC_Y) / len(QC_Y)
    print("# independent Python QC-RLSC fixture; "
          f"degree={degree};neighborhood={count};mse={mse:.17g};target={target:.17g}")
    print("coordinate,prediction,factor")
    for coordinate in PREDICTION_X:
        predicted = predict(coordinate)
        print(f"{coordinate:.17g},{predicted:.17g},{target / predicted:.17g}")


if __name__ == "__main__":
    main()
