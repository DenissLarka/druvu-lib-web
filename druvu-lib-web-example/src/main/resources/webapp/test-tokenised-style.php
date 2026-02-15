<?php require 'includes/header.php'; ?>
<h1>PHP Implementation Test</h1>

<h2>Test 1: Standard Echo Tag</h2>
<p><?php echo 'Hello from standard echo tag!'; ?></p>

<h2>Test 2: Short Echo Tag</h2>
<p><?= 'Hello from short echo tag!' ?></p>

<h2>Test 3: String Concatenation</h2>
<p><?= 'Hello' . ' ' . 'World' . '!' ?></p>

<h2>Test 4: Escape Sequences</h2>
<p><?php echo "Line 1\nLine 2\tTabbed"; ?></p>

<h2>Test 5: Mixed Quotes</h2>
<p><?= "Double quotes" . ' and ' . 'single quotes' ?></p>

<h2>Test 6: HTML in Strings</h2>
<p><?php echo '<strong>Bold text</strong> and <em>italic text</em>'; ?></p>

<h2>Test 7: Multiple Tags in One Line</h2>
<p>Before <?= 'middle' ?> After</p>

<h2>Test 8: Complex Concatenation</h2>
<p><?= '<div>' . 'Content' . '</div>' ?></p>

<h2>Test 9: Empty String</h2>
<p>Start <?= '' ?> End</p>

<h2>Test 10: Escaped Characters</h2>
<p><?= 'It\'s a \"test\"' ?></p>

<h2>Test 11: Include with Concatenated Path</h2>
<?php include 'includes/' . 'footer.php'; ?>
