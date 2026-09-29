<?php
/** Plugin Name: Od Pani Ewy - private staging checkout
 * Description: Test orders, synthetic checkout data, no real payment or outgoing email.
 * Version: 1.0.0
 */
defined('ABSPATH') || exit;
if (!defined('ODP_STAGING') || !ODP_STAGING) return;

add_filter('pre_wp_mail', '__return_false');
add_filter('wp_robots', function ($robots) { $robots['noindex'] = true; $robots['nofollow'] = true; return $robots; });
add_filter('woocommerce_enable_order_notes_field', '__return_false');
add_filter('woocommerce_checkout_fields', function () { return ['billing'=>[], 'shipping'=>[], 'account'=>[], 'order'=>[]]; }, 999);
add_filter('woocommerce_cart_needs_shipping_address', '__return_false');
add_filter('woocommerce_checkout_registration_enabled', '__return_false');
add_filter('woocommerce_checkout_registration_required', '__return_false');
add_filter('woocommerce_customer_default_location', function () { return 'base'; });
add_filter('woocommerce_checkout_posted_data', function ($data) {
    foreach (array_keys($data) as $key) {
        if (str_starts_with($key, 'billing_') || str_starts_with($key, 'shipping_')) unset($data[$key]);
    }
    return array_merge($data, [
        'billing_first_name'=>'Test', 'billing_last_name'=>'Team VPN',
        'billing_email'=>'demo@example.invalid', 'billing_country'=>'PL',
        'shipping_country'=>'PL', 'order_comments'=>'', 'createaccount'=>0,
    ]);
}, 999);
add_action('woocommerce_checkout_create_order', function ($order) {
    $order->update_meta_data('_odp_test_order', 'yes');
    $order->set_customer_note('Zamówienie testowe Team VPN. Bez płatności i wysyłki.');
}, 10);
add_action('woocommerce_before_checkout_form', function () {
    wc_print_notice('To zamówienie testowe. Nie podawaj danych osobowych. Nie pobierzemy opłaty i nic nie wyślemy.', 'notice');
});
add_filter('woocommerce_thankyou_order_received_text', function ($text, $order) {
    return $order && $order->get_meta('_odp_test_order') ? 'Test zakończony! Zapisaliśmy zamówienie testowe. Bez płatności i wysyłki.' : $text;
}, 10, 2);
// The staging gateway intentionally supports only the classic checkout.
add_filter('rest_pre_dispatch', function ($result, $server, $request) {
    if (preg_match('#^/wc/store/v[0-9]+/checkout(?:/|$)#', $request->get_route()) && $request->get_method() !== 'GET') {
        return new WP_Error('odp_classic_checkout', 'Użyj testowej strony /zamowienie/.', ['status'=>403]);
    }
    return $result;
}, 10, 3);
add_action('plugins_loaded', function () {
    if (!class_exists('WC_Payment_Gateway')) return;
    class ODP_Test_Gateway extends WC_Payment_Gateway {
        public function __construct() {
            $this->id = 'odp_test';
            $this->method_title = 'Od Pani Ewy - test';
            $this->title = 'Zamówienie testowe - bez płatności';
            $this->description = 'Zapisuje wyłącznie test w sklepie. Nie jest to InPost Pay. Nic nie zostanie wysłane.';
            $this->enabled = 'yes';
            $this->has_fields = false;
            $this->supports = ['products'];
            $this->order_button_text = 'Zapisz zamówienie testowe';
        }
        public function process_payment($order_id) {
            $order = wc_get_order($order_id);
            if (!$order || !$order->get_meta('_odp_test_order')) return ['result'=>'failure'];
            $order->update_status('on-hold', 'Test Team VPN: brak płatności i wysyłki.');
            WC()->cart->empty_cart();
            return ['result'=>'success', 'redirect'=>$this->get_return_url($order)];
        }
    }
    add_filter('woocommerce_payment_gateways', function ($gateways) { $gateways[] = 'ODP_Test_Gateway'; return $gateways; });
});
add_filter('woocommerce_available_payment_gateways', function ($gateways) {
    return isset($gateways['odp_test']) ? ['odp_test'=>$gateways['odp_test']] : [];
}, 999);
