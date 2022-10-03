import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.io.FileNotFoundException;

import server.Server;

public class App {
    private static final int PORT = 80;

    public static void main(String[] args) throws FileNotFoundException, IOException {
        Properties props = new Properties();
        props.load(new FileInputStream("etc/httpd.conf"));

        int poolSize = Integer.parseInt(props.getProperty("thread_limit"));

        (new Server(PORT, poolSize)).run();
    }
}
