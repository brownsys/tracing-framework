echo "Invoke this script from the src/main/native folder"

# build the linux ThreadCPUTimer library
echo "Building ThreadCPUTimer for Mac"
g++ -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin -o META-INF/lib/libthreadcputimer.dylib -shared mac/ThreadCPUTimer.c
echo "Build complete"
