@echo off
java -server -Xmx128m -Dnet.sf.odinms.wzpath=xml\ -Djavax.net.ssl.keyStore=filename.keystore -Djavax.net.ssl.keyStorePassword=passwd -Djavax.net.ssl.trustStore=filename.keystore -Djavax.net.ssl.trustStorePassword=passwd -jar lib/JavaStory-World.jar
pause