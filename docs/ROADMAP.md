# Roadmap

## Core features (targeted for v1.0)

- Support pod yaml fields
  - Support args and command fields
  - Support volumes (hostpath, HDFS maybe?)
- Provide status of the application
- Add quickstart section
- Write documentation about configuration options (client and AM)
- Add MiniYARNCluster integration tests for the AM communication protocols

## Main feature ideas for further releases

- Leverage ZooKeeper for storing yamls
  - Pod yaml update
- Deployment support
- Replicaset support
- Helm support
- Investigate any networking or service support for Hadoop
- `exec` / interactive Docker container commands (requires Hadoop 3.3.0)
- Namespace to different queue mapping

## Low priority items
 
- Performance testing
