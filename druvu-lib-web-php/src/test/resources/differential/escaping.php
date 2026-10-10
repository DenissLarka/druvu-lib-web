<?php $x = "<b>&\"'</b>"; $url = "a b&c=d/é"; ?>
<?= htmlspecialchars($x) ?>

<?= raw($x) ?>

<?= raw(htmlspecialchars($x)) ?>

<?= htmlentities("<a>") ?>

<?= raw(htmlspecialchars_decode("&lt;p&gt; &amp; &quot;")) ?>

<?= urlencode($url) ?> <?= rawurlencode($url) ?> <?= http_build_query(["q" => "a b", "n" => 1, "arr" => [1, 2], "m" => ["k" => "v"]]) ?>

<?= strip_tags("<p>Hello <b>World</b></p>") ?>

<?= nl2br("line1\nline2\r\nline3") ?>

<?php printf("%s has %d items (%.1f%%)\n", "cart", 3, 42.456); ?>
<?= vsprintf("%s-%s-%03d", ["a", "b", 5]) ?>

<?= sprintf("%'.10d|%+d|%+d|%u|%c|%e|%E|%10.4f|%-10s|%5.1f%%", 42, 5, -5, 3, 65, 12345.678, 0.00012, 3.14159, "left", 99.5) ?>
