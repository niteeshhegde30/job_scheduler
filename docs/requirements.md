# Job Scheduler — Requirements

## 1. Overview

The Job Scheduler is a distributed system that allows users to schedule tasks for execution.

A job can be:

- Executed immediately.
- Executed at a specific future time.
- Executed repeatedly according to a recurring schedule.

The scheduler is responsible for ensuring that scheduled jobs are persisted, dispatched, executed, and monitored reliably.

## 2. Functional Requirements

### 2.1 Schedule a Job Immediately

Users should be able to submit a job for immediate execution.

Example:

```http
POST /jobs
```

```json
{
  "task": "generate-report",
  "parameters": {
    "reportType": "daily"
  }
}
```

The scheduler should persist the job and arrange for its execution.

### 2.2 Schedule a Job for a Future Time

Users should be able to specify a future execution time.

Example:

```text
Execute at:
2026-08-10 10:00:00 UTC
```

The system should ensure that the job is executed close to its scheduled time.

Target:

```text
Execution should occur within approximately 2 seconds
of the scheduled time.
```

### 2.3 Schedule Recurring Jobs

Users should be able to schedule recurring jobs.

Examples:

```text
Every 5 minutes
Every hour
Every day at 10:00 AM
Every Monday at 09:00 AM
```

A recurring Job remains as the scheduling definition. Each occurrence produces a separate Execution.

```text
Job
 │
 ├── Execution #1
 ├── Execution #2
 ├── Execution #3
 └── ...
```

### 2.4 Monitor Job Status

Users should be able to determine the current status of a Job and its Executions.

Potential Job states:

```text
SCHEDULED
RUNNING
COMPLETED
FAILED
CANCELLED
```

Execution history should allow users to understand:

- When an execution was scheduled.
- When it started.
- When it completed.
- Whether it succeeded.
- Whether it failed.
- How many attempts were made.
- Failure information.

### 2.5 Cancel a Job

Users should be able to cancel a scheduled Job.

Cancellation should prevent future executions from being scheduled.

For an execution that is already running, cancellation semantics will be defined separately when execution lifecycle and worker behavior are implemented.

### 2.6 Reschedule a Job

Users should be able to change the schedule of a Job.

Examples:

```text
10:00 AM → 11:00 AM
Every hour → Every 2 hours
```

The updated schedule should apply to future executions.

### 2.7 Execute Tasks

The system must dispatch scheduled work to workers.

A worker:

1. Receives an execution.
2. Executes the associated Task.
3. Records the result.
4. Updates the execution status.

### 2.8 Retry Failed Executions

If an execution fails, the system should support retrying it.

The retry mechanism should eventually support:

- Retry count.
- Retry policy.
- Delayed retry.
- Maximum attempts.
- Permanent failure.

The exact retry implementation will be introduced in a later architecture stage.

## 3. Domain Requirements

### 3.1 Task

A Task represents executable work.

A Task should have a stable identity.

Conceptually:

```text
Task
-----------------
id
name
type
```

A Task can be referenced by multiple Jobs.

### 3.2 Job

A Job represents a scheduled instance of a Task.

Conceptually:

```text
Job = Task + Schedule + Parameters
```

A Job may be:

```text
ONE_TIME
RECURRING
```

### 3.3 Execution

An Execution represents one occurrence of a Job being executed.

Conceptually:

```text
Execution
-----------------
id
jobId
scheduledAt
startedAt
completedAt
status
attempt
error
```

## 4. Execution Guarantees

### 4.1 At-Least-Once Execution

The system must provide an at-least-once execution guarantee.

This means:

> A scheduled execution should not be silently lost, even if failures occur.

The consequence is that the same execution may potentially be executed more than once.

Therefore, task execution should eventually be designed with idempotency in mind.

Example:

```text
Execution X

Attempt 1 → Worker crashes
Attempt 2 → Execution succeeds
```

The system must be able to recover from the worker failure.

