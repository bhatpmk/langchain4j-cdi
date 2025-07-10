#!/bin/bash

set -e
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if ! command -v mvn >/dev/null 2>&1; then
  echo "Maven is not available in PATH."
  exit 1
fi

JAVA_HOME="${JAVA_HOME}"
WL_HOME="${WL_HOME}"
ADMIN_URL="${ADMIN_URL:-"t3://localhost:7001"}"
ADMIN_USER="${ADMIN_USER:-"weblogic"}"
ADMIN_PASSWORD="${ADMIN_PASSWORD}"
SERVER_NAME="${SERVER_NAME:-"AdminServer"}"

if [ -z "${JAVA_HOME}" ]; then
    echo "Environment variable JAVA_HOME not set"
    exit 1
fi

if [ -z "${WL_HOME}" ]; then
    echo "Environment variable WL_HOME not set"
    exit 1
fi

build_cdi_extn() {
  # There are some tests under langchain4j-cdi-portable-ext, which do not work with CDI 3.0.1. So tests are skipped from
  # that module
  echo "Building only the required modules"
  cd $BASE_DIR
  mvn clean install -pl langchain4j-cdi-core,langchain4j-cdi-portable-ext -am -Pskip-tests-portable-ext
}

build_demo() {
    echo "Building weblogic-car-booking"
    cd $BASE_DIR/examples/weblogic-car-booking
    mvn clean package
}

undeploy() {
  if [ -z "${ADMIN_PASSWORD}" ]; then
    echo "Environment variable ADMIN_PASSWORD not set"
    exit 1
  fi
  echo "Undeploying weblogic-car-booking..."
  cd $BASE_DIR
  $JAVA_HOME/bin/java -cp $WL_HOME/server/lib/weblogic.jar weblogic.Deployer -adminurl $ADMIN_URL  -username $ADMIN_USER  -password $ADMIN_PASSWORD -undeploy -name weblogic-car-booking -targets $SERVER_NAME
}

deploy() {
  if [ -z "${ADMIN_PASSWORD}" ]; then
    echo "Environment variable ADMIN_PASSWORD not set"
    exit 1
  fi
  echo "Deploying weblogic-car-booking..."
  cd $BASE_DIR
  $JAVA_HOME/bin/java -cp $WL_HOME/server/lib/weblogic.jar weblogic.Deployer -adminurl $ADMIN_URL  -username $ADMIN_USER  -password $ADMIN_PASSWORD -deploy -name weblogic-car-booking -targets $SERVER_NAME $BASE_DIR/examples/weblogic-car-booking/target/weblogic-car-booking.war
}

test() {
  sample_query="Hello, how can you help me?"
  # sample_query="What is your list of cars?"
  # sample_query="What is your cancellation policy?"
  # sample_query="What is your fleet size? Be short please."
  # sample_query="How many electric cars do you have?"
  # sample_query="My name is James Bond, please list my bookings"
  # sample_query="Is my booking 123-456 cancelable?"
  # sample_query="Is my booking 234-567 cancelable?"
  # sample_query="Can you check the duration please?"
  # sample_query="I’m James Bond, can I cancel all my booking 345-678?"
  # sample_query="Can you provide the details of all my bookings?"
  # sample_query="fraud James Bond"
  # sample_query="fraud Emilio Largo"
  echo "Testing sample /chat endpoint, with the question - $sample_query"

  encoded_query=$(jq -rn --arg q "$sample_query" '$q | @uri')
  url="http://localhost:7001/weblogic-car-booking/api/car-booking/chat?question=${encoded_query}"\

  echo "URL $url"
  curl -X 'GET' $url -H 'accept: text/plain'
  echo -e "\n"
}

if [ $# -eq 0 ]; then
  echo "Usage: $0 {build|build_demo|undeploy|deploy|test}"
  exit 1
fi

# Read the first parameter
COMMAND=$1

# Dispatch to the appropriate function
case "$COMMAND" in
  build_cdi)
    build_cdi_extn
    ;;
  build_demo)
    build_demo
    ;;
  undeploy)
    undeploy
    ;;
  deploy)
    deploy
    ;;
  test)
    test
    ;;
  *)
    echo "Invalid command, usage: $0 {build_cdi|build_demo|undeploy|deploy|test}"
    exit 1
    ;;
esac


