# Job Scheduler — Architecture

## 1. Overview

The Job Scheduler architecture will evolve incrementally.

The system will begin with a simple scheduler and progressively introduce distributed-system components as requirements expose limitations.

The objective is not to start with a complex architecture.

The objective is to understand:

> Why does each architectural component exist?

## 2. Architectural Principles

### 2.1 Start Simple

The first implementation should contain the minimum number of components required to satisfy the initial requirements.

Avoid introducing:

- Kafka
- Redis
- Kubernetes
- Multiple databases
- Distributed locks

unless a concrete requirement justifies them.

### 2.2 Evolve Through Bottlenecks

Each major architectural evolution should follow:

```text
Requirement
    ↓
Current limitation
    ↓
Potential solution
    ↓
Trade-off analysis
    ↓
Implementation
```

### 2.3 Separate Job Definition from Execution

The architecture separates:

```text
Job
```

from:

```text
Execution
```

because one Job can produce many executions.

Example:

```text
Daily Report Job
        │
        ├── Execution 1
        ├── Execution 2
        ├── Execution 3
        └── ...
```

## 3. Core Domain Model

```text
┌─────────────┐
│    Task     │
├─────────────┤
│ id          │
│ name        │
│ type        │
└──────┬──────┘
       │
       │ 1
       │
       │ *
┌──────▼──────┐
│     Job     │
├─────────────┤
│ id          │
│ taskId      │
│ schedule    │
│ parameters  │
│ status      │
└──────┬──────┘
       │
       │ 1
       │
       │ *
┌──────▼──────────┐
│    Execution    │
├─────────────────┤
│ id              │
│ jobId           │
│ scheduledAt     │
│ startedAt       │
│ completedAt     │
│ status          │
│ attempt         │
│ error           │
└─────────────────┘
```

## 4. Architecture Evolution

```text
V1  Simple Scheduler
 ↓
V2  Job + Execution Model
 ↓
V3  Persistent Scheduling
 ↓
V4  Retry & Failure Handling
 ↓
V5  Delayed Queue
 ↓
V6  Multiple Workers
 ↓
V7  Distributed Scheduler
 ↓
V8  Horizontal Scaling
 ↓
V9  Observability
 ↓
V10 Load Testing & Hardening
```

## 5. V1 — Simple Scheduler

### Architecture

```text
                    ┌──────────────┐
                    │    Client    │
                    └──────┬───────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │  Schedule API   │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │   Job Store     │
                  │   Database      │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │    Scheduler    │
                  └────────┬────────┘
                           │
                           ▼
                  ┌─────────────────┐
                  │     Worker      │
                  └────────┬────────┘
                           │
                           ▼
                         Task
```

### Components

#### Client

Creates and manages Jobs.

#### Schedule API

Responsible for:

- Accepting scheduling requests.
- Validating input.
- Persisting Jobs.
- Returning Job information.

#### Job Store

Provides durable storage for:

- Jobs.
- Executions.
- Status.

The initial implementation will use PostgreSQL.

#### Scheduler

Periodically checks the Job Store for jobs that are due.

It determines which jobs should be executed.

#### Worker

Receives work from the Scheduler and executes the corresponding Task.

### V1 Data Flow

```text
Client
  │
  │ Create Job
  ▼
Schedule API
  │
  │ Persist
  ▼
Database
  │
  │ Scheduler polls
  ▼
Scheduler
  │
  │ Dispatch
  ▼
Worker
  │
  │ Execute
  ▼
Task
  │
  │ Result
  ▼
Database
```

### V1 Limitations

The simple architecture is intentionally limited.

Database polling introduces:

- Database load.
- Scheduling latency.
- Difficulty achieving tight scheduling precision.
- Potential duplicate scheduling when multiple scheduler instances are introduced.

## 6. V2 — Job and Execution Separation

The next architectural step formalizes the domain model.

The Job becomes the schedule definition, while Execution represents an individual run.

### Jobs

```text
Jobs
--------------------------------
id
task_id
schedule
parameters
status
created_at
updated_at
```

### Executions

```text
Executions
--------------------------------
id
job_id
scheduled_at
started_at
completed_at
status
attempt
error
created_at
updated_at
```

## 7. V3 — Persistent Scheduling

The Scheduler periodically queries the database for upcoming executions.

```text
                Database
                   │
                   │
             Find due jobs
                   │
                   ▼
              Scheduler
                   │
                   ▼
                Worker
```

The database becomes the durable source of scheduling information.

This allows the system to recover after a scheduler restart.

## 8. V3 Limitation — Scheduling Precision

