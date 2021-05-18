#!/bin/sh
java -jar EncodedTextFile-<VERSION>.jar --server.port=9090 --server.ssl.key-store=keystore.p12 --server.ssl.key-store-password=123456 data.enc-txt 2>&1 1>>logs.txt &
echo started.