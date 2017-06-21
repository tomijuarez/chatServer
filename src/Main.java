/**
 * Created by tomi on 18/06/17.
 */
public class Main {
    public static void main (String args[]) {
        int port = 5656;
        ChatServer server = new ChatServer(port);
        server.serve();
    }
}
