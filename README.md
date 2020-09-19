# CONIO

<!--
TODO: CONIO LOGO??
-->

Conio \[Italian:co·nì·glio\] is the shortand of Container Orchestrator Network's InterOperability tool.
It's primary purpose to provide a way to translate various Kubernetes objects (from simple Pod definitions to Helm charts) to Hadoop-compatible (YARN) applications.

## TODOs / Roadmap for 1.0 release

- Add Travis integration
- Create configuration for Conio
- Create a list for supported version (both Hadoop and Kubernetes)
- Add unit tests
- Add MiniYARNCluster tests
- Add instructions for local runs
- Add performance tests (comparing to kubemark?)

## Configuration

<!--
TODO: write this part

we probably need these configurations:
- what queue should the app be placed in?
  this can be something like: static:"root.conio" or dynamic:"root."+namespace
-
-->

## Supported Hadoop and Kubernetes version

Hadoop: 3.3.0

## Run locally

### Pseudo distributed mode

### Using Dockerized Hadoop

Note: it should be used with Dind containers

## Performance

<!--
TODO: might be interesting to compare on some samples
note: this number does not imply anything
-->

## Architectural details

Actually Hadoop YARN and Kubernetes are not so different architecturally.

There are certain features that could never be translated though, and the project's purpose is not to fill this gap.
Like: <!-- TODO: fill this -->

Ideas:
- namespace -> queue?
- only Docker containers are supported
- no Services in Hadoop
- storage is different in Hadoop: but it can be resolved
-   
