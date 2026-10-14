---
type: "Component"
title: "Retry policies"
description: "How a task is retried: one policy per task (none, fixed delay or Fibonacci delay), applied by the executor before any fallback."
generated: {by: "ekw-ingest/0.1+claude-sonnet-5-5", at: "2026-10-08T21:05:00Z"}
verified:
  - {by: "process:citation-check", at: "2026-10-08T21:06:00Z"}
status: "stable"
sources:
  - id: "rp-facts"
    resource: "../facts/RetryPolicy.md"
    title: "RetryPolicy (code facts)"
  - id: "rp-none"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java#L57-L66"
    title: "RetryPolicy.None"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java", lines: [57, 66], sha256: "7a23d9a3cf48bc0e"}
  - id: "rp-linear"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java#L71-L96"
    title: "RetryPolicy.Linear"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java", lines: [71, 96], sha256: "358dcfc3d6460042"}
  - id: "rp-nonlinear"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java#L101-L130"
    title: "RetryPolicy.Nonlinear"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java", lines: [101, 130], sha256: "b984b7a7d0e2ba8b"}
  - id: "rp-execute"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java#L39"
    title: "RetryPolicy.execute (abstract)"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/RetryPolicy.java", lines: [39, 39], sha256: "c8711c92fdabdaa9"}
  - id: "task-default"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/Task.java#L44"
    title: "Task.Builder default policy"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/Task.java", lines: [44, 44], sha256: "96287f82fc11f10c"}
  - id: "task-setter"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/Task.java#L71-L74"
    title: "Task.Builder.retryPolicy"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/Task.java", lines: [71, 74], sha256: "15b5001827cdeb41"}
  - id: "te-execute-task"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/TaskExecutor.java#L215-L228"
    title: "TaskExecutor.executeTask"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/TaskExecutor.java", lines: [215, 228], sha256: "51a516a7802e4213"}
  - id: "te-handle-failure"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/src/main/java/com/debopam/asynexecutor/util/TaskExecutor.java#L239-L258"
    title: "TaskExecutor.handleFailure"
    x-ekw: {path: "src/main/java/com/debopam/asynexecutor/util/TaskExecutor.java", lines: [239, 258], sha256: "b0c2aeced63a9dae"}
  - id: "readme-retry-example"
    resource: "https://github.com/debopampoddar/asyncexecutorutil/blob/ef0d2c95941b822766c392fdc04640c8cc92d053/README.md#L91"
    title: "README, Handling Retries example"
    x-ekw: {path: "README.md", lines: [91, 91], sha256: "728e49ffd93892f3"}
x-ekw:
  layer: "knowledge"
  repo: "asyncexecutorutil"
  commit: "ef0d2c95941b822766c392fdc04640c8cc92d053"
  owner: "human:debopam"
  read: ["facts/RetryPolicy.md", "RetryPolicy.java L14-L135", "Task.java L36-L91", "TaskExecutor.java L215-L258"]
  not_read: ["RetryPolicyTest.java (only test names from the facts page)"]
---

<!-- ILLUSTRATIVE (stage 1 guide, 2026-10-08): the shape of an LLM-written page. The claims were checked by hand
against the code at commit ef0d2c9; the sha256 values are the first 16 hex digits of the hash of the cited lines. -->

# Retry policies

Every task carries exactly one retry policy. If the task's builder is not given one, the task gets
`RetryPolicy.none()`, which runs the action once and never retries.[^task-default][^rp-none] A
policy is attached with `Task.Builder.retryPolicy(...)`.[^task-setter]

## The three policies

| Policy                                           | Attempts               | Wait between attempts             | Retries when                      |
|--------------------------------------------------|------------------------|-----------------------------------|-----------------------------------|
| `None`                                           | 1                      | none                              | never                             |
| `Linear(maxRetries, interval, condition)`        | up to `maxRetries + 1` | the same `interval` each time     | `condition` accepts the exception |
| `Nonlinear(maxRetries, baseInterval, condition)` | up to `maxRetries + 1` | `baseInterval` × 1, 2, 3, 5, 8, … | `condition` accepts the exception |

- `Linear` loops from attempt 0 to `maxRetries`, stops early when the condition rejects the
  exception, and rethrows the last exception when it gives up.[^rp-linear]
- `Nonlinear` uses the same loop but advances a Fibonacci pair before each sleep, so the multipliers
  are 1, 2, 3, 5, 8 and so on.[^rp-nonlinear]

```mermaid
classDiagram
  class RetryPolicy {
    <<abstract>>
    +execute(Callable) T
    +none() RetryPolicy
  }
  RetryPolicy <|-- None
  RetryPolicy <|-- Linear
  RetryPolicy <|-- Nonlinear
  Task --> RetryPolicy : retryPolicy
  TaskExecutor ..> RetryPolicy : calls execute
```

*Diagram generated from the analyzer's type graph (inheritance, fields, resolved calls), not drawn
by the LLM.*

## Where the executor applies it

`TaskExecutor.executeTask` runs `task.retryPolicy().execute(task.action())`. On success it records
the result and calls the task's `onSuccess`; on failure it hands over to
`handleFailure`.[^te-execute-task] `handleFailure` runs the task's fallback if there is one. If
there is no fallback, or the fallback also fails, it records the failure and calls `onFailure`; it
rethrows only when fail-fast is on.[^te-handle-failure] So the order is: all retries first, then the
fallback.

## Adding your own policy

Subclass `RetryPolicy`, implement `<T> T execute(Callable<T> action)`, and pass an instance to
`Task.Builder.retryPolicy(...)`.[^rp-execute][^task-setter] The constructor is `protected`, so a
subclass must call
`super(maxRetries, baseInterval, condition)`.[^rp-facts]

## Where the documentation disagrees

The README's retry example uses `.retry(RetryPolicy.linear(3, Duration.ofSeconds(1)))`. The builder
method is
`retryPolicy`, and there is no `linear` factory.[^readme-retry-example][^task-setter][^rp-facts] The
[README's document page](../docs/readme.md) lists every disagreement.

> Synthesis: `Nonlinear` keeps its Fibonacci position in two instance fields that are never reset,
> so one instance
> shared by several tasks or runs would keep lengthening its delays. Use a new instance per task.
> (Inferred from the
> code; not tested.)

[^rp-facts]: RetryPolicy (code facts)
[^rp-none]: RetryPolicy.java L57-L66
[^rp-linear]: RetryPolicy.java L71-L96
[^rp-nonlinear]: RetryPolicy.java L101-L130
[^rp-execute]: RetryPolicy.java L39
[^task-default]: Task.java L44
[^task-setter]: Task.java L71-L74
[^te-execute-task]: TaskExecutor.java L215-L228
[^te-handle-failure]: TaskExecutor.java L239-L258
[^readme-retry-example]: README.md L91
