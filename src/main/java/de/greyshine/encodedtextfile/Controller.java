package de.greyshine.encodedtextfile;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static de.greyshine.encodedtextfile.Utils.MINUTES_10;
import static de.greyshine.encodedtextfile.Utils.getClientIpAddr;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@RestController
public class Controller {

    public static final List<String> accesses = new ArrayList<>();
    public static final List<String> LOGS = new ArrayList<>();
    /**
     * Http Heder with the token information
     */
    public static final String HEADER_TOKEN = "token";
    private static final Set<TokenHolder> tokenHolders = new HashSet<>();
    // 10 min
    private static final long VALID_INTERVAL = MINUTES_10;
    private static final int MAX_COUNT_BAD_LOGINS = 5;
    // 10 min
    private static final long badLoginTimeToLive = MINUTES_10;
    /**
     * Amount of calling '/status' in the browser until a screen is displayed.
     * Before that 404 is returned.
     */
    private static final int loginfoCallsNeeded = 3;
    private static final List<BadLogin> badLogins = new ArrayList<>();
    private static int loginfoCalls = 0;
    @Value("${version}")
    private String version;

    @Autowired
    private DataService dataService;

    @Autowired
    private Encryptor encryptor;

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
        sb.append("TokenHolders:\n");
        tokenHolders.forEach(th -> sb.append(th).append('\n'));
        // TODO write current logins
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

        final String ip = getClientIpAddr(request);
        final String password = loginRequestBody.getPassword();

        if (isTooManyBadLogins()) {

            internalLog(request, "too many bad logins");

            log.warn("user from {} exceeded bad logins password={}.", ip, password);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Too many users, you dick-head!");

        } else if (!dataService.isFileAndPasswordOrCreate(loginRequestBody.getFilename(), loginRequestBody.getPassword())) {

            log.info("login fail ip={}", ip);
            internalLog(request, "login failure");
            badLogins.add(new BadLogin(request));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Credentials suck!");
        }

        final File file = dataService.getFile(loginRequestBody.getFilename());

