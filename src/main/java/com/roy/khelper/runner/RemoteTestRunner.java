package com.roy.khelper.runner;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Runs a solution against the test cases of one problem. The IDE starts it in the user's project JDK, so it is
 * plain Java compiled for Java 11 and uses no Kotlin or IntelliJ classes.
 *
 * <p>Arguments: {@code <solution class> <tests dir> [time limit ms] [--no-hard-limit]}. The tests dir holds
 * {@code N.in} and {@code N.out} files, where N is the test number shown in the IDE. {@code --no-hard-limit} is for
 * debugging: a test waits on a breakpoint as long as needed instead of being stopped.
 *
 * <p>Each test loads the solution in a fresh class loader, so static fields (a shared reader, a top-level Kotlin
 * {@code val}) start clean for every test. The solution runs in a thread with a large stack, like online judges.
 *
 * <p>Exit code: 0 when every test passed, 1 when a test failed, 2 when the solution could not be started.
 */
public final class RemoteTestRunner {

    static final int EXIT_PASSED = 0;
    static final int EXIT_FAILED = 1;
    static final int EXIT_ERROR = 2;
    static final String NO_HARD_LIMIT = "--no-hard-limit";

    private static final String RESET = "\u001B[0m";
    private static final String GREEN = "\u001B[32m";
    private static final String RED = "\u001B[31m";
    private static final String YELLOW = "\u001B[33m";

    private static final long STACK_SIZE_BYTES = 256L * 1024 * 1024;
    private static final long MIN_HARD_LIMIT_MS = 10_000;
    private static final int HARD_LIMIT_FACTOR = 5;
    private static final int PREVIEW_CHARS = 1000;

    private RemoteTestRunner() {
    }

    public static void main(String[] args) {
        System.exit(run(args));
    }

    static int run(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: RemoteTestRunner <SolutionClassName> <TestsDir> [TimeLimitMs] [" + NO_HARD_LIMIT + "]");
            return EXIT_ERROR;
        }
        String className = args[0];
        Path testsDir = Paths.get(args[1]);
        long timeLimitMs = args.length > 2 ? Long.parseLong(args[2]) : 0;
        boolean hardLimit = !Arrays.asList(args).contains(NO_HARD_LIMIT);

        List<TestData> tests;
        try {
            tests = loadTests(testsDir);
        } catch (IOException e) {
            System.err.println(RED + "Could not read test cases: " + e.getMessage() + RESET);
            return EXIT_ERROR;
        }

        URL[] classpath = solutionClasspath();
        String entryClass;
        try {
            entryClass = findEntryClass(className, classpath);
        } catch (LinkageError e) {
            System.err.println(RED + "ERROR: Could not load " + className + ": " + e + RESET);
            System.err.println("The project JDK may be older than the Java version the solution was compiled for.");
            return EXIT_ERROR;
        }
        if (entryClass == null) {
            System.err.println(RED + "ERROR: Could not load class " + className + " or " + className + "Kt" + RESET);
            System.err.println("Check that the file compiled and that its package name matches its folder.");
            return EXIT_ERROR;
        }

        System.out.println("Running " + tests.size() + " active test cases...");
        System.out.println();

        int passed = 0;
        for (int i = 0; i < tests.size(); i++) {
            TestData test = tests.get(i);
            Result result = runTest(entryClass, classpath, test, timeLimitMs, hardLimit);
            report(test, result, timeLimitMs);
            if (result.passed) {
                passed++;
            }
            if (result.stillRunning) {
                int skipped = tests.size() - i - 1;
                if (skipped > 0) {
                    System.out.println(RED + "Stopped: the solution is still running, " + skipped
                            + " remaining test(s) skipped." + RESET);
                }
                break;
            }
        }

