package de.greyshine.encodedtextfile;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.util.Assert;

import java.io.File;

@SpringBootApplication
@Slf4j
public class Main {

    @Autowired
    private DataService dataService;

    public static void main(String[] args) throws Exception {

        if (isHelpArg(args)) {
            System.out.println("usage:");
            System.out.println("java -jar EncodedTextFile-VERSION.jar [--server.port=<http(s)-port=8080or8443>] [--server.ssl.key-store=<KEYSTORE.p12> --server.ssl.key-store-password==<KEYSTORE-PASSWORD>] <stores-dir:store>");
            System.out.println("");
            System.out.println("A key=value file 'application.properties' is read on the base path when exists.");
            System.out.println("path is " + Utils.getCanonicalPath(new File(".")));
            System.exit(0);
        }

        SpringApplication.run(Main.class, args);
    }

    private static boolean isHelpArg(String[] args) {

        if (args == null) {
            return false;
        }
        for (String arg : args) {

            arg = StringUtils.trimToEmpty(arg);

            if ("-h".equalsIgnoreCase(arg) || "--help".equalsIgnoreCase(arg)) {
                return true;
            }
        }

        return false;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        Assert.isTrue(!dataService.getFiles().isEmpty(), "no files on storage !");

        log.info("Have a good day and be peaceful ;-)");
    }
}
