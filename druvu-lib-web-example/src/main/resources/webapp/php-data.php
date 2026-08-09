<?php require 'includes/header.php'; ?>

<style>
    .demo { background: #fff; border: 1px solid #e0e0e0; border-left: 3px solid #8e44ad; border-radius: 4px; padding: 16px 20px; margin-bottom: 16px; }
    .demo h3 { margin: 0 0 10px 0; font-size: 0.85em; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
    .demo code { background: #f4f6f8; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }
    .badge { display: inline-block; padding: 1px 8px; border-radius: 10px; font-size: 0.8em; }
    .badge.yes { background: #e8f6ef; color: #1e8449; }
    .badge.no  { background: #fdedec; color: #b03a2e; }
    dl.pairs { margin: 0; }
    dl.pairs dt { color: #888; font-size: 0.8em; text-transform: uppercase; letter-spacing: 0.5px; margin-top: 8px; }
    dl.pairs dd { margin: 2px 0 0 0; }
</style>

<h1><?= $title ?></h1>
<p>The handler passed a record graph. The template reads it with <code>-&gt;</code> and can do nothing else to it —
   no setter, no method with arguments, no way past what it was shown.</p>

<div class="demo">
    <h3>Reading a record</h3>
    <dl class="pairs">
        <dt>Reference</dt><dd><?= $order->reference ?></dd>
        <dt>Customer</dt>
        <dd>
            <?= $order->customer->name ?>
            <?php if ($order->customer->preferred): ?>
                <span class="badge yes">preferred</span>
            <?php else: ?>
                <span class="badge no">standard</span>
            <?php endif; ?>
        </dd>
        <dt>Ships to</dt><dd><?= $order->shipTo->city ?>, <?= $order->shipTo->country ?></dd>
    </dl>
</div>

<div class="demo">
    <h3>Nested collections</h3>
    <ul>
        <?php foreach ($order->lines as $line): ?>
            <li><?= $line->quantity ?> &times; <?= $line->description ?> — <?= number_format($line->amount, 2) ?></li>
        <?php endforeach; ?>
    </ul>
    <p>Order total: <strong><?= number_format($order->total, 2) ?></strong>
       (<?= count($order->lines) ?> lines)</p>
    <p><code>$order-&gt;total</code> reads the record's own <code>total()</code> method — any no-argument method that
       returns something is readable as a property.</p>
</div>

<div class="demo">
    <h3>Null-safe reads</h3>
    <p>This order has no ship-to address at all:</p>
    <dl class="pairs">
        <dt>Reference</dt><dd><?= $backorder->reference ?></dd>
        <dt>City, via <code>?-&gt;</code></dt>
        <dd><?= $backorder->shipTo?->city ?? 'not set' ?></dd>
    </dl>
    <p><code>$backorder-&gt;shipTo?-&gt;city</code> yields null instead of failing, and <code>??</code> supplies the
       fallback.</p>
</div>

<div class="demo">
    <h3>A plain bean, read through its getters</h3>
    <dl class="pairs">
        <dt>Number</dt><dd><?= $invoice->number ?></dd>
        <dt>Paid</dt>
        <dd>
            <?php if ($invoice->paid): ?>
                <span class="badge yes">paid</span>
            <?php else: ?>
                <span class="badge no">outstanding</span>
            <?php endif; ?>
        </dd>
    </dl>
    <p><code>$invoice-&gt;number</code> found <code>getNumber()</code>, and <code>$invoice-&gt;paid</code> found
       <code>isPaid()</code>.</p>
</div>

<?php require 'includes/footer.php'; ?>
