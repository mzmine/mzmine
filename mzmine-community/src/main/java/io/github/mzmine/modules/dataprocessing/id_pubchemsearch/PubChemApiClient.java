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

import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Objects;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for executing PubChem searches defined by a PubChemSearch configuration object.
 * Provides a static method to run the search synchronously.
 */
public final class PubChemApiClient { // Final class, no instances needed

  private static final Logger LOGGER = Logger.getLogger(PubChemApiClient.class.getName());
  private static final int CID_CHUNK_SIZE = 50; // Chunk size for property fetching

  // Private constructor to prevent instantiation
  private PubChemApiClient() {
  }

  /**
   * Executes the PubChem search synchronously based on the provided configuration. This method
   * performs all necessary steps: finding CIDs (with polling if needed), fetching properties
   * (chunked), and parsing the results.
   *
   * @param searchConfig The configuration object defining the search parameters.
   * @return A PubChemSearchResult containing the final status and an observable list of results.
   * The list will be empty if status is ERROR or if no compounds were found.
   */
  public static PubChemSearchResult executeSearch(PubChemSearch searchConfig) {
    Objects.requireNonNull(searchConfig, "Search configuration cannot be null");
    LOGGER.log(Level.INFO, "Executing PubChem search for: {0}",
        searchConfig.getSearchCriteriaDescription());

    // Obtain or create HttpClient instance
    HttpClient httpClient = searchConfig.customHttpClient().orElseGet(
        () -> HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(searchConfig.requestTimeout()) // Use timeout from config
            .build());

    try {
      // Step 1: Find CIDs
      List<String> cids = findCidsInternal(searchConfig, httpClient);
      if (cids.isEmpty()) {
        LOGGER.info("No CIDs found for the search criteria.");
        return new PubChemSearchResult(TaskStatus.FINISHED, FXCollections.observableArrayList());
      }
      LOGGER.log(Level.INFO, "Found {0} CIDs. Fetching properties...", cids.size());

      // Step 2: Fetch Properties (Chunked)
      String finalJson = performChunkedCidPropertySearchInternal(searchConfig, httpClient, cids);
      if (finalJson == null || finalJson.isEmpty() || finalJson.equals(
          createEmptyPropertyTableJson())) {
        LOGGER.info("Property fetch returned no data (or empty structure).");
        return new PubChemSearchResult(TaskStatus.FINISHED, FXCollections.observableArrayList());
      }

      // Step 3: Parse JSON into CompoundData objects
      LOGGER.info("Parsing final JSON result...");
      List<CompoundData> compoundList = parsePropertiesJson(finalJson);
      LOGGER.log(Level.INFO, "Successfully parsed {0} compounds.", compoundList.size());

      // Step 4: Create ObservableList and return FINISHED result
      ObservableList<CompoundData> observableResults = FXCollections.observableArrayList(
          compoundList);
      return new PubChemSearchResult(TaskStatus.FINISHED, observableResults);

    } catch (IOException | InterruptedException | PubChemApiException | JSONException e) {
      // Handle exceptions from any step (CID search, Property fetch, JSON parsing)
      PubChemApiException wrappedEx = handleExecutionException("Search execution", e);
      LOGGER.log(Level.SEVERE, "PubChem search failed: " + wrappedEx.getMessage(),
          wrappedEx.getCause());
      return new PubChemSearchResult(TaskStatus.ERROR,
          FXCollections.observableArrayList()); // Return ERROR status with empty list
    }
  }

  // ========================================================================
  // Internal Helper Methods (Static, moved from previous record)
  // These now take PubChemSearch config and HttpClient as parameters
  // ========================================================================

  private static List<String> findCidsInternal(PubChemSearch config, HttpClient httpClient)
      throws IOException, InterruptedException, PubChemApiException {
    LOGGER.fine(
        () -> "Internal: Initiating CID search for: " + config.getSearchCriteriaDescription());
    List<String> foundCids =
        config.isMassSearch() ? performMassCidSearchInternal(config, httpClient)
            : performFormulaCidSearchInternal(config, httpClient);
    LOGGER.fine(() -> "Internal: CID search successful. Found " + foundCids.size() + " CIDs.");
    return foundCids;
  }

