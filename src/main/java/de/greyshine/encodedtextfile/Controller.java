package de.greyshine.encodedtextfile;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

@Slf4j
@RestController
public class Controller {

    private final static List<Character> PWD = new ArrayList<>();
    private static final long VALID_INTERVAL = 10 * 60 * 1000;
    private final static int maxBadLogins = 5;
    private final static int badLoginTimeToLive = 10 * 60 * 1000;
    private static String token = null;
    private static String ip = null;
    private static long maxValidTime = Long.MAX_VALUE;
    private static List<Long> badLogins = new ArrayList<>(maxBadLogins);

    public final String HEADER_TOKEN = "token";

    @Autowired
    private DataService dataService;

    public synchronized static String getPwd() {

        if (PWD.isEmpty()) {
            return null;
        }

        final StringBuilder sb = new StringBuilder();
        PWD.forEach(c -> sb.append(c));
        return sb.toString();
    }

    public synchronized void setPwd(String pwd) {

        Assert.isTrue(pwd == null || !pwd.isEmpty(), "Pwd is already set");

        if (pwd == null) {

            PWD.clear();

        } else {

            for (Character c : pwd.toCharArray()) {
                PWD.add(c);
            }
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

    @PostMapping(value = "/api/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized String login(@RequestBody LoginRequestBody loginRequestBody, HttpServletRequest request, HttpServletResponse response) throws IOException {

        final Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            final String hName = headers.nextElement();
            //log.debug( "HEADER {}={}", hName, request.getHeader(hName) );
        }

        cleanBadLogins();

        final String password = loginRequestBody.getPassword();

        if (badLogins.size() >= maxBadLogins) {

            log.warn("user from {} exceeded bad logins." + getClientIpAddr(request));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Too many users, you dick-head!");

        } else if (!dataService.isPassword(password)) {
            log.info("login fail ip={}", getClientIpAddr(request));
            badLogins.add(System.currentTimeMillis() + badLoginTimeToLive);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credentials suck!");
        }

        setPwd(password);
        ip = getClientIpAddr(request);

        if (token != null) {
            log.warn("resetting token '{}' ...", token);
        }

        token = Long.toHexString(System.currentTimeMillis()) + "-" + UUID.randomUUID();
        refreshValidTime();

        log.info("login ip={}", getClientIpAddr(request));
        log.info("token={}", token);

        return token;
    }

    private void cleanBadLogins() {
        for (Long timeToLiveUntil : new HashSet<>(badLogins)) {
            if (System.currentTimeMillis() > timeToLiveUntil) {
                badLogins.remove(timeToLiveUntil);
            }
        }
    }

    @PostMapping(value = "/api/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized boolean logout(HttpServletRequest request) {

        if (!isIp(request)) {
            log.info("logout from foreign IP ignored");
            return false;
        }

        final String token = request.getHeader(HEADER_TOKEN);
        log.info("logout token={}", token);

        final boolean result = isTokenValid(token, false);

        invalidateToken();

        return result;
    }

    private synchronized void refreshValidTime() {
        maxValidTime = System.currentTimeMillis() + VALID_INTERVAL;
    }

    private synchronized boolean isTokenValid(String token, boolean refreshValidity) {

        log.debug("token ->            {}", token);
        log.debug("Controller.token -> {}", Controller.token);
        log.debug("!PWD.empty -> {}", !PWD.isEmpty());
        log.debug("maxValidTime > System.currentTimeMillis() -> {}", maxValidTime > System.currentTimeMillis());

        final boolean isValid = token != null && token.equalsIgnoreCase(Controller.token) && !PWD.isEmpty() && maxValidTime > System.currentTimeMillis();

        if (isValid && refreshValidity) {
            refreshValidTime();
        } else if (!isValid) {
            this.invalidateToken();
        }

        return isValid;
    }

    public synchronized void invalidateToken() {
        setPwd(null);
        Controller.token = null;
        maxValidTime = Long.MAX_VALUE;
    }

    @GetMapping(value = "/api/data", produces = MediaType.TEXT_PLAIN_VALUE)
    public String get(HttpServletRequest request) {

        checkLoggedIn(request, true);

        final String pwd = getPwd();

        try {

            return dataService.load(pwd);

        } catch (IllegalArgumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "load error: " + exception.getMessage());
        }
    }

    @PostMapping(value = "/api/data", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void post(@RequestBody String payload, HttpServletRequest request) {

        checkLoggedIn(request, true);

        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no payload");
        }

        try {

            dataService.store(getPwd(), payload.trim());

        } catch (IllegalArgumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "safe error: " + exception.getMessage());
        }
    }

    private void checkLoggedIn(HttpServletRequest request, boolean refreshValidity) {

        final String token = request.getHeader(HEADER_TOKEN);

        boolean isOk = true;
        isOk = isOk && isTokenValid(token, refreshValidity);
        isOk = isOk && isIp(request);

        if (!isOk) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean isIp(HttpServletRequest request) {
        return request != null && ip != null && ip.equals(getClientIpAddr(request));
    }

    @Data
    public static class PwdForm {
        private String pwd;
    }

    @Data
    public static class LoginRequestBody {
        private String password;
    }

}
