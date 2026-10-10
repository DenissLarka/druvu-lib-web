<?php
$a = [3, 1, 2];
$m = ["x" => 10, "y" => 20];
$m["z"] = 30;
$a[] = 4;
$nested = ["p" => ["q" => ["r" => "deep"]]];
$neg = [-5 => "a"]; $neg[] = "b";
$keys = ["1" => "int", "01" => "str", true => "bool", "x" => "x"];
?>
count: <?= count($a) ?> <?= count($m) ?> <?= count([]) ?>

keys: <?= implode(",", array_keys($m)) ?> values: <?= implode(",", array_values($m)) ?>

in: <?= json_encode(in_array(2, $a)) ?> <?= json_encode(in_array("2", $a), true) ?> <?= json_encode(array_key_exists("x", $m)) ?> <?= array_search(20, $m) ?> <?= json_encode(array_search(99, $m)) ?>

merge: <?= implode(",", array_merge($a, [9, 8])) ?> <?= implode(",", array_keys(array_merge($m, ["x" => 1, "w" => 2]))) ?> <?= implode(",", array_merge($m, ["x" => 1, "w" => 2])) ?>

map/filter: <?= implode(",", array_map(fn($v) => $v * 2, $a)) ?> <?= implode(",", array_keys(array_filter($a, fn($v) => $v > 2))) ?> <?= implode(",", array_filter($a, fn($v) => $v > 2)) ?> <?= implode(",", array_filter([0, 1, "", "a", null, false, "0", [], 2])) ?>

sum/product: <?= array_sum($a) ?> <?= array_product($a) ?> <?= array_sum([1.5, 2]) ?> <?= array_sum([]) ?>

slice/reverse: <?= implode(",", array_slice($a, 1, 2)) ?> <?= implode(",", array_reverse($a)) ?> <?= implode(",", array_keys(array_reverse($m))) ?> <?= implode(",", array_keys(array_slice($m, 1))) ?> <?= implode(",", array_slice($a, -2)) ?>

unique/flip/combine/fill/chunk/column: <?= implode(",", array_unique([1, "1", 2, 2.0, "a", "a"])) ?> <?= implode(",", array_keys(array_flip($m))) ?> <?= implode(",", array_combine(["k1", "k2"], [1, 2])) ?> <?= implode(",", array_fill(5, 3, "v")) ?> <?= count(array_chunk([1, 2, 3, 4, 5], 2)) ?> <?= implode(",", array_column([["id" => 1, "n" => "a"], ["id" => 2, "n" => "b"]], "n", "id")) ?> <?= implode(",", array_keys(array_column([["id" => 1, "n" => "a"], ["id" => 2, "n" => "b"]], "n", "id"))) ?>

first/last: <?= array_key_first($m) ?> <?= array_key_last($m) ?> <?= json_encode(array_key_first([])) ?>

nested: <?= $nested["p"]["q"]["r"] ?> <?= $neg[-4] ?> <?= implode(",", array_keys($neg)) ?> <?php $nested["p"]["q"]["s"] = "new"; ?><?= count($nested["p"]["q"]) ?>

key normalisation: <?= implode("|", array_map(fn($k) => gettype($k) . ":" . json_encode($k), array_keys($keys))) ?> count=<?= count($keys) ?> <?= $keys[1] ?>

sort: <?php sort($a); ?><?= implode(",", $a) ?> <?php rsort($a); ?><?= implode(",", $a) ?> <?php $c = $m; asort($c); ?><?= implode(",", array_keys($c)) ?> <?php arsort($c); ?><?= implode(",", array_keys($c)) ?> <?php ksort($c); ?><?= implode(",", array_keys($c)) ?> <?php krsort($c); ?><?= implode(",", array_keys($c)) ?> <?php $w = ["bb", "a", "ccc"]; usort($w, fn($p, $q) => strlen($p) <=> strlen($q)); ?><?= implode(",", $w) ?> <?php $mixed = ["10", 9, "9a", 1.5, "b", "a"]; sort($mixed); ?><?= implode(",", $mixed) ?>

copy: <?php $copy = $m; $copy["x"] = 99; ?><?= $m["x"] ?> <?= $copy["x"] ?>

unset: <?php unset($m["y"]); ?><?= implode(",", array_keys($m)) ?> <?= isset($m["y"]) ? "y" : "no-y" ?> <?php $u = [1, 2, 3]; unset($u[1]); $u[] = 4; ?><?= implode(",", array_keys($u)) ?>

range: <?= implode(",", range(1, 5)) ?> <?= implode(",", range(0, 10, 5)) ?> <?= implode(",", range('a', 'e')) ?> <?= implode(",", range(5, 1, 2)) ?> <?= implode(",", range(0, 1, 0.25)) ?>

json: <?php $d = []; $d["k"]["sub"] = 1; $d["k"]["sub2"] = 2; ?><?= count($d["k"]) ?> <?= json_encode($d) ?> <?= json_encode([1, 2, 3]) ?> <?= json_encode(["a" => 1.5, "b" => null, "c" => true, "d" => "x/y", "e" => "ü", "f" => "q\"\\"]) ?> <?= json_encode([]) ?> <?= json_encode([3 => "x"]) ?> <?= json_encode([1 => "a", 2 => "b"]) ?> <?= json_encode([0 => "a", 1 => "b"]) ?> <?= json_encode(1.0) ?> <?= json_encode("ü", 256) ?> <?= json_encode("x/y", 64) ?>
