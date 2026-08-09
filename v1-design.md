# Job Scheduler — V1 Design

## 1. V1 Goal

The goal of V1 is to answer:

> **Can we reliably schedule a task for a future time, execute it, and track what happened?**

V1 is intentionally a simple, single-instance implementation. We are **not** trying to solve the 10K jobs/sec distributed scheduling problem yet.

### V1 scope

```text
Create Job
     ↓
Persist Job
     ↓
Scheduler detects due Job
     ↓
Put execution into delayed queue
     ↓
Worker executes Task
     ↓
Persist Execution result
```

V1 will support:

* One-time jobs
* Recurring jobs
* Job cancellation
* Job status
* Execution history
* Basic retries
* Multiple tasks
* Concurrent workers within a single application

V1 will **not** support:

* Multiple scheduler instances
* Distributed workers across machines
* Kafka/SQS
* Kubernetes
* Database sharding
* Leader election
* Distributed locks
* Multi-region deployment
* Authentication

---

# 2. V1 Architecture

```text
                         ┌───────────────┐
                         │    Client     │
                         └───────┬───────┘
                                 │
                                 ▼
                         ┌───────────────┐
                         │   REST API    │
                         └───────┬───────┘
                                 │
                                 ▼
                       ┌───────────────────┐
                       │    PostgreSQL     │
                       │                   │
                       │  Tasks            │
                       │  Jobs             │
                       │  Executions       │
                       └─────────┬─────────┘
                                 │
                                 │ Poll upcoming jobs
                                 ▼
                       ┌───────────────────┐
                       │    Scheduler      │
                       └─────────┬─────────┘
                                 │
                                 │ enqueue
                                 ▼
                       ┌───────────────────┐
                       │ Delayed Queue     │
                       │ Java DelayQueue   │
                       └─────────┬─────────┘
                                 │
                                 │ execution becomes due
                                 ▼
                       ┌───────────────────┐
                       │      Worker       │
                       │   Thread Pool     │
                       └─────────┬─────────┘
                                 │
                                 ▼
                              Task
```

There are five logical components:

1. REST API
2. PostgreSQL
3. Scheduler
4. Delayed Queue
5. Worker Pool

---

# 3. Important V1 Design Decision

## PostgreSQL is the source of truth

The in-memory queue is **not durable**.

This is intentional.

```text
PostgreSQL
    ↓
Source of truth

In-memory queue
    ↓
Temporary execution mechanism
```

If the application crashes:

```text
Application crashes
       ↓
In-memory queue lost
       ↓
Application restarts
       ↓
Scheduler reads PostgreSQL
       ↓
Recreates pending executions
```

This gives us a useful architectural property:

> The queue is a performance/execution mechanism, not our durable storage.

This also gives us a natural path to V2, where the in-memory queue can be replaced with a durable distributed queue.

---

# 4. Core Entities

V1 has three primary entities:

```text
Task
Job
Execution
```

---

## 4.1 Task

A `Task` represents executable work.

```text
Task
-------------------------
id
name
type
created_at
```

Example:

```text
id: 101
name: send-welcome-email
type: EMAIL
```

For V1, we don't need a sophisticated task registry.

We can define a Java interface:

```java
public interface TaskHandler {

    void execute(Map<String, Object> parameters);
}
```

Example handlers:

```text
SendEmailTask
GenerateReportTask
CleanupTask
```

---

# 5. Job

A `Job` represents the scheduling definition.

```text
Job
-------------------------
id
task_id
schedule_type
scheduled_at
cron_expression
parameters
status
created_at
updated_at
```

### Schedule types

```text
ONE_TIME
RECURRING
```

### One-time example

```text
Task:
generate-report

Scheduled:
2026-08-10 10:00:00
```

### Recurring example

```text
Task:
generate-report

Cron:
0 0 10 * * ?
```

The exact cron format/library will be finalized during implementation.

---

# 6. Execution

An important distinction:

> A Job is not an Execution.

For example:

```text
Job:
Daily Report
```

can create multiple executions:

```text
Execution #1 → Aug 8
Execution #2 → Aug 9
Execution #3 → Aug 10
Execution #4 → Aug 11
```

Relationship:

```text
Job 1
 │
 ├── Execution 101
 ├── Execution 102
 ├── Execution 103
 └── Execution 104
```

### Execution fields

