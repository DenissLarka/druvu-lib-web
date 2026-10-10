<?php require 'includes/header.php'; ?>

<style>
    .demo { background: #fff; border: 1px solid #e0e0e0; border-left: 3px solid #e67e22; border-radius: 4px; padding: 16px 20px; margin-bottom: 16px; }
    .demo h3 { margin: 0 0 10px 0; font-size: 0.85em; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
    .demo code, .demo pre { background: #f4f6f8; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }
    .demo pre { padding: 10px 12px; overflow-x: auto; }
    .token { font-family: monospace; font-size: 1.1em; color: #2c3e50; }
    button { padding: 6px 14px; }
</style>

<h1><?= $title ?></h1>
<p>A machine signs in with <code>Authorization: Bearer &lt;token&gt;</code> on a route registered with
   <code>UrlConfig.forMachines(...)</code>. The application hands tokens out and keeps only their hash.</p>

<?php if (isset($token)): ?>
<div class="demo">
    <h3>Your new token, shown once</h3>
    <p class="token"><?= $token ?></p>
    <p>Try it on the machine route:</p>
    <pre>curl -H "Authorization: Bearer <?= $token ?>" http://<?= $_SERVER['HTTP_HOST'] ?><?= link('api-ping') ?></pre>
    <p>Without the header the same URL answers 401 with <code>WWW-Authenticate: Bearer</code>; your browser session
       does not count there.</p>
</div>
<?php endif; ?>

<div class="demo">
    <h3>Issue a token for <?= $subject ?></h3>
    <p><?= $issued ?> issued so far.</p>
    <form method="post" action="<?= link('tokens') ?>">
        <button type="submit">Issue a new token</button>
    </form>
</div>

<?php require 'includes/footer.php'; ?>
