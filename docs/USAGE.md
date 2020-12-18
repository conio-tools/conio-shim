# Running locally

You can run Conio on Hadoop natively using a pseudo distributed cluster or in Docker containers.

## Pseudo distributed mode (Single node setup)

Note: Conio on Hadoop requires native libraries, which is only supported on Linux as of now. 
Therefore it is currently not possible to run Conio on Mac for example.

For Linux systems perform the following steps:

1. Download a supported Hadoop from [here](https://archive.apache.org/dist/hadoop/common/hadoop-3.3.0/hadoop-3.3.0.tar.gz).

1. Set up a Single Node Cluster based on [this document](https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html).

1. [Enable scheduling of Docker containers](https://hadoop.apache.org/docs/r3.3.0/hadoop-yarn/hadoop-yarn-site/DockerContainers.html) in YARN (like setting up the [Linux Container Executor](https://hadoop.apache.org/docs/r3.3.0/hadoop-yarn/hadoop-yarn-site/SecureContainer.html#Linux_Secure_Container_Executor)) and set up your Docker daemon accordingly.

1. Build Conio and obtain the far jar with its dependencies and 

1. Submit the yaml describing your Kubernetes object through a Conio application by using the Conio client: 
```bash
java -jar conio-1.0-SNAPSHOT-jar-with-dependencies.jar -yaml k8s-obj.yaml
```

## Using Dockerized Hadoop

- Set up any dockerized Hadoop solution and configure it according to the Pseudo distributed mode
- _or_ use the [conio-nano](https://github.com/conio-tools/conio-nano) project which is a fork of [big-data-europe/docker-hadoop](https://github.com/big-data-europe/docker-hadoop) with the same configurations as above, but suited for this project. 

If you use Conio-nano, you are probably going to need a similar command to start the Conio client in a Docker container that has the required files mounted in the container:
```bash
docker run -it -a stdin -a stdout -a stderr --env-file hadoop.env --network docker-hadoop_default -v $(PWD)/conio:/conio conio/base:master -- sudo -u conio java -jar /conio/conio.jar -yaml /conio/pod.yaml
```

Note that the jar containing the dependencies is renamed to _conio.jar_. 

### Container failure due to mount denied

In the NodeManager logs you may see mount failed options. If you use Mac and you use Docker for Mac, enable sharing specific mounts in the Preferences > File sharing tab: add `/opt` and `/etc`.   
