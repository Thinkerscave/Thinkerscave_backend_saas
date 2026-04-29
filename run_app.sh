#!/bin/bash
export JAVA_HOME="/Users/bikashkumarnishank/Library/Java/JavaVirtualMachines/ms-21.0.8/Contents/Home"
# Source .env file properly, handling comments and empty lines
if [ -f .env ]; then
  export $(grep -v '^#' .env | xargs)
fi
echo "Starting application with JAVA_HOME=$JAVA_HOME"
$JAVA_HOME/bin/java -jar target/User-Management-Service-0.0.1-SNAPSHOT.jar
