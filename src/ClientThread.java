import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by tomi on 18/06/17.
 */
public class ClientThread extends Thread {
    private String userNick;
    private Socket socket;

    private InputStream reader;
    private OutputStream writter;

    public ClientThread(Socket socket, String userNick) {
        this.socket = socket;
        this.userNick = userNick;
    }

    @Override
    public void run() {
        try {
            this.reader = this.socket.getInputStream();
            this.writter = this.socket.getOutputStream();
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
            return;
        }
    }
}
