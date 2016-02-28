# Build and Install

The tracing framework requires [Maven 3](https://maven.apache.org/download.cgi) and [protocol buffers version 2.5](https://github.com/google/protobuf/releases/tag/v2.5.0)

After cloning the [git repository](https://github.com/brownsys/tracing-framework), build and install with the following command:

    mvn clean package install -DskipTests

Next, download one of the pre-instrumented systems such as our instrumented fork of [Hadoop 2.7.2](https://github.com/brownsys/hadoop).

Build and install Hadoop using the normal approach:

    mvn clean package install -Pdist -DskipTests -Dmaven.javadoc="skip"