  private static List<String> performFormulaCidSearchInternal(PubChemSearch config,
      HttpClient httpClient) throws IOException, InterruptedException, PubChemApiException {
    String theFormula = config.formula()
        .orElseThrow(() -> new IllegalStateException("Formula is missing"));
    String encodedFormula = URLEncoder.encode(theFormula, StandardCharsets.UTF_8);
    String initialUrl = String.format("%s/compound/formula/%s/cids/JSON", config.baseApiUrl(),
        encodedFormula);
    HttpRequest request = buildGetRequest(initialUrl, config.requestTimeout());
    String pollUrlTemplate = config.baseApiUrl() + "/compound/listkey/%s/cids/JSON";
    String jsonResponse = executeRequestInternal(config, httpClient, request, pollUrlTemplate);
    return parseCidResponseInternal(jsonResponse);
  }

  private static List<String> performMassCidSearchInternal(PubChemSearch config,
      HttpClient httpClient) throws IOException, InterruptedException, PubChemApiException {
    double theMinMass = config.minMass()
        .orElseThrow(() -> new IllegalStateException("Min Mass is missing"));
    double theMaxMass = config.maxMass()
        .orElseThrow(() -> new IllegalStateException("Max Mass is missing"));
    String lowerMassStr = String.format(Locale.ROOT, "%.4f", theMinMass);
    String upperMassStr = String.format(Locale.ROOT, "%.4f", theMaxMass);
    String initialUrl = String.format("%s/compound/monoisotopic_mass/range/%s/%s/cids/JSON",
        config.baseApiUrl(), lowerMassStr, upperMassStr);
    HttpRequest request = buildGetRequest(initialUrl, config.requestTimeout());
    String pollUrlTemplate = config.baseApiUrl() + "/compound/listkey/%s/cids/JSON";
    String jsonResponse = executeRequestInternal(config, httpClient, request, pollUrlTemplate);
    return parseCidResponseInternal(jsonResponse);
  }

  private static List<String> parseCidResponseInternal(String jsonResponse)
      throws PubChemApiException {
    // This parsing logic remains mostly the same as before
    try {
      JSONObject r = new JSONObject(jsonResponse);
      if (r.has("IdentifierList") && !r.isNull("IdentifierList")) {
        JSONObject i = r.getJSONObject("IdentifierList");
        if (i.has("CID") && !i.isNull("CID")) {
          JSONArray cA = i.getJSONArray("CID");
          List<String> cs = new ArrayList<>(cA.length());
            for (int j = 0; j < cA.length(); j++) {
                cs.add(String.valueOf(cA.opt(j)));
            }
          cs.removeIf(String::isEmpty);
          return cs;
        }
      }
    } catch (JSONException e) {/* handled below */}
      if (jsonResponse.equals("{\"IdentifierList\": {\"CID\": []}}")) {
          return List.of();
      }
    try {
      JSONObject r = new JSONObject(jsonResponse);
      if (r.has("Fault")) {
        String fM = extractFaultMessageInternal(jsonResponse);
        if (fM.contains("PUGREST.NotFound") || fM.contains("No records found")) {
          LOGGER.fine(
              () -> "CID search resulted in a 'Not Found' fault, treating as empty result. Fault: "
                  + fM);
          return List.of();
        }
        throw new PubChemApiException("PubChem API error during CID search: " + fM);
      }
    } catch (JSONException e) {/* handled below */}
    if (jsonResponse != null && jsonResponse.contains("PUGREST.NotFound")) {
      LOGGER.fine(
          "CID search resulted in a 'Not Found' message (non-JSON?), treating as empty result.");
      return List.of();
    }
    LOGGER.warning("Unrecognized successful CID response structure: " + jsonResponse);
    try {
      new JSONObject(jsonResponse);
    } catch (JSONException e) {
      throw new PubChemApiException(
          "Failed to parse final CIDs response from PubChem: " + jsonResponse, e);
    }
    return List.of();
  }

