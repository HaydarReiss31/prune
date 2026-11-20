package com.haydareiss.pkgx;

import androidx.annotation.NonNull;

import com.hamondev.shevery.api.SheveryPrivilege;
import com.hamondev.shevery.api.SheveryRemoteProcess;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Shevery sunucusu üzerinden (ADB uid 2000 ya da root) komut çalıştırır.
 * Komut bir dizi olarak verilir; kabuk (sh -c) kullanılmaz, bu yüzden argümanlar
 * kabuk tarafından yorumlanmaz.
 */
final class PrivilegedShell {

    static final class Result {
        final int exitCode;
        final String out;
        final String err;

        Result(int exitCode, String out, String err) {
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }

        boolean ok() {
            if (exitCode != 0) return false;
            // pm bazı hatalarda 0 döndürüp "Failure [...]" yazabiliyor.
            return !(out.startsWith("Failure") || out.startsWith("Error") || out.startsWith("Exception"));
        }

        @NonNull
        String summary() {
            if (!out.isEmpty()) return out;
            if (!err.isEmpty()) return err;
            return "exit " + exitCode;
        }
    }

    private PrivilegedShell() {
    }

    /** Arka plan iş parçacığında çağrılmalı. */
    static Result run(String... cmd) {
        SheveryRemoteProcess process = null;
        try {
            process = SheveryPrivilege.newProcess(cmd, null, null);

            final InputStream errStream = process.getErrorStream();
            final StringBuilder errBuf = new StringBuilder();
            Thread errThread = new Thread(() -> errBuf.append(readAll(errStream)), "pkgx-stderr");
            errThread.start();

            String out = readAll(process.getInputStream());
            int code = process.waitFor();
            errThread.join(2000);

            return new Result(code, out.trim(), errBuf.toString().trim());
        } catch (Throwable t) {
            return new Result(-1, "", String.valueOf(t.getMessage() != null ? t.getMessage() : t));
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String readAll(InputStream in) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        try {
            int n;
            while ((n = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, n);
            }
        } catch (IOException ignored) {
        } finally {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }
}
