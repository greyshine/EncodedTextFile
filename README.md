# EncodedTextFile

The software lets you store text securly in a file.  
The data will be encrypted which is also based on SHA-256.  
The software is a web software. It runs by calling the internet browser.

The first time a file is accessed, then that password is used.

Access a file by entering

     <FILENAME>:<PASSWORD>

The filename must not contain the actual ending of `.dat`.

Example directory layout:

    /
    store/
        secret.dat
    application.properties
    EncodedTextFile-<VERSION>.jar
    keystore.p12

where content of application.property is like:

    server.port=8443
    server.ssl.store-type=pkcs12
    server.ssl.key-store=keystore.p12
    server.ssl.key-store-password=123456

For declaring a keystore.p12 file and setting the password see: 'Create a keystore'.  
This project is based on Java Springboot and VueJS.

Note: You do have to manually create empty `.dat` files in the `store` folder on the machine where the software is
started. Only existing empty files can initially be used as a store.

## <a name="sha256"></a> SHA-256 of the artefacts

Version 1.4:

- removed some development output on the page

  sha256:

Version 1.3:

- redo correct sha versioning of jar artefact
- Added maven cleaning of generated vue folders

  sha256: 60eaddf27749f5a190eb4072032add7d1ce5d2d8fbf2486a55392795e7304eb9

Version 1.2:

    sha256: 5afe010bbb4e38003a83efc46874831dfef1d535eef8d753893706095628ae54

_This SHA-256 has been wrongly/different included in this README.md before; original is the one uploaded with the packed
artefact._

## Starting the maven packaged jar artifact

Simply start on default http-port 8080:

    java -jar EncodedTextFile-<VERSION>.jar

Start on declared http-port <PORT>:

    java -jar EncodedTextFile-<VERSION>.jar --server.port=<PORT>

Start on declared https-port with a `PKCS12` keystore file Create a file name `application.properties`:

    server.port=8088
    server.ssl.store-type=pkcs12
    server.ssl.key-store=keystore.p12
    server.ssl.key-store-password=123456

and start the application. the logs will show what protocol and port it started:

    java -jar EncodedTextFile-<VERSION>.jar

Print out the output into a `logs.txt` file (output stream and error stream):

    java -jar EncodedTextFile-<VERSION>.jar 2>&1 1>>logs.txt &

Using a properties file in order to declare

## Create a keystore

[https://mkyong.com/spring-boot/spring-boot-ssl-https-examples/]()

    keytool -genkeypair -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore keystore.p12 -validity 3650

Passing/seeing an insecure webpage:  
Mouseclick some where and then enter `thisisunsafe`.  
Weired but true: https://www.technipages.com/google-chrome-bypass-your-connection-is-not-private-message

!!! The keystore has to be created on the machine where the java-jar is executed.  
Just copying the keystore.p12 from one machine to the other will not work.

## Create a SHA-256 of the artefact

Tested on MacOS:

    shasum -a 256 EncodedTextFile-<VERSION>.jar | cut -d" " -f1

or

    openssl dgst -sha256 EncodedTextFile-<VERSION>.jar | cut -d" " -f2

