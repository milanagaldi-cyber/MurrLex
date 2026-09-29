<?php
/**
 * Plugin Name: Od Pani Ewy - shop management
 * Description: Homepage selection, campaign links and basic search metadata.
 * Version: 1.1.0
 */
defined('ABSPATH') || exit;

function odp_growth_settings() {
    return wp_parse_args((array) get_option('odp_growth', []), [
        'category' => 0, 'featured' => 0, 'alternatives' => [],
        'coupon' => 'WIOSNA',
        'seo_title' => 'Szczoteczki od Pani Ewy | Kolorowe codzienne rytuały',
        'seo_description' => 'Poznaj szczoteczki od Pani Ewy. Wybierz swój kolor, porównaj produkty i znajdź pomysł na codzienny rytuał szczotkowania.',
    ]);
}
function odp_growth_category() {
    $id = absint(odp_growth_settings()['category']);
    if ($id && term_exists($id, 'product_cat')) return $id;
    $term = get_term_by('slug', 'szczoteczki', 'product_cat');
    return $term ? (int) $term->term_id : 0;
}
function odp_growth_product($id, $category = 0) {
    if (!function_exists('wc_get_product')) return false;
    $product = wc_get_product(absint($id));
    if (!$product || $product->get_status() !== 'publish' || !$product->is_visible() || $product->is_type('variation')) return false;
    if ($category && !has_term($category, 'product_cat', $product->get_id())) return false;
    return $product;
}
function odp_growth_featured() {
    $settings = odp_growth_settings();
    $id = $settings['featured'];
    if (!array_key_exists('featured', (array) get_option('odp_growth', [])) && function_exists('wc_get_product_id_by_sku')) $id = wc_get_product_id_by_sku('DEMO-BRUSH-01');
    return odp_growth_product($id, odp_growth_category());
}
function odp_growth_alternatives() {
    $settings = odp_growth_settings();
    $ids = (array) $settings['alternatives'];
    // Defaults apply only before the first save. An empty saved selection stays empty.
    if (!array_key_exists('alternatives', (array) get_option('odp_growth', [])) && function_exists('wc_get_product_id_by_sku')) {
        $ids = array_map('wc_get_product_id_by_sku', ['DEMO-BRUSH-02', 'DEMO-BRUSH-03', 'DEMO-BRUSH-04']);
    }
    $featured = odp_growth_featured();
    $result = [];
    foreach ($ids as $id) {
        $product = odp_growth_product($id, odp_growth_category());
        if ($product && (!$featured || $product->get_id() !== $featured->get_id())) $result[$product->get_id()] = $product->get_id();
    }
    return array_slice(array_values($result), 0, 3);
}
function odp_growth_coupon() {
    if (!class_exists('WC_Coupon')) return false;
    $code = odp_growth_settings()['coupon'];
    if (!$code) return false;
    $coupon = new WC_Coupon($code);
    if (!$coupon->get_id() || get_post_status($coupon->get_id()) !== 'publish') return false;
    $expiry = $coupon->get_date_expires();
    if ($expiry && $expiry->getTimestamp() < time()) return false;
    if ($coupon->get_usage_limit() && $coupon->get_usage_count() >= $coupon->get_usage_limit()) return false;
    return $coupon;
}
function odp_growth_sanitize($input) {
    $input = is_array($input) ? $input : [];
    $category = absint($input['category'] ?? 0);
    if (!$category || !term_exists($category, 'product_cat')) {
        $category = odp_growth_category();
        add_settings_error('odp_growth', 'category', 'Wybierz istniejącą kategorię produktów.');
    }
    $featured = odp_growth_product($input['featured'] ?? 0, $category);
    $ids = [];
    foreach ((array) ($input['alternatives'] ?? []) as $id) {
        $product = odp_growth_product($id, $category);
        if ($product && (!$featured || $product->get_id() !== $featured->get_id())) $ids[$product->get_id()] = $product->get_id();
    }
    if (count($ids) > 3) add_settings_error('odp_growth', 'limit', 'Zapisano pierwsze trzy dodatkowe produkty.', 'warning');
    $requested = array_filter(array_map('absint', (array) ($input['alternatives'] ?? [])));
    if ((!$featured && !empty($input['featured'])) || count($ids) < count(array_unique($requested))) {
        add_settings_error('odp_growth', 'products', 'Pominięto powtórzone produkty, produkty spoza kategorii lub niewidoczne w sklepie.', 'warning');
    }
    $code = sanitize_text_field($input['coupon'] ?? '');
    if ($code && class_exists('WC_Coupon')) {
        $coupon = new WC_Coupon($code);
        if (!$coupon->get_id() || get_post_status($coupon->get_id()) !== 'publish') {
            add_settings_error('odp_growth', 'coupon', 'Nie znaleziono opublikowanego kuponu. Baner pozostaje wyłączony.', 'warning');
            $code = '';
        }
    }
    return [
        'category' => $category,
        'featured' => $featured ? $featured->get_id() : 0,
        'alternatives' => array_slice(array_values($ids), 0, 3),
        'coupon' => $code,
        'seo_title' => sanitize_text_field($input['seo_title'] ?? ''),
        'seo_description' => sanitize_textarea_field($input['seo_description'] ?? ''),
    ];
}

