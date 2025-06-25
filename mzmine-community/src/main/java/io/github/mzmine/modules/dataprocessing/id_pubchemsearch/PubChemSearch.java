/*
 * Copyright (c) 2004-2025 The mzmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataprocessing.id_pubchemsearch;

import com.google.common.collect.Range;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger; // Keep logger if needed for config validation/warnings
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration record for a PubChem search request. This object holds the parameters but
 * does not perform the search itself. Use {@link PubChemApiClient#findCids(PubChemSearch)} or
 * {@link PubChemApiClient#findCidsAsync(PubChemSearch)} and
 * {@link PubChemApiClient#fetchPropertiesForChunk(PubChemSearch, List)} or
 * {@link PubChemApiClient#fetchPropertiesForChunkAsync(PubChemSearch, List)} to run the search.
 */
public record PubChemSearch(@Nullable String formula, @Nullable Double minMass,
                            @Nullable Double maxMass, HttpClient httpClient,
                            Duration requestTimeout, Duration pollInterval, Duration maxPollTime,
                            String baseApiUrl, String requestedProperties) {

  private static final String DEFAULT_PUBCHEM_API_BASE_URL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug";
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofMillis(200);
  private static final Duration DEFAULT_MAX_POLL_TIME = Duration.ofSeconds(30);
  private static final String DEFAULT_COMPOUND_PROPERTIES = "MolecularFormula,SMILES,InChI,InChIKey,IUPACName,Title,Charge,MonoisotopicMass";
  // No default HttpClient constant here, will be created on demand if not provided


  // --- Private Canonical Constructor for Validation ---
  public PubChemSearch {
    // Validation
    Objects.requireNonNull(requestTimeout, "Request timeout cannot be null");
    Objects.requireNonNull(pollInterval, "Poll interval cannot be null");
    Objects.requireNonNull(maxPollTime, "Max poll time cannot be null");
    Objects.requireNonNull(baseApiUrl, "Base API URL cannot be null");
    Objects.requireNonNull(requestedProperties, "Requested properties cannot be null");
    Objects.requireNonNull(httpClient, "customHttpClient Optional cannot be null");

    if (requestTimeout.isNegative() || requestTimeout.isZero()) {
      throw new IllegalArgumentException("Request timeout must be positive");
    }
    if (pollInterval.isNegative() || pollInterval.isZero()) {
      throw new IllegalArgumentException("Poll interval must be positive");
    }
    if (maxPollTime.isNegative() || maxPollTime.isZero()) {
      throw new IllegalArgumentException("Max poll time must be positive");
    }
    if (baseApiUrl.isBlank()) {
      throw new IllegalArgumentException("Base API URL cannot be blank");
    }
    if (requestedProperties.isBlank()) {
      throw new IllegalArgumentException("Requested properties cannot be blank");
    }

    // Check search criteria mutual exclusivity
    boolean hasFormula = formula != null && !formula.isBlank();
    boolean hasMass = minMass != null && maxMass != null;

    if (hasFormula == hasMass) { // If both are present or both are absent
      throw new IllegalArgumentException(
          "Must specify either a non-blank formula OR both minMass and maxMass, but not both or neither.");
    }
    if (hasMass && minMass > maxMass) {
      throw new IllegalArgumentException("minMass cannot be greater than maxMass");
    }
    if (hasMass && (minMass < 0 || maxMass < 0)) {
      throw new IllegalArgumentException("Mass values cannot be negative");
    }
  }

  // --- Static Factory Methods (Starting Points) ---
  public static PubChemSearch byFormula(String formula) {
    // Basic validation here, canonical constructor does the rest
    if (formula == null || formula.isBlank()) {
      throw new IllegalArgumentException("Chemical formula cannot be null or blank");
    }
    return new PubChemSearch(formula, null, null,
        // Search Criteria
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL).build(), DEFAULT_REQUEST_TIMEOUT,
        DEFAULT_POLL_INTERVAL, DEFAULT_MAX_POLL_TIME, DEFAULT_PUBCHEM_API_BASE_URL,
        DEFAULT_COMPOUND_PROPERTIES // Other Config Defaults
    );
  }

  public static PubChemSearch byMassRange(double minMass, double maxMass) {
    // Basic validation here, canonical constructor does the rest
    return new PubChemSearch(null, minMass, maxMass,
        // Search Criteria
        HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL).build(), DEFAULT_REQUEST_TIMEOUT,
        DEFAULT_POLL_INTERVAL, DEFAULT_MAX_POLL_TIME, DEFAULT_PUBCHEM_API_BASE_URL,
        DEFAULT_COMPOUND_PROPERTIES // Other Config Defaults
    );
  }

  public static PubChemSearch byMassRange(double mass, @NotNull final MZTolerance tolerance) {
    final Range<Double> range = tolerance.getToleranceRange(mass);
    return PubChemSearch.byMassRange(range.lowerEndpoint(), range.upperEndpoint());
  }

  // --- Configuration "with" Methods (Return new instances) ---

  public PubChemSearch withHttpClient(HttpClient client) {
    Objects.requireNonNull(client, "HttpClient cannot be null");
    return new PubChemSearch(formula, minMass, maxMass, client, requestTimeout, pollInterval,
        maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withRequestTimeout(Duration timeout) {
    return new PubChemSearch(formula, minMass, maxMass, httpClient, timeout, pollInterval,
        maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withPollInterval(Duration interval) {
    return new PubChemSearch(formula, minMass, maxMass, httpClient, requestTimeout, interval,
        maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withMaxPollTime(Duration maxTime) {
    return new PubChemSearch(formula, minMass, maxMass, httpClient, requestTimeout, pollInterval,
        maxTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withBaseApiUrl(String baseUrl) {
    return new PubChemSearch(formula, minMass, maxMass, httpClient, requestTimeout, pollInterval,
        maxPollTime, baseUrl, requestedProperties);
  }

  public PubChemSearch withRequestedProperties(String properties) {
    return new PubChemSearch(formula, minMass, maxMass, httpClient, requestTimeout, pollInterval,
        maxPollTime, baseApiUrl, properties);
  }

  // --- Convenience Getters ---
  public boolean isFormulaSearch() {
    return formula != null;
  }

  public boolean isMassSearch() {
    return minMass != null && maxMass != null;
  }

  public String getSearchCriteriaDescription() {
    if (isFormulaSearch()) {
      return "Formula=" + formula;
    }
    if (isMassSearch()) {
      return String.format(Locale.ROOT, "MassRange=[%.4f, %.4f]", minMass, maxMass);
    }
    return "Unknown Criteria";
  }
}
