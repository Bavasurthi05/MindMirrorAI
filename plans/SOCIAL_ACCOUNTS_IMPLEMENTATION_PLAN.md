# Social Accounts Integration Plan

## Status Summary

The current repository does not yet implement real social-account connection flows.

What exists today:
- A manual social-media analysis experience in the frontend that accepts pasted text.
- Backend analysis endpoints for social-content analysis.
- No OAuth-based social account linking, token storage, account sync, or import pipeline for posts/messages from external providers.

This plan covers implementing true social-account connectivity and ingestion for the MindMirror AI platform.

---

## Goals

1. Allow users to connect their social accounts securely.
2. Import relevant posts/content metadata for analysis and wellbeing insights.
3. Surface account status, sync progress, and privacy controls within the app.
4. Keep the implementation modular so additional providers can be added later.

---

## Scope

### In Scope for MVP
- Connect one or more social accounts through OAuth.
- Support at least one primary provider such as Instagram or X.
- Store only the minimum required access metadata securely.
- Sync recent posts/content into the app.
- Show connection status in the UI and allow disconnect/reconnect.
- Use social data for analysis and dashboard insights.

### Out of Scope for MVP
- Full historical backfill for every provider.
- Real-time streaming updates from social platforms.
- Bulk content moderation or full social listening dashboards.
- Deep platform-specific features beyond basic post import and analysis.

---

## Recommended MVP Providers

### Phase 1 MVP
- Instagram / Meta (via Meta Graph API)
- X / Twitter (via X API v2)

### Later Expansion
- LinkedIn
- Facebook Pages
- TikTok

For the first release, choose one provider with strong relevance to wellbeing analysis and less compliance complexity. Instagram is a good first candidate because it is widely used and aligns well with emotional/post sentiment analysis.

---

## High-Level Architecture

### Frontend
- Add a new Social Accounts page under the authenticated app.
- Allow users to click “Connect account”.
- Show connected-account cards with provider, status, last synced time, and actions such as reconnect and disconnect.
- Store the connection state through the existing authenticated API layer.

### Backend
- Add OAuth callback handling for supported providers.
- Store authorization tokens in a secure, encrypted form.
- Add services to refresh tokens when required.
- Add ingestion jobs that fetch recent content and normalize it into a common internal representation.
- Expose endpoints for:
  - listing connected accounts
  - connecting an account
  - disconnecting an account
  - triggering manual sync
  - retrieving imported social content

### Database
Add new persistence for:
- connected social accounts
- sync jobs
- imported social content entries
- provider-specific metadata

Recommended entities:
- SocialAccount
- SocialAccountConnection
- SocialPostImport
- SocialSyncJob

---

## Backend Implementation Plan

### Phase 1 — Foundation
1. Add new database migration for social-account tables.
2. Create domain entities and repositories for:
   - connected accounts
   - sync jobs
   - imported content
3. Add encrypted token storage support.
4. Add provider abstraction interfaces so the app can support multiple providers through one contract.

### Phase 2 — OAuth Flow
1. Implement OAuth start endpoint for the selected provider.
2. Implement OAuth callback endpoint to exchange authorization codes.
3. Validate state parameters to prevent CSRF.
4. Persist the account connection, provider metadata, and token payload.
5. Support basic token refresh for providers that require it.

### Phase 3 — Content Ingestion
1. Implement a provider adapter interface:
   - connect
   - fetch recent posts
   - normalize response into a common format
2. Add a scheduled or manual sync service.
3. Normalize imported content into:
   - provider
   - external id
   - content text
   - timestamp
   - media presence
   - metadata
4. Send content to the existing analysis pipeline for sentiment/emotion/triggers.

### Phase 4 — API Exposure
Add backend endpoints:
- GET /me/social-accounts
- POST /me/social-accounts/connect
- POST /me/social-accounts/{id}/sync
- DELETE /me/social-accounts/{id}
- GET /me/social-accounts/{id}/posts

---

## Frontend Implementation Plan

### Phase 1 — UI Shell
1. Create a Social Accounts page in the authenticated app.
2. Add a call-to-action card for “Connect account”.
3. Show empty-state messaging until at least one account is connected.

### Phase 2 — Connection UX
1. Add provider selection UI.
2. Redirect the user to the OAuth flow.
3. Handle the callback and show success/error feedback.
4. Show connected-account cards with provider name, account label, and sync status.

### Phase 3 — Sync and Management
1. Add a “Sync now” action.
2. Show progress and last sync timestamp.
3. Add disconnect and reconnect actions.
4. Add privacy messaging explaining what data is imported and why.

### Phase 4 — Insights Integration
1. Render imported social content in a dedicated section.
2. Surface analysis insights such as sentiment, emotion, and trigger patterns.
3. Optionally add a small widget on the dashboard summarizing recent social activity.

---

## Security and Privacy Requirements

1. Never store raw access tokens in plain text.
2. Use encrypted storage or a managed secret store.
3. Request the minimum permissions needed.
4. Show the user clear consent language before linking an account.
5. Support revocation of access from both the app and the provider console.
6. Apply rate limiting and audit logging around token exchange and sync operations.
7. Respect provider-specific API usage and privacy policies.

---

## Data Model Recommendation

### SocialAccount
Fields:
- id
- userId
- provider
- externalAccountId
- displayName
- profileImageUrl
- connectedAt
- lastSyncedAt
- status
- scopes
- tokenReference
- refreshTokenReference
- expiresAt

### SocialSyncJob
Fields:
- id
- accountId
- status
- startedAt
- completedAt
- itemCount
- errorMessage

### SocialPostImport
Fields:
- id
- accountId
- provider
- externalPostId
- contentText
- createdAt
- analysisStatus
- analysisResult
- metadataJson

---

## Integration Sequence

1. Create the backend database and domain model.
2. Implement the OAuth callback and secure token storage.
3. Add provider adapter and import jobs.
4. Expose APIs to the frontend.
5. Build the UI for connection and management.
6. Connect imported data to analytics and insights.
7. Add tests and error handling.

---

## Testing Plan

### Backend Tests
- OAuth callback success and failure cases
- Token encryption/decryption
- Sync job processing
- Import normalization and error handling

### Frontend Tests
- Connection success flow
- Empty-state rendering
- Connected account cards and actions
- Reconnect/disconnect behavior

### Integration Tests
- End-to-end flow from connect → sync → analysis output

---

## Suggested Delivery Milestones

### Milestone 1
- Single-provider OAuth connection flow
- Account listing and disconnect support

### Milestone 2
- Automatic sync of recent posts
- Imported content visible in the app

### Milestone 3
- Social content analysis integrated into insights and dashboard widgets

---

## Verification Criteria

The feature is complete when:
- a user can connect a supported social account successfully
- the app stores the connection securely
- recent posts are imported and visible
- the imported data can be analyzed and shown in the UI
- the user can disconnect and reconnect the account without errors
