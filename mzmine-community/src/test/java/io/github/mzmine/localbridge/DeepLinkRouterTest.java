/*
 * Copyright (c) 2004-2026 The mzmine Development Team
 */
package io.github.mzmine.localbridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DeepLinkRouterTest {

  private static final UUID INSTANCE = UUID.fromString("6ea6b58f-2e04-47ec-a9a2-5e3e3ff8c831");
  private static final UUID PROJECT = UUID.fromString("1e2c7208-672e-4d33-9568-35bb5fb09f73");
  private static final UUID RESOURCE = UUID.fromString("73e1da13-92cf-4b30-8718-2f6762087618");

  @Test
  void applicationConnectionLinkCarriesNoProjectOrAction() {
    final DeepLink link = DeepLinkRouter.parse("mzmine://app/connection");
    assertEquals(DeepLink.Destination.APP_CONNECTION, link.destination());
    assertEquals(null, link.instanceId());
    assertEquals(null, link.projectId());
    for (final String suffix : new String[]{"?token=secret", "#apply", "/", "/shell"}) {
      assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse("mzmine://app/connection" + suffix));
    }
    assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse(
        "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/app-connection"));
  }

  @Test
  void parsesOpaqueFeatureTableTarget() {
    final DeepLink link = DeepLinkRouter.parse(
        "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/feature-table/" + RESOURCE);

    assertEquals(INSTANCE, link.instanceId());
    assertEquals(PROJECT, link.projectId());
    assertEquals(DeepLink.Destination.FEATURE_TABLE, link.destination());
    assertEquals(RESOURCE, link.resourceId().orElseThrow());
  }

  @Test
  void onlyWizardMayOmitResource() {
    assertTrue(DeepLinkRouter.parse(
        "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/wizard").resourceId().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse(
        "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/proposal"));
  }

  @Test
  void rejectsUntrustedUriParts() {
    assertFalse(DeepLinkRouter.queue("mzmine://navigate/" + INSTANCE + "/" + PROJECT
        + "/wizard?path=/private/data"));
    assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse(
        "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/shell/" + RESOURCE));
    assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse(
        "https://navigate/" + INSTANCE + "/" + PROJECT + "/wizard"));
    assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse(
        "mzmine://navigate/1-1-1-1-1/" + PROJECT + "/wizard"));
    assertThrows(IllegalArgumentException.class, () -> DeepLinkRouter.parse(
        "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/wizard" + "x".repeat(256)));
  }

  @Test
  void removesOnlyDeepLinksBeforeCommandLineParsing() {
    final String link = "mzmine://navigate/" + INSTANCE + "/" + PROJECT + "/wizard";
    assertTrue(Arrays.equals(new String[]{"-batch", "workflow.mzbatch"},
        DeepLinkRouter.withoutDeepLinkArguments(new String[]{"-batch", link, "workflow.mzbatch"})));
  }
}
