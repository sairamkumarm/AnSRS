NAME := $(shell mvn help:evaluate -Dexpression='project.artifactId' -q -DforceStdout)
VERSION := $(shell mvn help:evaluate -Dexpression='project.version' -q -DforceStdout)
JAR_PATH := target/$(NAME)-$(VERSION).jar
GRAAL_VM_JAVA_PATH ?= java
.PHONY: build trace native

build:
	mvn clean package

trace: build
	$(GRAAL_VM_JAVA_PATH) -agentlib:native-image-agent=config-output-dir=src/main/resources/native-image -jar $(JAR_PATH)

native: build
	native-image --no-fallback -jar $(JAR_PATH) $(NAME)