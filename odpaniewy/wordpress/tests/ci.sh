#!/usr/bin/env bash
set -Eeuo pipefail
source_dir=$(pwd)/wordpress
test_dir=$(mktemp -d)
trap 'rm -rf -- "$test_dir"' EXIT
curl --fail --silent --show-error --location https://raw.githubusercontent.com/wp-cli/builds/gh-pages/phar/wp-cli.phar -o "$test_dir/wp.phar"
wp() { php "$test_dir/wp.phar" --path="$test_dir/site" --url=http://127.0.0.1:8089 "$@"; }
# Use the official ZIP: PHP PharData's tar extraction truncates long core paths.
curl --fail --silent --show-error --location https://wordpress.org/wordpress-7.1.2.zip -o "$test_dir/core.zip"
unzip -q "$test_dir/core.zip" -d "$test_dir"
mv "$test_dir/wordpress" "$test_dir/site"
wp config create --dbname=odp_ci --dbuser=root --dbpass=odp-ci-only --dbhost=127.0.0.1:3306 --skip-check --extra-php <<'PHP'
define('ODP_STAGING', true);
define('ODP_CI', true);
define('WP_HOME', 'http://127.0.0.1:8089');
define('WP_SITEURL', 'http://127.0.0.1:8089');
define('WP_ENVIRONMENT_TYPE', 'staging');
define('DISABLE_WP_CRON', true);
PHP
wp core install --url=http://127.0.0.1:8089 --title='Od Pani Ewy CI' --admin_user=ci-admin --admin_password="$(openssl rand -hex 24)" --admin_email=ci@example.invalid --skip-email
wp core verify-checksums
wp plugin install woocommerce --version=11.1.2 --activate
wp plugin verify-checksums woocommerce
mkdir -p "$test_dir/site/wp-content/mu-plugins"
cp -R "$source_dir/theme" "$test_dir/site/wp-content/themes/odpaniewy"
cp "$source_dir/mu-plugins/"* "$test_dir/site/wp-content/mu-plugins/"
wp eval-file "$source_dir/seed.php"
ODP_CI=1 wp eval-file "$source_dir/tests/growth-integration.php"
php -S 127.0.0.1:8089 -t "$test_dir/site" > "$test_dir/http.log" 2>&1 &
server_pid=$!
trap 'kill "$server_pid" 2>/dev/null || true; rm -rf -- "$test_dir"' EXIT
for attempt in {1..10}; do
  if curl --fail --silent --show-error --location --max-time 15 -D "$test_dir/home.headers" http://127.0.0.1:8089/ -o "$test_dir/home.html"; then break; fi
  sleep 1
done
if ! python3 "$source_dir/tests/check-home.py" "$test_dir/home.html"; then
  cat "$test_dir/home.headers"
  head -c 6000 "$test_dir/home.html"
  cat "$test_dir/http.log"
  exit 1
fi
