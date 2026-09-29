<?php
defined('ABSPATH') || exit;
get_header(); ?>
<section class="section odp-woo"><?php if (have_posts()): while (have_posts()): the_post(); ?>
<h1 class="page-title"><?php the_title(); ?></h1><?php the_content(); ?>
<?php endwhile; else: ?><h1>Nie ma tu tej strony.</h1><?php endif; ?></section>
<?php get_footer(); ?>
