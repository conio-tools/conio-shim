package org.conio.container.client;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.DataOutputBuffer;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.GetNewApplicationResponse;
import org.apache.hadoop.yarn.api.records.ApplicationId;
import org.apache.hadoop.yarn.api.records.ApplicationSubmissionContext;
import org.apache.hadoop.yarn.api.records.ContainerLaunchContext;
import org.apache.hadoop.yarn.api.records.LocalResource;
import org.apache.hadoop.yarn.api.records.LocalResourceType;
import org.apache.hadoop.yarn.api.records.LocalResourceVisibility;
import org.apache.hadoop.yarn.api.records.Priority;
import org.apache.hadoop.yarn.api.records.QueueInfo;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.api.records.ResourceRequest;
import org.apache.hadoop.yarn.api.records.URL;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.client.util.YarnClientUtils;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.DockerClientConfigHandler;
import org.apache.hadoop.yarn.util.resource.Resources;
import org.conio.container.engine.ApplicationMaster;
import org.conio.container.k8s.Pod;
import org.conio.container.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class Client {

    private static final Logger LOG = LoggerFactory
            .getLogger(Client.class);

    private static final String DEFAULT_QUEUE_NAME = "root.default";
    private static final String APP_NAME = "CONIO";
    private static final String APP_MASTER_JAR = "AppMaster.jar";
    private static final int DEFAULT_AM_MEMORY = 100;

    private final YarnClient yarnClient;
    private final Configuration conf;
    private final Options opts;

    private final long clientStartTime = System.currentTimeMillis();

    private String appMasterJar = "/conio/conio-1.0-SNAPSHOT-jar-with-dependencies.jar";
    private String dockerClientConfig;
    private String yamlFile;
    private String queueName = DEFAULT_QUEUE_NAME;

    public Client() {
        conf = new YarnConfiguration();
        // TODO make this configurable
        conf.addResource(new Path("/etc/hadoop/core-site.xml"));
        conf.addResource(new Path("/etc/hadoop/hdfs-site.xml"));
        conf.addResource(new Path("/etc/hadoop/yarn-site.xml"));
        conf.reloadConfiguration();
        yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        opts = createOptions();
    }

    private static Options createOptions() {
        Options opts = new Options();
        opts.addOption("y", "yaml", true, "the yaml file containing the description of the Kubernetes object");
        opts.addOption("q", "queue", true, "the queue this application will be submitted");
        return opts;
    }

    public void init(String[] args) throws ParseException, FileNotFoundException {
        if (args.length == 0) {
            throw new IllegalArgumentException("No args specified for client to initialize");
        }

        CommandLine cliParser = new GnuParser().parse(opts, args);

        yamlFile = cliParser.getOptionValue("yaml");
        if (yamlFile == null || yamlFile.isEmpty()) {
            throw new IllegalArgumentException("Empty yaml file provided as input");
        }
        File file = new File(yamlFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("");
        }

        parseYaml();

        String configuredQueue = cliParser.getOptionValue("queue");
        if (configuredQueue != null) {
            queueName = configuredQueue;
        }
    }

    private void parseYaml() throws FileNotFoundException {
        Yaml yaml = new Yaml();
        InputStream inputStream = new FileInputStream(yamlFile);

        Pod pod = yaml.loadAs(inputStream, Pod.class);
        LOG.info("The used image is " + pod.getSpec().getContainers().get(0).getImage());
    }

    // TODO set no retry for the AM
    public void run() throws YarnException, IOException {
        yarnClient.start();

        QueueInfo queueInfo = yarnClient.getQueueInfo(queueName);
        if (queueInfo == null) {
            throw new IllegalArgumentException(
                    String.format("Could not find %s queue.", queueName));
        }

        YarnClientApplication app = yarnClient.createApplication();
        GetNewApplicationResponse appResponse = app.getNewApplicationResponse();

        ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        ApplicationId applicationId = appContext.getApplicationId();

        // set AM resources
        {
            Resource capability = Resource.newInstance(0, 0);

            if (appContext.getAMContainerResourceRequests() == null) {
                List<ResourceRequest> amResourceRequests = new ArrayList<ResourceRequest>();
                amResourceRequests
                        .add(ResourceRequest.newInstance(Priority.newInstance(0),
                                "*", Resources.clone(Resources.none()), 1));
                appContext.setAMContainerResourceRequests(amResourceRequests);
            }

            capability.setMemorySize(100);
            capability.setVirtualCores(1);
            appContext.getAMContainerResourceRequests().get(0).setCapability(
                    capability);
        }

        // if AM crashes, restart child containers
        appContext.setKeepContainersAcrossApplicationAttempts(false);

        // setting the name of the application
        String appName = APP_NAME;
        appContext.setApplicationName(appName);

        // we might need this later
        // appContext.setApplicationTags(tags);

        // add local resources
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();
        FileSystem fs = FileSystem.get(conf);
        // add AM as resource
        addToLocalResources(fs, appMasterJar, APP_MASTER_JAR,
                applicationId.toString(), localResources, null);

        // TODO: support localizable files

        Map<String, String> env = new HashMap<String, String>();
        // TODO think about this part
        // env.put("", "");

        // TODO revisit this part of the code
        {
            // Add AppMaster.jar location to classpath
            // At some point we should not be required to add
            // the hadoop specific classpaths to the env.
            // It should be provided out of the box.
            // For now setting all required classpaths including
            // the classpath to "." for the application jar
            StringBuilder classPathEnv = new StringBuilder(ApplicationConstants.Environment.CLASSPATH.$$())
                    .append(ApplicationConstants.CLASS_PATH_SEPARATOR).append("./*");
            for (String c : conf.getStrings(
                    YarnConfiguration.YARN_APPLICATION_CLASSPATH,
                    YarnConfiguration.DEFAULT_YARN_CROSS_PLATFORM_APPLICATION_CLASSPATH)) {
                classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR)
                        .append(c.trim());
            }
            classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR).append(
                    "./log4j.properties");

            // add the runtime classpath needed for tests to work
            if (conf.getBoolean(YarnConfiguration.IS_MINI_YARN_CLUSTER, false)) {
                classPathEnv.append(ApplicationConstants.CLASS_PATH_SEPARATOR)
                        .append(System.getProperty("java.class.path"));
            }

            env.put("CLASSPATH", classPathEnv.toString());
        }

        // setting up AM command
        // TODO revisit this part of the code
        {
            Vector<CharSequence> vargs = new Vector<CharSequence>(30);

            // Set java executable command
            LOG.info("Setting up app master command");
            // Need extra quote here because JAVA_HOME might contain space on Windows,
            // e.g. C:/Program Files/Java...
            vargs.add("\"" + ApplicationConstants.Environment.JAVA_HOME.$$() + "/bin/java\"");
            // Set Xmx based on am memory size
            vargs.add("-Xmx" + DEFAULT_AM_MEMORY + "m");
            // Set class name
            vargs.add(ApplicationMaster.class.getName());

            vargs.add("--appname " + appName);

            // TODO - do we need this?
            vargs.add("1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stdout");
            vargs.add("2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/AppMaster.stderr");

            // Get final commmand
            StringBuilder command = new StringBuilder();
            for (CharSequence str : vargs) {
                command.append(str).append(" ");
            }

            List<String> commands = new ArrayList<String>();
            commands.add(command.toString());

            // Set up the container launch context for the application master
            ContainerLaunchContext amContainer = ContainerLaunchContext.newInstance(
                    localResources, env, commands, null, null, null);

            // Setup security tokens
            Credentials rmCredentials = null;
            if (UserGroupInformation.isSecurityEnabled()) {
                // Note: Credentials class is marked as LimitedPrivate for HDFS and MapReduce
                rmCredentials = new Credentials();
                String tokenRenewer = YarnClientUtils.getRmPrincipal(conf);
                if (tokenRenewer == null || tokenRenewer.length() == 0) {
                    throw new IOException(
                            "Can't get Master Kerberos principal for the RM to use as renewer");
                }

                // For now, only getting tokens for the default file-system.
                final Token<?> tokens[] =
                        fs.addDelegationTokens(tokenRenewer, rmCredentials);
                if (tokens != null) {
                    for (Token<?> token : tokens) {
                        LOG.info("Got dt for " + fs.getUri() + "; " + token);
                    }
                }
            }


            // this is important, please revise this!
            // TODO

            // Add the docker client config credentials if supplied.
            Credentials dockerCredentials = null;
            if (dockerClientConfig != null) {
                dockerCredentials =
                        DockerClientConfigHandler.readCredentialsFromConfigFile(
                                new Path(dockerClientConfig), conf, applicationId.toString());
            }

            if (rmCredentials != null || dockerCredentials != null) {
                DataOutputBuffer dob = new DataOutputBuffer();
                if (rmCredentials != null) {
                    rmCredentials.writeTokenStorageToStream(dob);
                }
                if (dockerCredentials != null) {
                    dockerCredentials.writeTokenStorageToStream(dob);
                }
                ByteBuffer tokens = ByteBuffer.wrap(dob.getData(), 0, dob.getLength());
                amContainer.setTokens(tokens);
            }

            appContext.setAMContainerSpec(amContainer);

            appContext.setQueue(queueName);
        }

        yarnClient.submitApplication(appContext);

        ApplicationMonitor monitor =
                new ApplicationMonitor(yarnClient, applicationId, clientStartTime);
        monitor.run();
    }

    // TODO refactor this function
    private void addToLocalResources(FileSystem fs, String fileSrcPath,
                                     String fileDstPath, String appId, Map<String, LocalResource> localResources,
                                     String resources) throws IOException {
        String suffix = Util.getRelativePath(APP_NAME, appId, fileDstPath);
        Path dst =
                new Path(fs.getHomeDirectory(), suffix);
        if (fileSrcPath == null) {
            FSDataOutputStream ostream = null;
            try {
                ostream = FileSystem
                        .create(fs, dst, new FsPermission((short) 0710));
                ostream.writeUTF(resources);
            } finally {
                IOUtils.closeQuietly(ostream);
            }
        } else {
            fs.copyFromLocalFile(new Path(fileSrcPath), dst);
        }
        FileStatus scFileStatus = fs.getFileStatus(dst);
        LocalResource scRsrc =
                LocalResource.newInstance(
                        URL.fromURI(dst.toUri()),
                        LocalResourceType.FILE, LocalResourceVisibility.APPLICATION,
                        scFileStatus.getLen(), scFileStatus.getModificationTime());
        localResources.put(fileDstPath, scRsrc);
    }
}
