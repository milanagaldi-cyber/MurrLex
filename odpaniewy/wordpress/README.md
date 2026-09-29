# Od Pani Ewy - WordPress / WooCommerce migration

Installed on 2026-09-29 at https://ml-staging-api.lexaailabs.com:8445/ (Team VPN only). WordPress 7.1.2, WooCommerce 11.1.2, PHP 8.3 and MariaDB 11.4 run in isolated containers. The original Node.js prototype is retained as rollback source.

## Components

- `theme/`: original comic design as a classic WordPress theme. Standard WooCommerce product, cart and checkout rendering, stock and coupon handling. No frontend API keys.
- `mu-plugins/odpaniewy-staging.php`: test-only payment method, synthetic checkout identity, outgoing email disabled, real payment gateways unavailable, classic checkout only.
- `catalog.json` and `seed.php`: one-time import of seven demo products, variable colours, stock, WIOSNA coupon limited to toothbrushes, 12 PLN test delivery and Polish pages. Existing SKU records are not overwritten. Seed never runs on ordinary deployment.
- `deploy/`: isolated Docker Compose stack, separate MariaDB and WordPress volumes, localhost-only backend on 18085, existing VPN-only Nginx entry point on 8445.

All prices, stock and names are demo data. Test orders are real WooCommerce database records marked `_odp_test_order=yes`, without payment or dispatch. No InPost connection is included. Cancelling a test order through WooCommerce restores stock using its normal order handling.

## Deployment

The permanent GitHub secret remains `ODP_SSH_PRIVATE_KEY`. Its forced SSH command accepts only the current `odpaniewy` branch SHA. The one-time installer replaces the root-owned deployment helper after WordPress is ready. Future code deployments create immutable releases, validate PHP, back up the database, recreate only the shop WordPress container and roll back code if health verification fails. Database volumes are retained. Theme/mu-plugin changes deploy automatically; core, WooCommerce and infrastructure updates remain deliberate administrative operations.

Private media is not committed to the public repository. Initial files live in `/var/lib/odpaniewy/media`. Later hero/poster/video replacements can be uploaded using WordPress Appearance > Customize > Od Pani Ewy, without root SSH.

## Move to another hosting provider

Transfer the WordPress files/uploads, theme, staging plugin and database backup. Configure the new WordPress home/site URL, TLS/domain routing and database credentials. Use WP-CLI search-replace with `--dry-run` first for URLs in stored content; WP-CLI handles serialized data. On a managed WordPress host Docker is optional. Keep the staging restrictions until payment, delivery, real catalogue, seller details and legal content are configured and verified.

## Before switch

Validate versions, memory and port availability on the actual VPS. Pin official Docker image digests, install WooCommerce from WordPress.org and verify checksums. Check catalogue, variable products, cross-category cart, coupon scope, shipping, one test order, its stock change, admin access, media and external VPN denial. Preserve the old Node release/Nginx configuration for rollback.
