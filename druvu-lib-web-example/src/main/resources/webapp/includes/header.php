<!DOCTYPE html>
<html>
<head>
    <title>DRUVU-LIB Web Examples</title>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1.2" />
    <link rel="stylesheet" type="text/css" href="<?= link('static/style/style.css') ?>" />
    <link rel="stylesheet" type="text/css" href="<?= webjar('w2ui-2.0.min.css') ?>" />
    <style>
        #main-toolbar { margin: 0 0 20px 0; }
        .page-content { padding: 0 20px 20px 20px; }
    </style>
</head>
<body>
<div id="main-toolbar"></div>
<script type="module">
    import { w2toolbar } from '<?= webjar('w2ui-2.0.es6.min.js') ?>';

    new w2toolbar({
        box: '#main-toolbar',
        name: 'mainToolbar',
        items: [
            { type: 'button', id: 'home',  text: 'Home',   icon: 'w2ui-icon-check' },
            { type: 'break' },
            { type: 'button', id: 'table', text: 'Table',  icon: 'w2ui-icon-columns' },
            { type: 'button', id: 'json',  text: 'JSON',   icon: 'w2ui-icon-info' },
            { type: 'button', id: 'ws',    text: 'Socket', icon: 'w2ui-icon-reload' },
            { type: 'spacer' },
            { type: 'html', html: '<span style="color:#888; font-size:0.85em; padding:0 12px;">DRUVU-LIB Web</span>' }
        ],
        onClick(event) {
            const routes = {
                home:  '<?= link('example') ?>',
                table: '<?= link('example-table') ?>',
                json:  '<?= link('example-json') ?>',
                ws:    '<?= link('example-socket') ?>'
            };
            if (routes[event.target]) window.location = routes[event.target];
        }
    }).render();
</script>
<div class="page-content">
