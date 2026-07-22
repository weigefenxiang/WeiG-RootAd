package com.weig.rootad;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

final class RootShell {
    static final String CONTROL = "/data/adb/modules/weig_rootad/bin/rulectl";
    private static Boolean masterMountSupported;

    record Result(int code, String output) {
        boolean ok() { return code == 0; }
    }

    private RootShell() {}

    static Result runControl(String arguments) {
        return run("'" + CONTROL + "' " + arguments);
    }

    static Result run(String command) {
        if (supportsMasterMount()) return execute(new String[]{"su", "-mm", "-c", command});
        return execute(new String[]{"su", "-c", command});
    }

    private static synchronized boolean supportsMasterMount() {
        if (masterMountSupported == null) {
            Result probe = execute(new String[]{"su", "-mm", "-c", "exit 0"});
            masterMountSupported = probe.ok();
        }
        return masterMountSupported;
    }

    private static Result execute(String[] command) {
        StringBuilder output = new StringBuilder();
        try {
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) output.append(line).append('\n');
                } catch (Exception ignored) {
                    // A forced timeout closes the stream; the timeout result below is authoritative.
                }
            }, "zeroad-root-output");
            readerThread.setDaemon(true);
            readerThread.start();
            if (!process.waitFor(25, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                readerThread.join(1_000);
                return new Result(124, "Root command timed out");
            }
            readerThread.join(1_000);
            return new Result(process.exitValue(), output.toString().trim());
        } catch (Exception error) {
            return new Result(127, error.getMessage() == null ? error.toString() : error.getMessage());
        }
    }

    static String quote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
