if [[ -z "$M2_REPO" ]]; then 
  M2_REPO=~/.m2/repository 
fi

MR_GENERATOR_JAR=$M2_REPO/edu/brown/cs/systems/mapreduce-generator/1.0/mapreduce-generator-1.0.jar

HADOOP_CMD=$HADOOP_HOME/bin/hadoop

$HADOOP_CMD jar $MR_GENERATOR_JAR edu.brown.cs.systems.mrgenerator.CommandLineInterface