// One-time migration; later changes in WooCommerce's native settings are respected.
if (defined('ODP_STAGING') && ODP_STAGING && add_option('odp_growth_attribution_v1', '1', '', false)) {
    update_option('woocommerce_feature_order_attribution_enabled', 'yes');
}
add_action('init', function () {
    if (!defined('ODP_STAGING') || !ODP_STAGING || get_option('odp_growth_privacy_v1')) return;
    $page = get_page_by_path('prywatnosc');
    if (!$page) return;
    $old = '<p>Sklep jest dostępny wyłącznie w Team VPN. Pliki cookie utrzymują koszyk. Testowe zamówienia zapisują wybrane produkty i wartości. Nie podawaj danych osobowych. Nie używamy analityki ani pikseli reklamowych.</p>';
    if ($page->post_content === $old) {
        $result = wp_update_post(['ID' => $page->ID, 'post_content' => '<p>Sklep jest dostępny wyłącznie w Team VPN. Pliki cookie utrzymują koszyk oraz źródło wizyty w bieżącej sesji. Po złożeniu testowego zamówienia WooCommerce zapisuje jego źródło, metki kampanii i informacje o sesji, obok produktów i wartości. Nie podawaj danych osobowych. Nie wysyłamy statystyk do zewnętrznych usług analitycznych ani pikseli reklamowych.</p>'], true);
        if (is_wp_error($result)) return;
    }
    update_option('odp_growth_privacy_v1', '1', false);
}, 20);
add_filter('option_page_capability_odp_growth_group', function () { return 'manage_woocommerce'; });
add_action('admin_init', function () {
    register_setting('odp_growth_group', 'odp_growth', ['type' => 'array', 'sanitize_callback' => 'odp_growth_sanitize']);
});
add_action('admin_menu', function () {
    add_menu_page('Od Pani Ewy', 'Od Pani Ewy', 'manage_woocommerce', 'odp-shop', 'odp_growth_admin', 'dashicons-store', 56);
});
add_action('admin_enqueue_scripts', function ($hook) {
    if ($hook !== 'toplevel_page_odp-shop' || !class_exists('WooCommerce')) return;
    wp_enqueue_script('wc-enhanced-select');
    wp_enqueue_style('woocommerce_admin_styles', WC()->plugin_url() . '/assets/css/admin.css', [], WC_VERSION);
    wp_enqueue_script('odp-admin', plugins_url('odpaniewy-admin.js', __FILE__), [], '1.1.0', true);
});
function odp_growth_select($name, $ids, $multiple = false) {
    echo '<select class="wc-product-search" style="width:100%;max-width:600px" name="odp_growth[' . esc_attr($name) . ']' . ($multiple ? '[]' : '') . '" ' . ($multiple ? 'multiple="multiple"' : '') . ' data-placeholder="Wyszukaj produkt po nazwie lub SKU" data-allow_clear="true" data-action="woocommerce_json_search_products" data-limit="20">';
    foreach ((array) $ids as $id) {
        $product = wc_get_product(absint($id));
        if ($product) echo '<option selected value="' . absint($id) . '">' . esc_html($product->get_formatted_name()) . '</option>';
    }
    echo '</select>';
}
function odp_growth_admin() {
    if (!current_user_can('manage_woocommerce')) return;
    echo '<div class="wrap"><h1>Od Pani Ewy - Twój sklep</h1>';
    if (!class_exists('WooCommerce')) { echo '<p>Włącz WooCommerce, aby zarządzać sklepem.</p></div>'; return; }
    $settings = odp_growth_settings();
    $featured = odp_growth_featured();
    settings_errors('odp_growth');
    echo '<p><a class="button" href="' . esc_url(home_url('/')) . '">Otwórz sklep</a> <a class="button" href="' . esc_url(admin_url('edit.php?post_type=shop_coupon')) . '">Promokody</a> <a class="button" href="' . esc_url(admin_url('admin.php?page=wc-admin&path=/analytics/coupons')) . '">Statystyki promokodów</a> <a class="button" href="' . esc_url(admin_url('admin.php?page=wc-orders')) . '">Zamówienia i źródła</a></p>';
    echo '<form action="options.php" method="post">';
    settings_fields('odp_growth_group');
    echo '<h2>Główny produkt i propozycje</h2><p>Wybierz jeden hit i 2-3 inne produkty z tej samej kategorii. Ceny i dostępność pobieramy z WooCommerce.</p><table class="form-table"><tr><th><label for="odp-category">Główna kategoria</label></th><td>';
    wp_dropdown_categories(['taxonomy' => 'product_cat', 'hide_empty' => false, 'name' => 'odp_growth[category]', 'id' => 'odp-category', 'selected' => odp_growth_category(), 'hierarchical' => true]);
    echo '<p class="description">Wybierz produkty przypisane bezpośrednio do tej kategorii.</p></td></tr><tr><th>Hit</th><td>';
    odp_growth_select('featured', $featured ? [$featured->get_id()] : []);
    echo '</td></tr><tr><th>Dodatkowe produkty</th><td>';
    odp_growth_select('alternatives', odp_growth_alternatives(), true);
    echo '<p class="description">Maksymalnie 3. Kolejność wyboru jest kolejnością na stronie. Pozostałe produkty są dostępne w katalogu.</p></td></tr><tr><th><label for="odp-coupon">Promowany kod</label></th><td><input id="odp-coupon" name="odp_growth[coupon]" class="regular-text" value="' . esc_attr($settings['coupon']) . '"><p class="description">Istniejący kod z WooCommerce. Puste pole wyłącza baner. Zniżki i ograniczenia ustawiasz w sekcji Promokody. Wygasłe i wykorzystane kody są ukrywane.</p></td></tr></table>';
    echo '<h2>SEO strony głównej</h2><p>Wersja Team VPN pozostaje wyłączona z indeksowania. Te teksty będą przydatne także po uruchomieniu publicznym.</p><table class="form-table"><tr><th><label for="odp-title">Tytuł</label></th><td><input id="odp-title" name="odp_growth[seo_title]" class="large-text" maxlength="100" value="' . esc_attr($settings['seo_title']) . '"></td></tr><tr><th><label for="odp-description">Opis</label></th><td><textarea id="odp-description" name="odp_growth[seo_description]" class="large-text" rows="3" maxlength="320">' . esc_textarea($settings['seo_description']) . '</textarea></td></tr></table>';
    submit_button('Zapisz ustawienia sklepu');
    echo '</form><hr><h2>Link do kampanii</h2><p>Utwórz osobny link dla każdego kanału lub autora. Kod rabatowy i źródło wizyty to różne informacje. Ten formularz nie zapisuje danych i nie zmienia ustawień powyżej.</p>';
    echo '<form id="odp-campaign"><table class="form-table">';
    foreach (['url' => ['Adres strony', home_url('/')], 'source' => ['Źródło (np. tiktok, instagram)', 'tiktok'], 'medium' => ['Medium (np. social, paid_social)', 'social'], 'campaign' => ['Nazwa kampanii', ''], 'content' => ['Autor lub materiał (opcjonalnie)', '']] as $key => [$label, $value]) {
        echo '<tr><th><label for="odp-' . esc_attr($key) . '">' . esc_html($label) . '</label></th><td><input class="large-text" id="odp-' . esc_attr($key) . '" name="' . esc_attr($key) . '" value="' . esc_attr($value) . '" ' . ($key !== 'content' ? 'required' : '') . '></td></tr>';
    }
    echo '</table><button class="button" type="submit">Utwórz link</button><p><label for="odp-campaign-result">Gotowy link</label></p><textarea id="odp-campaign-result" rows="3" class="large-text" readonly></textarea><p id="odp-campaign-status" role="status"></p></form>';
    $enabled = get_option('woocommerce_feature_order_attribution_enabled') === 'yes';
    echo '<h2>Źródła zamówień</h2><p>Zapisywanie źródeł: <strong>' . ($enabled ? 'włączone' : 'wyłączone') . '</strong>. <a href="' . esc_url(admin_url('admin.php?page=wc-settings&tab=advanced&section=features')) . '">Zmień w ustawieniach WooCommerce</a>.</p><p>Źródło znajdziesz przy nowych zamówieniach. To nie jest statystyka wszystkich odwiedzin; stare zamówienia nie otrzymają źródła wstecz. Raporty na tym serwerze zawierają dane testowe.</p><p>Przed publicznym startem skonfigurujemy osobno analitykę odwiedzin i zgodę na jej uruchomienie. Asystent AI zostanie dodany po wyborze usługi.</p></div>';
}

