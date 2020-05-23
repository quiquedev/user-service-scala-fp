#!/bin/sh
# run-app.sh

wait-for-postgres.sh

java -jar user-service.jar
