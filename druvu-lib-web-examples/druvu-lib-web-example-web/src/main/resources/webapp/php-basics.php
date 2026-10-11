<?php require 'includes/header.php'; ?>

<style>
    .demo { background: #fff; border: 1px solid #e0e0e0; border-left: 3px solid #3498db; border-radius: 4px; padding: 16px 20px; margin-bottom: 16px; }
    .demo h3 { margin: 0 0 10px 0; font-size: 0.85em; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
    .demo code { background: #f4f6f8; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }
    .demo .out { margin-top: 8px; padding: 8px 12px; background: #f9fafb; border-radius: 3px; }
</style>

<h1><?= $title ?></h1>
<p>Everything below arrived from <code>PhpBasics.java</code> as an ordinary variable.</p>

<div class="demo">
    <h3>Variables and interpolation</h3>
    <div class="out">
        <?= "Hello, {$visitor} — visit number {$visits}." ?>
    </div>
</div>

<div class="demo">
    <h3>Expressions</h3>
    <div class="out">
        <?= $visits ?> visits &times; <?= number_format($price, 2) ?> =
        <strong><?= number_format($visits * $price, 2) ?></strong>
    </div>
</div>

<div class="demo">
    <h3>Functions</h3>
    <div class="out">
        <code>ucwords()</code>: <?= ucwords($visitor) ?><br />
        <code>strtoupper()</code>: <?= strtoupper($visitor) ?><br />
        <code>strlen()</code>: <?= strlen($visitor) ?> characters<br />
        <code>implode()</code>: <?= implode(', ', $tags) ?>
    </div>
</div>

<div class="demo">
    <h3>Conditions</h3>
    <div class="out">
        <?php if ($visits > 5): ?>
            A regular visitor.
        <?php elseif ($visits > 1): ?>
            Been here before.
        <?php else: ?>
            First time.
        <?php endif; ?>
    </div>
</div>

<div class="demo">
    <h3>match, null coalescing, ternary</h3>
    <div class="out">
        <?php $band = match (true) {
            $visits >= 10 => 'gold',
            $visits >= 5 => 'silver',
            default => 'bronze',
        }; ?>
        <code>match</code>: <?= $band ?><br />
        <code>??</code>: <?= $nickname ?? 'no nickname set' ?><br />
        <code>?:</code>: <?= $visits ? 'has visited' : 'never visited' ?>
    </div>
</div>

<div class="demo" style="border-left-color: #e74c3c;">
    <h3>Output is escaped by default</h3>
    <p>The handler passed this string:</p>
    <div class="out"><code>&lt;script&gt;alert('xss')&lt;/script&gt;</code></div>
    <p><code>&lt;?= $untrusted ?&gt;</code> renders it as text. No script runs, and nobody had to remember to escape it:</p>
    <div class="out"><?= $untrusted ?></div>
    <p><code>&lt;?= raw($untrusted) ?&gt;</code> would inject it. That is the point of <code>raw()</code>: you write it
       on purpose, and you can search for it.</p>
</div>

<?php require 'includes/footer.php'; ?>
