# Media Integration Checklist (Step 8)

This checklist is for the final implementation step in `Media-PLAN.md`: joint debugging hardening, recovery behavior, sample configuration, and operational acceptance.

## 1. Prerequisites

- Backend is running (`flexboot4-bootstrap` or a deployment that includes `flexboot4-media-starter`)
- A valid admin JWT token is available
- At least one configured and enabled `MediaServer` (ZLM)
- At least one configured and enabled `MediaGateway` (GB28181)
- Optional for full coverage:
  - a real GB28181 device
  - a fixed-address RTSP channel
  - an upstream GB platform for cascade verification

## 2. Functional Acceptance Matrix

### 2.1 Media Server and Hook

1. Call `POST /api/admin/media/server/{id}/test` or `POST /api/admin/media/server/test`
2. Verify result:
   - `code == 0`
   - `data.success == true`
3. Trigger stream publish/unpublish on ZLM
4. Verify hook callbacks update `media_stream_session` and channel play status

### 2.2 GB28181 Gateway

1. Start gateway: `POST /api/admin/media/gateway/{id}/start`
2. Verify `runtimeStatus == RUNNING`
3. Device registers to gateway
4. Verify device/channel records are created or updated

### 2.3 Device/Channel Live, Playback, PTZ, Snapshot

1. Start live: `POST /api/admin/media/channel/live`
2. Verify session created and channel `playStatus == ONLINE`
3. Stop live: `POST /api/admin/media/channel/live/stop/{sessionId}`
4. Verify session closed and channel returns to `STOPPED`
5. Playback query: `POST /api/admin/media/channel/playback/query`
6. Start/stop playback if records exist
7. PTZ command: `POST /api/admin/media/channel/ptz` (supported devices should return `true`)
8. Snapshot upload from frontend succeeds (`bizType=media_snapshot`)

### 2.4 Screen Layout

1. Save layout: `POST /api/admin/media/screen/save-layout`
2. Query detail: `GET /api/admin/media/screen/{id}/detail`
3. Verify 1/4/9/custom layouts can be persisted and replayed

### 2.5 Cascade

1. Bind channels: `POST /api/admin/media/cascade/bind`
2. Register cascade: `POST /api/admin/media/cascade/{id}/register`
3. Verify platform status is written by REGISTER response handling
4. Verify upstream platform can discover bound channels

## 3. Recovery and Hardening Drills

The following drills validate the runtime maintenance logic (`MediaRuntimeMaintenanceTask`).

### Drill A: ZLM hook timeout

1. Stop ZLM hook callbacks or isolate network
2. Wait longer than `media.server-hook-timeout-seconds`
3. Verify `media_server.status == OFFLINE` and `lastError` contains timeout hint

### Drill B: GB device keepalive timeout

1. Disconnect GB device keepalive traffic
2. Wait longer than `media.device-keepalive-timeout-seconds`
3. Verify:
   - device `onlineStatus == OFFLINE`
   - channels moved to `OFFLINE/STOPPED`

### Drill C: Gateway runtime drift

1. Force in-memory runtime loss (process restart or runtime drop) while DB still marks gateway `RUNNING`
2. Wait one maintenance cycle
3. Verify:
   - if `media.gateway-auto-recover=true`, gateway is restarted automatically
   - otherwise gateway transitions to `ERROR`

### Drill D: Zombie session cleanup

1. Create stale `PENDING` or `STREAMING` sessions (or simulate timeout conditions)
2. Wait beyond configured session timeout
3. Verify sessions are closed/failed and channel play status is corrected

## 4. Suggested Go-Live Criteria

- All matrix checks pass
- All drills pass
- No orphan `STREAMING` session remains after stop/timeout drills
- Gateway restart and media server outage behavior are understood by ops

## 5. Fast Smoke Script

Use:

- `scripts/media-integration-smoke.ps1`

The script executes a small subset of high-value checks using actual API calls.