```text
Execution
-------------------------
id
job_id
scheduled_at
started_at
completed_at
status
attempt
error_message
created_at
updated_at
```

---

# 7. Execution State Machine

V1 execution states:

```text
PENDING
RUNNING
COMPLETED
FAILED
CANCELLED
```

State transition:

```text
                 ┌─────────────┐
                 │   PENDING   │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │   RUNNING   │
                 └──────┬──────┘
                        │
              ┌─────────┴─────────┐
              │                   │
              ▼                   ▼
         ┌───────────┐       ┌──────────┐
         │ COMPLETED │       │  FAILED  │
         └───────────┘       └────┬─────┘
                                  │
                                  │ retry?
                                  ▼
                               PENDING
```

### Retry semantics

Retries belong to the same Execution.

Example:

```text
Execution 100
    attempt 1 → FAILED
    attempt 2 → FAILED
    attempt 3 → SUCCESS
```

This is different from:

```text
Execution 100
Execution 101
Execution 102
```

Those represent different scheduled occurrences.

---

# 8. Job State

Job state is separate from Execution state.

V1 job states:

```text
SCHEDULED
CANCELLED
COMPLETED
```

For recurring jobs, the Job remains `SCHEDULED` while individual executions complete or fail.

Example:

```text
Daily Report Job
     │
     ├── Aug 8  → SUCCESS
     ├── Aug 9  → SUCCESS
     ├── Aug 10 → FAILED
     └── Aug 11 → PENDING
```

The Job itself remains:

```text
SCHEDULED
```

A recurring Job does not become `COMPLETED` after a single successful execution.

---

# 9. Scheduling Flow

## One-time Job

Client:

```http
POST /api/v1/jobs
```

API:

```text
1. Validate request
2. Create Job
3. Persist Job
4. Create Execution
5. Return Job
```

Scheduler then ensures the Execution reaches the delayed queue.

```text
Database
    │
    │ upcoming executions
    ▼
Scheduler
    │
    ▼
Delayed Queue
```

---

# 10. Why Do We Need Both Job and Execution?

Consider:

```text
Job:
"Run report every day at 10 AM"
```

The scheduler needs to know:

```text
What is the next occurrence?
```

The system also needs to record:

```text
What happened yesterday?
What happened today?
Did today's execution fail?
How long did it take?
```

Therefore:

```text
Job
 │
 ├── Schedule definition
 │
 └── Execution history
```

This separation becomes even more important when retries and distributed scheduling are introduced in later versions.

---

# 11. Scheduler

V1 has **one Scheduler instance**.

The Scheduler runs periodically:

```text
Every N seconds
       │
       ▼
Query PostgreSQL
       │
       ▼
Find upcoming executions
       │
       ▼
Put into Delayed Queue
```

The Scheduler is **not responsible for executing tasks**.

Its responsibility is:

> **Move scheduled work toward execution.**

This keeps scheduling and execution concerns separate.

---

# 12. V1 Scheduling Window

For V1:

```text
Lookahead window = 1 minute
```

Example:

```text
Current time: 10:00

Scheduler looks for:
10:00 → 10:01
```

We are intentionally using a smaller window than the production-oriented design described in the system-design article.

The important concept is the same:

> The Scheduler discovers upcoming work and places it into a mechanism that handles the actual execution timing.

We can later move to a 5-minute window when introducing a durable delayed queue.

---

# 13. Delayed Queue

The delayed queue is represented by an abstraction:

```java
public interface DelayedQueue {

    void schedule(Execution execution);

    Execution take();
}
```

The initial implementation will use Java's `DelayQueue`.

Conceptually:

```text
DelayedQueue
      │
      ├── Execution A → due in 20 sec
      ├── Execution B → due in 5 sec
      └── Execution C → due now
                              │
                              ▼
                           Worker
```

`DelayQueue` is a good fit for V1 because it directly models:

> Give me an item only when its delay has expired.

---

# 14. Worker Pool

V1 uses a thread pool for concurrent execution.

```text
                  DelayedQueue
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
       Worker 1     Worker 2     Worker 3
```

Implementation can use:

```java
ExecutorService executorService;
```

or a Spring-managed thread pool.

### Worker flow

```text
1. Take Execution
2. Mark RUNNING
3. Find TaskHandler
4. Execute Task
5. Mark COMPLETED
```

On failure:

