# Roadmap

## Core features (targeted for v1.0)

- Support pod yaml fields
  - Proper multiple container support
  - Support args and command fields
  - Support volumes (hostpath, HDFS maybe?)
  - Support restart policy
- Provide status of the application
- Pod yaml update
- Add quickstart section
- Write documentation about configuration options (client and AM)
- Add MiniYARNCluster integration tests for the AM communication protocols

## Main feature ideas for further releases

- Leverage ZooKeeper for storing yamls
- Deployment support
- Replicaset support
- Helm support
- Investigate any networking or service support for Hadoop
- `exec` / interactive Docker container commands (requires Hadoop 3.3.0)
- Namespace to different queue mapping

## Low priority items
 
- Performance testing
