# Job Scheduler — V1 Implementation Structure

## 1. Project Structure

The V1 project uses a modular Spring Boot structure that keeps the architecture visible in the codebase and provides clean extension points for future versions.

```text
job-scheduler/
│
├── README.md
├── requirements.md
├── architecture.md
├── v1-design.md
├── implementation-structure.md
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
|
|
|
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── niteesh/
│   │   │           └── job_scheduler/
│   │   │
│   │   │               ├── JobSchedulerApplication.java
│   │   │               │
│   │   │               ├── config/
│   │   │               │   ├── SchedulerConfig.java
│   │   │               │   ├── WorkerConfig.java
│   │   │               │   └── DatabaseConfig.java
│   │   │               │
│   │   │               ├── controller/
│   │   │               │   └── JobController.java
│   │   │               │
│   │   │               ├── dto/
│   │   │               │   ├── CreateJobRequest.java
│   │   │               │   ├── JobResponse.java
│   │   │               │   └── ExecutionResponse.java
│   │   │               │
│   │   │               ├── domain/
│   │   │               │   ├── Task.java
│   │   │               │   ├── Job.java
│   │   │               │   ├── Execution.java
│   │   │               │   │
│   │   │               │   ├── enums/
│   │   │               │   │   ├── JobStatus.java
│   │   │               │   │   ├── ExecutionStatus.java
│   │   │               │   │   └── ScheduleType.java
│   │   │               │   │
│   │   │               │   └── repository/
│   │   │               │       ├── JobRepository.java
│   │   │               │       ├── ExecutionRepository.java
│   │   │               │       └── TaskRepository.java
│   │   │               │
│   │   │               ├── service/
│   │   │               │   ├── JobService.java
│   │   │               │   ├── ExecutionService.java
│   │   │               │   └── TaskService.java
│   │   │               │
│   │   │               ├── scheduler/
│   │   │               │   ├── Scheduler.java
│   │   │               │   └── SchedulerService.java
│   │   │               │
│   │   │               ├── queue/
│   │   │               │   ├── DelayedQueue.java
│   │   │               │   └── InMemoryDelayedQueue.java
│   │   │               │
│   │   │               ├── worker/
│   │   │               │   ├── Worker.java
│   │   │               │   └── WorkerPool.java
│   │   │               │
│   │   │               ├── task/
│   │   │               │   ├── TaskHandler.java
│   │   │               │   ├── TaskRegistry.java
│   │   │               │   └── handlers/
│   │   │               │       ├── GenerateReportTask.java
│   │   │               │       └── SendEmailTask.java
│   │   │               │
│   │   │               └── exception/
│   │   │                   ├── JobNotFoundException.java
│   │   │                   ├── TaskNotFoundException.java
│   │   │                   └── GlobalExceptionHandler.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_tasks_table.sql
│   │               ├── V2__create_jobs_table.sql
│   │               └── V3__create_executions_table.sql
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── niteesh/
│                   └── job_scheduler/
│                       ├── controller/
│                       ├── service/
│                       ├── scheduler/
│                       ├── queue/
│                       ├── worker/
│                       └── integration/
│
└── docker-compose.yml
```

---

# 2. Package Responsibilities

The package structure maps directly to the architecture.

```text
                    REST API
                       │
                       ▼
                  controller
                       │
                       ▼
                    service
                       │
              ┌────────┴────────┐
              ▼                 ▼
          repository         task
              │                 │
              ▼                 │
          PostgreSQL            │
                                │
Scheduler ──→ queue ──→ worker ─┘
```

| Package | Responsibility |
|---|---|
| `domain/` | Core business entities and enums |
| `controller/` | REST/API boundary |
| `dto/` | API request/response models |
| `service/` | Business/application operations |
| `scheduler/` | Discover upcoming executions |
| `queue/` | Delay work until it is due |
| `worker/` | Execute scheduled work |
| `task/` | Actual task implementations |
| `repository/` | Database access |
| `config/` | Application configuration |
| `exception/` | Application exceptions and API error handling |

---

## 2.1 `domain/`

Contains the core business objects:

```text
Task
Job
Execution
```

and their states.

These classes should represent business concepts and should contain as little framework-specific logic as practical.

---

## 2.2 `service/`

Contains application/business operations:

```text
JobService
ExecutionService
TaskService
```

Example operations:

```java
jobService.createJob(...)
jobService.cancelJob(...)
executionService.getExecutions(...)
```

Services coordinate business operations between the API, domain objects, repositories, and other components.

---

## 2.3 `scheduler/`

The Scheduler is deliberately separated from the general service layer.

Its responsibility is:

> Find upcoming work and put it into the queue.

It should **not execute tasks**.

This keeps scheduling and execution concerns separate.

