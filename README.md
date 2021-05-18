# EncodedTextFile

## Create a keystore  
[https://mkyong.com/spring-boot/spring-boot-ssl-https-examples/]()

    keytool -genkeypair -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650

Passing/seeing an insecure webpage:  
Mouseclick some where and then enter `thisisunsafe`.  
Weired but true: https://www.technipages.com/google-chrome-bypass-your-connection-is-not-private-message

!!! The keystore has to be created on the machine where the java-jar is executed.  
Just copying the keystore.p12 from one machine to the other will not work.


## Starting the maven packaged jar artifact

    java -jar EncodedTextFile-1.0-SNAPSHOT.jar --https-port=9090 --server.ssl.key-store=keystore.p12 --server.ssl.key-store-password=123456 data.enc-txt 2>&1 1>>logs.txt &
