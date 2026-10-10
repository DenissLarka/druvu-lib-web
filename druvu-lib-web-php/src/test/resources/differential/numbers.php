<?php $a = 7; $b = 2; ?>
Arithmetic: <?= $a + $b ?> <?= $a - $b ?> <?= $a * $b ?> <?= $a / $b ?> <?= $a % $b ?> <?= $a ** $b ?> <?= intdiv($a, $b) ?> <?= fmod(7.5, 2) ?>

Precedence: <?= 1 + 2 * 3 ?> <?= (1 + 2) * 3 ?> <?= 2 ** 3 ** 2 ?> <?= -2 ** 2 ?> <?= 10 - 4 - 3 ?> <?= 2 * 3 % 4 ?> <?= 1 . 2 + 3 ?>

Floats: <?= 0.1 + 0.2 ?> <?= 1 / 3 ?> <?= 10 / 5 ?> <?= 1e20 ?> <?= 1.5e-7 ?> <?= 100000000000000000000 ?> <?= 0.1 + 0.7 ?> <?= 1.0 ?> <?= 2.50 ?> <?= 1e15 ?> <?= 1e14 ?> <?= 123456789012345678 ?> <?= 1/7 ?> <?= 2/3 ?>

Rounding: <?= round(2.5) ?> <?= round(3.5) ?> <?= round(-2.5) ?> <?= round(1.9549, 2) ?> <?= floor(-1.5) ?> <?= ceil(-1.5) ?> <?= abs(-3) ?> <?= max(1, 5, 3) ?> <?= min([4, 2, 8]) ?> <?= pow(2, 10) ?> <?= sqrt(16) ?> <?= pow(2, 0.5) ?> <?= round(1234.5678, -2) ?>

Juggling: <?= "10" + 5 ?> <?= "1.5" + 1 ?> <?= "3" . 4 ?> <?= 7 . "" ?> <?= (int) "12abc" ?> <?= (int) 3.99 ?> <?= (float) "1e3" ?> <?= (string) 1.0 ?> <?= (bool) "0" ? "t" : "f" ?> <?= (bool) "0.0" ? "t" : "f" ?> <?= (bool) [] ? "t" : "f" ?> <?= intval("0x1A") ?> <?= intval(" 42 ") ?> <?= (int) -3.99 ?>

Comparison: <?= json_encode("10" == "1e1") ?> <?= json_encode(0 == "a") ?> <?= json_encode("1" == "01") ?> <?= json_encode(100 == "1e2") ?> <?= json_encode(null == false) ?> <?= json_encode([] == false) ?> <?= json_encode("abc" == 0) ?> <?= json_encode(1 === 1.0) ?> <?= 1 <=> 2 ?> <?= "b" <=> "a" ?> <?= [1, 2] <=> [1, 3] ?> <?= json_encode("abc" < "abd") ?> <?= json_encode("10" < "9") ?> <?= json_encode("10" < "9a") ?> <?= json_encode(null < 1) ?> <?= json_encode(true > false) ?>

Overflow: <?= 9223372036854775807 + 1 ?> <?= 9223372036854775807 * 2 ?> <?= -9223372036854775807 - 2 ?>

Increment: <?php $i = 5; $i++; ++$i; $i--; $f = 1.5; $f++; ?><?= $i ?> <?= $f ?>

Assign ops: <?php $x = 24; $x += 5; $x -= 5; $x *= 2; $x /= 4; $x .= "!"; $y = null; $y ??= "dflt"; $z = 10; $z %= 3; $p = 2; $p **= 3; ?><?= $x ?> <?= $y ?> <?= $z ?> <?= $p ?>

Logic: <?= json_encode(true && false) ?> <?= json_encode(true || false) ?> <?= json_encode(!true) ?> <?= json_encode(true xor true) ?> <?= json_encode((true and false)) ?> <?= json_encode(1 <=> 1) ?> <?= json_encode("a" ?: "b") ?> <?= json_encode(0 ?: "b") ?>
