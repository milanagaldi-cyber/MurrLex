<?php
// Invoke once using WP-CLI eval-file. Never run automatically on ordinary deploys.
defined('WP_CLI') && WP_CLI || exit;
if (!class_exists('WooCommerce')) WP_CLI::error('Activate WooCommerce first.');
if (!defined('ODP_STAGING') || !ODP_STAGING) WP_CLI::error('Seed is limited to staging.');
if (get_option('odp_seed_version') === '1') { WP_CLI::success('Shop already initialized; existing data preserved.'); return; }
$catalog = json_decode(file_get_contents(__DIR__.'/catalog.json'), true, 512, JSON_THROW_ON_ERROR);
$categories = [];
foreach (['szczoteczki'=>'Szczoteczki', 'pasty'=>'Pasty', 'kosmetyki'=>'Kosmetyki'] as $slug=>$label) {
    $term = term_exists($slug, 'product_cat');
    if (!$term) $term = wp_insert_term($label, 'product_cat', ['slug'=>$slug]);
    if (is_wp_error($term)) WP_CLI::error($term->get_error_message());
    $categories[$slug] = (int)$term['term_id'];
}
foreach ($catalog as $index=>$row) {
    if (wc_get_product_id_by_sku($row['id'])) continue;
    $variable = count($row['variants']) > 1;
    $product = $variable ? new WC_Product_Variable() : new WC_Product_Simple();
    $product->set_name($row['name']);
    $product->set_slug($row['slug']);
    $product->set_sku($row['id']);
    $product->set_status('publish');
    $product->set_description($row['note']);
    $product->set_short_description($row['subtitle']);
    $product->set_category_ids([$categories[$row['category']]]);
    $product->set_featured(!empty($row['featured']));
    $product->set_menu_order($index);
    $product->set_reviews_allowed(false);
    $product->set_tax_status('none');
    $product->update_meta_data('_odp_colors', $row['colors']);
    $product->update_meta_data('_odp_group', $row['group']);
    if ($variable) {
        $attribute = new WC_Product_Attribute();
        $attribute->set_name('Kolor');
        $attribute->set_options(array_column($row['variants'], 'label'));
        $attribute->set_visible(true);
        $attribute->set_variation(true);
        $product->set_attributes([$attribute]);
    } else {
        $product->set_regular_price(number_format($row['price']/100, 2, '.', ''));
        $product->set_manage_stock(true);
        $product->set_stock_quantity($row['variants'][0]['stock']);
        $product->set_stock_status($row['variants'][0]['stock'] ? 'instock' : 'outofstock');
    }
    $product->save();
    if ($variable) foreach ($row['variants'] as $variant) {
        $child = new WC_Product_Variation();
        $child->set_parent_id($product->get_id());
        $child->set_attributes(['kolor'=>$variant['label']]);
        $child->set_sku($row['id'].'-'.$variant['id']);
        $child->set_regular_price(number_format($row['price']/100, 2, '.', ''));
        $child->set_manage_stock(true);
        $child->set_stock_quantity($variant['stock']);
        $child->set_stock_status($variant['stock'] ? 'instock' : 'outofstock');
        $child->set_tax_status('none');
        $child->save();
    }
    if ($variable) WC_Product_Variable::sync($product->get_id());
}
$pages = [
    'szczoteczki'=>['Szczoteczki od Pani Ewy', ''],
    'pasty'=>['Dobry duet dla szczoteczki', '[products category="pasty" columns="3" limit="12" paginate="true"]'],
    'kosmetyki'=>['Więcej dobrych rytuałów', '<p>Już w przyszłości. Asortyment kosmetyków jest w przygotowaniu.</p>'],
    'koszyk'=>['Twój koszyk', '[woocommerce_cart]'],
    'zamowienie'=>['Zamówienie testowe', '[woocommerce_checkout]'],
    'moje-konto'=>['Moje konto', '[woocommerce_my_account]'],
    'sklep'=>['Wszystkie produkty', ''],
    'kontakt'=>['Kontakt', '<p>Dane kontaktowe sprzedawcy zostaną dodane przed uruchomieniem sprzedaży.</p>'],
    'dostawa'=>['Dostawa i płatności', '<p>Testowa dostawa kosztuje 12,00 zł. Nie przyjmujemy płatności i nie nadajemy przesyłek. InPost Pay nie jest jeszcze podłączony.</p>'],
    'zwroty'=>['Zwroty i reklamacje', '<p>W sklepie testowym nie ma rzeczywistych zakupów. Procedura zostanie uzupełniona przed rozpoczęciem sprzedaży.</p>'],
    'regulamin'=>['Regulamin - do przygotowania', '<p>To sklep testowy, nie oferta handlowa. Regulamin wymaga zatwierdzenia przez sprzedawcę przed uruchomieniem sprzedaży.</p>'],
    'prywatnosc'=>['Prywatność w sklepie testowym', '<p>Sklep jest dostępny wyłącznie w Team VPN. Pliki cookie utrzymują koszyk. Testowe zamówienia zapisują wybrane produkty i wartości. Nie podawaj danych osobowych. Nie używamy analityki ani pikseli reklamowych.</p>'],
];
$ids = [];
foreach ($pages as $slug=>[$title,$content]) {
    $existing = get_page_by_path($slug);
    $ids[$slug] = $existing ? $existing->ID : wp_insert_post(['post_type'=>'page', 'post_status'=>'publish', 'post_name'=>$slug, 'post_title'=>$title, 'post_content'=>$content], true);
    if (is_wp_error($ids[$slug])) WP_CLI::error($ids[$slug]->get_error_message());
}
foreach (['cart'=>'koszyk','checkout'=>'zamowienie','myaccount'=>'moje-konto','shop'=>'sklep'] as $option=>$slug) update_option('woocommerce_'.$option.'_page_id', $ids[$slug]);
update_option('show_on_front', 'page');
update_option('page_on_front', $ids['szczoteczki']);
update_option('wp_page_for_privacy_policy', $ids['prywatnosc']);
update_option('woocommerce_currency', 'PLN');
update_option('woocommerce_default_country', 'PL');
update_option('woocommerce_default_customer_address', 'base');
update_option('woocommerce_calc_taxes', 'no');
update_option('woocommerce_enable_guest_checkout', 'yes');
update_option('woocommerce_enable_signup_and_login_from_checkout', 'no');
update_option('woocommerce_enable_myaccount_registration', 'no');
update_option('woocommerce_enable_coupons', 'yes');
update_option('woocommerce_permalinks', ['product_base'=>'/produkt', 'category_base'=>'kategoria', 'tag_base'=>'tag-produktu', 'attribute_base'=>'']);
update_option('blog_public', '0');
update_option('timezone_string', 'Europe/Warsaw');
update_option('woocommerce_coming_soon', 'no');
update_option('woocommerce_feature_order_attribution_enabled', 'no');
update_option('woocommerce_allow_tracking', 'no');
$zone = new WC_Shipping_Zone(0);
$existing_flat = array_filter($zone->get_shipping_methods(), fn($m)=>$m->id === 'flat_rate');
if (!$existing_flat) {
    $instance = $zone->add_shipping_method('flat_rate');
    update_option('woocommerce_flat_rate_'.$instance.'_settings', ['title'=>'Dostawa testowa (bez wysyłki)', 'tax_status'=>'none', 'cost'=>'12.00']);
}
if (!wc_get_coupon_id_by_code('WIOSNA')) {
    $coupon = new WC_Coupon();
    $coupon->set_code('WIOSNA');
    $coupon->set_discount_type('percent');
    $coupon->set_amount(5);
    $coupon->set_product_categories([$categories['szczoteczki']]);
    $coupon->set_description('Test: 5% na szczoteczki. Bez past i dostawy.');
    $coupon->save();
}
switch_theme('odpaniewy');
global $wp_rewrite;
$wp_rewrite->set_permalink_structure('/%postname%/');
flush_rewrite_rules(true);
update_option('odp_seed_version', '1');
WP_CLI::success('Native WooCommerce test shop initialized.');
