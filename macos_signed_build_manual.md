# MZmine macOS Signing & Notarization – Developer Handbook

Short guide for recreating the notarized macOS build locally so it mirrors the GitHub Actions pipeline.

---

## 1. Prerequisites
- macOS with admin access.
- Xcode command line tools (`xcode-select --install`) for `codesign`, `notarytool`, `stapler`.
- Node.js (or `nvm`) to install `appdmg` (`npm install -g appdmg`).
- Repository checkout that matches `mzmine-community/build.gradle`.

## 2. Required Credentials
Gather these from the team password vault before starting:

| Purpose | Value / Example | Notes |
|---------|-----------------|-------|
| Developer ID certificate bundle | `DeveloperID.p12` | Exported from Apple Developer portal. |
| Certificate import password | – | Set when the `.p12` was exported. |
| Signing identity string | `Developer ID Application: <Org Name> (<TEAMID>)` | Matches `MACOS_APP_IDENTITY_ID`. |
| Apple Developer Team ID | `TEAMID` | Matches `MACOS_APP_IDENTITY_TEAM_ID`. |
| Apple ID email | e.g. `ansgar.korf@mzio.io` | Hard-coded in `build.gradle` unless changed. |
| App-specific password | 16-character password for the Apple ID | Needed for `notarytool`; regular passwords fail. |

> Store the `.p12`, import password, and app-specific password in the shared vault (1Password/Bitwarden/Vault) with restricted access. Include a note pointing to this handbook.

---

## 3. Optional Dedicated Signing Keychain
Use a throwaway keychain to keep the main keychain clean. Skip if you already trust the cert in `login.keychain-db`.

```bash
SEC_PASS="<temporary keychain password>"
CERT_PASS="<p12 export password>"

security create-keychain -p "$SEC_PASS" mzmine-signing.keychain-db
security default-keychain -s mzmine-signing.keychain-db
security unlock-keychain -p "$SEC_PASS" mzmine-signing.keychain-db
security import /path/to/DeveloperID.p12 \
  -k mzmine-signing.keychain-db \
  -P "$CERT_PASS" \
  -T /usr/bin/codesign -T /usr/bin/security
security set-key-partition-list -S apple-tool:,apple:,codesign: -s -k "$SEC_PASS" mzmine-signing.keychain-db
security find-identity -v -p codesigning mzmine-signing.keychain-db
```

The final command should list your `Developer ID Application` identity. If you reuse the login keychain, just unlock it and run the `set-key-partition-list` command once.

---

## 4. Environment Variables
`build.gradle` reads these at startup:

```bash
export MACOS_APP_IDENTITY_ID="Developer ID Application: <Org Name> (<TEAMID>)"
export MACOS_APP_SPECIFIC_PWD="<app-specific-password>"
export MACOS_APP_IDENTITY_TEAM_ID="<TEAMID>"
# change only if build.gradle's appleID changes:
# export MACOS_APPLE_ID="<apple-id-email>"
```

Confirm the identity string matches the output of `security find-identity`.

---

## 5. Run the Signed Build
From the repository root:

```bash
./gradlew -p mzmine-community notarizeApp
```

The Gradle task performs:
1. `jpackage` to build `mzmine.app`.
2. `codesign` on every binary and the `.app` (`build.gradle` lines 459–606).
3. DMG creation via `appdmg`.
4. `xcrun notarytool submit --wait …` with strict status check (`build.gradle` lines 643–666).
5. `xcrun stapler staple` on both DMG and app bundle (`build.gradle` lines 668–688).
6. Portable ZIP creation.

Build artifacts land in `mzmine-community/build/jpackage/`.

---

## 6. Post-Build Verification
1. Mount the DMG and copy `mzmine.app` to `/Applications`.
2. Gatekeeper check:
   ```bash
   spctl --assess --type exec --verbose /Applications/mzmine.app
   ```
   Expect `source=Notarized Developer ID`.
3. Optional deep verification:
   ```bash
   codesign --verify --deep --strict --verbose=4 /Applications/mzmine.app
   ```

If notarization fails, run:
```bash
xcrun notarytool history --apple-id "<apple-id>" \
  --password "$MACOS_APP_SPECIFIC_PWD" \
  --team-id "$MACOS_APP_IDENTITY_TEAM_ID"
```
and review rejection details.

---

## 7. Cleanup (Optional)
```bash
security delete-keychain mzmine-signing.keychain-db
unset MACOS_APP_IDENTITY_ID MACOS_APP_SPECIFIC_PWD MACOS_APP_IDENTITY_TEAM_ID
```
