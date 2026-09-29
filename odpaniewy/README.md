# Od Pani Ewy shop preview

Standalone Polish shop prototype, developed on the `odpaniewy` branch. The existing MurrLex service and database are independent.

## Preview

Node.js 22+ is required. No npm dependencies. Run `node server.mjs --local-demo` locally and open `http://127.0.0.1:4187/odpaniewy/szczoteczki`. `ODP_BASE=/` supports deployment at the root of a separate domain. Run `node --test tests/*.test.mjs` to verify the app.

The catalogue, prices, stock and checkout are demonstrations. There are no real payments, orders or personal-data forms. WIOSNA discounts brushes by 5%; delivery and toothpaste are excluded.

## Private staging

URL: `https://ml-staging-api.lexaailabs.com:8445/szczoteczki` (Team VPN required).

Linux production uses `ODP_ACCESS=vpn-socket` and a Unix socket in `/run/odpaniewy`. The dedicated Nginx listener, source allow-list and firewall protect HTML, API and all assets. The Node service rejects TCP connections in this mode. No MurrLex login or subscription is needed. Shopping carts use independent random Secure/HttpOnly session cookies, CSRF and exact Origin validation.

User-supplied images and video are intentionally absent from this public repository. They are installed separately under `/var/lib/odpaniewy/media`, served through the protected app using `ODP_ASSETS`. Do not publish this directory as a static alias or commit its contents.

## Deployment

The workflow `.github/workflows/deploy-odpaniewy.yml` runs on changes to this shop in branch `odpaniewy`. Repository secret `ODP_SSH_PRIVATE_KEY` supplies a dedicated SSH key. Its account only accepts `deploy <40-character SHA>`, and the server verifies that SHA is the current branch head. Tests run before switching releases. The only restart is `odpaniewy.service`; a failed application health check restores the previous release. Templates in `deploy/` are installed by an administrator, never automatically installed by the limited deploy key.

Server paths: `/opt/odpaniewy/repo`, `/opt/odpaniewy/releases`, `/opt/odpaniewy/current`, `/opt/odpaniewy/runtime`. Keep the existing `/opt/MurrLex` checkout unchanged. Changing infrastructure requires a separate administrative action.

## Future hosting

The intended production shop is a separate WordPress + WooCommerce installation under `odpaniewy.pl`, with one database, catalogue and cart. This Node mock is a presentation stage. Production requires moving the design to a WooCommerce theme and replacing the mock catalogue/cart/checkout with WooCommerce and real shipping/payment integrations. VPN protection is removed only when the public launch is explicitly approved.