function odp_growth_external_seo() {
    return defined('WPSEO_VERSION') || defined('RANK_MATH_VERSION') || defined('AIOSEO_VERSION') || defined('SEOPRESS_VERSION');
}
function odp_growth_seo_page() {
    if (odp_growth_external_seo() || is_feed() || is_404() || is_search() || is_paged()) return false;
    if (function_exists('is_cart') && (is_cart() || is_checkout() || is_account_page())) return false;
    return is_front_page() || is_singular('product') || is_page();
}
add_filter('pre_get_document_title', function ($title) {
    return !odp_growth_external_seo() && is_front_page() && odp_growth_settings()['seo_title'] ? odp_growth_settings()['seo_title'] : $title;
});
add_action('wp_head', function () {
    if (!odp_growth_seo_page()) return;
    $id = get_queried_object_id();
    if (is_front_page()) $description = odp_growth_settings()['seo_description'];
    else $description = get_post_field('post_excerpt', $id) ?: get_post_field('post_content', $id);
    $description = preg_replace('/\s+/u', ' ', trim(wp_strip_all_tags(strip_shortcodes($description))));
    if (function_exists('mb_substr')) $description = mb_substr($description, 0, 320);
    $url = is_front_page() ? home_url('/') : get_permalink($id);
    $image = get_the_post_thumbnail_url($id, 'large');
    if (is_front_page() && function_exists('odp_asset')) $image = odp_asset('hero-fixed.png');
    if ($description) echo '<meta name="description" content="' . esc_attr($description) . '">' . "\n";
    foreach (['og:type' => 'website', 'og:locale' => get_locale(), 'og:site_name' => get_bloginfo('name'), 'og:title' => wp_get_document_title(), 'og:description' => $description, 'og:url' => $url, 'og:image' => $image] as $property => $content) {
        if ($content) echo '<meta property="' . esc_attr($property) . '" content="' . esc_attr($content) . '">' . "\n";
    }
    echo '<meta name="twitter:card" content="summary_large_image">' . "\n";
}, 5);
