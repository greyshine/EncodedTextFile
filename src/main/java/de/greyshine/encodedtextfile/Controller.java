package de.greyshine.encodedtextfile;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;

@Slf4j
@RestController
public class Controller {

    @Value("${version}")
    private String version;

    public final static List<String> accesses = new ArrayList<>();
    public static final List<String> LOGS = new ArrayList<>();
    private final static List<Character> PWD = new ArrayList<>();
    // 10 min
    private static final long VALID_INTERVAL = 10 * 60 * 1000;
    private final static int maxBadLogins = 5;
    // 10 min
    private final static int badLoginTimeToLive = 10 * 60 * 1000;
    private final static int loginfoCallsNeeded = 3;
    private static final List<BadLogin> badLogins = new ArrayList<>(maxBadLogins);
    private static String token = null;
    private static String ip = null;
    /**
     * Time until the token is valid
     */
    private static long maxTokenValidTime = Long.MAX_VALUE;
    private static int loginfoCalls = 0;
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

    @GetMapping(value = "/status", produces = MediaType.TEXT_PLAIN_VALUE)
    public String loginfos() {

        if (loginfoCalls < loginfoCallsNeeded) {
            loginfoCalls++;
            log.info((loginfoCallsNeeded - loginfoCalls) + " missing status calls");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        loginfoCalls = 0;
        cleanBadLogins();

        final StringBuilder sb = new StringBuilder();

        sb.append("version: ").append(version).append('\n');
        sb.append("time: ").append(new Date()).append('\n');
        sb.append("----------------------------------------\n");
        sb.append("cur login=" + (PWD.size() > 0) + "\n");
        sb.append("----------------------------------------\n");
        sb.append("badlogins: ").append(badLogins.size()).append("; ").append(badLogins).append('\n');

        badLogins.forEach(badLogin -> sb.append(badLogin).append('\n'));

        sb.append("----------------------------------------\n");
        sb.append("Logs (" + LOGS.size() + "):\n");
        LOGS.forEach(log -> sb.append("- ").append(log).append('\n'));

        return sb.toString().trim();
    }

    @PostMapping(value = "/api/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized String login(@RequestBody LoginRequestBody loginRequestBody, HttpServletRequest request, HttpServletResponse response) throws IOException {

        final String password = loginRequestBody.getPassword();
        cleanBadLogins();

        if (badLogins.size() >= maxBadLogins) {

            internalLog(request, "too many bad logins");

            log.warn("user from {} exceeded bad logins password={}.", getClientIpAddr(request), password);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Too many users, you dick-head!");

        } else if (!dataService.isPassword(password)) {
            log.info("login fail ip={}", getClientIpAddr(request));
            internalLog(request, "login failure");
            badLogins.add(new BadLogin(request));
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

        for (BadLogin badLogin : new HashSet<>(badLogins)) {

            if (!badLogin.isValid()) {
                badLogins.remove(badLogin);
            }
        }
    }

    @PostMapping(value = "/api/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized boolean logout(HttpServletRequest request) {

        final String ip = getClientIpAddr(request);

        if (!ip.equals(Controller.ip)) {
            log.info("logout from bad IP ({}); ignored", ip);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }

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
        maxTokenValidTime = System.currentTimeMillis() + VALID_INTERVAL;
    }

    private synchronized boolean isTokenValid(String token, boolean refreshValidity) {

        log.debug("token ->            {}", token);
        log.debug("Controller.token -> {}", Controller.token);
        log.debug("!PWD.empty -> {}", !PWD.isEmpty());
        log.debug("maxTokenValidTime > System.currentTimeMillis() -> {}", maxTokenValidTime > System.currentTimeMillis());

        final boolean isValid = token != null && token.equalsIgnoreCase(Controller.token) && !PWD.isEmpty() && maxTokenValidTime > System.currentTimeMillis();

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
        maxTokenValidTime = Long.MAX_VALUE;
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


        final boolean isCorrectToken = isTokenValid(token, refreshValidity);
        final boolean isCorrectIp = isIp(request);
        final boolean isOk = isCorrectToken && isCorrectIp;

        if (!isOk) {
            final String logText = "checkLoggedIn forbidden [tokenOk=" + isCorrectToken + " (" + token + "), ipOk=" + isCorrectIp + " (" + getClientIpAddr(request) + ")]";
            log.info(logText);
            internalLog(request, logText);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean isIp(HttpServletRequest request) {
        return request != null && ip != null && ip.equals(getClientIpAddr(request));
    }

    private synchronized void internalLog(HttpServletRequest request, Object object) {
        while (LOGS.size() > 300) {
            LOGS.remove(LOGS.size() - 1);
        }
        LOGS.add(0, LocalDateTime.now().format(ISO_LOCAL_DATE_TIME) + ", " + getClientIpAddr(request) + ", " + object);
    }

    @Data
    public static class PwdForm {
        private String pwd;
    }

    @Data
    public static class LoginRequestBody {
        private String password;
    }

    @Data
    private class BadLogin {

        private final String time = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        private final String ip;
        private long timeToLiveUntil;

        private BadLogin(HttpServletRequest request) {
            this.timeToLiveUntil = System.currentTimeMillis() + Controller.badLoginTimeToLive;
            ip = getClientIpAddr(request);
        }

        public boolean isValid() {
            return timeToLiveUntil > System.currentTimeMillis();
        }
    }
}