```text
1. Catch exception
2. Increment attempt
3. Retry if allowed
4. Otherwise mark FAILED
```

---

# 15. Retry Strategy

For V1:

```text
Maximum attempts = 3
```

Example:

```text
Attempt 1
    ↓
5 sec
    ↓
Attempt 2
    ↓
10 sec
    ↓
Attempt 3
```

The exact retry configuration can be made configurable later.

For V1, the important objective is to establish the retry lifecycle.

---

# 16. API Design

Keep the initial API intentionally small.

## Create Job

```http
POST /api/v1/jobs
```

## Get Job

```http
GET /api/v1/jobs/{jobId}
```

## Cancel Job

```http
POST /api/v1/jobs/{jobId}/cancel
```

## Get Executions

```http
GET /api/v1/jobs/{jobId}/executions
```

Additional APIs can be introduced when the corresponding requirements arise.

---

# 17. Example Create Request

## One-time Job

```json
{
  "taskName": "generate-report",
  "scheduleType": "ONE_TIME",
  "scheduledAt": "2026-08-10T10:00:00Z",
  "parameters": {
    "reportType": "DAILY"
  }
}
```

## Recurring Job

```json
{
  "taskName": "generate-report",
  "scheduleType": "RECURRING",
  "cronExpression": "0 0 10 * * *",
  "parameters": {
    "reportType": "DAILY"
  }
}
```

---

# 18. Database Schema

V1 intentionally uses a straightforward PostgreSQL schema.

## tasks

```text
tasks
--------------------------------
id              BIGSERIAL PK
name            VARCHAR
type            VARCHAR
created_at      TIMESTAMP
```

## jobs

```text
jobs
--------------------------------
id              BIGSERIAL PK
task_id         BIGINT FK
schedule_type   VARCHAR
scheduled_at    TIMESTAMP NULL
cron_expression VARCHAR NULL
parameters      JSONB
status          VARCHAR
created_at      TIMESTAMP
updated_at      TIMESTAMP
```

## executions

```text
executions
--------------------------------
id              BIGSERIAL PK
job_id          BIGINT FK
scheduled_at    TIMESTAMP
started_at      TIMESTAMP NULL
completed_at    TIMESTAMP NULL
status          VARCHAR
attempt         INT
error_message   TEXT NULL
created_at      TIMESTAMP
updated_at      TIMESTAMP
```

### V1 database design deliberately does not include

* Time-bucket partitioning
* Write sharding
* Database replication
* Distributed database
* Complex indexes beyond those required by the V1 query patterns

Those will be introduced only when we evolve the system.

---

# 19. When Do We Create Executions?

## One-time Job

Create its Execution when the Job is created.

```text
Create Job
    ↓
Create Execution
    ↓
PENDING
```

## Recurring Job

Create the next Execution when the Scheduler processes the Job.

```text
Recurring Job
     ↓
Scheduler
     ↓
Calculate next occurrence
     ↓
Create Execution
```

This keeps recurring schedules manageable.

When we introduce a distributed Scheduler, we will revisit this because duplicate execution creation becomes a major concern.

---

# 20. V1 Failure Recovery

The database is the source of truth, so the system can recover from application crashes.

Example:

```text
Scheduler
   ↓
DelayedQueue
   ↓
Application crashes
```

Everything in the in-memory queue is lost.

After restart:

```text
Application starts
      ↓
Scheduler starts
      ↓
Reads PostgreSQL
      ↓
Finds pending executions
      ↓
Re-enqueues them
```

This provides basic recovery without requiring a durable message broker.

---

# 21. Complete V1 Architecture

```text
                           Client
                             │
                             ▼
                      ┌─────────────┐
                      │ REST API    │
                      └──────┬──────┘
                             │
                             ▼
                  ┌────────────────────┐
                  │     PostgreSQL     │
                  │                    │
                  │ Tasks              │
                  │ Jobs               │
                  │ Executions         │
                  └─────────┬──────────┘
                            │
                            │ poll
                            ▼
                  ┌────────────────────┐
                  │     Scheduler      │
                  │                    │
                  │ Single instance    │
                  └─────────┬──────────┘
                            │
                            │ schedule
                            ▼
                  ┌────────────────────┐
                  │    DelayedQueue    │
                  │    Java DelayQueue │
                  └─────────┬──────────┘
                            │
                            │ due
                            ▼
                  ┌────────────────────┐
                  │    Worker Pool     │
                  │                    │
                  │ W1  W2  W3  ...    │
                  └─────────┬──────────┘
                            │
                            ▼
                       TaskHandler
```

