<?php
defined('ABSPATH') || exit;
get_header();
echo '<section class="section odp-woo">';
woocommerce_content();
echo '</section>';
get_footer();
