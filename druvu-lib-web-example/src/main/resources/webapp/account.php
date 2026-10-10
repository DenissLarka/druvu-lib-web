<?php require 'includes/header.php'; ?>

<style>
    .demo { background: #fff; border: 1px solid #e0e0e0; border-left: 3px solid #27ae60; border-radius: 4px; padding: 16px 20px; margin-bottom: 16px; }
    .demo h3 { margin: 0 0 10px 0; font-size: 0.85em; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
    .demo code { background: #f4f6f8; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }
</style>

<h1><?= $title ?></h1>
<p>This page is registered with <code>UrlConfig.signedIn(AccountHandler.class)</code>: anyone signed in may see it,
   no particular permission needed. Jetty challenged you before the handler ran.</p>

<div class="demo">
    <h3>Who you are</h3>
    <p>Subject: <code><?= $subject ?></code> — also in the template as <code>$_SERVER['REMOTE_USER']</code> = <code><?= $_SERVER['REMOTE_USER'] ?></code></p>
    <?php if ($email != "" || $displayName != ""): ?>
    <p>From the provider's claims: <code><?= $displayName ?></code> <code><?= $email ?></code></p>
    <?php else: ?>
    <p>No email or display name: Basic authentication carries none. An OpenID provider fills them.</p>
    <?php endif; ?>
</div>

<div class="demo">
    <h3>What you may do</h3>
    <?php if (count($permissions) == 0): ?>
    <p>Nothing that needs a permission.</p>
    <?php else: ?>
    <ul><?php foreach ($permissions as $permission): ?><li><code><?= $permission ?></code></li><?php endforeach; ?></ul>
    <?php endif; ?>
</div>

<div class="demo">
    <h3>Your API tokens</h3>
    <p><?= $tokens ?> issued so far. <a href="<?= link('tokens') ?>">Issue one</a> for a script or a desktop app.</p>
</div>

<?php require 'includes/footer.php'; ?>