---

# 22. What V1 Teaches Us

V1 gives us hands-on experience with:

```text
Domain modeling
      ↓
Scheduling
      ↓
Concurrency
      ↓
Thread pools
      ↓
Delayed execution
      ↓
Persistence
      ↓
Retries
      ↓
Failure recovery
      ↓
Idempotency
```

The goal is not merely to implement these features.

We should also identify where this architecture starts to break.

That will drive the subsequent versions.

---

# 23. V1 → V2 Evolution

## V1 limitation: In-memory queue

The queue is lost when the application crashes.

```text
V1
In-memory DelayQueue
        ↓
Application crash
        ↓
Queued work lost
```

### V2

Replace it with a durable/distributed delayed queue.

```text
V2
PostgreSQL
    ↓
Scheduler
    ↓
Durable Queue
    ↓
Workers
```

---

## V2 limitation: Single Scheduler

A single Scheduler is a single point of failure.

```text
V2
       Scheduler
           │
           ▼
         Queue
```

### V3

Introduce multiple Scheduler instances.

```text
V3

              ┌── Scheduler 1
Database ─────┼── Scheduler 2
              └── Scheduler 3
```

This introduces the duplicate-scheduling problem.

---

## V3 limitation: Duplicate scheduling

Multiple schedulers may discover the same execution.

```text
Scheduler 1 ──┐
              ├── Same Execution
Scheduler 2 ──┘
```

### V4

Introduce coordination mechanisms such as:

* Partition ownership
* Distributed locking
* Leader election

The exact solution will be decided when we reach V4.

---

## V4 limitation: Database scalability

At sufficiently high throughput, the database becomes a bottleneck.

```text
V4
Database
   ↓
High write/read volume
   ↓
Bottleneck
```

### V5

Introduce scalable database access patterns:

```text
Time buckets
     ↓
Efficient time-based queries
     ↓
Potential hot partitions
     ↓
Write sharding
```

---

## V5 Goal

Eventually we should be able to benchmark the system against workloads approaching:

```text
10K jobs/sec
```

rather than simply claiming that the architecture supports that scale.

---

# 24. V1 Decisions Summary

| Decision                 | V1                   |
| ------------------------ | -------------------- |
| Language                 | Java                 |
| Framework                | Spring Boot          |
| Database                 | PostgreSQL           |
| Scheduler                | Single instance      |
| Queue                    | Java `DelayQueue`    |
| Workers                  | Thread pool          |
| Execution guarantee      | At-least-once        |
| Retry                    | 3 attempts           |
| Job types                | One-time + recurring |
| Job persistence          | PostgreSQL           |
| Execution persistence    | PostgreSQL           |
| Queue durability         | No                   |
| API                      | REST                 |
| Authentication           | No                   |
| Distributed coordination | No                   |
| Kubernetes               | No                   |
| Database sharding        | No                   |
| Multi-region             | No                   |

---

# 25. V1 Definition of Done

V1 is considered complete when we can demonstrate the following end-to-end scenarios:

### Scenario 1 — One-time successful job

```text
Create Job
    ↓
Job persisted
    ↓
Execution created
    ↓
Scheduler discovers it
    ↓
Execution enters DelayQueue
    ↓
Worker executes it
    ↓
Execution = COMPLETED
```

### Scenario 2 — Failed job with retry

```text
Execution
    ↓
Attempt 1 → FAILED
    ↓
Retry
    ↓
Attempt 2 → FAILED
    ↓
Retry
    ↓
Attempt 3 → SUCCESS
```

### Scenario 3 — Recurring job

```text
Recurring Job
    ↓
Execution #1
    ↓
SUCCESS
    ↓
Execution #2
    ↓
SUCCESS
    ↓
Execution #3
```

### Scenario 4 — Job cancellation

```text
SCHEDULED Job
     ↓
Cancel
     ↓
CANCELLED
```

The cancelled Job must not produce new executions.

### Scenario 5 — Application restart

```text
Pending Execution
      ↓
Application crashes
      ↓
Application restarts
      ↓
Scheduler reads PostgreSQL
      ↓
Execution is re-enqueued
      ↓
Worker executes it
```

These scenarios will form the foundation of our V1 integration tests.