---

## 2.4 `queue/`

This is one of the most important abstractions in the project.

```java
public interface DelayedQueue {

    void schedule(Execution execution);

    Execution take();
}
```

V1 implementation:

```text
DelayedQueue
      │
      └── InMemoryDelayedQueue
               │
               └── Java DelayQueue
```

Future implementation:

```text
DelayedQueue
      │
      ├── InMemoryDelayedQueue
      │
      └── SqsDelayedQueue
```

The abstraction allows the queue implementation to evolve without changing the Scheduler or Worker contracts.

---

## 2.5 `worker/`

Responsible for executing scheduled work:

```text
Take execution
      ↓
Execute task
      ↓
Update execution
      ↓
Retry if required
```

The Worker should **not know how the Scheduler found the job**.

This maintains a clean separation between scheduling and execution.

---

## 2.6 `task/`

Represents the actual work being scheduled.

```java
public interface TaskHandler {

    void execute(Map<String, Object> parameters);
}
```

The registry maps task names/types to handlers:

```text
TaskRegistry
     │
     ├── GenerateReportTask
     ├── SendEmailTask
     └── CleanupTask
```

The Scheduler does not need to know what a task actually does.

---

## 2.7 `controller/`

Contains REST controllers and exposes the public API.

Initial controller:

```text
JobController
```

The controller should primarily handle:

- HTTP request/response mapping
- Request validation
- Calling the appropriate service
- HTTP-level error handling

Business logic should remain in the service/domain layers.

---

## 2.8 `dto/`

Contains API-specific request and response models.

Examples:

```text
CreateJobRequest
JobResponse
ExecutionResponse
```

DTOs prevent API contracts from becoming tightly coupled to persistence/domain models.

---

## 2.9 `repository/`

Contains persistence interfaces.

Examples:

```text
JobRepository
ExecutionRepository
TaskRepository
```

These are responsible for database access and should not contain scheduling or task-execution logic.

---

## 2.10 `config/`

Contains application-level configuration.

Examples:

```text
SchedulerConfig
WorkerConfig
DatabaseConfig
```

This is where we configure things such as:

- Scheduler interval
- Scheduling lookahead window
- Worker thread-pool size
- Database-related configuration

---

## 2.11 `exception/`

Contains application-specific exceptions and global REST error handling.

Examples:

```text
JobNotFoundException
TaskNotFoundException
GlobalExceptionHandler
```

This keeps error handling consistent across APIs.

---

# 3. Architectural Mapping

The implementation should make the architecture obvious:

```text
                         REST API
                            │
                            ▼
                       controller
                            │
                            ▼
                         service
                            │
                            ▼
                       repository
                            │
                            ▼
                       PostgreSQL


                       PostgreSQL
                            │
                            ▼
                       scheduler
                            │
                            ▼
                         queue
                            │
                            ▼
                         worker
                            │
                            ▼
                       task handler
```

The major responsibility boundaries are:

| Component | Responsibility |
|---|---|
| Controller | HTTP/API boundary |
| Service | Business/application operations |
| Repository | Persistence |
| Scheduler | Discover upcoming executions |
| Queue | Delay work until it is due |
| Worker | Execute an execution |
| TaskHandler | Perform actual task work |

---

# 4. Avoid Over-Engineering V1

We should intentionally avoid generic packages that become dumping grounds.

Do **not** create packages such as:

```text
util/
common/
helper/
manager/
misc/
```

unless a concrete need appears.

Every class should have a clear architectural responsibility.

We also should not introduce abstractions merely because we know they might be useful later.

The key principle is:

> **Build the simplest V1 that has the right extension points for future versions.**

---

# 5. V1 → V2 Extension Points

The main abstraction we intentionally introduce early is the delayed queue:

```text
DelayedQueue
     │
     └── InMemoryDelayedQueue
```

Later:

```text
DelayedQueue
     │
     ├── InMemoryDelayedQueue
     │
     └── SqsDelayedQueue
```

This lets us replace the queue implementation without rewriting the Scheduler and Worker around a new infrastructure technology.

Similarly, the TaskHandler abstraction keeps task execution independent of scheduling:

```text
Scheduler
    │
    ▼
Execution
    │
    ▼
Worker
    │
    ▼
TaskHandler
```

---

# 6. Implementation Order

We should build incrementally rather than creating every class and implementing them randomly.

## Step 1 — Project Skeleton

Set up:

```text
Spring Boot
Gradle
PostgreSQL
Flyway
```

---

## Step 2 — Domain + Database

Implement:

```text
Task
Job
Execution
```

Then:

```text
Flyway migrations
        ↓
Repositories
```

---

## Step 3 — Job API

Implement:

