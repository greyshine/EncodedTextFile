package de.greyshine.encodedtextfile;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@Slf4j
public class Main {

    //HTTP port
    // TODO disable when not set -> ${http.port:null}
    @Value("${http.port:}")
    private Integer httpPort = null;

    @Value("${server.port:}")
    private Integer httpsPort = null;

    @Autowired
    private DataService dataService;

    public static void main(String[] args) throws Exception {

        if (args.length < 1) {
            System.err.println("usage e.g.: java -jar EcodedTextFile-VERSION.jar [--server.port=<https-port:default=8443>]  [--http.port=<http-port:default=none>] <filename.dat>");
            System.err.println("keystore type is PKCS12 when starting with SSL/HTTPS (--server.port=<httpsPortNum> is in use)");
            System.err.println("path is " + new File(".").getAbsolutePath());
            System.exit(1);
        }

        boolean isSsl = false;
        boolean isParameterKeystore = false;
        boolean isParameterKeystorePassword = false;

        final List<String> argsList = new ArrayList<>(Arrays.asList(args));
        for (int i = 0; i < args.length; i++) {
            final String arg = args[i];
            if (arg.toLowerCase().startsWith("--server.port=")) {
                isSsl = true;
                argsList.add(0, "--server.ssl.keyStoreType=PKCS12");
            } else if (arg.toLowerCase().startsWith("--server.ssl.key-store=")) {
                isParameterKeystore = true;
            } else if (arg.toLowerCase().startsWith("--server.ssl.key-store-password=")) {
                isParameterKeystorePassword = true;
            }
        }
        args = argsList.toArray(new String[0]);
        for (int i = 0; i < args.length; i++) {
            //System.out.println("arg[" + i + "] " + args[i]);
        }

        if (isSsl && !isParameterKeystore) {
            System.err.println("no parameter '--server.ssl.key-store=<KEYSTORE>.p12'");
            System.exit(1);
        } else if (isSsl && !isParameterKeystorePassword) {
            System.err.println("no parameter '--server.ssl.key-store-password=<PASSWORD>'");
            System.exit(1);
        }

        final String lastArg = args[args.length - 1];
        final File file = new File(lastArg).getCanonicalFile();
        if (!file.exists()) {
            file.createNewFile();
            if (!file.exists()) {
                System.err.println("cannot create file: " + file);
                System.exit(1);
            }
        }

        SpringApplication.run(Main.class, args);
    }

    // Let's configure additional connector to enable support for both HTTP and HTTPS
    @Bean
    public ServletWebServerFactory servletContainer() {

        final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();

        if (httpPort != null && httpPort > -1) {

            final Connector connector = new Connector(Http11NioProtocol.class.getCanonicalName());
            connector.setPort(httpPort);

            tomcat.addAdditionalTomcatConnectors(connector);
        }
        return tomcat;
    }

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {

        log.info("http-port: {}", this.httpPort);
        log.info("https-port: {}", this.httpsPort);

        log.info("file: {}", dataService.getFile());

        if (dataService.getFile() == null) {
            System.err.println("no file declared");
            System.exit(1);
        }

        log.info("Started ;-)");
    }
}
