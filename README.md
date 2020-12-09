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

You can run Conio on Hadoop natively or in Docker containers. 

### Pseudo distributed mode (Single node setup)

Note: Conio on Hadoop requires natives libraries, which is only supported on Linux as of now. 
Therefore it is currently not possible to run Conio on Mac.

For Linux systems perform the following steps:

1. Download a supported Hadoop from [here](https://archive.apache.org/dist/hadoop/common/hadoop-3.3.0/hadoop-3.3.0.tar.gz).

1. Set up a Single Node Cluster based on this [document](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html).

1. Configure to use [Linux Container Executor](https://hadoop.apache.org/docs/r3.3.0/hadoop-yarn/hadoop-yarn-site/SecureContainer.html#Linux_Secure_Container_Executor) in YARN

1. [Enable scheduling of Docker containers](https://hadoop.apache.org/docs/r3.3.0/hadoop-yarn/hadoop-yarn-site/DockerContainers.html) in YARN and set up your Docker daemon accordingly.

1. Submit the CONIO application to the cluster using this command:
```bash
java -jar conio-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Using Dockerized Hadoop

A suitable choice is [big-data-europe/docker-hadoop](https://github.com/big-data-europe/docker-hadoop) which uses Docker compose to run Hadoop daemons.

Note: it should be used with Dind containers

```bash
docker run -it -a stdin -a stdout -a stderr --env-file hadoop.env --network docker-hadoop_default conio/base:master -- sudo -u conio java -jar /conio/conio-1.0-SNAPSHOT-jar-with-dependencies.jar -yaml /conio/pod.yaml --queue default
```

#### Container failure due to mount denied

In Docker for Mac set 

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
