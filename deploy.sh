#!/usr/bin/env bash
mvn clean package -U -Dmaven.test.skip=true
mvn deploy -Dmaven.test.skip=true 
