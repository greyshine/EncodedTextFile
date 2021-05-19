import de.greyshine.encodedtextfile.Encryptor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;

@Slf4j
public class EncryptorTests {

    @Test
    public void test() throws IOException {

        final String password = "password";
        final String text = "Some Data to be encrypted";

        final Encryptor encryptor = new Encryptor();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();

        encryptor.encrypt(password, text, baos);

        Assertions.assertNotNull(baos.toString("UTF-8"), "encrypted text is null");
        Assertions.assertNotEquals(text, baos.toString("UTF-8"));

        final String decryptedData = encryptor.decrypt(password, new ByteArrayInputStream(baos.toByteArray()));

        log.info("decrypted:\n{}", decryptedData);

        Assertions.assertNotNull(decryptedData, "decrypted text is null");
        Assertions.assertEquals(decryptedData, text);
    }

    @Test
    public void testRandomData() throws IOException {

        final int charCount = 4000;
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder();

        sb.setLength(0);
        while (sb.length() < 15) {
            sb.append((char) random.nextInt());
        }

        final String password = sb.toString();

        sb.setLength(0);
        while (sb.length() < charCount) {
            final char c = (char) random.nextInt();
            sb.append(c);
        }

        final String text = sb.toString().trim();

        final Encryptor encryptor = new Encryptor();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        encryptor.encrypt(password, text, baos);

        //log.info( "encrypted:\n{}", baos.toString("UTF-8") );

        final String textDecrypted = encryptor.decrypt(password, new ByteArrayInputStream(baos.toByteArray()));

        Assertions.assertEquals(text, textDecrypted, "Texts are not equal:>\n" + text + "\n\n" + textDecrypted);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            encryptor.decrypt(new StringBuffer(password).reverse().toString(), new ByteArrayInputStream(baos.toByteArray()));
        });
    }
}
