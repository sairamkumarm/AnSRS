NAME := $(shell mvn help:evaluate -Dexpression='project.artifactId' -q -DforceStdout)
VERSION := $(shell mvn help:evaluate -Dexpression='project.version' -q -DforceStdout)
JAR_PATH := target/$(NAME)-$(VERSION).jar

.PHONY: build native

build:
	mvn clean package

native: build
	native-image --no-fallback -jar $(JAR_PATH) $(NAME)