Database polling alone may not satisfy:

```text
Execute within approximately 2 seconds
of scheduled time.
```

Example:

```text
Scheduler polling interval = 60 seconds

Job scheduled:
10:00:01

Scheduler polls:
10:01:00

Execution delay:
~59 seconds
```

This motivates a more precise delivery mechanism.

## 9. V4 — Delayed Queue Architecture

The architecture introduces a two-layer scheduling mechanism.

The database provides durable scheduling state.

A delayed message queue provides timely delivery.

```text
                         ┌──────────────┐
                         │    Client    │
                         └──────┬───────┘
                                │
                                ▼
                     ┌────────────────────┐
                     │    Schedule API    │
                     └─────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │      Job Store      │
                    │                     │
                    │ Jobs                │
                    │ Executions          │
                    └─────────┬───────────┘
                              │
                       Upcoming jobs
                              │
                              ▼
                    ┌─────────────────────┐
                    │      Scheduler      │
                    └─────────┬───────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │   Delayed Queue     │
                    └─────────┬───────────┘
                              │
                        Scheduled time
                              │
                              ▼
                    ┌─────────────────────┐
                    │      Workers        │
                    └─────────┬───────────┘
                              │
                              ▼
                             Task
```

## 10. Two-Layer Scheduling

### Database

Responsible for:

- Durable job definitions.
- Durable execution records.
- Recovery.
- Long-term scheduling state.

### Delayed Queue

Responsible for:

- Timely delivery.
- Buffering work.
- Decoupling Scheduler and Workers.
- Supporting distributed workers.

Conceptually:

```text
Database
    │
    │ Durable schedule
    ▼
Scheduler
    │
    │ Near-term executions
    ▼
Delayed Queue
    │
    │ At execution time
    ▼
Workers
```

## 11. V5 — Worker Pool

A single Worker becomes a bottleneck.

Workers can scale horizontally:

```text
                         ┌──────────────┐
                         │ Delayed Queue│
                         └──────┬───────┘
                                │
                  ┌─────────────┼─────────────┐
                  │             │             │
                  ▼             ▼             ▼
             ┌────────┐    ┌────────┐    ┌────────┐
             │Worker 1│    │Worker 2│    │Worker 3│
             └────────┘    └────────┘    └────────┘
```

Each Worker can process executions independently.

## 12. Worker Failure Handling

Workers may fail while processing an execution:

```text
Worker crashes
Network connection drops
Process terminates
Machine becomes unavailable
```

The architecture therefore needs a mechanism to make unfinished work available again.

The eventual queue-based design can provide:

- Visibility timeout.
- Redelivery.
- Retry.
- Dead-letter queue.

Conceptually:

```text
Queue
  │
  ▼
Worker
  │
  ├── Success → Complete
  │
  └── Failure / Timeout
          │
          ▼
       Retry
          │
          ▼
       Worker
```

## 13. At-Least-Once Execution

The system targets:

```text
At-least-once execution
```

An execution should not be lost, but it may execute more than once.

Example:

```text
Queue
  │
  ▼
Worker
  │
  │ Execute
  │
  └── Worker crashes before acknowledging
          │
          ▼
      Message redelivered
          │
          ▼
      Task executes again
```

Therefore, task execution should eventually be designed to tolerate duplicate execution where possible.

This introduces idempotency as an important concept.

## 14. V6 — Distributed Scheduler

A single Scheduler is a potential availability bottleneck.

The eventual architecture should support multiple Scheduler instances:

```text
             ┌─────────────┐
             │  Job Store  │
             └──────┬──────┘
                    │
          ┌─────────┼─────────┐
          │         │         │
          ▼         ▼         ▼
      Scheduler  Scheduler  Scheduler
          │         │         │
          └─────────┼─────────┘
                    │
                    ▼
              Delayed Queue
```

Multiple Scheduler instances introduce a major problem:

> How do we prevent multiple Scheduler instances from scheduling the same execution?

This will be addressed in the distributed scheduler stage.

## 15. Distributed Scheduling Challenges

Multiple schedulers introduce concerns such as:

- Duplicate scheduling.
- Race conditions.
- Leader election.
- Distributed locking.
- Partition ownership.
- Scheduler failure.
- Recovery after failure.

The project should not introduce a distributed lock simply because multiple instances exist.

The mechanism should be selected after analyzing:

- Scheduling throughput.
- Database capabilities.
- Partitioning strategy.
- Failure semantics.

## 16. Horizontal Scaling

The final architecture should allow independent scaling of major components.

