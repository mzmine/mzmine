/*
 * (C) Copyright 2015-2018 by MSDK Development Team
 *
 * This software is dual-licensed under either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1 as published by the Free
 * Software Foundation
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by the Eclipse Foundation.
 */

package io.github.mzmine.modules.dataprocessing.id_sirius;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IMolecularFormula;
import org.openscience.cdk.smiles.SmilesParser;
import org.openscience.cdk.tools.manipulator.MolecularFormulaManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import de.unijena.bioinf.ChemistryBase.algorithm.Scored;
import de.unijena.bioinf.ChemistryBase.chem.MolecularFormula;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.ChemistryBase.fp.CdkFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.FingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.MaskedFingerprintVersion;
import de.unijena.bioinf.ChemistryBase.fp.PredictionPerformance;
import de.unijena.bioinf.ChemistryBase.fp.ProbabilityFingerprint;
import de.unijena.bioinf.ChemistryBase.ms.Ms2Experiment;
import de.unijena.bioinf.ChemistryBase.ms.ft.FTree;
import de.unijena.bioinf.babelms.json.FTJsonWriter;
import de.unijena.bioinf.babelms.ms.JenaMsWriter;
import de.unijena.bioinf.babelms.utils.Base64;
import de.unijena.bioinf.chemdb.BioFilter;
import de.unijena.bioinf.chemdb.ChemicalDatabaseException;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeLayer;
import de.unijena.bioinf.chemdb.CompoundCandidateChargeState;
import de.unijena.bioinf.chemdb.FingerprintCandidate;
import de.unijena.bioinf.chemdb.RESTDatabase;
import de.unijena.bioinf.chemdb.SearchStructureByFormula;
import de.unijena.bioinf.fingerid.blast.CovarianceScoring;
import de.unijena.bioinf.fingerid.blast.Fingerblast;
import de.unijena.bioinf.fingerid.blast.FingerblastScoringMethod;
import de.unijena.bioinf.fingerid.blast.ScoringMethodFactory;
import de.unijena.bioinf.fingerid.predictor_types.PredictorType;
import de.unijena.bioinf.utils.systemInfo.SystemInformation;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import io.github.msdk.MSDKException;
import io.github.msdk.MSDKMethod;
import io.github.msdk.MSDKRuntimeException;
import io.github.msdk.datamodel.IonAnnotation;

/**
 * <p>
 * Class FingerIdWebMethod
 * </p>
 * This class wraps the FingerId API provided by boecker-labs Uses the results of
 * SiriusIdentificationMethod and returns exteneded IonAnnotations
 */
public class FingerIdWebMethod implements MSDKMethod<List<IonAnnotation>> {

  private final static SmilesParser smp = new SmilesParser(DefaultChemObjectBuilder.getInstance());
  private final static CloseableHttpClient client = HttpClients.createSystem(); // Threadsafe
  private final static Logger logger = LoggerFactory.getLogger(FingerIdWebMethod.class);
  private final static BasicNameValuePair UID =
      new BasicNameValuePair("uid", SystemInformation.generateSystemKey());
  private final static String FINGERID_SOURCE = "https://www.csi-fingerid.uni-jena.de";
  /* TODO: This field must be altered whenever boecker-labs updates its API!!! */
  private final static String FINGERID_VERSION = "1.1.3";
  private final SearchStructureByFormula searchDB;
  private final static Gson gson = new Gson();

  // Do not use caching
  private final static File siriusCacheLocation = null;

  private final Ms2Experiment experiment;
  private final SiriusIonAnnotation ionAnnotation;
  private final PredictionPerformance[] perf;
  private final MaskedFingerprintVersion version;
  private final Fingerblast blaster;
  private final int candidatesAmount;
  private List<IonAnnotation> newAnnotations;
  private int finishedItems;
  private boolean cancelled;