  private static String performChunkedCidPropertySearchInternal(PubChemSearch config,
      HttpClient httpClient, List<String> cidsToSearch)
      throws IOException, InterruptedException, PubChemApiException, JSONException {

    if (cidsToSearch == null || cidsToSearch.isEmpty()) {
      return createEmptyPropertyTableJson();
    }

    JSONArray allProperties = new JSONArray();
    int totalSize = cidsToSearch.size();
    int chunkCount = (int) Math.ceil((double) totalSize / CID_CHUNK_SIZE);
    LOGGER.fine(() -> "Internal: Starting chunked property fetch for " + totalSize + " CIDs in "
        + chunkCount + " chunks.");

    for (int i = 0; i < totalSize; i += CID_CHUNK_SIZE) {
      int fromIndex = i;
      int toIndex = Math.min(i + CID_CHUNK_SIZE, totalSize);
      List<String> chunk = cidsToSearch.subList(fromIndex, toIndex);
      int currentChunkNum = (i / CID_CHUNK_SIZE) + 1;

      LOGGER.log(Level.FINE,
          "Internal: Processing property fetch chunk {0} of {1} (CIDs {2} to {3})",
          new Object[]{currentChunkNum, chunkCount, fromIndex + 1, toIndex});

      String chunkJsonResult = performSinglePropertySearchInternal(config, httpClient, chunk);

      try {
        JSONObject chunkResponse = new JSONObject(chunkJsonResult);
        if (chunkResponse.has("PropertyTable") && !chunkResponse.isNull("PropertyTable")) {
          JSONObject propertyTable = chunkResponse.getJSONObject("PropertyTable");
          if (propertyTable.has("Properties") && !propertyTable.isNull("Properties")) {
            JSONArray propertiesArray = propertyTable.getJSONArray("Properties");
            for (int j = 0; j < propertiesArray.length(); j++) {
              allProperties.put(propertiesArray.getJSONObject(j));
            }
            LOGGER.log(Level.FINER, "Internal: Chunk {0}: Added {1} properties.",
                new Object[]{currentChunkNum, propertiesArray.length()});
          } else {
            LOGGER.log(Level.FINER,
                "Internal: Chunk {0}: No 'Properties' array found in PropertyTable.",
                currentChunkNum);
          }
        } else {
          if (chunkResponse.has("Fault")) {
            String faultMsg = extractFaultMessageInternal(chunkJsonResult);
            throw new PubChemApiException(
                "PubChem API error during property fetch for chunk " + currentChunkNum + ": "
                    + faultMsg);
          } else {
            LOGGER.log(Level.WARNING,
                "Internal: Chunk {0}: Response did not contain expected 'PropertyTable'. Body: {1}",
                new Object[]{currentChunkNum, chunkJsonResult});
            throw new PubChemApiException(
                "Unexpected response structure during property fetch for chunk " + currentChunkNum
                    + ": Missing PropertyTable");
          }
        }
      } catch (JSONException e) {
        LOGGER.log(Level.SEVERE,
            "Internal: Failed to parse JSON response for chunk " + currentChunkNum + ". Body: "
                + chunkJsonResult, e);
        throw new JSONException(
            "Failed to parse JSON response for chunk " + currentChunkNum + ": " + e.getMessage());
      }
    }

    JSONObject finalRoot = new JSONObject();
    JSONObject finalPropertyTable = new JSONObject();
    finalPropertyTable.put("Properties", allProperties);
    finalRoot.put("PropertyTable", finalPropertyTable);

    LOGGER.log(Level.FINE,
        "Internal: Finished processing all chunks. Total properties retrieved: {0}",
        allProperties.length());
    return finalRoot.toString();
  }