        String line = "=".repeat(50);
        System.out.println(line);
        if (passed == tests.size()) {
            System.out.println(GREEN + "All tests passed! (" + passed + "/" + tests.size() + ")" + RESET);
        } else {
            System.out.println(RED + "Summary: " + passed + "/" + tests.size() + " passed" + RESET);
        }
        System.out.println(line);
        return passed == tests.size() ? EXIT_PASSED : EXIT_FAILED;
    }

    private static void report(TestData test, Result result, long timeLimitMs) {
        String color = result.passed ? GREEN : RED;
        String time = " (" + result.timeMs + " ms)";
        System.out.println("Test " + test.number + ": " + color + result.verdict + RESET + time);

        if (result.passed && timeLimitMs > 0 && result.timeMs > timeLimitMs) {
            System.out.println(YELLOW + "  Slower than the time limit of " + timeLimitMs
                    + " ms. Local time includes class loading and a cold JIT." + RESET);
        }
        if (result.passed) {
            System.out.println();
            return;
        }

        System.out.println("-".repeat(30));
        if (result.error != null) {
            System.out.println(RED + "Error: " + result.error + RESET);
        } else {
            System.out.println("Input:\n" + preview(test.input) + "\n");
            System.out.println("Expected:\n" + preview(test.expected) + "\n");
            System.out.println("Got:\n" + RED + preview(result.output) + RESET + "\n");
        }
    }

    static Result runTest(String className, URL[] classpath, TestData test, long timeLimitMs, boolean hardLimit) {
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        PrintStream capture = new PrintStream(captured, true, StandardCharsets.UTF_8);
        URLClassLoader loader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader());
        Throwable[] failure = new Throwable[1];

        Thread thread = new Thread(null, () -> {
            try {
                invokeEntryPoint(loader.loadClass(className));
            } catch (InvocationTargetException e) {
                failure[0] = e.getCause();
            } catch (Throwable e) {
                failure[0] = e;
            }
        }, "solution-test-" + test.number, STACK_SIZE_BYTES);
        thread.setDaemon(true);
        thread.setContextClassLoader(loader);

        PrintStream originalOut = System.out;
        InputStream originalIn = System.in;
        //~ 0 means no limit: Thread.join(0) waits until the solution ends
        long hardLimitMs = hardLimit ? Math.max(MIN_HARD_LIMIT_MS, timeLimitMs * HARD_LIMIT_FACTOR) : 0;
        long start = System.nanoTime();
        boolean stillRunning;
        try {
            System.setIn(new ByteArrayInputStream(test.input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(capture);
            thread.start();
            thread.join(hardLimitMs);
            stillRunning = thread.isAlive();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stillRunning = thread.isAlive();
        } finally {
            capture.flush();
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        long timeMs = (System.nanoTime() - start) / 1_000_000;

        if (stillRunning) {
            return new Result(false, "TIME LIMIT EXCEEDED", "", timeMs,
                    "Still running after " + hardLimitMs + " ms", true);
        }
        closeQuietly(loader);

        String output = captured.toString(StandardCharsets.UTF_8);
        if (failure[0] != null) {
            return new Result(false, "RUNTIME ERROR", output, timeMs, describe(failure[0]), false);
        }
        boolean same = normalize(output).equals(normalize(test.expected));
        return new Result(same, same ? "ACCEPTED" : "WRONG ANSWER", output, timeMs, null, false);
    }

    /** Supports a legacy {@code solve()} instance method, {@code main(String[])} and Kotlin's {@code main()}. */
    private static void invokeEntryPoint(Class<?> type) throws Exception {
        Method solve = findMethod(type, "solve");
        if (solve != null && !Modifier.isStatic(solve.getModifiers())) {
            solve.invoke(type.getDeclaredConstructor().newInstance());
            return;
        }
        Method main = findMethod(type, "main", String[].class);
        if (main != null) {
            main.invoke(null, (Object) new String[0]);
            return;
        }
        Method mainNoArgs = findMethod(type, "main");
        if (mainNoArgs != null) {
            mainNoArgs.invoke(null);
            return;
        }
        throw new IllegalStateException("No entry point solve() or main() in class " + type.getName());
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            return type.getMethod(name, parameters);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /** Compares outputs like most judges: line endings, trailing spaces and trailing blank lines do not matter. */
    static List<String> normalize(String text) {
        List<String> lines = Arrays.stream(text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1))
                .map(String::stripTrailing)
                .collect(Collectors.toCollection(ArrayList::new));
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) {
            lines.remove(lines.size() - 1);
        }
        while (!lines.isEmpty() && lines.get(0).isEmpty()) {
            lines.remove(0);
        }
        return lines;
    }

    static List<TestData> loadTests(Path dir) throws IOException {
        List<TestData> tests = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            List<Integer> numbers = files
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("\\d+\\.in"))
                    .map(name -> Integer.parseInt(name.substring(0, name.length() - 3)))
                    .sorted()
                    .collect(Collectors.toList());
            for (int number : numbers) {
                String input = Files.readString(dir.resolve(number + ".in"), StandardCharsets.UTF_8);
                Path expectedFile = dir.resolve(number + ".out");
                String expected = Files.exists(expectedFile)
                        ? Files.readString(expectedFile, StandardCharsets.UTF_8)
                        : "";
                tests.add(new TestData(number, input, expected));
            }
        }
        return tests;
    }

    private static String findEntryClass(String className, URL[] classpath) {
        try (URLClassLoader loader = new URLClassLoader(classpath, ClassLoader.getPlatformClassLoader())) {
            for (String candidate : new String[]{className, className + "Kt"}) {
                try {
                    Class.forName(candidate, false, loader);
                    return candidate;
                } catch (ClassNotFoundException ignored) {
                    // try the next candidate
                }
            }
        } catch (IOException ignored) {
            // closing failed, nothing to clean up
        }
        return null;
    }

    /**
     * The classpath for the solution. When the IDE shortens the command line with a wrapper class loader, the real
     * classpath is in that loader, not in {@code java.class.path}.
     */
    private static URL[] solutionClasspath() {
        ClassLoader own = RemoteTestRunner.class.getClassLoader();
        if (own instanceof URLClassLoader) {
            return ((URLClassLoader) own).getURLs();
        }
        List<URL> urls = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
            if (entry.isEmpty()) {
                continue;
            }
            try {
                urls.add(new File(entry).toURI().toURL());
            } catch (MalformedURLException ignored) {
                // skip entries that are not valid paths
            }
        }
        return urls.toArray(new URL[0]);
    }

    private static String describe(Throwable error) {
        StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        return trace.toString().lines()
                .filter(line -> !line.contains("com.roy.khelper.runner.RemoteTestRunner")
                        && !line.contains("java.base/jdk.internal.reflect")
                        && !line.contains("java.base/java.lang.reflect.Method.invoke")
                        && !line.contains("java.base/java.lang.Thread.run"))
                .collect(Collectors.joining("\n"));
    }

    private static String preview(String text) {
        String trimmed = text.strip();
        return trimmed.length() <= PREVIEW_CHARS ? trimmed : trimmed.substring(0, PREVIEW_CHARS) + "\n...";
    }

    private static void closeQuietly(URLClassLoader loader) {
        try {
            loader.close();
        } catch (IOException ignored) {
            // nothing to clean up
        }
    }

    static final class TestData {
        final int number;
        final String input;
        final String expected;

        TestData(int number, String input, String expected) {
            this.number = number;
            this.input = input;
            this.expected = expected;
        }
    }

    static final class Result {
        final boolean passed;
        final String verdict;
        final String output;
        final long timeMs;
        final String error;
        //~ True when the solution thread did not stop. Later tests would share its System.in and System.out.
        final boolean stillRunning;

        Result(boolean passed, String verdict, String output, long timeMs, String error, boolean stillRunning) {
            this.passed = passed;
            this.verdict = verdict;
            this.output = output;
            this.timeMs = timeMs;
            this.error = error;
            this.stillRunning = stillRunning;
        }
    }
}