  /**
   * <p>
   * Constructor for a FingerIdWebMethod
   * </p>
   * 
   * @param experiment - Ms2Experiment that was used to get ionAnnotation
   * @param ionAnnotation - SiriusIonAnnotation returned from SiriusIdentificationMethod with FTree
   *        field specified
   * @param candidatesAmount - amount of candidates to be returned from this method
   * @throws MSDKException if any
   */
  public FingerIdWebMethod(@Nonnull Ms2Experiment experiment,
      @Nonnull SiriusIonAnnotation ionAnnotation, @Nonnull Integer candidatesAmount)
      throws MSDKException {


    this.experiment = experiment;
    this.ionAnnotation = ionAnnotation;

    this.searchDB =
        new RESTDatabase(siriusCacheLocation, BioFilter.ALL, URI.create(FINGERID_SOURCE));

    // This disables annoying logging dumps by Apache HTTP, which are erroneously enabled by the
    // RESTDatabase class
    java.util.logging.Logger.getLogger("org.apache.http.wire")
        .setLevel(java.util.logging.Level.INFO);
    java.util.logging.Logger.getLogger("org.apache.http.headers")
        .setLevel(java.util.logging.Level.INFO);

    try {
      final TIntArrayList list = new TIntArrayList(4096);
      perf = getStatistics(getType(), list);

      version = buildFingerprintVersion(list);
      blaster = createBlaster(perf);

    } catch (IOException e) {
      throw new MSDKException(e);
    }

    newAnnotations = new LinkedList<>();
    finishedItems = 0;
    if (candidatesAmount == 0)
      this.candidatesAmount = version.size();
    else
      this.candidatesAmount = candidatesAmount;

  }

  /**
   * <p>
   * Simplified FingerIdWebMethod constructor
   * </p>
   * In this case, FingerId will return unspecified amount of compounds (500-7000)
   * 
   * @param experiment - Ms2Experiment object
   * @param ionAnnotation - SiriusIonAnnotation with FTree specified
   * @throws MSDKException if any
   */
  public FingerIdWebMethod(@Nonnull Ms2Experiment experiment,
      @Nonnull SiriusIonAnnotation ionAnnotation) throws MSDKException {
    this(experiment, ionAnnotation, 0);
  }

  /**
   * <p>
   * Method returns URIBuilder for the Web server
   * </p>
   * 
   * @param path API functionality you are interested in
   * @return builder
   * @throws URISyntaxException if any
   */
  private URIBuilder getFingerIdURI(String path) throws URISyntaxException {
    if (path == null)
      path = "";

    URIBuilder builder = new URIBuilder(FINGERID_SOURCE);
    builder.setPath("/csi-fingerid-" + FINGERID_VERSION + path);

    return builder;
  }

  /**
   * <p>
   * Method returns possible FingerprintCandidates according to MolecularFormula
   * </p>
   * Method makes a request to the remote DB
   * 
   * @return List of FingerprintCandidates
   * @throws ChemicalDatabaseException
   */
  private List<FingerprintCandidate> getCandidates() throws ChemicalDatabaseException {
    PrecursorIonType ionType = experiment.getPrecursorIonType();
    IMolecularFormula iFormula = ionAnnotation.getFormula();
    MolecularFormula formula =
        MolecularFormula.parse(MolecularFormulaManipulator.getString(iFormula));

    final CompoundCandidateChargeState chargeState =
        CompoundCandidateChargeState.getFromPrecursorIonType(ionType);
    if (cancelled)
      return null;
    if (chargeState != CompoundCandidateChargeState.NEUTRAL_CHARGE) {
      final List<FingerprintCandidate> intrinsic =
          searchDB.lookupStructuresAndFingerprintsByFormula(formula);
      intrinsic
          .removeIf((f) -> !f.hasChargeState(CompoundCandidateChargeLayer.Q_LAYER, chargeState));
      // all intrinsic formulas have to contain a p layer?
      final MolecularFormula hydrogen = MolecularFormula.parse("H");
      final List<FingerprintCandidate> protonated =
          searchDB.lookupStructuresAndFingerprintsByFormula(
              ionType.getCharge() > 0 ? formula.subtract(hydrogen) : formula.add(hydrogen));
      protonated
          .removeIf((f) -> !f.hasChargeState(CompoundCandidateChargeLayer.P_LAYER, chargeState));

      intrinsic.addAll(protonated);
      return intrinsic;
    } else {
      final List<FingerprintCandidate> candidates =
          searchDB.lookupStructuresAndFingerprintsByFormula(formula);
      candidates.removeIf((f) -> !f.hasChargeState(CompoundCandidateChargeLayer.P_LAYER,
          CompoundCandidateChargeState.NEUTRAL_CHARGE));
      return candidates;
    }
  }

