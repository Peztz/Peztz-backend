# SmartThings cage sensor integration

## Data flow

```text
Zigbee sensor
  -> SmartThings Station
  -> SmartThings Cloud API
  -> SmartThings device health check
  -> Spring smartthings module
  -> sensor_reading (every unique raw measurement)
  -> pet_logs (meaningful transitions while an admission session is active)
```

The backend never connects to Zigbee directly. `smartthings_device` maps the
SmartThings Cloud device ID to the existing `cage.cage_id`.

## Verified devices

| Type | SmartThings device ID | Capability / attribute |
|---|---|---|
| Illuminance | `f629bdb6-304d-42db-8954-428621a80fae` | `illuminanceMeasurement.illuminance` |
| Contact | `4667bc8a-35e5-4c0b-9ab0-cdb16e0edbc3` | `contactSensor.contact` |

Temperature/humidity parsing is also supported through
`temperatureMeasurement.temperature` and
`relativeHumidityMeasurement.humidity` for a future compatible sensor.

## Database preparation

For an existing Peztz database, apply
`docs/sql/smartthings_sensor_migration.sql` before starting the updated Spring
application. The broader `peztz_domain_migration.sql` also contains these
objects for a full domain deployment, but should not be rerun only to add the
sensor feature. The application uses `ddl-auto=validate` and does not create
the tables automatically.

The migration adds:

- `smartthings_device`: SmartThings device-to-cage mapping and health metadata
- `sensor_reading`: deduplicated raw sensor history, including the original JSON

No migration was applied automatically by the application or by the test suite.

## Environment variables

```text
SMARTTHINGS_ACCESS_TOKEN=<SmartThings PAT>
SMARTTHINGS_POLLING_ENABLED=false
SMARTTHINGS_POLLING_INTERVAL_MS=60000
SMARTTHINGS_LOW_LIGHT_THRESHOLD_LUX=50
```

For Docker Compose, put the real values in the Git-ignored `infra/.env` file.
The Compose configuration passes all four settings to the Spring container.
After replacing an expired PAT or changing the polling settings, recreate the
Spring container so that it reads the new environment:

```shell
cd infra
docker compose up -d --force-recreate spring
```

Polling is disabled by default. Keep it disabled while the Station is offline.
Use the manual sync API for the first live verification, and enable polling only
after both sensors return current timestamps.

The PAT must remain in an environment variable. Do not store it in PostgreSQL,
Git, frontend code, or API request bodies. The current PAT approach is suitable
for development/demo use. Newly issued PATs expire after 24 hours, so replace
the value in `infra/.env` and recreate the Spring container before a demo.
Multi-customer production onboarding should use SmartThings OAuth.

## API sequence

All examples use the PEZTZ login access token in `Authorization`. This is not the
SmartThings PAT.

### 1. Link the illuminance sensor to a cage

```http
POST /api/smartthings/cages/{cageId}/devices
Authorization: Bearer {peztzAccessToken}
Content-Type: application/json

{
  "deviceId": "f629bdb6-304d-42db-8954-428621a80fae",
  "deviceType": "ILLUMINANCE",
  "label": "Cage illuminance sensor"
}
```

### 2. Link the contact sensor to the same cage

```http
POST /api/smartthings/cages/{cageId}/devices
Authorization: Bearer {peztzAccessToken}
Content-Type: application/json

{
  "deviceId": "4667bc8a-35e5-4c0b-9ab0-cdb16e0edbc3",
  "deviceType": "CONTACT",
  "label": "Cage door sensor"
}
```

Registering the same device for the same cage updates its type/label and
reactivates it. Linking it to a different cage returns `409 Conflict`.

### 3. Fetch and store one live value

```http
POST /api/smartthings/devices/{deviceId}/sync
Authorization: Bearer {peztzAccessToken}
```

The response reports how many new readings were saved. A repeated SmartThings
measurement timestamp returns `savedReadingCount: 0` and does not duplicate the
row or event.

### 4. Read current cage state

```http
GET /api/smartthings/cages/{cageId}/readings/latest
Authorization: Bearer {peztzAccessToken}
```

### 5. Read cage sensor history

```http
GET /api/smartthings/cages/{cageId}/readings?limit=100
Authorization: Bearer {peztzAccessToken}
```

Optional `from` and `to` values must be supplied together as ISO-8601 timestamps.
The maximum `limit` is 500.

## Derived session events

Raw readings are stored even when the cage has no active admission session.
When an active session exists, these transitions are also stored in `pet_logs`:

| Condition | `pet_logs.log_type` |
|---|---|
| Contact changes to open | `DOOR_OPEN` |
| Contact changes from open to closed | `DOOR_CLOSED` |
| Illuminance crosses below the configured threshold | `LOW_LIGHT` |
| Illuminance crosses back above the threshold | `LIGHT_RECOVERED` |

A reading is linked to the active admission session only when its SmartThings
measurement timestamp is within that session's time range. Older cached
measurements remain available as raw readings with a null `session_id`, but do
not create derived `pet_logs` events. Out-of-order measurements are also kept as
raw history without generating backward transition events.

Because the existing report query reads `pet_logs` by cage/session, these
meaningful SmartThings events are available to the current report pipeline.
The `camera_id` remains null for sensor-originated events, so existing
camera-only event APIs continue to return camera events only.

## Offline behavior

Before fetching device status, the backend calls the SmartThings device health
endpoint. Only the `ONLINE` state is ingested; `OFFLINE`, `UNHEALTHY`, or an
unknown state marks the mapping offline and does not ingest a cached status.
When the Station cannot reach its configured Wi-Fi, no fresh Zigbee state is
uploaded to SmartThings Cloud. Development can continue with the JSON fixtures under
`spring-server/src/test/resources/smartthings`; live verification should be done
after the Station reconnects to its original network.
