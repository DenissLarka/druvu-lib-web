<?php
$name = "Ada";
$items = ["pen", "cup", "ink"];
$n = 3;
?>
Single quotes keep <?= 'Hello, $name' ?> literal.
Interpolated: <?= "Hello, $name! You have {$n} items: {$items[0]}, {$items[1]} and $items[2]." ?>

Heredoc:
<?= <<<TXT
  Dear $name,
    indented {$items[2]} line
  TXT ?>

Nowdoc: <?= <<<'TXT'
no $interpolation here
TXT ?>

Concat: <?= $name . " has " . $n . " items" ?>

Case: <?= strtoupper($name) ?> <?= strtolower("MiXeD") ?> <?= ucfirst("word") ?> <?= ucwords("hello big world") ?> <?= lcfirst("Word") ?>

Length/pos/sub: <?= strlen("hello") ?> <?= strpos("hello", "l") ?> <?= substr("hello", 1, 3) ?> <?= substr("hello", -2) ?> <?= json_encode(strpos("hello", "z")) ?>

Replace/repeat/pad: <?= str_replace("l", "L", "hello") ?> <?= str_repeat("ab", 3) ?> [<?= str_pad("7", 3, "0") ?>] [<?= str_pad("7", 3) ?>]

Trim: [<?= trim("  x  ") ?>] [<?= ltrim("  x  ") ?>] [<?= rtrim("  x  ") ?>] [<?= trim("--x--", "-") ?>]

Split/join: <?= implode(", ", explode("-", "a-b-c")) ?> <?= implode("|", str_split("abcdef", 2)) ?> <?= implode("|", explode(",", "a,b,c,d", 2)) ?>

Contains: <?= json_encode(str_contains("haystack", "st")) ?> <?= json_encode(str_starts_with("haystack", "hay")) ?> <?= json_encode(str_ends_with("haystack", "ack")) ?>

Wrap:
<?= wordwrap("The quick brown fox jumps", 10, "\n", true) ?>

Sprintf: <?= sprintf("%05.2f|%-5s|%5s|%d|%03d|%%|%s", 3.14159, "ab", "ab", 42, 7, "end") ?>

Number format: <?= number_format(1234567.891) ?> <?= number_format(1234567.891, 2) ?> <?= number_format(1234567.891, 2, ',', '.') ?> <?= number_format(0.5) ?> <?= number_format(-1234.567, 1) ?>
