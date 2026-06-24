package io.github.mzmine.modules.dataprocessing.id_pubchemsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import com.google.common.collect.Lists;
import io.github.mzmine.javafx.concurrent.threading.FxThread;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.stream.Collectors;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.NotNull;
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
import java.util.Objects;
import java.util.concurrent.*; // Import concurrent classes
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client for interacting with the PubChem PUG REST API. Supports both synchronous and asynchronous
 * operations. Allows fetching properties in manageable chunks.
 */
public class PubChemApiClient implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PubChemApiClient.class.getName());
  public static final int DEFAULT_CID_CHUNK_SIZE = 50; // Made public for clarity

  private final ExecutorService executorService; // For asynchronous operations
  private final boolean shutdownExecutorOnClose; // Whether this instance owns the executor
  private final ObjectMapper jsonMapper; // Instance of ObjectMapper

  /**
   * Creates a PubChemApiClient with a default HttpClient and a default cached thread pool
   * ExecutorService. The default executor will be shut down when close() is called.
   */
  public PubChemApiClient() {
    this(Executors.newCachedThreadPool(), // Default executor
        true); // Owns the default executor
  }

  /**
   * Creates a PubChemApiClient with a provided HttpClient and ExecutorService.
   *
   * @param executorService         The ExecutorService to use for asynchronous tasks.
   * @param shutdownExecutorOnClose If true, the provided executorService will be shut down when
   *                                close() is called. Set to false if the executor is managed
   *                                externally.
   */
  public PubChemApiClient(ExecutorService executorService, boolean shutdownExecutorOnClose) {
    this.executorService = Objects.requireNonNull(executorService,
        "ExecutorService cannot be null");
    this.shutdownExecutorOnClose = shutdownExecutorOnClose;
    this.jsonMapper = createObjectMapper(); // Create instance mapper
  }

  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new Jdk8Module()); // Still needed for Optionals
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    return mapper;
  }

  // --- Public API Methods ---

  /**
   * Asynchronously finds PubChem Compound IDs (CIDs) based on the search configuration. Handles
   * polling for asynchronous results from PubChem if necessary.
   *
   * @param searchConfig The search configuration.
   * @return A CompletableFuture that will complete with a List of CIDs (empty if none found), or
   * complete exceptionally if an error occurs.
   */
  public CompletableFuture<List<String>> findCidsAsync(PubChemSearch searchConfig) {
    Objects.requireNonNull(searchConfig, "Search configuration cannot be null");
    LOGGER.log(Level.FINE, "Queueing async CID search for: {0}",
        searchConfig.getSearchCriteriaDescription());

    // Run the potentially blocking find operation in the executor
    return CompletableFuture.supplyAsync(() -> {
      try {
        // Call the internal synchronous logic within the async task
        return findCidsInternal(searchConfig);
      } catch (IOException | InterruptedException | PubChemApiException | JSONException e) {
        // Wrap checked exceptions in a runtime exception to fail the future
        throw new CompletionException(handleExecutionException("Async CID search", e));
      }
    }, executorService);
  }

  /**
   * Asynchronously fetches and parses compound properties for a given *single chunk* of CIDs.
   *
   * @param searchConfig The search configuration (used for API URL, properties, timeout).
   * @param cidsChunk    A list of CIDs for which to fetch properties (should be <= reasonable chunk
   *                     size, e.g., 50).
   * @return A CompletableFuture that will complete with a List of parsed CompoundData objects for
   * the chunk, or complete exceptionally if an error occurs during fetch or parsing.
   */
  public CompletableFuture<List<CompoundData>> fetchPropertiesForChunkAsync(
      PubChemSearch searchConfig, List<String> cidsChunk) {
    Objects.requireNonNull(searchConfig, "Search configuration cannot be null");
    Objects.requireNonNull(cidsChunk, "CID chunk list cannot be null");
    if (cidsChunk.isEmpty()) {
      LOGGER.fine(
          "fetchPropertiesForChunkAsync called with empty CID list, returning empty result immediately.");
      return CompletableFuture.completedFuture(Collections.emptyList());
    }
    // Basic check, could add warning if size is large
    // if (cidsChunk.size() > DEFAULT_CID_CHUNK_SIZE) { ... }

    LOGGER.log(Level.FINE, "Queueing async property fetch for chunk of {0} CIDs.",
        cidsChunk.size());

    return CompletableFuture.supplyAsync(() -> {
      try {
        // 1. Fetch raw JSON for the chunk
        String chunkJsonResult = performSinglePropertySearchInternal(searchConfig, cidsChunk);
        // 2. Parse the JSON using Jackson
        return parsePropertiesJsonWithJackson(chunkJsonResult);
      } catch (IOException | InterruptedException | PubChemApiException e) {
        // Wrap checked exceptions
        throw new CompletionException(
            handleExecutionException("Async property fetch for chunk", e));
      }
    }, executorService);
  }

  /**
   * Synchronously finds PubChem Compound IDs (CIDs). Use findCidsAsync for non-blocking
   * operations.
   *
   * @param searchConfig The search configuration.
   * @return A List of CIDs (empty if none found).
   * @throws PubChemApiException  If an API error occurs.
   * @throws IOException          If a network error occurs.
   * @throws InterruptedException If the thread is interrupted (e.g., during polling sleep).
   */
  public List<String> findCids(@NotNull PubChemSearch searchConfig)
      throws PubChemApiException, IOException, InterruptedException {
    LOGGER.log(Level.INFO, "Executing synchronous CID search for: {0}",
        searchConfig.getSearchCriteriaDescription());
    try {
      return findCidsInternal(searchConfig);
    } catch (JSONException e) {
      // Wrap org.json exception from internal parsing
      throw handleExecutionException("Sync CID search parsing", e);
    }
  }

  /**
   * Synchronously fetches and parses properties for a single chunk of CIDs. Use
   * fetchPropertiesForChunkAsync for non-blocking operations.
   *
   * @param searchConfig The search configuration.
   * @param cidsChunk    The list of CIDs for this chunk.
   * @return A List of parsed CompoundData objects.
   * @throws PubChemApiException     If an API error occurs.
   * @throws IOException             If a network error occurs.
   * @throws InterruptedException    If the thread is interrupted.
   * @throws JsonProcessingException If Jackson parsing fails.
   */
  public List<CompoundData> fetchPropertiesForChunk(PubChemSearch searchConfig,
      List<String> cidsChunk)
      throws PubChemApiException, IOException, InterruptedException, JsonProcessingException {
    Objects.requireNonNull(searchConfig, "Search configuration cannot be null");
    Objects.requireNonNull(cidsChunk, "CID chunk list cannot be null");
    if (cidsChunk.isEmpty()) {
      return Collections.emptyList();
    }
    LOGGER.log(Level.INFO, "Executing synchronous property fetch for chunk of {0} CIDs.",
        cidsChunk.size());
    String chunkJsonResult = performSinglePropertySearchInternal(searchConfig, cidsChunk);
    return parsePropertiesJsonWithJackson(chunkJsonResult);
  }

  /**
   * Shuts down the internally managed ExecutorService if applicable. Call this when the client is
   * no longer needed to release resources, but only if you used the default constructor or set
   * shutdownExecutorOnClose to true.
   */
  public void close() {
    if (shutdownExecutorOnClose) {
      executorService.shutdown();
      try {
        if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
          executorService.shutdownNow();
        }
      } catch (InterruptedException e) {
        executorService.shutdownNow();
        Thread.currentThread().interrupt();
        LOGGER.log(Level.SEVERE, "Interrupted while waiting for ExecutorService termination.", e);
      }
    }
  }

  public static PubChemSearchResult runAsync(PubChemSearch searchConfig, ExecutorService executor) {
    // Create client, passing the executor, indicating it's managed externally
    final PubChemApiClient client = new PubChemApiClient(executor, false);

    // 2. Start CID search asynchronously
    final CompletableFuture<List<String>> cidsFuture = client.findCidsAsync(searchConfig);

    final ObservableList<CompoundData> resultsList = FXCollections.observableArrayList();
    final Property<TaskStatus> status = new SimpleObjectProperty<>(TaskStatus.PROCESSING);
    final PubChemSearchResult result = new PubChemSearchResult(status, resultsList);

    // 3. Chain operations: Once CIDs are found, fetch properties for each chunk
    CompletableFuture<List<CompoundData>> allCompoundsFuture = cidsFuture.thenComposeAsync(cids -> {
      if (cids.isEmpty()) {
        LOGGER.info("No CIDs found, skipping property fetch.");
        client.close();
        return CompletableFuture.completedFuture(Collections.emptyList());
      }
      LOGGER.info("Found " + cids.size() + " CIDs. Fetching properties in chunks...");

      // Create a list of futures, one for each chunk fetch
      final List<CompletableFuture<List<CompoundData>>> chunkFutures = new ArrayList<>();
      Lists.partition(cids, PubChemApiClient.DEFAULT_CID_CHUNK_SIZE).forEach(chunk -> {
        // when the properties are found, already add them to the results
        final CompletableFuture<List<CompoundData>> chunkAddResultsToList = client.fetchPropertiesForChunkAsync(
            searchConfig, chunk).whenComplete((compounds, throwable) -> {
          if (throwable != null) {
            // Handle errors from CID search or property fetch/parse
            LOGGER.log(Level.SEVERE,
                "Asynchronous search failed while fetching compound properties!", throwable);
            status.setValue(TaskStatus.ERROR);
            client.close();
            return;
          }
          // in case the list us used in gui
          FxThread.runLater(() -> resultsList.addAll(compounds));
        });
        chunkFutures.add(chunkAddResultsToList);
      });

      // Combine the results from all chunk futures
      return combineChunkResults(chunkFutures);
    }, executor); // Use the executor for composing steps

    // 4. Handle the final result (or errors)
    allCompoundsFuture.whenCompleteAsync((compoundList, throwable) -> {
      if (throwable != null) {
        // Handle errors from CID search or property fetch/parse
        LOGGER.log(Level.SEVERE, "Asynchronous search failed!", throwable);
        status.setValue(TaskStatus.ERROR);
        client.close();
        return;
      }

      // Process the final list of compounds
      LOGGER.info("Asynchronous search completed successfully. Found " + compoundList.size()
          + " compounds.");

      status.setValue(TaskStatus.FINISHED);
      client.close();
      LOGGER.info("Demo finished.");
    }, executor); // Use executor for the final completion stage too

    LOGGER.info("... Main thread continues while async search runs ...");

    return result;
  }

  private List<String> findCidsInternal(PubChemSearch config)
      throws IOException, InterruptedException, PubChemApiException, JSONException {
    LOGGER.fine(() -> "Initiating CID search for: " + config.getSearchCriteriaDescription());
    List<String> foundCids = config.isMassSearch() ? performMassCidSearchInternal(config)
        : performFormulaCidSearchInternal(config);
    LOGGER.fine(() -> "CID search successful. Found " + foundCids.size() + " CIDs.");
    return foundCids;
  }

  private List<String> performFormulaCidSearchInternal(PubChemSearch config)
      throws IOException, InterruptedException, PubChemApiException, JSONException {
    if(!config.isFormulaSearch()) {
      throw new IllegalStateException("Search formula is not set. %s".formatted(config.toString()));
    }
    String theFormula = config.formula();
    String encodedFormula = URLEncoder.encode(theFormula, StandardCharsets.UTF_8);
    String initialUrl = String.format("%s/compound/formula/%s/cids/JSON", config.baseApiUrl(),
        encodedFormula);
    HttpRequest request = buildGetRequest(initialUrl, config.requestTimeout());
    String pollUrlTemplate = config.baseApiUrl() + "/compound/listkey/%s/cids/JSON";
    // Pass config for polling parameters
    String jsonResponse = executeRequestInternal(config, request, pollUrlTemplate);
    return parseCidResponseInternal(jsonResponse);
  }

  private List<String> performMassCidSearchInternal(PubChemSearch config)
      throws IOException, InterruptedException, PubChemApiException, JSONException {
    if (!config.isMassSearch()) {
      throw new IllegalStateException("Search masses are not set. %s".formatted(config.toString()));
    }
    double theMinMass = config.minMass();
    double theMaxMass = config.maxMass();
    String lowerMassStr = String.format(Locale.ROOT, "%.4f", theMinMass);
    String upperMassStr = String.format(Locale.ROOT, "%.4f", theMaxMass);
    String initialUrl = String.format("%s/compound/monoisotopic_mass/range/%s/%s/cids/JSON",
        config.baseApiUrl(), lowerMassStr, upperMassStr);
    HttpRequest request = buildGetRequest(initialUrl, config.requestTimeout());
    String pollUrlTemplate = config.baseApiUrl() + "/compound/listkey/%s/cids/JSON";
    // Pass config for polling parameters
    String jsonResponse = executeRequestInternal(config, request, pollUrlTemplate);
    return parseCidResponseInternal(jsonResponse);
  }

  private String performSinglePropertySearchInternal(PubChemSearch config, List<String> cidsChunk)
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
    // Pass config for potential polling (though unlikely for POST)
    return executeRequestInternal(config, request, null);
  }

  private HttpRequest buildGetRequest(String url, Duration timeout) {
    return HttpRequest.newBuilder().uri(URI.create(url)).timeout(timeout)
        .header("Accept", "application/json").GET().build();
  }

  private String executeRequestInternal(PubChemSearch config, HttpRequest initialRequest,
      String pollUrlTemplate)
      throws IOException, InterruptedException, PubChemApiException, JSONException {
    // Uses instance httpClient
    HttpResponse<String> response = sendRequestInternal(initialRequest, config.httpClient());
    int statusCode = response.statusCode();
    String responseBody = response.body();
    if (statusCode == 200 && !isWaitingResponseInternal(responseBody)) {
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
      // Pass config for polling parameters
      return pollForResultInternal(config, listKey, pollUrlTemplate);
    }
    return handleNonSuccessResponseInternal(statusCode, responseBody, initialRequest.uri());
  }

  private HttpResponse<String> sendRequestInternal(HttpRequest request, HttpClient httpClient)
      throws IOException, InterruptedException {
    LOGGER.log(Level.FINER, "Sending {0} request to {1}",
        new Object[]{request.method(), request.uri()});
    // Uses instance httpClient
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    LOGGER.log(Level.FINER, "Received status {0} from {1}",
        new Object[]{response.statusCode(), request.uri()});
    return response;
  }

  private String handleNonSuccessResponseInternal(int statusCode, String responseBody,
      URI requestUri) throws PubChemApiException {
    // Logic is the same, uses static logger
    String uriStr = requestUri.toString();
    if (statusCode == 404) {
      LOGGER.fine(() -> "Received 404 Not Found for " + uriStr + ". Treating as empty result.");
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

  // Polling now uses config for timings and instance client
  private String pollForResultInternal(PubChemSearch config, String listKey, String pollUrlTemplate)
      throws IOException, InterruptedException, PubChemApiException, JSONException {
    Instant pollDeadline = Instant.now().plus(config.maxPollTime());
    String pollUrl = String.format(pollUrlTemplate, listKey);

    while (Instant.now().isBefore(pollDeadline)) {
      // Use config for poll interval
      long sleepMillis = config.pollInterval().toMillis();
      long remainingMillis = Duration.between(Instant.now(), pollDeadline).toMillis();
      if (remainingMillis <= 0) {
        break;
      }
      if (remainingMillis > sleepMillis) {
        LOGGER.log(Level.FINEST, "Polling ListKey {0}: Sleeping for {1} ms.",
            new Object[]{listKey, sleepMillis});
        TimeUnit.MILLISECONDS.sleep(sleepMillis);
      } else if (remainingMillis > 50) {
        LOGGER.log(Level.FINEST,
            "Polling ListKey {0}: Near deadline, sleeping for remaining {1} ms.",
            new Object[]{listKey, remainingMillis});
        TimeUnit.MILLISECONDS.sleep(remainingMillis);
        if (Instant.now().isAfter(pollDeadline)) {
          break;
        }
      } else {
        break;
      }

      // Use config for request timeout
      HttpRequest pollRequest = buildGetRequest(pollUrl, config.requestTimeout());
      LOGGER.log(Level.FINE, "Polling ListKey {0}: Requesting {1}", new Object[]{listKey, pollUrl});
      HttpResponse<String> pollResponse;
      try {
        // Uses instance client via sendRequestInternal
        pollResponse = sendRequestInternal(pollRequest, config.httpClient());
      } catch (IOException | InterruptedException e) {
        PubChemApiException wrappedEx = handleExecutionException(
            "Polling request for ListKey " + listKey, e);
        LOGGER.log(Level.WARNING, "Polling request failed: " + wrappedEx.getMessage(),
            wrappedEx.getCause());
        throw wrappedEx;
      }

      int pollStatusCode = pollResponse.statusCode();
      String pollBody = pollResponse.body();
      // 200 is finished, 202 is waiting
      if (pollStatusCode == 202 || pollStatusCode == 200) {
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
    // Use config for max poll time in message
    String timeoutMsg =
        "Polling timed out after " + config.maxPollTime() + " for ListKey: " + listKey;
    LOGGER.warning(timeoutMsg);
    throw new PubChemApiException(timeoutMsg);
  }

  // --- Parsing and Utility Methods (Mostly static or use instance mapper) ---

  // Still uses org.json for structure check
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

  // Still uses org.json
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

  // Still uses org.json
  private static String extractFaultMessageInternal(String responseBody) {
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

  private static List<String> parseCidResponseInternal(String jsonResponse)
      throws PubChemApiException, JSONException {
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

  // Static helper
  private static String createEmptyPropertyTableJson() {
    return "{\"PropertyTable\": {\"Properties\": []}}";
  }

  // Uses instance jsonMapper
  private List<CompoundData> parsePropertiesJsonWithJackson(String jsonResult) throws IOException {

    if (jsonResult == null || jsonResult.isBlank() || jsonResult.equals(
        createEmptyPropertyTableJson())) {
      return Collections.emptyList();
    }

    JsonNode rootNode = this.jsonMapper.readTree(jsonResult);
    JsonNode propertiesNode = rootNode.path("PropertyTable").path("Properties");

    if (propertiesNode.isMissingNode() || !propertiesNode.isArray()) {
      LOGGER.warning("Could not find 'PropertyTable.Properties' array in JSON response.");
      JsonNode faultNode = rootNode.path("Fault");
      if (!faultNode.isMissingNode()) {
        String faultMsg = extractFaultMessageInternal(jsonResult);
        LOGGER.warning("Received fault message instead of properties: " + faultMsg);
      }
      return Collections.emptyList();
    }

    List<CompoundData> compounds = this.jsonMapper.readerForListOf(CompoundData.class)
        .readValue(propertiesNode);
    compounds.removeIf(Objects::isNull);
    return compounds;
  }

  // Static exception helper
  private static PubChemApiException handleExecutionException(String context, Exception e) {
    // Check order matters: JsonProcessingException is an IOException
    if (e instanceof PubChemApiException) {
      return (PubChemApiException) e;
    } else if (e instanceof JsonProcessingException) {
      LOGGER.log(Level.SEVERE, "Jackson JSON processing error during " + context, e);
      return new PubChemApiException(
          "Jackson JSON processing error during " + context + ": " + e.getMessage(), e);
    } else if (e instanceof IOException) {
      return new PubChemApiException("Network error during " + context + ": " + e.getMessage(), e);
    } else if (e instanceof InterruptedException) {
      Thread.currentThread().interrupt();
      return new PubChemApiException("Operation interrupted during " + context, e);
    } else if (e instanceof JSONException) {
      LOGGER.log(Level.SEVERE, "org.json processing error during " + context, e);
      return new PubChemApiException(
          "org.json processing error during " + context + ": " + e.getMessage(), e);
    }
    // Catch CompletionException specifically if wrapping occurred earlier
    else if (e instanceof CompletionException && e.getCause() != null) {
      // Try to handle the cause
      if (e.getCause() instanceof PubChemApiException) {
        return (PubChemApiException) e.getCause();
      }
      if (e.getCause() instanceof IOException) {
        return new PubChemApiException(
            "Network error during " + context + ": " + e.getCause().getMessage(), e.getCause());
      }
      // ... add other specific cause checks if needed
      LOGGER.log(Level.SEVERE,
          "Unexpected error during " + context + " (wrapped in CompletionException)", e.getCause());
      return new PubChemApiException(
          "Unexpected error during " + context + ": " + e.getCause().getMessage(), e.getCause());
    } else {
      LOGGER.log(Level.SEVERE, "Unexpected error during " + context, e);
      return new PubChemApiException("Unexpected error during " + context + ": " + e.getMessage(),
          e);
    }
  }

  // --- Nested Exception Class ---
  public static class PubChemApiException extends Exception {

    public PubChemApiException(String message) {
      super(message);
    }

    public PubChemApiException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Helper method to combine results from multiple CompletableFutures, each returning a List.
   */
  private static <T> CompletableFuture<List<T>> combineChunkResults(
      List<CompletableFuture<List<T>>> futures) {
    // Create a future that completes when all individual chunk futures complete
    CompletableFuture<Void> allDoneFuture = CompletableFuture.allOf(
        futures.toArray(new CompletableFuture[0]));

    // When all are done, collect the results from each future
    return allDoneFuture.thenApply(v -> // 'v' is void here
        futures.stream().map(
                CompletableFuture::join) // Get result from each completed future (join is safe here as allDoneFuture guarantees completion)
            .flatMap(List::stream)       // Flatten the List<List<T>> into a single Stream<T>
            .collect(Collectors.toList()) // Collect into the final List<T>
    );
  }
}
