<?php require 'includes/header.php'; ?>
<h1>PHP Servlet Test Cases</h1>

<div class="test-case">
    <h2>Test 1: Simple Single-Quoted String</h2>
    <div class="result">
        <?php echo 'Hello from PHP Servlet!'; ?>
    </div>
</div>

<div class="test-case">
    <h2>Test 2: Double-Quoted String</h2>
    <div class="result">
        <?php echo "This is a double-quoted string"; ?>
    </div>
</div>

<div class="test-case">
    <h2>Test 3: HTML Content</h2>
    <div class="result">
        <?php echo '<strong>Bold text from PHP</strong>'; ?>
    </div>
</div>

<div class="test-case">
    <h2>Test 4: Multiple Echo Tags</h2>
    <div class="result">
        <?php echo 'First '; ?>
        <?php echo 'Second '; ?>
        <?php echo 'Third'; ?>
    </div>
</div>

<div class="test-case">
    <h2>Test 5: Complex HTML</h2>
    <div class="result">
        <?php echo '<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>'; ?>
    </div>
</div>
<?php require 'includes/footer.php'; ?>
