NAME := $(shell mvn help:evaluate -Dexpression='project.artifactId' -q -DforceStdout)
VERSION := $(shell mvn help:evaluate -Dexpression='project.version' -q -DforceStdout)
JAR_PATH := target/$(NAME)-$(VERSION).jar

ifndef GRAALVM_HOME
$(error GRAALVM_HOME is not set. Set it to your GraalVM install path or specify GRAAL_VM_JAVA_PATH on the make command line)
endif

GRAAL_VM_JAVA_PATH ?= $(GRAALVM_HOME)/bin/java

.PHONY: build trace native

trace:
	$(GRAAL_VM_JAVA_PATH) \
		-agentlib:native-image-agent=config-output-dir=src/main/resources/native-image \
		-jar $(JAR_PATH)

build:
	mvn clean package

native:
	$(MAKE) trace
	$(MAKE) build
	native-image --no-fallback -jar $(JAR_PATH) $(NAME)
