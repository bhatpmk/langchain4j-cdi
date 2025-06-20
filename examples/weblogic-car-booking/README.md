#  LangChain4j with WebLogic

## Introduction

This example demonstrates [LangChain4J](https://docs.langchain4j.dev/) with WebLogic 15.1. It aims at studying how to leverage LLMs capabilities in enterprise java applications. 

It is derived from my [Quarkus-LangChain4j](https://github.com/jefrajames/car-booking) example used to illustrate my talk at [JChateau 2024](https://www.jchateau.org).

It is based on a simplified car booking application inspired from the [Java meets AI](https://www.youtube.com/watch?v=BD1MSLbs9KE) talk from [Lize Raes](https://www.linkedin.com/in/lize-raes-a8a34110/) at Devoxx Belgium 2023 with additional work from [Jean-François James](http://jefrajames.fr/). The original demo is from [Dmytro Liubarskyi](https://www.linkedin.com/in/dmytro-liubarskyi/). The car booking company is called "Miles of Smiles" and the application exposes two AI services:

. a chat service to freely discuss with a customer assistant
. a fraud service to determine if a customer is a fraudster, which is not yet enabled here.

For the sake of simplicity, there is no database interaction, the application is standalone and can be used "as is".

Note: This is an initial version of the application, with only /chat endpoint. Once the application is enhanced, the instructions will be updated.

## Technical context

The project has been developed and tested with:

* Java 17
* WebLogic 15.1
* LangChain4j 1.0.1
* Maven 3.9.9

As WebLogic 15.1.1 is Jakarta EE 9.1 compliant, certain changes are made to build langchani4j-cdi-core and langchain4j-cdi-portable-extn with
Jakarta EE API 9.1.0, Jakarta Enterprise CDI API version 3.0.1 and Weld 4.0.3.Final.

The project expects Ollama server is accessible via the URL http://localhost:11434/ and LLaMA 3 language model llama3.1 is running.

## Packaging the application

### Build langchani4j-cdi-core and langchain4j-cdi-portable-extn, before building weblogic-car-booking
```
cd <project_root>/langchain4j-cdi-core; mvn clean install
cd <project_root>/langchain4j-cdi-config; mvn clean install
cd <project_root>/langchain4j-cdi-portable-extn; mvn clean install -DskipTests
cd <project_root>/examples/weblogic-car-booking; mvn clean package
```

## Configuration

All configuration is centralized in src/main/java/resources/META-INF/llm-config.properties microprofile-config.properties.
The configuration is packaged inside WEB-INF/classes/META-INF in the .war file built from maven-war-plugin. 

## Running the application

* Start WebLogic 15.1.1 server
* Deploy the application weblogic-car-booking.war using a tool of your choice. Here is the sample deployment command using weblogic.Deployer
```
$JAVA_HOME/bin/java -cp $WL_HOME/server/lib/weblogic.jar weblogic.Deployer -adminurl t3://<admin host>:<admin port>  -username <user>  -password <password> -deploy -name weblogic-car-booking -targets AdminServer <path>/weblogic-car-booking.war
```

## Access chat service
```
curl -X 'GET' 'http://<host>:<port>/weblogic-car-booking/api/car-booking/chat?question=I%20want%20to%20book%20a%20car%20how%20can%20you%20help%20me%3F' -H 'accept: text/plain'
```

For more information, please see my [Quarkus-LangChain4j](https://github.com/jefrajames/car-booking) example.
