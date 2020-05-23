#!/bin/sh
# wait-for-postgres-and-start app.sh

set -e
  
 >&2 echo "Checking Postgres status"

until PGPASSWORD=$DB_FLYWAY_PASSWORD psql -h $DB_HOST -p $DB_PORT -d $DB_DATABASE -U $DB_FLYWAY_USER -c '\q'; do
  >&2 echo "Postgres is down - retry in 1 second"
  sleep 1
done

>&2 echo "Postgres is up - starting app"

exec java -jar user-service.jar

