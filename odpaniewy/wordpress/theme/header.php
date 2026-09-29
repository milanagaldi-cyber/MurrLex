<?php defined('ABSPATH') || exit; ?>
<!doctype html><html <?php language_attributes(); ?>><head><meta charset="<?php bloginfo('charset'); ?>"><meta name="viewport" content="width=device-width, initial-scale=1"><meta name="theme-color" content="#087E8B"><meta name="odp-shop" content="woocommerce"><?php wp_head(); ?></head>
<body <?php body_class(); ?>><?php wp_body_open(); ?>
<a class="skip" href="#main">Przejdź do treści</a>
<?php if (defined('ODP_STAGING') && ODP_STAGING): ?><div class="demo-bar"><span>SKLEP TESTOWY</span> Produkty i ceny demo. Bez płatności i wysyłki.</div><?php endif; ?>
<header><div class="header-inner"><a class="brand" href="<?php echo esc_url(home_url('/szczoteczki/')); ?>" aria-label="Od Pani Ewy - strona główna"><?php odp_brand(); ?></a>
<nav aria-label="Menu główne"><?php foreach (['szczoteczki'=>'Szczoteczki', 'pasty'=>'Pasty', 'kosmetyki'=>'Kosmetyki'] as $slug=>$label): ?><a href="<?php echo esc_url(home_url('/'.$slug.'/')); ?>" <?php if (is_page($slug) || ($slug === 'szczoteczki' && is_front_page())) echo 'aria-current="page"'; ?>><?php echo esc_html($label); ?><?php if ($slug === 'kosmetyki') echo '<small>wkrótce</small>'; ?></a><?php endforeach; ?></nav>
<a class="cart-link" href="<?php echo esc_url(function_exists('wc_get_cart_url') ? wc_get_cart_url() : home_url('/koszyk/')); ?>"><svg class="cart-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M5 7h15l-2 9H7L4 3H1M8 20h.01M17 20h.01"/></svg> Koszyk <b id="cart-count"><?php echo function_exists('WC') && WC()->cart ? absint(WC()->cart->get_cart_contents_count()) : 0; ?></b></a>
</div></header><main id="main" tabindex="-1">
