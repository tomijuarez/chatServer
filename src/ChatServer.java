import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomi on 17/06/17.
 */
public class ChatServer {
    private int port;

    private ServerSocket socket;
    private List<ClientThread> clients = new ArrayList<>();


    public ChatServer(int port) {
        this.port = port;
    }

    private void connect() {
        try {
            this.socket = new ServerSocket(this.port);
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
            return;
        }
    }

    public void serve() {
        this.connect();

        Socket clientSocket;
        ClientThread client;

        while(true) {
            try {

                this.socket.accept();
            }
            catch(IOException e) {
                System.out.println(e.getMessage());
            }

        }
    }
}
