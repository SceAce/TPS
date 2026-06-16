#!/usr/bin/env bash
# Script: prepare the first local TPS deployment database and runtime folders.

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SQL_FILE="${TPS_INIT_SQL:-$ROOT_DIR/deploy/database/tps-init.sql}"

DB_HOST="${TPS_DB_HOST:-127.0.0.1}"
DB_PORT="${TPS_DB_PORT:-3306}"
DB_USERNAME="${TPS_DB_USERNAME:-root}"
DB_PASSWORD="${TPS_DB_PASSWORD:-root}"
UPLOAD_DIR="${TPS_UPLOAD_DIR:-$ROOT_DIR/img}"

usage() {
  cat <<EOF
Usage: $(basename "$0") [--dry-run]

Environment variables:
  TPS_DB_HOST       MySQL/MariaDB host, default: 127.0.0.1
  TPS_DB_PORT       MySQL/MariaDB port, default: 3306
  TPS_DB_USERNAME   Database user, default: root
  TPS_DB_PASSWORD   Database password, default: root
  TPS_INIT_SQL      SQL package path, default: deploy/database/tps-init.sql
  TPS_UPLOAD_DIR    Upload directory, default: ./img
  MYSQL_BIN         mysql executable path, default: mysql from PATH
  DRY_RUN           Set to 1 to print actions without connecting to MySQL

After deployment, start backend with:
  TPS_DB_USERNAME="$DB_USERNAME" TPS_DB_PASSWORD='<password>' ./start-backend.sh
EOF
}

DRY_RUN="${DRY_RUN:-0}"
for arg in "$@"; do
  case "$arg" in
    --dry-run)
      DRY_RUN=1
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $arg" >&2
      usage >&2
      exit 2
      ;;
  esac
done

if [ ! -f "$SQL_FILE" ]; then
  echo "SQL package not found: $SQL_FILE" >&2
  exit 1
fi

MYSQL_CMD="${MYSQL_BIN:-$(command -v mysql 2>/dev/null || true)}"
if [ "$DRY_RUN" != "1" ] && { [ -z "$MYSQL_CMD" ] || [ ! -x "$MYSQL_CMD" ]; }; then
  echo "mysql executable not found. Install MariaDB/MySQL client first, or set MYSQL_BIN." >&2
  exit 1
fi

echo "Preparing TPS first deployment"
echo "Root dir: $ROOT_DIR"
echo "SQL package: $SQL_FILE"
echo "Database: $DB_USERNAME@$DB_HOST:$DB_PORT/tps"
echo "Upload dir: $UPLOAD_DIR"

if [ "$DRY_RUN" = "1" ]; then
  echo
  echo "DRY_RUN=1, no database connection will be made."
  echo "Would run:"
  echo "  mkdir -p \"$UPLOAD_DIR\""
  echo "  mysql --host=\"$DB_HOST\" --port=\"$DB_PORT\" --user=\"$DB_USERNAME\" --password=*** < \"$SQL_FILE\""
  exit 0
fi

mkdir -p "$UPLOAD_DIR"

"$MYSQL_CMD" \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --user="$DB_USERNAME" \
  --password="$DB_PASSWORD" \
  < "$SQL_FILE"

echo
echo "Database package imported successfully."
echo "Next:"
echo "  TPS_DB_USERNAME=\"$DB_USERNAME\" TPS_DB_PASSWORD='<password>' ./start-backend.sh"
