package javastory.login;

/**
 * Hello world!
 *
 */
public class StartLogin {

    public static void main(String[] args) {
        System.setProperty("org.javastory.world.ip", "127.0.0.1");
        System.setProperty("org.javastory.login.config",
                "login.properties");
        
        LoginServer.start();

    }
}