```text
POST /api/v1/jobs
GET /api/v1/jobs/{id}
POST /api/v1/jobs/{id}/cancel
GET /api/v1/jobs/{id}/executions
```

---

## Step 4 — Task Registry

Implement:

```text
TaskHandler
     ↓
TaskRegistry
     ↓
Sample task implementations
```

Example handlers:

```text
GenerateReportTask
SendEmailTask
```

---

## Step 5 — Delayed Queue

Implement:

```text
DelayedQueue
     ↓
InMemoryDelayedQueue
     ↓
Java DelayQueue
```

---

## Step 6 — Scheduler

Implement:

```text
PostgreSQL
    ↓
Scheduler
    ↓
DelayedQueue
```

The Scheduler should identify upcoming executions and enqueue them.

---

## Step 7 — Workers

Implement:

```text
DelayedQueue
    ↓
WorkerPool
    ↓
TaskHandler
```

Workers execute due executions concurrently.

---

## Step 8 — Retry + Recovery

Implement:

```text
Failure
   ↓
Retry
   ↓
Maximum attempts
   ↓
FAILED
```

Also verify that pending executions can be recovered from PostgreSQL after an application restart.

---

## Step 9 — Integration Tests

Test the complete lifecycle rather than testing only individual classes.

Important scenarios include:

```text
Create Job
    ↓
Schedule
    ↓
Execute
    ↓
Persist result
```

and failure/retry/recovery scenarios.

---

# 7. Recommended V1 Package Structure

The final Java package structure is:

```text
com.niteesh.job_scheduler
│
├── JobSchedulerApplication.java
│
├── config/
│   ├── SchedulerConfig.java
│   ├── WorkerConfig.java
│   └── DatabaseConfig.java
│
├── controller/
│   └── JobController.java
│
├── dto/
│   ├── CreateJobRequest.java
│   ├── JobResponse.java
│   └── ExecutionResponse.java
│
├── domain/
│   ├── Task.java
│   ├── Job.java
│   ├── Execution.java
│   │
│   ├── enums/
│   │   ├── JobStatus.java
│   │   ├── ExecutionStatus.java
│   │   └── ScheduleType.java
│   │
│   └── repository/
│       ├── JobRepository.java
│       ├── ExecutionRepository.java
│       └── TaskRepository.java
│
├── service/
│   ├── JobService.java
│   ├── ExecutionService.java
│   └── TaskService.java
│
├── scheduler/
│   ├── Scheduler.java
│   └── SchedulerService.java
│
├── queue/
│   ├── DelayedQueue.java
│   └── InMemoryDelayedQueue.java
│
├── worker/
│   ├── Worker.java
│   └── WorkerPool.java
│
├── task/
│   ├── TaskHandler.java
│   ├── TaskRegistry.java
│   └── handlers/
│       ├── GenerateReportTask.java
│       └── SendEmailTask.java
│
└── exception/
    ├── JobNotFoundException.java
    ├── TaskNotFoundException.java
    └── GlobalExceptionHandler.java
```

---

# 8. Testing Structure

Tests should mirror the major architectural components:

```text
src/test/java/com/niteesh/job_scheduler/
│
├── controller/
│
├── service/
│
├── scheduler/
│
├── queue/
│
├── worker/
│
└── integration/
```

### Unit tests

Focus on individual components:

```text
JobServiceTest
SchedulerTest
InMemoryDelayedQueueTest
WorkerTest
TaskRegistryTest
```

### Integration tests

Focus on end-to-end behavior:

```text
JobSchedulerIntegrationTest
```

Examples:

```text
Create job → schedule → execute → completed
Create failing job → retry → failed
Recurring job → multiple executions
Cancel job → no future execution
Restart application → pending work recovered
```

---

# 9. First Implementation Milestone

The first milestone should be deliberately small:

```text
Spring Boot
    ↓
PostgreSQL
    ↓
Flyway
    ↓
Task / Job / Execution
    ↓
Repositories
```

At the end of this milestone, we should be able to:

1. Start the application.
2. Connect to PostgreSQL.
3. Run Flyway migrations.
4. Persist a Task.
5. Persist a Job.
6. Persist an Execution.
7. Read them back.

Only after this is working should we introduce the Scheduler and queue.

---

# 10. Guiding Principle

The implementation should evolve alongside the architecture:

```text
V1
Simple
Single instance
Easy to understand
        ↓
Identify bottleneck
        ↓
V2
Add durable queue
        ↓
Identify bottleneck
        ↓
V3
Distributed workers/schedulers
        ↓
Identify bottleneck
        ↓
V4+
Scalability and coordination
```

We should not implement future distributed-system complexity prematurely.

The goal is to make every architectural evolution traceable to a concrete limitation in the previous version.
