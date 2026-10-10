package com.druvu.php;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Every template under {@code src/test/resources/differential} is rendered by the engine and by the real {@code php} on
 * the PATH, and the two outputs must be identical. The templates are self-contained (they set their own variables), PHP
 * runs with the engine's fixed settings (precision 14, UTC, short tags off), the engine runs with escaping off and the
 * debug functions on so stock PHP semantics are the subject, and {@code shim.php} gives PHP the engine's own helpers.
 * Skipped when no {@code php} is installed; the CI runner has one.
 */
public class TestDifferentialAgainstPhp {
    private static final Path DIR = templateDirectory();
    private static final Path SHIM = DIR.resolve("shim.php");

    private PhpEngine engine;

    @BeforeClass
    public void requirePhp() throws IOException, InterruptedException {
        try {
            Process php =
                    new ProcessBuilder("php", "-v").redirectErrorStream(true).start();
            String banner = new String(php.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            php.waitFor();
            System.out.println("Differential tests run against: "
                    + banner.lines().findFirst().orElse("?"));
        } catch (IOException e) {
            throw new SkipException("php is not on the PATH; differential tests skipped");
        }
        PhpEngineConfig stockPhp =
                PhpEngineConfig.DEFAULTS.withoutEscaping().withDebugFunctions().withoutTemplateCache();
        engine = new PhpEngine(TestDifferentialAgainstPhp::load, stockPhp);
    }

    @DataProvider
    public Object[][] templates() throws IOException {
        try (Stream<Path> files = Files.list(DIR)) {
            return files.map(file -> DIR.relativize(file).toString())
                    .filter(name -> name.endsWith(".php") && !name.equals("shim.php"))
                    .sorted()
                    .map(name -> new Object[] {name})
                    .toArray(Object[][]::new);
        }
    }

    @Test(dataProvider = "templates")
    public void theEngineRendersWhatPhpRenders(String template) throws IOException, InterruptedException {
        String expected = php(template);
        String actual = engine.render("/" + template, Map.of()).orElseThrow();
        assertThat(actual).as(template).isEqualTo(expected);
    }

    private static String php(String template) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(List.of(
                "php", "-n",
                "-d", "precision=14",
                "-d", "short_open_tag=0",
                "-d", "date.timezone=UTC",
                "-d", "display_errors=stderr",
                "-d", "error_reporting=E_ALL",
                "-d", "auto_prepend_file=" + SHIM));
        command.add(template);
        Process php = new ProcessBuilder(command).directory(DIR.toFile()).start();
        byte[] out = php.getInputStream().readAllBytes();
        String err = new String(php.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
        int exit = php.waitFor();
        assertThat(err).as("php stderr for " + template).isEmpty();
        assertThat(exit).as("php exit code for " + template).isZero();
        return new String(out, StandardCharsets.UTF_8);
    }

    private static String load(String path) throws IOException {
        Path file = DIR.resolve(path.startsWith("/") ? path.substring(1) : path);
        return Files.exists(file) ? Files.readString(file) : null;
    }

    private static Path templateDirectory() {
        try {
            return Path.of(TestDifferentialAgainstPhp.class
                    .getResource("/differential")
                    .toURI());
        } catch (Exception e) {
            throw new IllegalStateException("differential templates not on the test classpath", e);
        }
    }
}
