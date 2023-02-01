#!/bin/sh

set -m

dockerd & # workaround, use docker-entrypoint.sh somehow

sleep 10

/app/run-java.sh
