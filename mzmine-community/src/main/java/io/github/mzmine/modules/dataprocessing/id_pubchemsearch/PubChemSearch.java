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

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger; // Keep logger if needed for config validation/warnings

/**
 * Immutable configuration record for a PubChem search request. This object holds the parameters but
 * does not perform the search itself. Use PubChemApiClient.executeSearch(searchConfig) to run the
 * search.
 */
public record PubChemSearch(
    // --- Search Parameters (Mutually Exclusive) ---
    Optional<String> formula, Optional<Double> minMass, Optional<Double> maxMass,

    // --- Configuration ---
    Optional<HttpClient> customHttpClient, // Optional custom client
    Duration requestTimeout, Duration pollInterval, Duration maxPollTime, String baseApiUrl,
    String requestedProperties) {

  // --- Logger for configuration related messages ---
  private static final Logger CONFIG_LOGGER = Logger.getLogger(PubChemSearch.class.getName());

  // --- Constants for Defaults ---
  private static final String DEFAULT_PUBCHEM_API_BASE_URL = "https://pubchem.ncbi.nlm.nih.gov/rest/pug";
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(30);
  private static final Duration DEFAULT_POLL_INTERVAL = Duration.ofSeconds(2);
  private static final Duration DEFAULT_MAX_POLL_TIME = Duration.ofMinutes(5);
  private static final String DEFAULT_COMPOUND_PROPERTIES = "MolecularFormula,CanonicalSMILES,InChI,InChIKey,IUPACName,Title,Charge,MonoisotopicMass";
  // No default HttpClient constant here, will be created on demand if not provided


  // --- Private Canonical Constructor for Validation ---
  public PubChemSearch {
    // Validation
    Objects.requireNonNull(requestTimeout, "Request timeout cannot be null");
    Objects.requireNonNull(pollInterval, "Poll interval cannot be null");
    Objects.requireNonNull(maxPollTime, "Max poll time cannot be null");
    Objects.requireNonNull(baseApiUrl, "Base API URL cannot be null");
    Objects.requireNonNull(requestedProperties, "Requested properties cannot be null");
    Objects.requireNonNull(customHttpClient, "customHttpClient Optional cannot be null");
    Objects.requireNonNull(formula, "formula Optional cannot be null");
    Objects.requireNonNull(minMass, "minMass Optional cannot be null");
    Objects.requireNonNull(maxMass, "maxMass Optional cannot be null");

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
    boolean hasFormula = formula.isPresent() && !formula.get().isBlank();
    boolean hasMass = minMass.isPresent() && maxMass.isPresent();

    if (hasFormula == hasMass) { // If both are present or both are absent
      throw new IllegalArgumentException(
          "Must specify either a non-blank formula OR both minMass and maxMass, but not both or neither.");
    }
    if (hasMass && minMass.get() > maxMass.get()) {
      throw new IllegalArgumentException("minMass cannot be greater than maxMass");
    }
    if (hasMass && (minMass.get() < 0 || maxMass.get() < 0)) {
      throw new IllegalArgumentException("Mass values cannot be negative");
    }
  }

  // --- Static Factory Methods (Starting Points) ---

  public static PubChemSearch byFormula(String formula) {
    // Basic validation here, canonical constructor does the rest
    if (formula == null || formula.isBlank()) {
      throw new IllegalArgumentException("Chemical formula cannot be null or blank");
    }
    return new PubChemSearch(Optional.of(formula), Optional.empty(), Optional.empty(),
        // Search Criteria
        Optional.empty(), // Default HttpClient
        DEFAULT_REQUEST_TIMEOUT, DEFAULT_POLL_INTERVAL, DEFAULT_MAX_POLL_TIME,
        DEFAULT_PUBCHEM_API_BASE_URL, DEFAULT_COMPOUND_PROPERTIES // Other Config Defaults
    );
  }

  public static PubChemSearch byMassRange(double minMass, double maxMass) {
    // Basic validation here, canonical constructor does the rest
    return new PubChemSearch(Optional.empty(), Optional.of(minMass), Optional.of(maxMass),
        // Search Criteria
        Optional.empty(), // Default HttpClient
        DEFAULT_REQUEST_TIMEOUT, DEFAULT_POLL_INTERVAL, DEFAULT_MAX_POLL_TIME,
        DEFAULT_PUBCHEM_API_BASE_URL, DEFAULT_COMPOUND_PROPERTIES // Other Config Defaults
    );
  }

  // --- Configuration "with" Methods (Return new instances) ---

  public PubChemSearch withHttpClient(HttpClient client) {
    Objects.requireNonNull(client, "HttpClient cannot be null");
    return new PubChemSearch(formula, minMass, maxMass, Optional.of(client), requestTimeout,
        pollInterval, maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withoutCustomHttpClient() {
    return new PubChemSearch(formula, minMass, maxMass, Optional.empty(), requestTimeout,
        pollInterval, maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withRequestTimeout(Duration timeout) {
    return new PubChemSearch(formula, minMass, maxMass, customHttpClient, timeout, pollInterval,
        maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withPollInterval(Duration interval) {
    return new PubChemSearch(formula, minMass, maxMass, customHttpClient, requestTimeout, interval,
        maxPollTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withMaxPollTime(Duration maxTime) {
    return new PubChemSearch(formula, minMass, maxMass, customHttpClient, requestTimeout,
        pollInterval, maxTime, baseApiUrl, requestedProperties);
  }

  public PubChemSearch withBaseApiUrl(String baseUrl) {
    return new PubChemSearch(formula, minMass, maxMass, customHttpClient, requestTimeout,
        pollInterval, maxPollTime, baseUrl, requestedProperties);
  }

  public PubChemSearch withRequestedProperties(String properties) {
    return new PubChemSearch(formula, minMass, maxMass, customHttpClient, requestTimeout,
        pollInterval, maxPollTime, baseApiUrl, properties);
  }

  // --- Convenience Getters ---
  public boolean isFormulaSearch() {
    return formula.isPresent();
  }

  public boolean isMassSearch() {
    return minMass.isPresent();
  } // Presence implies maxMass is also checked by constructor

  public String getSearchCriteriaDescription() {
    if (isFormulaSearch()) {
      return "Formula=" + formula.orElse("?");
    }
    if (isMassSearch()) {
      return String.format(Locale.ROOT, "MassRange=[%.4f, %.4f]", minMass.orElse(Double.NaN),
          maxMass.orElse(Double.NaN));
    }
    return "Unknown Criteria";
  }
}
