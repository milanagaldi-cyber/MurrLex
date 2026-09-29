<?php
defined('ABSPATH') || exit;
add_action('after_setup_theme', function () {
    add_theme_support('title-tag');
    add_theme_support('post-thumbnails');
    add_theme_support('woocommerce');
    add_theme_support('wc-product-gallery-slider');
    add_theme_support('wc-product-gallery-lightbox');
    add_theme_support('html5', ['search-form', 'gallery', 'caption', 'style', 'script']);
});
add_action('wp_enqueue_scripts', function () {
    $uri = get_template_directory_uri();
    wp_enqueue_style('odp-design', $uri . '/design.css', [], '1.0.0');
    wp_enqueue_style('odp-commerce', $uri . '/commerce.css', ['odp-design'], '1.0.0');
    wp_enqueue_script('odp-ui', $uri . '/ui.js', [], '1.0.0', true);
    if (class_exists('WooCommerce')) wp_enqueue_script('wc-cart-fragments');
});
function odp_asset($name) {
    $settings = ['hero-fixed.png'=>'odp_hero_image', 'poster.webp'=>'odp_story_poster', 'story.mp4'=>'odp_story_video'];
    $attachment = isset($settings[$name]) ? absint(get_theme_mod($settings[$name])) : 0;
    return ($attachment ? wp_get_attachment_url($attachment) : false) ?: home_url('/assets/' . $name);
}
add_action('customize_register', function ($customizer) {
    $customizer->add_section('odp_media', ['title'=>'Od Pani Ewy - ilustracja i film']);
    foreach (['odp_hero_image'=>['Główna ilustracja', 'image'], 'odp_story_poster'=>['Okładka filmu', 'image'], 'odp_story_video'=>['Film', 'video']] as $key=>[$label,$mime]) {
        $customizer->add_setting($key, ['sanitize_callback'=>'absint']);
        $customizer->add_control(new WP_Customize_Media_Control($customizer, $key, ['label'=>$label, 'section'=>'odp_media', 'mime_type'=>$mime]));
    }
});
function odp_brand() { ?>
<span class="brand-mark">e<span>✦</span></span><span>od Pani Ewy<small>MAŁE RYTUAŁY. WIELKIE UŚMIECHY.</small></span>
<?php }
function odp_art($product) {
    $colors = (array) $product->get_meta('_odp_colors');
    if (!$colors) return wc_placeholder_img();
    $out = '<div class="product-art" role="img" aria-label="Schemat demonstracyjny, nie zdjęcie produktu">';
    if ($product->get_meta('_odp_group') === 'paste') $out .= '<div class="paste">DEMO</div>';
    else foreach ($colors as $color) if (in_array($color, ['turkus','roz','fiolet','pomarancz'], true)) $out .= '<div class="brush '.esc_attr($color).'"></div>';
    return $out . '</div>';
}
add_filter('woocommerce_product_get_image', function ($image, $product) {
    return !$product->get_image_id() && $product->get_meta('_odp_colors') ? odp_art($product) : $image;
}, 10, 2);
add_action('wp', function () {
    if (!class_exists('WooCommerce')) return;
    remove_action('woocommerce_before_main_content', 'woocommerce_output_content_wrapper', 10);
    remove_action('woocommerce_after_main_content', 'woocommerce_output_content_wrapper_end', 10);
    add_action('woocommerce_before_main_content', function () { echo '<section class="section odp-woo">'; }, 10);
    add_action('woocommerce_after_main_content', function () { echo '</section>'; }, 10);
    remove_action('woocommerce_sidebar', 'woocommerce_get_sidebar', 10);
    if (is_product()) {
        global $product;
        $product = wc_get_product(get_queried_object_id());
        if ($product && !$product->get_image_id() && $product->get_meta('_odp_colors')) {
            remove_action('woocommerce_before_single_product_summary', 'woocommerce_show_product_images', 20);
            add_action('woocommerce_before_single_product_summary', function () use ($product) {
                echo '<div class="images featured-visual">' . odp_art($product) . '<span class="art-caption">Schemat demonstracyjny, nie zdjęcie produktu</span></div>';
            }, 20);
        }
    }
});
add_filter('woocommerce_add_to_cart_fragments', function ($fragments) {
    $fragments['b#cart-count'] = '<b id="cart-count">' . absint(WC()->cart->get_cart_contents_count()) . '</b>';
    return $fragments;
});
add_filter('loop_shop_columns', function () { return 3; });
