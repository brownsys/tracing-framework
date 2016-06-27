## Prerequisites

This section steps over installing pre-requisites.  If you already have these installed, you may skip this.
The tracing framework requires:
* Java 7
* [Maven 3](https://maven.apache.org/download.cgi)
* [Protocol Buffers 2.5](https://github.com/google/protobuf/releases/tag/v2.5.0)
* [AspectJ](https://eclipse.org/aspectj/downloads.php)

Alternatively, João Loff has put together a Docker container for Pivot Tracing which can be found [here](https://github.com/jfloff/pivot-tracing-docker).

## Java 7 - Windows

1. Download and install the latest Java 1.7 JDK from [Oracle](http://www.oracle.com/technetwork/java/javase/downloads/jdk7-downloads-1880260.html). 
2. Set `JAVA_HOME` environment variable to the folder that the JDK is contained within (e.g. `C:\java\jdk1.7.0_40`).
3. Add the `JAVA_HOME/bin` folder to your path, i.e. `%JAVA_HOME%\bin`.

If successful, after restarting, invoking `java -version` from the command line should report the version of Java that you downloaded.

## Java 7 - Linux

1. Install OpenJDK 7

		apt-get install openjdk-7-jre openjdk-7-jdk
2. Set the correct Java version

		update-java-alternatives -s java-1.7.0-openjdk-amd64
3. Set the `JAVA_HOME` environment variable to point to `/usr/lib/jvm/java-7-openjdk-amd64`

If you wish to use the Oracle JDK instead of OpenJDK, use the following commands:

	apt-get install python-software-properties
	add-apt-repository ppa:webupd8team/java
	apt-get update
	apt-get install oracle-java7-installer
	update-java-alternatives -s java-7-oracle

If successful, after restarting, invoking `java -version` from the command line should report the version of Java that you downloaded.

## Maven 3 - Windows

1. Download and install the latest version of [Maven](http://maven.apache.org/download.cgi).  Extract to a folder somewhere on your computer.  
2. Set the `M2_HOME` environment to the folder containing Maven (e.g. `C:\maven\apache-maven-3.1.0`).
3. Add the `M2_HOME/bin` folder to your path, i.e. `%M2_HOME%\bin`.
4. Choose a folder for Maven to use as a repository.  Set the `M2_REPO` to this folder.  By default, it is a folder called '.m2' in your home directory.

If successful, after restarting, you should be able to invoke the `mvn` command from the command line.

## Maven 3 - Linux

	apt-get install maven

## Protobuf 2.5 - Windows

1. Download the version 2.5 of the [Protocol Buffers compiler](https://github.com/google/protobuf/releases/tag/v2.5.0).  Place the executable in a folder somewhere on your computer.
2. Add the folder to your path

If successful, after restarting, you should be able to invoke the `protoc` command from the command line.  (It will say 'Missing input file')

## Protobuf 2.5 - Linux

	apt-get install protobuf-compiler

Depending on your version of Linux, the version of protocol buffers provided by apt is 2.4 instead of the 2.5 required by hadoop.  Check protobuf version with `protoc --version`.   If it is not 2.5 or higher, execute the following steps to install the newer version

	apt-get -y remove protobuf-compiler
	curl -# -O https://protobuf.googlecode.com/files/protobuf-2.5.0.tar.gz
	gunzip protobuf-2.5.0.tar.gz
	tar -xvf protobuf-2.5.0.tar
	cd protobuf-2.5.0
	./configure --prefix=/usr
	make
	make install


## AspectJ - Windows / Linux

1. Download the latest stable release of [AspectJ](https://eclipse.org/aspectj/downloads.php)
2. Install AspectJ by invoking `java -jar aspectj-1.7.3.jar` (replacing 1.7.3 with the version you downloaded)
3. Windows: Add the bin folder to your path

If successful, after restarting, you should be able to invoke the `ajc` command from the command line.

## Ant - Windows

Ant is only a pre-requisite for ZooKeeper

1. Download the latest release of [Ant](http://ant.apache.org/bindownload.cgi)
2. Add the bin folder to your path

If successful, after restarting, you should be able to invoke the 'ant' command from the command line.

## Ant - Linux

Ant is only a pre-requisite for ZooKeeper

`apt-get install ant`

