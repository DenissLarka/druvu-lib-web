<?php $title = "Report"; $rows = [["n" => "a", "v" => 1], ["n" => "b", "v" => 2]]; ?>
<?php include 'parts/header.php'; ?>
<table>
<?php foreach ($rows as $row): ?>
<?php include 'parts/row.php'; ?>
<?php endforeach; ?>
</table>
<?php include_once 'parts/once.php'; include_once 'parts/once.php'; require_once 'parts/once.php'; include 'parts/once.php'; ?>

<?php $value = include 'parts/value.php'; ?>returned: <?= $value ?>

set by the include: <?= $fromHeader ?>, seen by it: <?= $seenInHeader ?>