  private static String performSinglePropertySearchInternal(PubChemSearch config,
      HttpClient httpClient, List<String> cidsChunk)
      throws IOException, InterruptedException, PubChemApiException {
      if (cidsChunk == null || cidsChunk.isEmpty()) {
          return createEmptyPropertyTableJson();
      }

    String cidString = String.join(",", cidsChunk);
    String url = String.format("%s/compound/cid/property/%s/JSON", config.baseApiUrl(),
        config.requestedProperties());
    String postBody = "cid=" + URLEncoder.encode(cidString, StandardCharsets.UTF_8);

    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url))
        .timeout(config.requestTimeout())
        .header("Content-Type", "application/x-www-form-urlencoded")
        .header("Accept", "application/json").POST(HttpRequest.BodyPublishers.ofString(postBody))
        .build();
    return executeRequestInternal(config, httpClient, request, null); // No polling expected
  }

  private static HttpRequest buildGetRequest(String url, Duration timeout) {
    return HttpRequest.newBuilder().uri(URI.create(url)).timeout(timeout)
        .header("Accept", "application/json").GET().build();
  }

  private static String executeRequestInternal(PubChemSearch config, HttpClient httpClient,
      HttpRequest initialRequest, String pollUrlTemplate)
      throws IOException, InterruptedException, PubChemApiException {
    HttpResponse<String> response = sendRequestInternal(httpClient, initialRequest); // Pass client
    int statusCode = response.statusCode();
    String responseBody = response.body();
    if (statusCode == 200 && !isWaitingResponseInternal(responseBody)) {
      LOGGER.fine(() -> "Internal: Request to " + initialRequest.uri() + " successful (sync).");
      return responseBody;
    }
    if ((statusCode == 202 || (statusCode == 200 && isWaitingResponseInternal(responseBody)))
        && pollUrlTemplate != null) {
      String listKey = extractListKeyInternal(responseBody);
      if (listKey == null) {
        String msg = "PubChem indicated waiting, but no ListKey found. URI: " + initialRequest.uri()
            + ". Body: " + responseBody;
        LOGGER.warning(msg);
        throw new PubChemApiException(msg);
      }
      LOGGER.info("PubChem request queued. ListKey: " + listKey + ". Polling initiated...");
      return pollForResultInternal(config, httpClient, listKey,
          pollUrlTemplate); // Pass config and client
    }
    return handleNonSuccessResponseInternal(statusCode, responseBody, initialRequest.uri());
  }

  private static HttpResponse<String> sendRequestInternal(HttpClient httpClient,
      HttpRequest request) throws IOException, InterruptedException {
    LOGGER.log(Level.FINER, "Internal: Sending {0} request to {1}",
        new Object[]{request.method(), request.uri()});
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    LOGGER.log(Level.FINER, "Internal: Received status {0} from {1}",
        new Object[]{response.statusCode(), request.uri()});
    return response;
  }

  private static String handleNonSuccessResponseInternal(int statusCode, String responseBody,
      URI requestUri) throws PubChemApiException {
    // Logic remains the same, just ensure logging and exception creation are correct
    String uriStr = requestUri.toString();
    if (statusCode == 404) {
      LOGGER.fine(
          () -> "Internal: Received 404 Not Found for " + uriStr + ". Treating as empty result.");
        if (requestUri.getPath().contains("/cids/JSON") || requestUri.getPath()
            .contains("/listkey/")) {
            return "{\"IdentifierList\": {\"CID\": []}}";
        } else {
            return createEmptyPropertyTableJson();
        }
    } else if (statusCode == 400) {
      String faultMsg = extractFaultMessageInternal(responseBody);
      String errorMsg = String.format("PubChem API Bad Request (400) for URL [%s]. Response: %s",
          uriStr, faultMsg);
      LOGGER.warning(errorMsg);
      throw new PubChemApiException(errorMsg);
    } else if (statusCode == 503 || statusCode == 504) {
      String errorMsg = String.format(
          "PubChem service unavailable or timed out (%d) for URL [%s]. Try again later.",
          statusCode, uriStr);
      LOGGER.warning(errorMsg);
      throw new PubChemApiException(errorMsg);
    } else {
      String errorMsg = String.format(
          "PubChem API request failed with status code %d for URL [%s]. Response: %s", statusCode,
          uriStr, responseBody != null ? responseBody : "(No Body)");
      LOGGER.warning(errorMsg);
      throw new PubChemApiException(errorMsg);
    }
  }

  private static String pollForResultInternal(PubChemSearch config, HttpClient httpClient,
      String listKey, String pollUrlTemplate)
      throws IOException, InterruptedException, PubChemApiException {
    Instant pollDeadline = Instant.now().plus(config.maxPollTime());
    String pollUrl = String.format(pollUrlTemplate, listKey);

    while (Instant.now().isBefore(pollDeadline)) {
      long sleepMillis = config.pollInterval().toMillis();
      long remainingMillis = Duration.between(Instant.now(), pollDeadline).toMillis();
        if (remainingMillis <= 0) {
            break;
        }
      if (remainingMillis > sleepMillis) {
        LOGGER.log(Level.FINEST, "Internal: Polling ListKey {0}: Sleeping for {1} ms.",
            new Object[]{listKey, sleepMillis});
        TimeUnit.MILLISECONDS.sleep(sleepMillis);
      } else if (remainingMillis > 50) {
        LOGGER.log(Level.FINEST,
            "Internal: Polling ListKey {0}: Near deadline, sleeping for remaining {1} ms.",
            new Object[]{listKey, remainingMillis});
        TimeUnit.MILLISECONDS.sleep(remainingMillis);
          if (Instant.now().isAfter(pollDeadline)) {
              break;
          }
      } else {
        break;
      }

      HttpRequest pollRequest = buildGetRequest(pollUrl,
          config.requestTimeout()); // Use timeout from config
      LOGGER.log(Level.FINE, "Internal: Polling ListKey {0}: Requesting {1}",
          new Object[]{listKey, pollUrl});
      HttpResponse<String> pollResponse;
      try {
        pollResponse = sendRequestInternal(httpClient, pollRequest); // Pass client
      } catch (IOException | InterruptedException e) {
        PubChemApiException wrappedEx = handleExecutionException(
            "Polling request for ListKey " + listKey, e);
        LOGGER.log(Level.WARNING, "Polling request failed: " + wrappedEx.getMessage(),
            wrappedEx.getCause());
        throw wrappedEx;
      }

      int pollStatusCode = pollResponse.statusCode();
      String pollBody = pollResponse.body();
      if (pollStatusCode == 200) {
        if (isWaitingResponseInternal(pollBody)) {
          LOGGER.log(Level.FINE, "... still waiting (ListKey: {0})", listKey);
          continue;
        } else {
          LOGGER.info("Polling successful for ListKey: " + listKey);
          return pollBody;
        }
      } else {
        try {
          return handleNonSuccessResponseInternal(pollStatusCode, pollBody, pollRequest.uri());
        } catch (PubChemApiException e) {
          throw new PubChemApiException(
              "Polling failed for ListKey " + listKey + ": " + e.getMessage(), e.getCause());
        }
      }
    }
    String timeoutMsg =
        "Polling timed out after " + config.maxPollTime() + " for ListKey: " + listKey;
    LOGGER.warning(timeoutMsg);
    throw new PubChemApiException(timeoutMsg);
  }

  private static boolean isWaitingResponseInternal(String responseBody) {
      if (responseBody == null || !responseBody.trim().startsWith("{")) {
          return false;
      }
    try {
      return new JSONObject(responseBody).has("Waiting");
    } catch (JSONException e) {
      return false;
    }
  }

  private static String extractListKeyInternal(String responseBody) {
      if (responseBody == null) {
          return null;
      }
    try {
      JSONObject obj = new JSONObject(responseBody);
        if (obj.has("Waiting") && !obj.isNull("Waiting")) {
            return obj.getJSONObject("Waiting").optString("ListKey", null);
        }
    } catch (JSONException e) { /* Ignore */ }
    return null;
  }

  private static String extractFaultMessageInternal(String responseBody) { /* ... no changes ... */
      if (responseBody == null) {
          return "(No Body)";
      }
    try {
      JSONObject faultJson = new JSONObject(responseBody);
      if (faultJson.has("Fault")) {
        JSONObject fault = faultJson.getJSONObject("Fault");
        String details = fault.optString("Details", "");
        String message = fault.optString("Message", responseBody);
        return message + (details.isEmpty() ? "" : " Details: [" + details + "]");
      }
    } catch (JSONException e) { /* Ignore */ }
    return responseBody;
  }

  private static String createEmptyPropertyTableJson() {
    return "{\"PropertyTable\": {\"Properties\": []}}";
  }

  private static PubChemApiException handleExecutionException(String context, Exception e) {
    // Logic remains the same, just ensure logging and exception creation are correct
      if (e instanceof PubChemApiException) {
          return (PubChemApiException) e;
      } else if (e instanceof IOException) {
          return new PubChemApiException("Network error during " + context + ": " + e.getMessage(),
              e);
      } else if (e instanceof InterruptedException) {
          Thread.currentThread().interrupt();
          return new PubChemApiException("Operation interrupted during " + context, e);
      } else if (e instanceof JSONException) {
          LOGGER.log(Level.SEVERE, "JSON processing error during " + context, e);
          return new PubChemApiException("JSON processing error during " + context + ": " + e.getMessage(), e);
      } else {
          LOGGER.log(Level.SEVERE, "Unexpected error during " + context, e);
          return new PubChemApiException("Unexpected error during " + context + ": " + e.getMessage(),
              e);
      }
  }

  // --- JSON Parsing to CompoundData ---
  private static List<CompoundData> parsePropertiesJson(String jsonResult) throws JSONException {
    List<CompoundData> compounds = new ArrayList<>();
    JSONObject root = new JSONObject(jsonResult);

    if (!root.has("PropertyTable") || root.isNull("PropertyTable")) {
      LOGGER.warning("Final JSON result missing 'PropertyTable'.");
      return Collections.emptyList();
    }
    JSONObject propertyTable = root.getJSONObject("PropertyTable");

    if (!propertyTable.has("Properties") || propertyTable.isNull("Properties")) {
      LOGGER.warning("PropertyTable missing 'Properties' array.");
      return Collections.emptyList();
    }
    JSONArray propertiesArray = propertyTable.getJSONArray("Properties");

    for (int i = 0; i < propertiesArray.length(); i++) {
      JSONObject propObj = propertiesArray.getJSONObject(i);

      // Extract fields safely using opt methods
      int cid = propObj.optInt("CID",
          -1); // Assuming CID is always present, use -1 as sentinel if not
      if (cid == -1) {
        LOGGER.warning("Skipping property object with missing CID: " + propObj);
        continue; // Skip entries without a CID
      }

      Optional<String> formula = Optional.ofNullable(propObj.optString("MolecularFormula", null));
      Optional<String> smiles = Optional.ofNullable(propObj.optString("CanonicalSMILES", null));
      Optional<String> inchi = Optional.ofNullable(propObj.optString("InChI", null));
      Optional<String> inchiKey = Optional.ofNullable(propObj.optString("InChIKey", null));
      Optional<String> iupac = Optional.ofNullable(propObj.optString("IUPACName", null));
      Optional<String> title = Optional.ofNullable(propObj.optString("Title", null));

      // OptionalInt/Double require checking if the key exists before getting
      OptionalInt charge =
          propObj.has("Charge") ? OptionalInt.of(propObj.getInt("Charge")) : OptionalInt.empty();
      OptionalDouble mass =
          propObj.has("MonoisotopicMass") ? OptionalDouble.of(propObj.getDouble("MonoisotopicMass"))
              : OptionalDouble.empty();

      compounds.add(
          new CompoundData(cid, formula, smiles, inchi, inchiKey, iupac, title, charge, mass));
    }

    return compounds;
  }

  // Nested Exception Class (can remain here or be top-level)
  public static class PubChemApiException extends Exception {

    public PubChemApiException(String message) {
      super(message);
    }

    public PubChemApiException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
