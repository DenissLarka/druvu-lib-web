<?php require 'includes/header.php'; ?>

<style>
    .demo { background: #fff; border: 1px solid #e0e0e0; border-left: 3px solid #27ae60; border-radius: 4px; padding: 16px 20px; margin-bottom: 16px; }
    .demo h3 { margin: 0 0 10px 0; font-size: 0.85em; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
    .demo code { background: #f4f6f8; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }
    table.grid { border-collapse: collapse; width: 100%; }
    table.grid th, table.grid td { border-bottom: 1px solid #eee; padding: 6px 10px; text-align: left; }
    table.grid th { color: #888; font-size: 0.8em; text-transform: uppercase; letter-spacing: 0.5px; }
    table.grid td.num { text-align: right; }
    table.grid tr.total td { font-weight: 600; border-top: 2px solid #ddd; border-bottom: none; }
</style>

<h1><?= $title ?></h1>
<p>A Java <code>List</code> arrives as a PHP list, a <code>Map</code> as a keyed array.</p>

<div class="demo">
    <h3>foreach over objects — the colon form</h3>
    <table class="grid">
        <tr><th>Product</th><th>Category</th><th class="num">Qty</th><th class="num">Unit</th><th class="num">Total</th></tr>
        <?php foreach ($products as $product): ?>
            <tr>
                <td><?= $product->name ?></td>
                <td><?= $product->category ?></td>
                <td class="num"><?= $product->quantity ?></td>
                <td class="num"><?= number_format($product->unitPrice, 2) ?></td>
                <td class="num"><?= number_format($product->total, 2) ?></td>
            </tr>
        <?php endforeach; ?>
        <tr class="total">
            <td colspan="4">Total</td>
            <td class="num">
                <?php $sum = 0; ?>
                <?php foreach ($products as $product) { $sum += $product->total; } ?>
                <?= number_format($sum, 2) ?>
            </td>
        </tr>
    </table>
</div>

<div class="demo">
    <h3>foreach with keys</h3>
    <ul>
        <?php foreach ($stockByWarehouse as $city => $units): ?>
            <li><?= $city ?>: <?= $units ?> units</li>
        <?php endforeach; ?>
    </ul>
    <p>Total on hand: <strong><?= array_sum(array_values($stockByWarehouse)) ?></strong> units across
       <?= count($stockByWarehouse) ?> warehouses.</p>
</div>

<div class="demo">
    <h3>for, while, do…while</h3>
    <div>
        <code>for</code>:
        <?php for ($i = 1; $i <= 5; $i++): ?><?= $i ?> <?php endfor; ?>
    </div>
    <div>
        <code>while</code>:
        <?php $n = 16; ?>
        <?php while ($n > 1): ?><?= $n ?> <?php $n = intdiv($n, 2); ?><?php endwhile; ?>
    </div>
    <div>
        <code>do…while</code>:
        <?php $countdown = 3; ?>
        <?php do { ?><?= $countdown ?> <?php $countdown--; } while ($countdown > 0); ?>
    </div>
</div>

<div class="demo">
    <h3>Array functions and arrow functions</h3>
    <?php $names = array_map(fn($p) => $p->name, $products); ?>
    <?php $writing = array_filter($products, fn($p) => $p->category === 'writing'); ?>
    <div>
        <code>array_map</code>: <?= implode(', ', $names) ?><br />
        <code>array_filter</code>: <?= count($writing) ?> of <?= count($products) ?> are writing supplies<br />
        <code>range</code> + <code>array_sum</code>: <?= array_sum(range(1, 10)) ?>
    </div>
</div>

<div class="demo">
    <h3>Sorting</h3>
    <?php $cities = array_keys($stockByWarehouse); ?>
    <?php sort($cities); ?>
    <div><code>sort</code>: <?= implode(' · ', $cities) ?></div>
</div>

<?php require 'includes/footer.php'; ?>
