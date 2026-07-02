# Gmail Integration — Google Cloud Setup Guide

This guide walks through the **one-time Google Cloud configuration** required before ASTRA can
read a user's Gmail. It produces the **OAuth Client IDs** that the native OAuth flow (Phase B)
needs. Everything here is done in the browser on the Google Cloud Console — no code.

> **Positioning note.** Gmail is an *opt-in* feature. Messages are **fetched from Google** over the
> network, then **analyzed entirely on-device**. Nothing is sent anywhere else. The UI must state
> this clearly so the "on-device / private" promise stays honest.

## Project values (ASTRA-specific)

| Field | Value |
|-------|-------|
| Android package name | `com.kevin.astra` |
| Android **debug** SHA-1 | `43:6A:AC:75:04:7B:23:8B:B7:AC:6A:2C:3B:93:CD:B0:5B:92:80:09` |
| iOS bundle ID | `com.kevin.astra.Astra<TEAM_ID>` — resolve `<TEAM_ID>` from your Apple Developer team (see step 4b) |
| Required scope | `https://www.googleapis.com/auth/gmail.readonly` (read-only) |

> The SHA-1 above is your **debug** signing key (`~/.android/debug.keystore`). For a Play Store
> release you must **also** add the **release** keystore's SHA-1 (and Play App Signing's SHA-1) as
> extra Android OAuth clients. Re-run `keytool -list -v -keystore <release.keystore> -alias <alias>`.

---

## Step 1 — Create (or pick) a Google Cloud project

1. Go to <https://console.cloud.google.com/>.
2. Top bar → project selector → **New Project**.
3. Name it `ASTRA` → **Create**. Make sure it's selected afterward.

## Step 2 — Enable the Gmail API

1. **APIs & Services → Library** (<https://console.cloud.google.com/apis/library>).
2. Search **Gmail API** → open it → **Enable**.

## Step 3 — Configure the OAuth consent screen

1. **APIs & Services → OAuth consent screen**.
2. User type: **External** → **Create**.
3. Fill the required fields:
   - App name: `ASTRA`
   - User support email: your email
   - Developer contact email: your email
4. **Scopes** → **Add or remove scopes** → filter for Gmail → check
   **`.../auth/gmail.readonly`** → **Update** → **Save and continue**.
5. **Test users** → **Add users** → add **your own Gmail address** (and any tester).
   → **Save and continue**.

> Keep the app in **Testing** publishing status while developing. Testing mode allows up to 100
> test users and skips Google's verification. You only need to submit for verification (which can
> take days and may require a security assessment for Gmail scopes) if you publish to all users.

## Step 4 — Create the OAuth Client IDs

**APIs & Services → Credentials → Create credentials → OAuth client ID.** Create one per platform.

### 4a — Android client

- Application type: **Android**
- Name: `ASTRA Android (debug)`
- Package name: `com.kevin.astra`
- SHA-1 certificate fingerprint: `43:6A:AC:75:04:7B:23:8B:B7:AC:6A:2C:3B:93:CD:B0:5B:92:80:09`
- **Create**.

> Android OAuth clients have **no client secret** — identity is proven by package name + SHA-1.
> Repeat this step later with the release SHA-1 for production builds.

### 4b — iOS client

- Application type: **iOS**
- Name: `ASTRA iOS`
- Bundle ID: your resolved bundle id, `com.kevin.astra.Astra<TEAM_ID>`.
  - Find `<TEAM_ID>` in Xcode: open `iosApp/iosApp.xcodeproj` → target **iosApp** → **Signing &
    Capabilities** → the **Bundle Identifier** shown there is the real value. (Or read your Team ID
    at <https://developer.apple.com/account> → Membership.)
- **Create**.
- Note the generated **iOS URL scheme** (the *reversed client ID*, e.g.
  `com.googleusercontent.apps.1234567890-abcdef`). It's needed as a URL scheme in `Info.plist`
  for the OAuth redirect — we'll add it in Phase B.

### 4c — (optional) Web client for testing

If you want to try the flow from a script/Postman before the app is wired, also create a **Web
application** client (it *does* have a client secret) and use the OAuth Playground
(<https://developers.google.com/oauthplayground>) with the `gmail.readonly` scope.

---

## Step 5 — What to hand back for Phase B

Collect these and give them to the dev step (they'll go into config, **not** committed as secrets):

| Item | Where it goes |
|------|---------------|
| **Android OAuth Client ID** (`...apps.googleusercontent.com`) | Android auth config |
| **iOS OAuth Client ID** | iOS auth config |
| **iOS reversed client ID** (URL scheme) | `iosApp/Info.plist` URL types |
| (optional) Web client id + secret | local testing only |

> **Do not commit** client IDs/secrets into the repo. We'll inject them via a local config file
> (git-ignored) or build config fields. The Android client has no secret; the iOS client has no
> secret either (native apps use PKCE), so the main things to protect are the Web client secret and
> any refresh tokens (stored in the platform keystore/Keychain).

---

## What already exists in code (Phase A — done)

The credential-agnostic core is already implemented and tested (`domain/gmail/`):

- `GmailApiClient` — Ktor client for `messages.list` + `messages.get?format=raw`.
- `GmailRepository` — turns fetched messages into `LoadedEmailDocument`s via the shared
  `DefaultEmailExtractor`, so Gmail and imported `.eml` share the same RAG path.
- `GmailTokenProvider` — the single seam Phase B fills: it just needs to return a valid access
  token obtained from the OAuth flow.

## Next (Phase B — after you have the Client IDs)

- Android: OAuth via AppAuth-Android or Credential Manager + Authorization API, requesting the
  `gmail.readonly` scope; store the refresh token in EncryptedSharedPreferences/Keystore.
- iOS: OAuth via GoogleSignIn or AppAuth-iOS + `ASWebAuthenticationSession`; store the refresh
  token in the Keychain.
- Both implement `GmailTokenProvider` (refreshing the access token as needed).
