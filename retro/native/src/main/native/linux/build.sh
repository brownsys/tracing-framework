echo "Invoke this script from the src/main/native folder"

# build the linux ThreadCPUTimer library
echo "Building ThreadCPUTimer for Linux"
g++ -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -o META-INF/lib/libthreadcputimer.so -shared linux/ThreadCPUTimer.c -lrt
echo "Build complete"