## 5. Non-Functional Requirements

### 5.1 Throughput

The target system should support:

```text
10,000 jobs / second
```

This is a target for the eventual distributed architecture. The initial implementation does not need to achieve this scale.

### 5.2 Scheduling Precision

The system should execute scheduled jobs within approximately:

```text
2 seconds
```

of their scheduled execution time.

Example:

```text
Scheduled:
10:00:00

Acceptable:
10:00:00 → 10:00:02
```

### 5.3 Availability Over Consistency

The system prioritizes:

```text
Availability > Consistency
```

When the system must make a trade-off between remaining operational and immediately presenting perfectly consistent state, availability is preferred.

A temporarily stale status is preferable to making the entire scheduling system unavailable.

### 5.4 Reliability

The system should tolerate failures such as:

- Worker crashes.
- Scheduler crashes.
- Temporary database failures.
- Message delivery failures.
- Network failures.
- Task execution failures.

The system should recover without silently losing scheduled executions.

### 5.5 Scalability

The architecture should eventually support horizontal scaling.

The system should be able to scale:

```text
Scheduler instances
Workers
Message processing
API instances
```

independently where appropriate.

### 5.6 Security

The eventual production architecture should support:

- Authentication.
- Authorization.
- Secure API access.
- Secure task execution.
- Protection against unauthorized job manipulation.

Security is not part of the first implementation milestone.

### 5.7 Deployment

The project should eventually support:

- Automated builds.
- Automated tests.
- Containerized deployment.
- CI/CD.
- Production-like environments.

## 6. Performance Requirements

The final target system should support:

```text
Throughput:
    10,000 jobs/sec

Scheduling precision:
    ~2 seconds
```

Performance testing will be added later.

The initial implementation will prioritize correctness and understandability over maximum throughput.

## 7. Reliability Requirements

The scheduler must avoid silently losing work.

Important scenarios include:

```text
Scheduler crashes
Worker crashes
Worker becomes unavailable
Database temporarily unavailable
Message delivery fails
Execution fails
Network communication fails
```

The architecture should progressively introduce mechanisms to handle these scenarios.

## 8. Initial Scope

The first implementation will focus on:

```text
Create Job
Get Job
Cancel Job
Reschedule Job
Create Execution
Execute Task
Track Execution Status
Retry Failed Execution
```

## 9. Out of Scope for Initial Implementation

The following will not be implemented initially:

- Authentication.
- Authorization.
- UI/dashboard.
- Multi-region deployment.
- Complex workflow orchestration.
- Advanced task dependency graphs.
- Cross-region replication.
- Advanced worker routing.
- Maximum-scale optimization.
- Production-grade security hardening.

These may be introduced later if they support the learning objectives.

## 10. Requirements Evolution

Requirements will drive architectural evolution.

```text
Basic scheduling
      ↓
Persistence
      ↓
Execution tracking
      ↓
Retries
      ↓
Timely execution
      ↓
Delayed messaging
      ↓
Multiple workers
      ↓
Fault tolerance
      ↓
Distributed scheduling
      ↓
Horizontal scaling
      ↓
10K jobs/sec
```

Each architectural change should be justified by a requirement or a limitation discovered in the previous implementation.

## 11. Success Criteria

### Correctness

- Jobs execute according to their schedules.
- Recurring jobs create independent executions.
- Execution state is persisted.
- Failed executions can recover.

### Reliability

- Worker failures do not silently lose executions.
- Scheduler failures can be recovered from.
- Retries work correctly.

### Scalability

- Multiple workers can process jobs concurrently.
- Scheduler capacity can be increased.
- The architecture has a clear path toward 10K jobs/sec.

### Observability

The system can eventually expose:

- Job metrics.
- Execution metrics.
- Failure rates.
- Queue depth.
- Scheduler lag.
- Worker health.

## 12. Guiding Principle

> Every architectural decision should be explainable in terms of a requirement, scalability constraint, reliability concern, or operational problem.
