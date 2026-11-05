package io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.community;

import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClass;
import io.github.mzmine.modules.dataprocessing.id_lipidid.common.lipids.custom_class.CustomLipidClassJson;
import io.github.mzmine.util.io.JsonUtils;
import io.github.mzmine.util.web.HttpResponseException;
import io.github.mzmine.util.web.HttpUtils;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Downloads lipid classes from the community lipid library and builds links to share classes.
 * The library is a public GitHub repository with a static catalog website.
 */
public final class LipidLibraryClient {

  public static final String REPOSITORY_URL = "https://github.com/mzmine/mzmine-custom-lipid-library";
  /**
   * The catalog website. Can be changed with the system property
   * {@value #WEBSITE_URL_PROPERTY}, e.g., for a mirror or for testing.
   */
  public static final String WEBSITE_URL;
  public static final String WEBSITE_URL_PROPERTY = "mzmine.lipidlibrary.url";
  public static final String TERMS_URL = REPOSITORY_URL + "/blob/main/LICENSE.md";
  public static final String CONTRIBUTOR_AGREEMENT_URL =
      REPOSITORY_URL + "/blob/main/CONTRIBUTOR_AGREEMENT.md";
  private static final String SUBMISSION_URL =
      REPOSITORY_URL + "/issues/new?template=submit-lipid-class.yml";
  private static final long TIMEOUT_SECONDS = 20;

  static {
    final String url = System.getProperty(WEBSITE_URL_PROPERTY,
        "https://mzmine.github.io/mzmine-custom-lipid-library/");
    WEBSITE_URL = url.endsWith("/") ? url : url + "/";
  }

  private LipidLibraryClient() {
  }

  /**
   * @return the catalog of all classes in the library
   * @throws IOException           if the catalog cannot be read
   * @throws HttpResponseException if the server does not answer with OK
   */
  public static @NotNull LipidLibraryCatalog fetchCatalog()
      throws IOException, InterruptedException, HttpResponseException {
    final String json = HttpUtils.get(WEBSITE_URL + "index.json", TIMEOUT_SECONDS);
    return JsonUtils.MAPPER.readValue(json, LipidLibraryCatalog.class);
  }

  /**
   * @param entry a catalog entry
   * @return the classes in the class file of the entry, usually one
   * @throws IOException           if the file cannot be read
   * @throws HttpResponseException if the server does not answer with OK
   */
  public static @NotNull List<CustomLipidClass> fetchClasses(@NotNull LipidLibraryEntry entry)
      throws IOException, InterruptedException, HttpResponseException {
    final String json = HttpUtils.get(WEBSITE_URL + entry.file(), TIMEOUT_SECONDS);
    return CustomLipidClassJson.fromJson(json);
  }

  /**
   * @param entry a catalog entry
   * @return the page of the class on the catalog website
   */
  public static @NotNull String websiteUrl(@NotNull LipidLibraryEntry entry) {
    return WEBSITE_URL + "#/class/" + URLEncoder.encode(entry.id(), StandardCharsets.UTF_8);
  }

  /**
   * GitHub issue forms are prefilled from URL parameters named like the form field ids. The class
   * JSON is too long for a URL and is pasted by the user from the clipboard.
   *
   * @param lipidClass the class to share
   * @return the URL of the submission form with prefilled title
   */
  public static @NotNull String submissionUrl(@NotNull CustomLipidClass lipidClass) {
    final String title = "[Lipid class] " + lipidClass.getAbbr() + " " + lipidClass.getName();
    return SUBMISSION_URL + "&title=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
  }
}
