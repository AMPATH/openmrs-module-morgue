# Morgue Module — Storage Management API

This document describes the REST endpoints for the morgue **storage unit**, **compartment**, and **storage assignment** resources, added on top of the existing `morgue_deceased`/patient endpoints.

All endpoints are served under:

```
/openmrs/ws/rest/v1/morgue
```

Standard OpenMRS REST conventions apply:
- Representations: `?v=default`, `?v=full`, `?v=ref`, or a custom `?v=custom:(prop1,prop2)`
- Pagination: `?limit=` and `?startIndex=`
- All resources require authentication; requests must carry a valid session or Basic Auth credentials.
- All endpoints support CORS (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`).

---

## 1. Storage Unit

Represents a physical storage unit (e.g. a fridge/freezer bank) tied to a `Location`.

**Base path:** `/morgue/storage-unit`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/morgue/storage-unit` | List all (non-voided) storage units |
| `GET` | `/morgue/storage-unit/{uuid}` | Get a single storage unit by UUID |
| `POST` | `/morgue/storage-unit` | Create a new storage unit |
| `POST` | `/morgue/storage-unit/{uuid}` | Update an existing storage unit |
| `DELETE` | `/morgue/storage-unit/{uuid}` | Void a storage unit (requires `?reason=`) |
| `DELETE` | `/morgue/storage-unit/{uuid}?purge=true` | Permanently delete a storage unit |

### Representation (default)

```json
{
  "uuid": "string",
  "display": "string",
  "location": { "uuid": "string", "display": "string" }
}
```

`?v=full` additionally includes `auditInfo` (creator, dateCreated, changedBy, dateChanged, voided info).

### Create example

```http
POST /openmrs/ws/rest/v1/morgue/storage-unit
Content-Type: application/json

{
  "name": "Fridge Bank A",
  "location": "a1b2c3d4-uuid-of-location"
}
```

---

## 2. Compartment

Represents an individual compartment within a storage unit. Each compartment belongs to exactly one storage unit.

**Base path:** `/morgue/compartment`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/morgue/compartment` | List all (non-voided) compartments |
| `GET` | `/morgue/compartment?storageUnit={uuid}` | List compartments for a given storage unit |
| `GET` | `/morgue/compartment/{uuid}` | Get a single compartment by UUID |
| `POST` | `/morgue/compartment` | Create a new compartment |
| `POST` | `/morgue/compartment/{uuid}` | Update an existing compartment |
| `DELETE` | `/morgue/compartment/{uuid}` | Void a compartment (requires `?reason=`) |
| `DELETE` | `/morgue/compartment/{uuid}?purge=true` | Permanently delete a compartment |

### Representation (default)

```json
{
  "uuid": "string",
  "display": "string",
  "status": "VACANT",
  "storageUnit": { "uuid": "string", "display": "string" }
}
```

`status` is computed from active storage assignments and is always either `VACANT` or `OCCUPIED`.

### Create example

```http
POST /openmrs/ws/rest/v1/morgue/compartment
Content-Type: application/json

{
  "name": "Compartment 4",
  "storageUnit": "e5f6g7h8-uuid-of-storage-unit"
}
```

### Search example

```http
GET /openmrs/ws/rest/v1/morgue/compartment?storageUnit=e5f6g7h8-uuid-of-storage-unit
```

---

## 3. Storage Assignment

Represents a patient's occupancy of a compartment — i.e. an "admission" and "discharge" record for the morgue. This resource enforces business rules rather than being a plain CRUD wrapper:

- A patient **cannot** have two active (non-discharged) assignments at once.
- A compartment **cannot** hold two active assignments at once.

**Base path:** `/morgue/storage-assignment`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/morgue/storage-assignment/{uuid}` | Get a single assignment by UUID |
| `GET` | `/morgue/storage-assignment?patient={uuid}` | List all assignments for a patient |
| `POST` | `/morgue/storage-assignment` | **Assign** a patient to a compartment (new record, no `uuid`) |
| `POST` | `/morgue/storage-assignment/{uuid}` | **Discharge** an existing assignment |
| `DELETE` | `/morgue/storage-assignment/{uuid}` | Void an assignment (requires `?reason=`); also discharges it |
| `DELETE` | `/morgue/storage-assignment/{uuid}?purge=true` | Permanently delete an assignment |

### Representation (default)

```json
{
  "uuid": "string",
  "patient": { "uuid": "string", "display": "string" },
  "compartment": { "uuid": "string", "display": "string" },
  "dateAdmitted": "2026-09-16T10:00:00.000+0300",
  "dateDischarged": null,
  "status": "OCCUPIED"
}
```

### Assign a patient (create)

```http
POST /openmrs/ws/rest/v1/morgue/storage-assignment
Content-Type: application/json

{
  "patient": "patient-uuid-here",
  "compartment": "compartment-uuid-here"
}
```

Routes through `MorgueService.assignPatientToCompartment(...)`. Returns **400/APIException** if:
- the patient already has an active assignment, or
- the compartment is already occupied.

### Discharge an assignment (update)

```http
POST /openmrs/ws/rest/v1/morgue/storage-assignment/{uuid}
Content-Type: application/json

{}
```

Routes through `MorgueService.dischargeAssignment(...)`, which sets `dateDischarged` to now and `status` to `DISCHARGED`. Returns **400/APIException** if the assignment was already discharged.

### List a patient's assignment history

```http
GET /openmrs/ws/rest/v1/morgue/storage-assignment?patient=patient-uuid-here
```

Returns all (non-voided) assignments for the patient, past and present.

### List assignments by location

```http
GET /openmrs/ws/rest/v1/morgue/storage-assignment?location=location-uuid-here&status=OCCUPIED&admittedOnOrAfter=2026-09-01
```

`location` is required and accepts a location UUID. The optional filters are:

| Parameter | Description |
|---|---|
| `status` | Filter by `OCCUPIED` or `DISCHARGED` |
| `createdOnOrAfter` | Include assignments created at or after this date |
| `admittedOnOrAfter` | Include assignments admitted at or after this date |
| `admittedOnOrBefore` | Include assignments admitted at or before this date |

The response includes the patient and compartment references:

```json
[
  {
    "uuid": "assignment-uuid",
    "patient": { "uuid": "patient-uuid", "display": "Patient Name" },
    "compartment": { "uuid": "compartment-uuid", "display": "Compartment 4" },
    "dateAdmitted": "2026-09-16T10:00:00.000+0300",
    "dateDischarged": null,
    "status": "OCCUPIED"
  }
]
```

---

## Status values

The `status` column is a free-text `varchar`, backed by application-level constants rather than a DB-level enum:

| Value | Meaning |
|---|---|
| `OCCUPIED` | Assignment is active — patient currently occupies the compartment |
| `DISCHARGED` | Patient has been discharged from the compartment |
| `VACANT` | Computed compartment status — no active assignment exists |

---

## Data model reference

```
morgue_storage_unit
  └── location_id → location(location_id)

morgue_compartment
  └── storage_unit_id → morgue_storage_unit(storage_unit_id)

morgue_storage_assignment
  ├── compartment_id → morgue_compartment(compartment_id)
  └── patient_id → patient(patient_id)
```

All three tables follow the standard OpenMRS `BaseOpenmrsData` audit pattern (`uuid`, `creator`, `date_created`, `changed_by`, `date_changed`, `voided`, `voided_by`, `date_voided`, `voided_reason`).

---

## Known gaps / follow-ups

- `MorgueStorageAssignmentResource.save()` currently distinguishes create vs. discharge purely by whether an `id`/`uuid` is present — richer update semantics (e.g. transferring a patient to a different compartment) are not yet supported.
- Privilege-based access control (`@Authorized` with a named privilege, e.g. `Manage Morgue Storage`) has not yet been scoped — currently just requires authentication.