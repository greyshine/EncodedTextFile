package de.greyshine.encodedtextfile;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class Encryptor {

    /**
     * TODO is it really needed to be threadlocal?
     */
    private static final ThreadLocal<MessageDigest> TL_MESSAGEDIGEST = new ThreadLocal<MessageDigest>() {
        @SneakyThrows
        @Override
        protected MessageDigest initialValue() {
            return MessageDigest.getInstance("SHA-256");
        }
    };
    public final String versionPrePostFix = "VERSION-1";

    public String getPrePostFix() {
        return versionPrePostFix;
    }

    public void encrypt(String password, String text, OutputStream out) throws IOException {

        if (password == null || text == null || out == null) {
            return;
        }

        final DataOutputStream dos = new DataOutputStream(out);
        final Iterator<Integer> saltIter = getPwdIterator(password);

        text = addPrePostFix(text);

        for (Character c : text.toCharArray()) {
            final int i = c ^ saltIter.next();
            dos.writeInt(i);
        }

        dos.flush();
    }

    public String decrypt(String password, InputStream inputStream) throws IOException {

        if (password == null || inputStream == null) {
            return null;
        }

        final DataInputStream dis = new DataInputStream(inputStream);
        final Iterator<Integer> saltIter = getPwdIterator(password);
        final StringBuilder result = new StringBuilder();

        while (dis.available() > 0) {
            final int i = dis.readInt();
            final int c = i ^ saltIter.next();
            ;
            result.append((char) c);
        }

        return removePrePostFix(result.toString());
    }

    private String addPrePostFix(String text) {
        return getPrePostFix() + (text == null ? "" : text) + getPrePostFix();
    }

    private String removePrePostFix(String text) {

        final String prePostFix = getPrePostFix();
        final int prePostFixLength = prePostFix.length();

        Assert.notNull(text, "text is null");
        Assert.isTrue(text.startsWith(prePostFix), "text does not start as expected");
        Assert.isTrue(text.endsWith(prePostFix), "text does not end as expected");
        Assert.isTrue(text.length() >= 2 * prePostFixLength, "text not as long as expected");

        return text.substring(prePostFixLength, text.length() - prePostFixLength);
    }

    protected Iterator<Integer> getPwdIterator(String password) {

        password = password == null ? "" : password;

        final String pwdSha256 = toSha256(password);

        // start at index which is determined by the password
        int i = password.length();
        for (byte b : password.getBytes(StandardCharsets.UTF_8)) {
            i += b;
        }
        i *= password.length();
        i = Math.abs(i);

        final AtomicInteger salt = new AtomicInteger(i);
        i = i % password.length();

        final int startIndex = i;

        return new Iterator<Integer>() {

            final byte[] pwdBytes = pwdSha256.getBytes(StandardCharsets.UTF_8);
            int index = startIndex;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {

                final int result = pwdBytes[index++] ^ salt.getAndAdd(index);

                if (index >= pwdBytes.length) {
                    index = 0;
                }

                return result;
            }
        };
    }

    /**
     * See https://www.baeldung.com/sha-256-hashing-java
     *
     * @param text
     * @return
     */
    private String toSha256(String text) {

        Assert.notNull(text, "text is null");

        final byte[] bytes = TL_MESSAGEDIGEST.get().digest(text.getBytes(StandardCharsets.UTF_8));
        final StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            sb.append(byte2String(b));
        }

        //log.debug( "text={} -> sha256={}", text, sb );
        return sb.toString();
    }

    private String byte2String(byte b) {
        final String hex = Integer.toHexString(0xff & b);
        return hex.length() == 1 ? '0' + hex : hex;
    }

    private byte string2byte(String b) {
        return (byte) (Integer.parseInt(b, 16) & 0xff);
    }
}
