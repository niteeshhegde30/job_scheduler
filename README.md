# Distributed Job Scheduler

A production-oriented distributed job scheduler built in Java, designed as a hands-on system design and distributed systems project.

The project is based on the Hello Interview Job Scheduler problem and will progressively evolve from a simple scheduler into a highly available, horizontally scalable distributed system.

## Goals

- Learn and practice High-Level System Design (HLD).
- Understand distributed scheduling systems.
- Build a production-quality backend system.
- Explore reliability, concurrency, scalability, and fault tolerance.
- Build a meaningful GitHub project demonstrating backend engineering depth.
- Understand the trade-offs behind architectural decisions rather than simply assembling technologies.

## Problem Statement

A job scheduler allows users to define work that should be executed immediately, at a future time, or repeatedly according to a schedule.

The system is responsible for persisting jobs, determining when they should execute, dispatching them to workers, executing the underlying task, tracking execution status, and recovering from failures.

The target system described by the reference design supports:

- Up to 10,000 jobs/second.
- Execution within approximately 2 seconds of the scheduled time.
- At-least-once execution.
- High availability.
- Retry and failure recovery.
- Horizontal scalability.

## Core Concepts

### Task

A Task represents the work that can be executed.

Examples:

```text
send_email
generate_report
cleanup_files
```

A Task answers:

> What work should be performed?

A Task can be reused by multiple Jobs.

### Job

A Job represents a scheduled instance of a Task.

Conceptually:

```text
Job = Task + Schedule + Parameters
```

Example:

```text
Task:
    send_email

Schedule:
    Every day at 10:00 AM

Parameters:
    recipient = john@example.com
    template = daily-report
```

A Job answers:

> What should be executed, when should it be executed, and with which parameters?

A Job can be one-time or recurring.

### Execution

An Execution represents one occurrence of a Job actually being executed.

For a recurring Job:

```text
Daily Report Job

Execution 1 → Aug 8, 10:00 AM → SUCCESS
Execution 2 → Aug 9, 10:00 AM → SUCCESS
Execution 3 → Aug 10, 10:00 AM → FAILED
Execution 4 → Aug 11, 10:00 AM → SUCCESS
```

The conceptual relationship is:

```text
Task
  │
  ▼
Job
  │
  ├── Execution 1
  ├── Execution 2
  ├── Execution 3
  └── ...
```

## Architecture Evolution

The architecture will evolve incrementally:

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
V10 Load Testing & Production Hardening
```

The project intentionally starts simple. Each new distributed-system component should be introduced only when a requirement or limitation justifies it.

## Initial Architecture

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

Initial flow:

```text
1. Client creates a Job.
2. Schedule API validates and persists the Job.
3. Scheduler identifies Jobs that are due.
4. Scheduler dispatches the Job to a Worker.
5. Worker executes the Task.
6. Worker records the Execution result.
7. Failed executions can be retried.
```

## Technology Stack

The intended backend stack is:

```text
Language:
    Java

Framework:
    Spring Boot

Database:
    PostgreSQL

Messaging:
    Introduced when the architecture requires it

Containerization:
    Docker

Orchestration:
    Kubernetes (later stage)

Observability:
    Metrics + Logging + Tracing (later stage)

Testing:
    JUnit
    Integration Testing
    Testcontainers
```

Technologies will only be introduced when they solve a concrete system requirement.

## Learning Philosophy

For every major architectural component:

```text
Requirement
    ↓
Problem
    ↓
Simple solution
    ↓
Identify limitation
    ↓
Scale / failure requirement
    ↓
Architectural change
    ↓
Implementation
    ↓
Testing
```

The goal is not merely to implement the final architecture. The goal is to understand why the architecture needed to evolve.
