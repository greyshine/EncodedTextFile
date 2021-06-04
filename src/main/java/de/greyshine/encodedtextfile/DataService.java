package de.greyshine.encodedtextfile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Service
@Slf4j
public class DataService {

    public final static String FILE_ENDING = "dat";
    public final static Pattern PATTERN_FILE_NAME = Pattern.compile("[a-z0-9_\\-]{1,}");

    private File dataDir = new File("./store").getCanonicalFile();

    @Autowired
    private Encryptor encryptor;

    public DataService(ApplicationArguments args) throws IOException {

        final String lastArg = args.getSourceArgs().length == 0 ? null : args.getSourceArgs()[args.getSourceArgs().length - 1];

        if (lastArg != null && !lastArg.trim().startsWith("--")) {

            final File dir = Utils.getCanonicalFile(new File(lastArg));

            if (!dir.isDirectory()) {
                System.err.println("Given base directory does not exist: " + dir);
                System.exit(1);
            }

            dataDir = dir;
        }

        final List<File> files = getFiles();
        log.info("dataDir={}", dataDir);
        log.info("files: {}", files);

        if (files.isEmpty()) {
            log.error("no files found at {}", dataDir);
        }

        for (File file : files) {
            if (file.length() == 0) {
                log.warn("intial setup needed: {}\nfirst access sets password", file);
            }
        }

    }

    public List<File> getFiles() {
        final List<File> files = new ArrayList<>();
        for (File file : dataDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".dat")) {
                files.add(file);
            }
        }
        return files;
    }

    public File getDataDir() {
        return dataDir;
    }

    public boolean isFileAndPasswordOrCreate(String filename, String password) {

        if (isBlank(filename) || password == null) {
            return false;
        }

        final File file = getFile(filename);

        if (file == null || !file.isFile()) {

            return false;

        } else if (file.length() == 0) {

            try {
                initFile(file, password);
            } catch (IOException e) {
                log.error("Cannot initialize file: {} (password={}})", file, password == null ? "<null>" : "*****");
                throw new IllegalStateException("Initial File initialization failed: " + e);
            }
        }

        try {
            load(filename, password);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public File getFile(final String name) {

        Assert.notNull(name, "No propper file declared: " + name);
        Assert.isTrue(PATTERN_FILE_NAME.matcher(name).matches(), "No propper file declared: " + name);
        Assert.isTrue(!name.contains(".."), "No '..' in filename allowed: " + name);

        final File file = new File(dataDir, name + "." + FILE_ENDING);

        return file.isFile() ? file : null;
    }

    public String load(String name, String password) throws IOException {
        return encryptor.decrypt(password, getFile(name));
    }

    public void initFile(File file, String password) throws IOException {

        Assert.isTrue(file.isFile(), "File is not a file: " + file);
        Assert.isTrue(file.canWrite(), "File cannot write: " + file);
        Assert.isTrue(isNotBlank(password), "Password is blank");

        encryptor.encrypt(password, "Initialized...", file);

        log.info("initialized file {}", file);
    }
}
