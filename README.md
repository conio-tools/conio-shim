# Conio shim

<!--
TODO: CONIO LOGO??
-->

![build](https://github.com/conio-tools/conio-shim/workflows/Build/badge.svg?branch=master)

Conio \[Italian:co·nì·glio\] is the abbreviation of Container Orchestrator Network's InterOperability tools. Conio is a set of tools providing interoperability between different container orchestrators like Hadoop YARN and Kubernetes.
 
The purpose of the Conio shim layer is to translate various Kubernetes objects (starting from simple Pod definitions to complex Helm charts) to Hadoop-compatible (YARN) applications.

Disclaimer: this tool is in pre-alpha stage and under heavy development. It hasn't been tested thoroughly yet, and you should use with extreme care.  

## Usage

You can read it [here](/docs/USAGE.md) how to run Conio locally. 

## Roadmap

The tool is currently in pre-alpha stage. You can check the roadmap [here](docs/ROADMAP.md).

## Motivation

So first of all: why on earth would you want to do this?

### Cons

- Kubernetes has a tremendously different set of features:
  - By building a bridge between these two frameworks there are some feature differences that can never be mitigated. A few of the most important differences with regard to Conio:
    - _Services_: Hadoop provides no support for creating services, proxying or any kind of network management. Any service related configurations in your yaml files will be omitted, as Conio could not .
    - _Controller pattern_: There is large number of software written to Kubernetes to manage certain resources and workloads. 
    Conio does not fully translate of the communication with the API server in the Hadoop environment (for the YARN ResourceManager and NodeManagers). 
    Controllers written for Kubernetes could not be interpreted in Hadoop.  
    - _Autoscaling_: Hadoop is **not** cloud native, so it has limitations regarding flexibility.
    - _Storage_: Hadoop's Distributed FileSystem is a very different than any on-demand or dynamically provisioned volume/claim and mount in Kubernetes. The list of differences is long, but most of the Volume related configurations are not supported in Hadoop.    
- Docker on YARN provides only a limited set of features from the Docker ecosystem, and Hadoop is missing important container technologies, like the CRI standard.
- Though there are some ongoing efforts, currently Hadoop is not very elastic, and is not cloud native at all.
- The common "popularity" card: Kubernetes is a rapidly growing ecosystem right now, and Hadoop is gathering less attention (2020). 
Generally it is wiser to leverage the most recent/active stack with all its quickly evolving improvements, if you have general purposes. 

### Pros

From the cons above the ideal workloads that fit this project are primarily batch jobs that requires no communication, or general network communication with external services.

Usually the containerized workloads that:
- run for a while 
- has some input that is given during start
- run processing without interaction 
- dumps its output to some location

can be most probably translatable.
 
This project might be a good choice if:
- You are moving from a Hadoop-based on prem setup to the cloud, but sometimes need backward compatibility when running primarily batch jobs
- You have large on-prem clusters where you want to have the same client-side flexibility that Kubernetes provides, but:
  - moving to the cloud is not suitable (e.g. security concerns against public/hybrid cloud)
  - moving to on-prem Kubernetes is not a suitable choice (e.g. lagging-in-tech corporations, company policy, IT decisions) 
- You would like to leverage an existing Hadoop YARN feature for workload management (e.g. highly configurable and efficient scheduler).

## Supported Hadoop and Kubernetes version

For Hadoop the Docker on YARN support is a hard requirement, so the Hadoop 3.1 is the minimum required version.

There are numerous changes and bugfixes in 3.2, so I recommend 3.2<=. The tool is primarily tested against 3.2.1.   

There is currently no limitation for the Kubernetes version. 
