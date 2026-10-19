# Task 3 — Data model and API list

## Data model: ColdChainEvent

| Field | Type | Description |
|---|---|---|
| id | Long | Generated primary key |
| unitId | String | Identifier for the cold-storage unit |
| location | String | Human-readable storage location |
| temperatureC | Double | Recorded temperature in Celsius |
| minTempC | Double | Configured lower limit for this event |
| maxTempC | Double | Configured upper limit for this event |
| recordedAt | LocalDateTime | Time the event was recorded |
| notes | String | Optional observation |
| status | Derived String | NORMAL, LOW, or HIGH; calculated from the limits |

A reading is **LOW** when temperature is below `minTempC`, **HIGH** when it is above `maxTempC`, and **NORMAL** when it is within the inclusive range.

## Web pages

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | Dashboard, search, filters, summary, event entry form |
| POST | `/events` | Save a temperature event |
| GET | `/events/{id}` | View event details |
| GET | `/alerts` | View all out-of-range events |

## REST endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/health` | Health response for deployment checks |
| GET | `/api/events` | List events; optional `q` and `status` query parameters |
| POST | `/api/events` | Create an event using a JSON body |
| GET | `/api/summary` | Return total readings, unit count, normal readings, and exceptions |

## Example POST `/api/events` body

```json
{
  "unitId": "VAX-005",
  "location": "Vaccine Store E",
  "temperatureC": 5.1,
  "minTempC": 2.0,
  "maxTempC": 8.0,
  "notes": "Routine manual check"
}
```

## Example local checks
- `http://localhost:8080/`
- `http://localhost:8080/alerts`
- `http://localhost:8080/health`
- `http://localhost:8080/api/events`
- `http://localhost:8080/api/summary`