        if (!file.isFile()) {
            badLogins.add(new BadLogin(request));
            internalLog(request, "login failure; bad file: " + file);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "What are you trying to assecc?");
        }

        final TokenHolder otherTokenHolder = getTokenHolder(file);
        if (otherTokenHolder != null) {
            tokenHolders.remove(otherTokenHolder);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Someone else is logged in.");
        }

        final TokenHolder tokenHolder = new TokenHolder()
                .file(file)
                .password(password)
                .ip(ip)
                .refreshAccess();

        tokenHolders.add(tokenHolder);

        log.info("login ip={}, token={}", tokenHolder.getIp(), tokenHolder.getToken());
        return tokenHolder.getToken();
    }

    private synchronized TokenHolder getTokenHolder(File file) {

        for (TokenHolder tokenHolder : new HashSet<>(tokenHolders)) {
            if (tokenHolder.isFile(file)) {
                return tokenHolder;
            }
        }

        return null;
    }

    private void cleanBadLogins() {
        for (BadLogin badLogin : new HashSet<>(badLogins)) {
            if (!badLogin.isValid()) {
                badLogins.remove(badLogin);
            }
        }
    }

    private boolean isTooManyBadLogins() {
        cleanBadLogins();
        return badLogins.size() >= MAX_COUNT_BAD_LOGINS;
    }

    @PostMapping(value = "/api/logout", consumes = MediaType.APPLICATION_JSON_VALUE)
    public synchronized boolean logout(HttpServletRequest request) {

        final TokenHolder tokenHolder = getTokenHolder(request, false, false);

        log.info("logout {}", tokenHolder);

        if (tokenHolder != null) {
            tokenHolder.invalidate();
        } else {

            final String logText = "logout of not existing token holder: " + Utils.getClientIpAddr(request);
            LOGS.add(logText);
            log.info("{}", logText);
        }

        return true;
    }

    @GetMapping(value = "/api/data", produces = MediaType.TEXT_PLAIN_VALUE)
    public String get(HttpServletRequest request) {

        final TokenHolder tokenHolder = getTokenHolder(request, true, true);

        try {

            return encryptor.decrypt(tokenHolder.getPassword(), tokenHolder.getFile());

        } catch (IllegalArgumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "load error: " + exception.getMessage());
        }
    }

    @PostMapping(value = "/api/data", consumes = MediaType.TEXT_PLAIN_VALUE)
    public void post(@RequestBody String payload, HttpServletRequest request) {

        final TokenHolder tokenHolder = getTokenHolder(request, true, true);

        if (payload == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "no payload");
        }

        try {

            encryptor.encrypt(tokenHolder.getPassword(), payload.trim(), tokenHolder.getFile());

        } catch (IllegalArgumentException | IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "safe error: " + exception.getMessage());
        }
    }

    private TokenHolder getTokenHolder(HttpServletRequest request, boolean refreshValidity, boolean throw403Exception) {

        final String headerToken = request.getHeader(HEADER_TOKEN);

        if (headerToken == null && throw403Exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        } else if (isBlank(headerToken)) {
            return null;
        } else if (tokenHolders.isEmpty()) {
            return null;
        }

        final String ip = getClientIpAddr(request);
        log.debug("amount TokenHolders to check: {}, {}", tokenHolders.size(), tokenHolders);

        for (TokenHolder aTokenHolder : new HashSet<>(tokenHolders)) {

            if (!aTokenHolder.isToken(headerToken)) {
                continue;
            }

            // only use the token holder with matching IP
            if (!aTokenHolder.isIp(ip)) {
                continue;
            }

            if (aTokenHolder.isValid(headerToken, refreshValidity)) {
                return aTokenHolder;
            } else {
                aTokenHolder.invalidate();
            }
        }

        if (throw403Exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return null;
    }

    private synchronized void internalLog(HttpServletRequest request, Object object) {

        while (LOGS.size() > 500) {
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
        private String filename;

        public void setPassword(String password) {

            this.filename = null;
            this.password = null;

            final String[] result = new String[2];
            if (isBlank(password)) {
                return;
            }

            final int idx = password.indexOf(':');
            if (idx < 0) {
                return;
            }

            this.filename = password.substring(0, idx);
            this.password = password.substring(idx + 1);
        }
    }

    @Data
    public static class SaveRequestBody {
        private String file;
        private String data;
    }

    /**
     * Class / Object holding the information of a logged in user.
     */
    @Data
    private static class TokenHolder {

        private String token = Long.toHexString(System.currentTimeMillis()) + "-" + UUID.randomUUID();
        private File file;
        private String ip;
        private long lastAccess = 0;
        private String password;

        public TokenHolder refreshAccess() {
            lastAccess = System.currentTimeMillis();
            return this;
        }

        TokenHolder ip(String ip) {
            this.ip = ip;
            return this;
        }

        TokenHolder file(File file) {
            this.file = file;
            return this;
        }

        TokenHolder password(String password) {
            this.password = password;
            return this;
        }

        synchronized void invalidate() {

            this.file = null;
            this.lastAccess = 0;
            this.password = null;

            Controller.tokenHolders.remove(this);
        }

        boolean isValid(String token, boolean refreshValidity) {

            if (this.token == null) {
                return false;
            } else if (file == null || !file.isFile()) {
                return false;
            }

            if (lastAccess + MINUTES_10 < System.currentTimeMillis()) {
                log.info("isValid({}, {}):{} due to timeout ({} ms)", token, refreshValidity, false, System.currentTimeMillis() - (lastAccess + MINUTES_10));
                return false;
            }

            if (isBlank(password)) {
                return false;
            }

            if (!StringUtils.equals(token, this.token)) {
                return false;
            }

            if (refreshValidity) {
                lastAccess = System.currentTimeMillis();
            }

            return true;
        }

        public boolean isToken(String token) {
            return isNotBlank(this.token) && this.token.equals(token);
        }

        public boolean isFile(File file) {
            final String pathThisFile = Utils.getCanonicalPath(this.file);
            final String pathParameterFile = Utils.getCanonicalPath(file);
            return StringUtils.equals(pathThisFile, pathParameterFile);
        }

        public boolean isIp(String ip) {
            return isNotBlank(this.ip) && this.ip.equals(ip);
        }

        public String toString() {
            return TokenHolder.class.getSimpleName() + "{token=" + token + ", file=" + this.file + ", ip=" + this.ip + "}";
        }
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
