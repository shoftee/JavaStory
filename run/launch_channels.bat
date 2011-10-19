@echo off
java -server -Xms10m -Xmx512m -Dorg.javastory.wzpath=xml\ -Djavax.net.ssl.keyStore=filename.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=filename.keystore -Djavax.net.ssl.trustStorePassword=passwd -jar lib/JavaStory-Channel.jar
pause