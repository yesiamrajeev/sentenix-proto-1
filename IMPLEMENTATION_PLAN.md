# HerShield — Dynamic Update Feature Implementation Plan

## 1. Goal

Add an end-to-end "check for update" flow:

- Android app sends an HTTP GET to a Go server running on AWS EC2.
- The request carries a role header (`X-Client-Role: admin` or `user`) controlled by an on-screen toggle.
- The server returns a JSON envelope containing an XML config payload **only** when the header is `admin`; otherwise it returns `204 No Content`.
- The app parses the XML and live-updates the user `HomeFragment`: shows the SOS button, swaps colors, swaps text labels.
- The "updated" state persists across app restarts via `SharedPreferences`.
- The server emits a rich, Istio/Envoy-flavored multi-line log stream per request so it can be demoed live.

## 2. Architecture

```
┌────────────────────────┐                    ┌──────────────────────────┐
│ Android — HomeFragment │                    │ Go server (Dockerized)   │
│                        │                    │ on AWS EC2 t2.micro      │
│  [admin/user toggle]   │                    │ :8080/check-update       │
│  [Check for update]    │                    │                          │
│                        │  GET /check-update │ ┌──────────────────────┐ │
│  ┌──────────────────┐  │  X-Client-Role     │ │ logger: envoy-style  │ │
│  │ UpdateClient     │  │ ─────────────────► │ │ + istio sidecar tags │ │
│  │  (OkHttp)        │  │                    │ └──────────────────────┘ │
│  └──────────────────┘  │                    │                          │
│  ┌──────────────────┐  │  200 {xml:"..."}   │ admin → read config.xml  │
│  │ XML parser       │  │  OR 204 No Content │ user  → 204              │
│  │  (XmlPullParser) │  │ ◄───────────────── │                          │
│  └──────────────────┘  │                    │                          │
│  ┌──────────────────┐  │                    │                          │
│  │ ConfigApplier    │  │                    │                          │
│  │ ConfigStore      │  │                    │                          │
│  │  (SharedPrefs)   │  │                    │                          │
│  └──────────────────┘  │                    │                          │
└────────────────────────┘                    └──────────────────────────┘
```

## 3. Wire protocol

- **Endpoint:** `GET /check-update`
- **Request header:** `X-Client-Role: admin` or `X-Client-Role: user`
- **Admin response:** `200 OK`
  ```json
  { "version": 2, "xml": "<config version=\"2\">...</config>" }
  ```
- **User response:** `204 No Content` (empty body)
- Server URL constant lives in `UpdateClient.java`. Local dev: `http://10.0.2.2:8080/check-update`. Production: swap to EC2 public IP.

## 4. XML config schema (v1)

```xml
<config version="2">
  <sos visible="true"/>
  <colors>
    <primary>#2E7D32</primary>
    <sosButton>#D32F2F</sosButton>
    <uploadButton>#1976D2</uploadButton>
  </colors>
  <texts>
    <appTitle>HerShield v2</appTitle>
    <tagline>Now smarter, faster, safer!</tagline>
    <uploadLabel>Upload Report</uploadLabel>
  </texts>
</config>
```

| Element | Target view in `fragment_home.xml` | Property changed |
|---|---|---|
| `<sos visible>` | `@id/send_sos_button` | `visibility = VISIBLE / GONE` |
| `<colors><primary>` | `@id/welcomeTextView`, `@id/SCYNC` | `setTextColor` |
| `<colors><sosButton>` | `@id/send_sos_button` | background tint |
| `<colors><uploadButton>` | `@id/uplbtn` | background tint |
| `<texts><appTitle>` | `@id/welcomeTextView` | `setText` |
| `<texts><tagline>` | `@id/SCYNC` | `setText` |
| `<texts><uploadLabel>` | `@id/uplbtn` | `setText` |

## 5. Default ("pre-update") state on app open

- Toggle defaults to **user** (debug toggle, not persisted).
- SOS button **hidden** (`visibility="gone"` in XML).
- Original colors and text from current layout.
- After successful "Update Now", config is persisted; on every subsequent launch `HomeFragment.onCreateView` reads it and reapplies before the screen is shown.

## 6. UX flow — "Check for update" button

1. User taps **Check for update**.
2. App shows a non-cancellable `ProgressDialog` "Checking for updates…".
3. Background: OkHttp call + a forced minimum 4-second wait; whichever finishes last unblocks step 4.
4. Dialog dismisses.
   - `204` → toast "No updates available".
   - `200` → AlertDialog **"Update Available — Update Now / Not Now"**.
5. On **Update Now** → non-cancellable dialog "Updating…" for exactly 5 seconds, then:
   - Parse XML.
   - Apply changes to live views.
   - Persist to SharedPreferences.
   - Dismiss dialog, toast "Updated".

## 7. Server logging (demo-grade)

Every request emits a 10-line technical-looking stream with ISO-8601 timestamps, log levels, component tags, request IDs (UUID), trace IDs (32-hex), span IDs (16-hex).

