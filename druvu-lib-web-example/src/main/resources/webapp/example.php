<?php require 'includes/header.php'; ?>

<style>
    .dashboard-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .card { background: #fff; border: 1px solid #e0e0e0; border-radius: 6px; padding: 20px; }
    .card h3 { margin: 0 0 6px 0; font-size: 0.85em; color: #888; text-transform: uppercase; letter-spacing: 0.5px; }
    .card .value { font-size: 2em; font-weight: 600; color: #2c3e50; }
    .card .sub { font-size: 0.8em; color: #27ae60; margin-top: 4px; }
    .card.accent-blue   { border-top: 3px solid #3498db; }
    .card.accent-green  { border-top: 3px solid #27ae60; }
    .card.accent-orange { border-top: 3px solid #e67e22; }
    .card.accent-purple { border-top: 3px solid #8e44ad; }
    #activity-grid { width: 100%; height: 280px; }
    .section-title { font-size: 1.1em; color: #2c3e50; margin: 24px 0 12px 0; font-weight: 600; }
</style>

<div class="dashboard-cards">
    <div class="card accent-blue">
        <h3>Handlers</h3>
        <div class="value">5</div>
        <div class="sub">HTTP + WebSocket</div>
    </div>
    <div class="card accent-green">
        <h3>Template Engine</h3>
        <div class="value">PHP</div>
        <div class="sub">ServiceLoader plugin</div>
    </div>
    <div class="card accent-orange">
        <h3>Server</h3>
        <div class="value">Jetty 12</div>
        <div class="sub">EE10 Servlet API</div>
    </div>
    <div class="card accent-purple">
        <h3>Functions</h3>
        <div class="value">3</div>
        <div class="sub">webjar, context, link</div>
    </div>
</div>

<div class="section-title">Recent Activity</div>
<div id="activity-grid"></div>

<script type="module">
    import { w2grid } from '<?= webjar('w2ui-2.0.es6.min.js') ?>';

    new w2grid({
        box: '#activity-grid',
        name: 'activityGrid',
        show: { footer: true },
        columns: [
            { field: 'recid', text: '#', size: '50px' },
            { field: 'component', text: 'Component', size: '25%', sortable: true },
            { field: 'action', text: 'Action', size: '30%', sortable: true },
            { field: 'status', text: 'Status', size: '15%', sortable: true },
            { field: 'time', text: 'Time', size: '25%', sortable: true }
        ],
        records: [
            { recid: 1, component: 'WebBoot',           action: 'Server started on port 8081',  status: 'OK', time: 'just now' },
            { recid: 2, component: 'PhpServlet',         action: 'Template engine registered',   status: 'OK', time: '1s ago' },
            { recid: 3, component: 'BuiltInFunctions',   action: 'webjar(), context(), link()',   status: 'OK', time: '1s ago' },
            { recid: 4, component: 'DispatcherServlet',  action: '5 handlers mapped',            status: 'OK', time: '1s ago' },
            { recid: 5, component: 'WebSocketSetup',     action: 'WS upgrade listener active',   status: 'OK', time: '1s ago' },
            { recid: 6, component: 'DefaultServlet',     action: 'Serving /webjars/*, /static/*', status: 'OK', time: '1s ago' }
        ]
    }).render();
</script>

<?php require 'includes/footer.php'; ?>
