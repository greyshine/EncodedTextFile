package de.greyshine.encodedtextfile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;

@Slf4j
public final class Utils {

    public static final long MINUTES_1 = 60 * 1000;
    public static final long MINUTES_10 = 10 * MINUTES_1;

    private Utils() {
    }

    public static File getCanonicalFile(File file) {

        if (file == null) {
            return null;
        }

        try {
            return file.getCanonicalFile();
        } catch (IOException exception) {
            // intended to nothing
        }

        return file.getAbsoluteFile();

    }

    public static String getCanonicalPath(File file) {
        return file == null ? null : getCanonicalFile(file).getAbsolutePath();
    }

    public static void createFile(File file) {

        if (file == null || file.isDirectory() || file.isFile()) {
            return;
        }

        try {
            file.createNewFile();
        } catch (IOException e) {
            log.warn("file creation failed: " + file, e);
        }
    }

    /**
     * See: https://stackoverflow.com/a/21529994/845117
     *
     * @param request
     * @return the client's IP
     */
    public static String getClientIpAddr(HttpServletRequest request) {

        Assert.notNull(request, "HttpServletRequest must not be null");

        String ip = request.getHeader("X-Forwarded-For");

        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getHeader("REMOTE_ADDR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase("unknown")) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
