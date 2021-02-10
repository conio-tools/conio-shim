# CONIO container engine

The CONIO container engine translate a Kubernetes pod definition to a YARN application.

It uses the generic architecture of a Hadoop YARN architecture:
 - There's a CONIO client that submits the application by establishing a connection to the ResourceManager
 - CONIO ApplicationMaster that should be a shim layer managing the CONIO containers belonging this Pod definition