```text
API
 │
 ├── API 1
 ├── API 2
 └── API 3

Scheduler
 │
 ├── Scheduler 1
 ├── Scheduler 2
 └── Scheduler 3

Workers
 │
 ├── Worker 1
 ├── Worker 2
 ├── Worker 3
 └── Worker N
```

## 17. Final High-Level Architecture

```text
                              ┌──────────────┐
                              │    Client    │
                              └──────┬───────┘
                                     │
                                     ▼
                           ┌───────────────────┐
                           │    Schedule API   │
                           └─────────┬─────────┘
                                     │
                                     ▼
                           ┌───────────────────┐
                           │     Job Store     │
                           │                   │
                           │ Jobs              │
                           │ Executions        │
                           └─────────┬─────────┘
                                     │
                                     │ Upcoming jobs
                                     ▼
                           ┌───────────────────┐
                           │    Schedulers     │
                           │                   │
                           │ Scheduler 1       │
                           │ Scheduler 2       │
                           │ Scheduler N       │
                           └─────────┬─────────┘
                                     │
                                     ▼
                           ┌───────────────────┐
                           │   Delayed Queue   │
                           │                   │
                           │ Retry             │
                           │ Visibility        │
                           │ DLQ               │
                           └─────────┬─────────┘
                                     │
                                     ▼
                           ┌───────────────────┐
                           │      Workers      │
                           │                   │
                           │ Worker 1          │
                           │ Worker 2          │
                           │ Worker N          │
                           └─────────┬─────────┘
                                     │
                                     ▼
                                    Task
```

## 18. Responsibility Summary

| Component | Responsibility |
|---|---|
| Client | Create and manage Jobs |
| Schedule API | Validate and accept scheduling requests |
| Job Store | Persist Jobs and Executions |
| Scheduler | Identify upcoming executions |
| Delayed Queue | Deliver executions near scheduled time |
| Workers | Execute Tasks |
| Retry mechanism | Recover failed executions |
| DLQ | Isolate repeatedly failed messages |
| Observability | Monitor system health and performance |

## 19. Data Flows

### Scheduling

```text
Client
  │
  │ Create Job
  ▼
API
  │
  │ Validate
  ▼
Job Store
  │
  │ Persist
  ▼
Response
```

### Dispatching

```text
Job Store
    │
    │ Find upcoming executions
    ▼
Scheduler
    │
    │ Schedule message
    ▼
Delayed Queue
```

### Execution

```text
Delayed Queue
      │
      │ Message available
      ▼
Worker
      │
      │ Execute
      ▼
Task
      │
      │ Result
      ▼
Execution Store
```

### Failure

```text
Worker
  │
  │ Failure
  ▼
Retry
  │
  ├── Retry succeeds → COMPLETED
  │
  └── Max retries exceeded
              │
              ▼
             DLQ
```

## 20. Important Design Trade-offs

### Availability vs Consistency

The system prioritizes:

```text
Availability > Consistency
```

A temporarily stale status is preferable to making the scheduling service unavailable.

### At-Least-Once vs Exactly-Once

The system targets:

```text
At-Least-Once
```

rather than exactly-once execution.

Exactly-once execution is difficult to guarantee across distributed components, so duplicate execution must be considered.

### Database vs Queue

The database and queue have different responsibilities:

```text
Database
    → Durable state

Queue
    → Timely delivery and decoupling
```

The queue should not become the sole source of truth for job definitions.

### Simplicity vs Scale

The first version favors simplicity.

Later versions introduce complexity only when required by:

- Throughput.
- Scheduling precision.
- Availability.
- Fault tolerance.

## 21. Future Architecture Questions

The following questions will drive later sections:

1. How should upcoming executions be queried efficiently?
2. How should recurring schedules be represented?
3. How should executions be created for recurring Jobs?
4. How do we avoid duplicate scheduling?
5. How should Scheduler instances coordinate?
6. How should jobs be partitioned?
7. How should workers be scaled?
8. How should retries work?
9. How do we handle duplicate execution?
10. How do we guarantee at-least-once execution?
11. How should failed messages be isolated?
12. How do we achieve approximately 2-second scheduling precision?
13. How do we approach 10K jobs/sec?
14. How do we monitor scheduler lag?
15. How do we recover from Scheduler failures?

## 22. Current Status

Completed:

```text
Requirements             ✅
Core Entities            ✅
Initial Architecture     ✅
```

Next:

```text
High-Level Design
Data Flow
API boundaries
Scheduling flow
Execution flow
```

The architecture should continue evolving only after the corresponding requirements and limitations are understood.