  /**
   * <p>
   * Method builds PredictionPerformance array
   * </p>
   * Method is copied from boecker-lab
   * 
   * @param predictorType PredictorType object (CSI_FINGERID_POSITIVE or CSI_FINGERID_NEGATIVE)
   * @param fingerprintIndizes list to be filled
   * @return new PredictionPerformance array used to configure MaskedFingerprintVersion
   * @throws IOException if any
   */
  private PredictionPerformance[] getStatistics(PredictorType predictorType,
      final TIntArrayList fingerprintIndizes) throws IOException {
    fingerprintIndizes.clear();
    final HttpGet get;
    try {
      get = new HttpGet(getFingerIdURI("/webapi/statistics.csv")
          .setParameter("predictor", predictorType.toBitsAsString()).build());
    } catch (URISyntaxException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    final TIntArrayList[] lists = new TIntArrayList[5];
    ArrayList<PredictionPerformance> performances = new ArrayList<>();
    try (CloseableHttpResponse response = client.execute(get)) {
      HttpEntity e = response.getEntity();
      final BufferedReader br = new BufferedReader(
          new InputStreamReader(e.getContent(), ContentType.getOrDefault(e).getCharset()));
      String line;
      while ((line = br.readLine()) != null) {
        String[] tabs = line.split("\t");
        final int index = Integer.parseInt(tabs[0]);
        PredictionPerformance p = new PredictionPerformance(Double.parseDouble(tabs[1]),
            Double.parseDouble(tabs[2]), Double.parseDouble(tabs[3]), Double.parseDouble(tabs[4]));
        performances.add(p);
        fingerprintIndizes.add(index);
      }
    }
    return performances.toArray(new PredictionPerformance[performances.size()]);
  }

  /**
   * <p>
   * Method builds a Scoring method according to FingerprintVersion & alpha of your data
   * </p>
   * Method is copied from boecker-lab
   * 
   * @param fpVersion FingerprintVersion object
   * @param alpha value of your data
   * @return new Scoring method
   * @throws IOException if any
   */
  private CovarianceScoring getCovarianceScoring(FingerprintVersion fpVersion, double alpha)
      throws IOException {
    final HttpGet get;
    try {
      get = new HttpGet(getFingerIdURI("/webapi/covariancetree.csv").build());
    } catch (URISyntaxException e) {
      logger.error(e.getMessage(), e);
      throw new RuntimeException(e);
    }
    CovarianceScoring covarianceScoring;
    try (CloseableHttpResponse response = client.execute(get)) {
      if (!isSuccessful(response))
        throw new IOException("Cannot get covariance scoring tree information.");
      HttpEntity e = response.getEntity();
      covarianceScoring = CovarianceScoring.readScoring(e.getContent(),
          ContentType.getOrDefault(e).getCharset(), fpVersion, alpha);
    }
    return covarianceScoring;
  }

  /**
   * <p>
   * Method checks status of the response
   * </p>
   * 
   * @param response to be checked
   * @return status code < 400
   */
  private boolean isSuccessful(CloseableHttpResponse response) {
    return response.getStatusLine().getStatusCode() < 400;
  }

  /**
   * <p>
   * Method processes SiriusAnnotations
   * </p>
   * 
   * @return sorted list of possible candidates
   * @throws MSDKException if any
   * @throws MSDKRuntimeException if thread for updating job status was interrupted
   */
  private List<Scored<FingerprintCandidate>> processSiriusAnnotation()
      throws MSDKException, MSDKRuntimeException {
    ProbabilityFingerprint print;
    List<Scored<FingerprintCandidate>> scored;

    List<FingerprintCandidate> candidates;
    try {
      // Initiate the job
      FingerIdJob job = submitJob();
      // Get ProbabilityFingerprint
      print = processFingerIdJob(job);
      // Get List<FingerprintCandidates>
      candidates = getCandidates();
      // Sort candidates
      if (cancelled)
        return null;
      scored = blaster.score(candidates, print);
    } catch (ChemicalDatabaseException e) {
      logger.error("Connection with PubChem DB failed.");
      throw new MSDKRuntimeException(e);
    } catch (URISyntaxException e) {
      logger.error("Failed to construct URI");
      throw new MSDKException(e);
    } catch (IOException e) {
      throw new MSDKException(e);
    } catch (TimeoutException e) {
      logger.error("Timeout on job status update has expired!");
      throw new MSDKRuntimeException(e);
    } catch (InterruptedException i) {
      throw new MSDKRuntimeException(i);
    }

    return scored;
  }

  /**
   * <p>
   * Constructs MaskedFingerprintVersion
   * </p>
   * 
   * @param predictionIndiсes list with indiсes returned from API according to PredictorType
   * @return new MaskedFingerprintVersion object
   * @throws IOException
   */
  private MaskedFingerprintVersion buildFingerprintVersion(final TIntArrayList predictionIndiсes)
      throws IOException {
    CdkFingerprintVersion version = CdkFingerprintVersion.withECFP();
    MaskedFingerprintVersion.Builder maskedBuiled = MaskedFingerprintVersion.buildMaskFor(version);
    maskedBuiled.disableAll();

    int[] indiсes = predictionIndiсes.toArray();
    for (int index : indiсes) {
      maskedBuiled.enable(index);
    }

    return maskedBuiled.toMask();
  }

  /**
   * <p>
   * Method creates Fingerblast object that will score FingerprintCandidates
   * </p>
   * 
   * @param perf values returned from Web API
   * @return new Fingerblast object
   * @throws IOException
   */
  private Fingerblast createBlaster(PredictionPerformance[] perf) throws IOException {
    FingerblastScoringMethod method = null;
    PredictorType type = getType();
    if (type == PredictorType.CSI_FINGERID_NEGATIVE)
      method = new ScoringMethodFactory.CSIFingerIdScoringMethod(perf);
    else
      method = getCovarianceScoring(version, 1d / perf[0].withPseudoCount(0.25).numberOfSamples());

    return new Fingerblast(method, null);
  }

  /**
   * <p>
   * Method returns PredictorType
   * </p>
   * As there were no access to boecker-lab code, this solution was used
   * 
   * @return type
   */
  private PredictorType getType() {
    int charge = this.experiment.getPrecursorIonType().getCharge();
    if (charge > 0)
      return PredictorType.CSI_FINGERID_POSITIVE;
    return PredictorType.CSI_FINGERID_NEGATIVE;
  }

  /**
   * <p>
   * Method submits job to the Web server and returns its id, securityToken
   * </p>
   * 
   * @return new FingerIdJob
   * @throws IOException if any
   * @throws URISyntaxException if any
   */
  private FingerIdJob submitJob() throws IOException, URISyntaxException {
    final HttpPost post = new HttpPost(getFingerIdURI("/webapi/predict.json").build());
    final FTree ftree = ionAnnotation.getFTree();
    post.setEntity(buildParams(ftree));

    final String securityToken;
    final long jobId;
    // SUBMIT JOB

    try (CloseableHttpResponse response = client.execute(post)) {
      if (cancelled)
        return null;
      if (response.getStatusLine().getStatusCode() == 200) {
        GetResponse getResponse;
        synchronized (gson) {
          BufferedReader reader =
              new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
                  ContentType.getOrDefault(response.getEntity()).getCharset()));
          getResponse = gson.fromJson(reader, GetResponse.class);
        }
        securityToken = getResponse.securityToken;
        jobId = getResponse.jobId;
        return new FingerIdJob(jobId, securityToken, version);
      } else {
        RuntimeException re = new RuntimeException(response.getStatusLine().getReasonPhrase());
        logger.debug("Submitting Job failed", re);
        throw re;
      }
    }
  }

  /**
   * <p>
   * Method creates params for Web request
   * </p>
   * 
   * @param ftree tree to be encoded as a string
   * @return new params
   * @throws IOException if any
   */
  private UrlEncodedFormEntity buildParams(FTree ftree) throws IOException {
    final String stringMs = getExperimentAsString();
    final String jsonTree = getTreeAsString(ftree);

    final NameValuePair ms = new BasicNameValuePair("ms", stringMs);
    final NameValuePair tree = new BasicNameValuePair("ft", jsonTree);
    final NameValuePair predictor =
        new BasicNameValuePair("predictors", PredictorType.getBitsAsString(getType()));

    final UrlEncodedFormEntity params =
        new UrlEncodedFormEntity(Arrays.asList(ms, tree, predictor, UID));
    return params;
  }

  /**
   * <p>
   * Method for transformation of FTree into a param in web request
   * </p>
   * 
   * @param ftree FTree 0bject
   * @return FTree in form of string
   * @throws IOException if any
   */
  private String getTreeAsString(FTree ftree) throws IOException {
    final FTJsonWriter writer = new FTJsonWriter();
    final StringWriter sw = new StringWriter();
    writer.writeTree(sw, ftree);
    return sw.toString();
  }

  /**
   * <p>
   * Method for transformation of Ms2Experiment into a param in web request
   * </p>
   * 
   * @return Ms2Experiment in form of string
   * @throws IOException if any
   */
  private String getExperimentAsString() throws IOException {
    final JenaMsWriter writer = new JenaMsWriter();
    final StringWriter sw = new StringWriter();
    try (final BufferedWriter bw = new BufferedWriter(sw)) {
      writer.write(bw, experiment);
    }
    return sw.toString();
  }

  /**
   * <p>
   * Method receives results of the FingerIdJob
   * </p>
   * Method periodically checks the state of the job on a Webserver and, if ready, returns the
   * result
   * 
   * @param job - FingerIdJob for identifying ProbabilityFingerprint
   * @return ProbabilityFingerprint returned from Web API
   * @throws URISyntaxException if any
   * @throws InterruptedException if any
   * @throws TimeoutException if any
   * @throws IOException if any
   */
  private ProbabilityFingerprint processFingerIdJob(FingerIdJob job)
      throws URISyntaxException, InterruptedException, TimeoutException, IOException {
    new HttpGet(getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId))
        .setParameter("securityToken", job.securityToken).build());
    for (int k = 0; k < 600; ++k) {
      if (cancelled)
        return null;
      Thread.sleep(3000 + 30 * k);
      if (updateJobStatus(job)) {
        return job.prediction;
      } else if (Objects.equals(job.state, "CRASHED")) {
        throw new RuntimeException(
            "Job crashed: " + (job.errorMessage != null ? job.errorMessage : ""));
      }
    }
    throw new TimeoutException("Reached timeout");
  }

  /**
   * <p>
   * Method for updating the status of FingerIdJob
   * </p>
   * Method makes a get request with Job id, token and returns its status or its results
   * 
   * @param job - FingerIdJob object
   * @return true if job is done, false if job is still not ready
   * @throws URISyntaxException if any
   */
  private boolean updateJobStatus(FingerIdJob job) throws URISyntaxException {
    final HttpGet get = new HttpGet(
        getFingerIdURI("/webapi/job.json").setParameter("jobId", String.valueOf(job.jobId))
            .setParameter("securityToken", job.securityToken).build());
    try (CloseableHttpResponse response = client.execute(get)) {
      GetResponse getResponse;
      BufferedReader reader =
          new BufferedReader(new InputStreamReader(response.getEntity().getContent(),
              ContentType.getOrDefault(response.getEntity()).getCharset()));
      synchronized (gson) {
        getResponse = gson.fromJson(reader, GetResponse.class);
      }
      if (getResponse.prediction != null) {
        getResponse.plattBytes = Base64.decode(getResponse.prediction);
        final double[] platts = parseBinaryToDoubles(getResponse.plattBytes);
        job.prediction = new ProbabilityFingerprint(job.v, platts);

        if (getResponse.iokrVector != null) {
          getResponse.iokrBytes = Base64.decode(getResponse.iokrVector);
          job.iokrVector = parseBinaryToDoubles(getResponse.iokrBytes);
        }

        return true;
      } else {
        job.state = getResponse.state != null ? getResponse.state : "SUBMITTED";
      }
      if (getResponse.errors != null) {
        job.errorMessage = getResponse.errors;
      }
    } catch (Throwable t) {
      logger.error("Error when updating job #" + job.jobId, t);
      throw new MSDKRuntimeException(t);
    }
    return false;
  }

  /**
   * <p>
   * Method parses binary values into doubles
   * </p>
   * 
   * @param bytes - array of bytes
   * @return new double array
   */
  private double[] parseBinaryToDoubles(byte[] bytes) {
    final TDoubleArrayList data = new TDoubleArrayList(2000);
    final ByteBuffer buf = ByteBuffer.wrap(bytes);
    buf.order(ByteOrder.LITTLE_ENDIAN);
    while (buf.position() < buf.limit()) {
      data.add(buf.getDouble());
    }
    return data.toArray();
  }

  @Nullable
  @Override
  public Float getFinishedPercentage() {
    if (newAnnotations != null && newAnnotations.size() == 0) // Fixes issue with empty result list
      return 1f;
    return 1f * finishedItems / candidatesAmount;
  }

  @Nullable
  @Override
  public List<IonAnnotation> execute() throws MSDKException {
    List<Scored<FingerprintCandidate>> candidates = processSiriusAnnotation();
    Set<String> visitedSMILES = new TreeSet<>();

    for (Scored<FingerprintCandidate> scoredCandidate : candidates) {
      if (cancelled)
        return null;
      final SiriusIonAnnotation extendedAnnotation = new SiriusIonAnnotation(ionAnnotation);
      final FingerprintCandidate candidate = scoredCandidate.getCandidate();

      synchronized (smp) {
        try {
          String smilesString = candidate.getSmiles();
          if (visitedSMILES.contains(smilesString))
            continue;
          IAtomContainer container = smp.parseSmiles(smilesString);
          extendedAnnotation.setChemicalStructure(container);
          extendedAnnotation.setSMILES(smilesString);
          visitedSMILES.add(smilesString);
        } catch (org.openscience.cdk.exception.InvalidSmilesException e) {
          logger.error("Incorrect SMILES string");
          throw new MSDKException(e);
        }
      }
      extendedAnnotation.setInchiKey(candidate.getInchiKey2D());
      extendedAnnotation.setFingerIdScore(scoredCandidate.getScore());
      extendedAnnotation.setDescription(candidate.getName());
      extendedAnnotation.setDBLinks(candidate.getLinks());
      newAnnotations.add(extendedAnnotation);
      finishedItems++;
      if (finishedItems == candidatesAmount)
        break;
    }


    return newAnnotations;
  }

  @Nullable
  @Override
  public List<IonAnnotation> getResult() {
    return newAnnotations;
  }

  @Override
  public void cancel() { // TODO: make it
    cancelled = true;
  }


  /**
   * <p>
   * Class GetResponse
   * </p>
   * Used as a container for Get responses from WebAPI #securityToken - generated string by server
   * during first POST (when you register your job on a server) #jobId - the same, as securityToken
   * Both parameters are used later to identify the job later.
   *
   * #prediction - the PredictedFingerprint in form of String (Base64) #errors - String with error
   * message #state - the state of the task (updated during updateJob method)
   * ------------------------------------ #plakkBytes & iokrBytes are not currently used
   */
  private class GetResponse {
    public String securityToken;
    public int jobId;
    byte[] plattBytes;
    byte[] iokrBytes;

    String iokrVector;
    String prediction;
    String errors;
    String state;
  }

  /**
   * <p>
   * Class FingerIdJob
   * </p>
   * Class-container for results of the request to get ProbabilityFingerprint #jobId - the id of the
   * task on the Webserver (API) #securityToken - generated string by server during first POST (when
   * you register your job on a server) #MaskedFingerprintVersion - used for generating
   * ProbabilityFingerprint #state - status of the job on Webserver #errorMessage - the error
   * received from GetResponse ------------------------------------ #iokrVector - currently not used
   */
  private class FingerIdJob {
    public long jobId;
    public String securityToken;
    public MaskedFingerprintVersion v;
    public ProbabilityFingerprint prediction;
    public String errorMessage;
    public String state;
    public double[] iokrVector;

    public FingerIdJob(long jobId, String securityToken, MaskedFingerprintVersion version) {
      this.jobId = jobId;
      this.securityToken = securityToken;
      this.v = version;
    }
  }
}

