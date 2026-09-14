package com.example.ramstatusbar;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * 稳健的 root 命令执行工具。
 *
 * 解决以下常见问题：
 * 1. 部分设备 PATH 中没有 su，需要逐个尝试常见路径；
 * 2. su 进程的 stdout/stderr 缓冲区填满会导致 waitFor() 死锁，
 *    必须在后台线程持续读取；
 * 3. 写完命令后必须关闭输入流发送 EOF，su 才会退出；
 * 4. waitFor() 必须带超时，避免极端情况下永久卡死。
 */
public final class RootUtils {

    private RootUtils() {
    }

    private static final String[] SU_PATHS = {
            "su",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/vendor/bin/su",
            "magisk"
    };

    /**
     * 以 root 执行一条 shell 命令，返回是否成功。
     */
    public static boolean exec(String command) {
        Process su = null;
        try {
            su = startSuProcess();
            if (su == null) {
                return false;
            }

            // 后台线程持续读取输出，防止缓冲区填满导致死锁
            drain(su.getInputStream());
            drain(su.getErrorStream());

            DataOutputStream os =
                    new DataOutputStream(
                            su.getOutputStream()
                    );

            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close(); // 发送 EOF，让 su 退出

            if (!su.waitFor(
                    10,
                    TimeUnit.SECONDS
            )) {
                su.destroy();
                return false;
            }

            return su.exitValue() == 0;

        } catch (Throwable t) {
            return false;
        } finally {
            if (su != null) {
                try {
                    su.destroy();
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static Process startSuProcess() {
        for (String path : SU_PATHS) {
            try {
                return Runtime.getRuntime()
                        .exec(path);
            } catch (Throwable ignored) {
                // 尝试下一个路径
            }
        }
        return null;
    }

    private static void drain(
            final InputStream in) {

        if (in == null) {
            return;
        }

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            byte[] buf =
                                    new byte[1024];

                            while (in.read(buf)
                                    != -1) {
                                // 丢弃输出
                            }

                        } catch (Throwable ignored) {
                        }
                    }
                },
                "root-utils-drain"
        ).start();
    }
}
