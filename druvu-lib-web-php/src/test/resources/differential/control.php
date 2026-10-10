<?php $n = 7; $list = ["a" => 1, "b" => 2, "c" => 3]; ?>
<?php if ($n > 10): ?>big<?php elseif ($n > 5): ?>medium<?php else: ?>small<?php endif; ?>

<?php if ($n % 2 == 0) { echo "even"; } else { echo "odd"; } ?>

<?php for ($i = 0; $i < 3; $i++): ?>[<?= $i ?>]<?php endfor; ?>

<?php $i = 3; while ($i > 0) { echo $i--; } ?>

<?php $j = 0; do { echo "d$j"; $j++; } while ($j < 2); ?>

<?php foreach ($list as $k => $v): ?><?= $k ?>=<?= $v ?>;<?php endforeach; ?>

<?php foreach ([1, 2, 3, 4, 5, 6] as $v) { if ($v == 2) continue; if ($v == 5) break; echo $v; } ?>

<?php foreach ([[1, 2], [3, 4]] as $row) { foreach ($row as $cell) { if ($cell == 3) break 2; echo $cell; } } ?>

<?php foreach ([[1, 2], [3, 4]] as $row) { foreach ($row as $cell) { if ($cell % 2 == 1) continue 2; echo $cell; } } ?>

<?php switch ($n) { case 1: echo "one"; break; case 7: echo "seven"; case 8: echo "+eight"; break; default: echo "other"; } ?>

<?php switch ("7") { case 7: echo "loose"; break; default: echo "strict"; } ?>

<?= match(true) { $n < 5 => "low", $n < 10 => "mid", default => "high" } ?> <?= match($n) { 1, 2 => "small", 7 => "lucky", default => "meh" } ?>

<?= $n > 5 ? "yes" : "no" ?> <?= $undefined ?? "default" ?> <?= $n ?: "zero" ?> <?= 0 ?: "zero" ?> <?= isset($n) ? "set" : "unset" ?> <?= empty($list) ? "empty" : "full" ?> <?= isset($list["z"]) ? "z" : "no z" ?> <?= $list["z"] ?? "no z again" ?> <?= isset($list["a"], $list["b"]) ? "both" : "not both" ?> <?= empty($list["a"]) ? "a empty" : "a set" ?>

<?php $s = 0; foreach (range(1, 10) as $v): if ($v % 2): continue; endif; $s += $v; endforeach; ?><?= $s ?>

<?php $k = 0; while (true): $k++; if ($k >= 3): break; endif; endwhile; ?><?= $k ?>

<?php for ($i = 0, $j = 10; $i < $j; $i += 3, $j -= 3) { echo "$i-$j "; } ?>

<?php foreach ($list as $v) { } echo $v; ?>

<?php foreach ([] as $v) { echo "never"; } echo "empty loop ok"; ?>