Example for an admin request:
```
2026-05-20T15:42:11.234Z INFO  envoy.http[main]        downstream connection accepted remote=10.0.0.42:54231 x-request-id=8e3f4a91-1c0d-4b87-9e1a-4f1c2b3a5d6e
2026-05-20T15:42:11.235Z INFO  istio.proxy[inbound]    request entering via istio gateway gateway=ingressgateway listener=0.0.0.0:8080 cluster=inbound|8080||hershield-config.default.svc.cluster.local
2026-05-20T15:42:11.236Z INFO  istio.mixer[authn]      mTLS handshake verified peer=spiffe://cluster.local/ns/default/sa/default cipher=TLS_AES_256_GCM_SHA384
2026-05-20T15:42:11.237Z INFO  app.config-svc[req]     GET /check-update HTTP/1.1 trace_id=4a3b... span_id=8f1c...
2026-05-20T15:42:11.238Z DEBUG app.config-svc[headers] User-Agent="okhttp/4.12" X-Client-Role="admin" Accept="*/*"
2026-05-20T15:42:11.239Z INFO  app.config-svc[authz]   role check header=X-Client-Role value=admin decision=ALLOW policy=admin-only.v1
2026-05-20T15:42:11.240Z INFO  app.config-svc[config]  loading config.xml path=/app/config.xml version=2 size=348B
2026-05-20T15:42:11.241Z INFO  app.config-svc[encode]  serialized response content-type=application/json bytes=412
2026-05-20T15:42:11.242Z INFO  envoy.http[main]        response sent status=200 duration_ms=8 upstream_service_time=7
2026-05-20T15:42:11.243Z INFO  istio.proxy[outbound]   egress recorded bytes_sent=412 bytes_received=0
```

For `user` the `authz` line reads `decision=DENY policy=admin-only.v1` and the request short-circuits to `204`.

Plus on startup: banner with version, listening addr, "envoy sidecar attached (simulated)". Plus a heartbeat every 30 s with goroutine count and memstats. ANSI colors (INFO green, DEBUG cyan, WARN yellow, ERROR red), toggleable via `LOG_COLOR=false`.

## 8. Files to create / modify

### Create
- `server/main.go`
- `server/go.mod`
- `server/config.xml`
- `server/Dockerfile`
- `server/DEPLOY.md`
- `app/src/main/java/com/example/sentenix_proto_1/update/UpdateClient.java`
- `app/src/main/java/com/example/sentenix_proto_1/update/ConfigApplier.java`
- `app/src/main/java/com/example/sentenix_proto_1/update/ConfigStore.java`

### Modify
- `app/build.gradle.kts` — add OkHttp dependency.
- `app/src/main/res/layout/fragment_home.xml` — add toggle + Check-for-update button; hide SOS by default.
- `app/src/main/java/com/example/sentenix_proto_1/Fragments/HomeFragment.java` — wire new controls, dialogs, persistence.

## 9. Phase plan

| # | Phase | Acceptance check |
|---|---|---|
| 1 | Add OkHttp dependency | `./gradlew :app:dependencies` lists `okhttp`. |
| 2 | Build Go server + Dockerfile + logging | `curl -H 'X-Client-Role: admin' localhost:8080/check-update` returns 200 JSON; logs show the 10-line stream. `user` returns 204 with DENY log. |
| 3 | XML config schema + sample response | `server/config.xml` validates against schema in §4; server embeds it correctly in JSON. |
| 4 | Add toggle + Check-for-update buttons to `fragment_home.xml` | Layout renders without overlap; new view IDs `roleToggle`, `checkUpdateButton` exist. |
| 5 | Wire HTTP call + checking/update dialogs | Tapping Check-for-update shows "Checking" dialog for ≥4 s, then either "Update Available" dialog or "No updates" toast. |
| 6 | XML parser + ConfigApplier | Calling `ConfigApplier.apply(xml, rootView)` mutates SOS visibility, colors, text on screen. |
| 7 | Persist applied config via SharedPreferences | Killing the app and reopening preserves the applied changes. |
| 8 | Default state: hide SOS button initially | Fresh install shows no SOS button; appears only after an admin update. |
| 9 | Local end-to-end test | Manual: Docker container + emulator demonstrate full flow including persistence. |
| 10 | AWS EC2 deployment guide | `server/DEPLOY.md` is a working step-by-step that a fresh user can follow. |

## 10. Open assumptions

- Local dev URL: `http://10.0.2.2:8080/check-update` (emulator loopback to host). EC2: swap to `http://<ec2-public-ip>:8080/check-update` via a single constant.
- Toggle is `SwitchCompat`, defaults to `user` on every launch (not persisted — it is a debug toggle).
- XML is parsed with the Android-built-in `XmlPullParser` (no new dependency).
- No changes to `AdminMain`, `PoliceMain`, login flow, or any Firebase logic.
- Plain HTTP, no TLS. Cleartext-traffic permission added to `network_security_config.xml` (Android 9+ blocks cleartext to arbitrary hosts by default).
