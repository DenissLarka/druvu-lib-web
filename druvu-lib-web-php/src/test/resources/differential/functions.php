<?php $mul = fn($x, $y = 2) => $x * $y; $outer = 10; $add = fn($x) => $x + $outer; $nested = fn($x) => fn($y) => $x + $y; ?>
<?= $mul(3) ?> <?= $mul(3, 3) ?> <?= $add(5) ?> <?= implode(",", array_map($add, [1, 2])) ?> <?= implode(",", array_map(fn($p) => $p["n"], [["n" => "a"], ["n" => "b"]])) ?> <?= $nested(1)(2) ?>

<?php $outer = 20; ?><?= $add(5) ?>

<?= implode(",", array_map(fn($a, $b) => $a . $b, ["x", "y"], [1, 2])) ?> <?= implode(",", array_filter(["a" => 1, "b" => 2, "c" => 3], fn($k) => $k != "b", ARRAY_FILTER_USE_KEY)) ?>
