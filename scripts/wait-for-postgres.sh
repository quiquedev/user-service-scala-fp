#!/bin/sh
# wait-for-postgres.sh

set -e
  
PGUSER=$DB_FLYWAY_USER
PGPASSWORD=$DB_FLYWAY_PASSWORD

until  psql -h $DB_HOST -d $DB_DATABASE -c '\q'; do
  >&2 echo "Postgres is unavailable - sleeping"
  sleep 1
done

