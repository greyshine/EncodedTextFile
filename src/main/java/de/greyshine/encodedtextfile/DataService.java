package de.greyshine.encodedtextfile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class DataService {

    final File data;

    @Autowired
    private Encryptor encryptor;

    public DataService(ApplicationArguments args) throws IOException {

        data = new File(args.getSourceArgs()[args.getSourceArgs().length - 1]).getAbsoluteFile();
        log.info("file exists/read {} {}", data.exists(), data.canWrite());

        if (!data.exists()) {
            data.createNewFile();
        }

        Assert.isTrue(data.canWrite(), "Cannot access file: " + data);
    }

    public File getFile() {
        return data;
    }


    public void buildInitialFileIfNeccessary(String password) throws IOException {

        if (data.isFile() && data.canWrite()) {
            return;
        }

        Assert.isTrue(password != null && !password.isEmpty(), "Password is blank");
        store(password, "");
    }

    public synchronized void store(final String password, final String data) throws IOException {

        final File file = getFile();

        Assert.notNull(file, "No file declared");
        Assert.isTrue(file.canWrite(), "Cannot access file: " + file);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            encryptor.encrypt(password, data, fos);
            fos.flush();
        }
    }

    public synchronized String load(String password) throws IOException {

        final File file = getFile();

        Assert.notNull(file, "No file declared");
        Assert.isTrue(file.canWrite(), "Cannot access file: " + file);

        final StringBuilder sb = new StringBuilder();
        try (FileInputStream fis = new FileInputStream(file)) {
            return encryptor.decrypt(password, fis);
        }
    }

    public boolean isPassword(String password) throws IOException {

        password = password == null ? null : password.trim();

        if (password == null || password.isEmpty()) {
            return false;
        }

        if (data.length() == 0) {

            store(password, "Welcome with a new blank file...");
            return true;

        } else {

            try {
                load(password);
                return true;
            } catch (Exception e) {
                log.error("failed to check password '{}'", password.replaceAll(".", "*"));
                return false;
            }
        }
    }
}
