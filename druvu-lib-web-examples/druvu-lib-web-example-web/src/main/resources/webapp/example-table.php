<?php require 'includes/header.php'; ?>

<h2>Table Example (w2ui Grid)</h2>
<div id="grid" style="width: 100%; height: 460px;"></div>

<script type="module">
    import { w2grid } from '<?= webjar('w2ui-2.0.es6.min.js') ?>';

    new w2grid({
        name: 'exampleGrid',
        box: '#grid',
        show: { toolbar: true, footer: true },
        columns: [
            { field: 'recid', text: 'ID', size: '60px', sortable: true },
            { field: 'fname', text: 'First Name', size: '20%', sortable: true },
            { field: 'lname', text: 'Last Name', size: '20%', sortable: true },
            { field: 'email', text: 'Email', size: '30%', sortable: true },
            { field: 'sdate', text: 'Start Date', size: '120px', sortable: true }
        ],
        records: [
            { recid: 1, fname: 'John', lname: 'Doe', email: 'jdoe@example.com', sdate: '2024-01-15' },
            { recid: 2, fname: 'Jane', lname: 'Smith', email: 'jsmith@example.com', sdate: '2024-02-20' },
            { recid: 3, fname: 'Bob', lname: 'Johnson', email: 'bjohnson@example.com', sdate: '2024-03-10' },
            { recid: 4, fname: 'Alice', lname: 'Williams', email: 'awilliams@example.com', sdate: '2024-04-05' },
            { recid: 5, fname: 'Charlie', lname: 'Brown', email: 'cbrown@example.com', sdate: '2024-05-18' },
            { recid: 6, fname: 'Diana', lname: 'Miller', email: 'dmiller@example.com', sdate: '2024-06-22' },
            { recid: 7, fname: 'Edward', lname: 'Davis', email: 'edavis@example.com', sdate: '2024-07-01' },
            { recid: 8, fname: 'Fiona', lname: 'Garcia', email: 'fgarcia@example.com', sdate: '2024-08-14' }
        ]
    }).render();
</script>

<?php require 'includes/footer.php'; ?>
