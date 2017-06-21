import com.sun.security.ntlm.Client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by tomi on 18/06/17.
 */
public class ClientThread extends Thread {
    private String userNick;
    private Socket socket;
    private List<ClientThread> clients;
    private ChatServer server;

    private DataInputStream reader;
    private OutputStream writter;

    //Strings para el protocolo de comunicación con el servidor.
    private static final String MESSAGE = "MSG";
    private static final String NEW_CHANNEL = "NC";
    private static final String TOGGLE_CHANNEL = "TC";
    private static final String TOGGLE_USER = "TU";
    private static final String SUBSCRIBE_CHANNEL = "SC";
    private static final String UNSUBSCRIBE_CHANNEL = "UC";
    private static final String REGISTER_USER = "RU";
    private static final String LOGOUT = "LO";
    private static final String SEPARATOR = "-";
    private static final String LOAD_CHANNELS = "LC";
    private static final String LOAD_USERS = "LU";
    private static final String SEP_ITEMS = ";";
    private static final String CLEAR = "CL";
    private static final String NEW_LINE = "\r\n";

    private String currentChannel = "";
    private String currentUser = "";

    public ClientThread(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    public void setClients(List<ClientThread> clients) {
        this.clients = clients;
    }

    public void sendResponse(String msg) {
        try {
            System.out.println("ENVIANDO: "+msg);
            this.writter.write((msg+ClientThread.NEW_LINE).getBytes());
        } catch (IOException e) {
            return;
        }
    }

    private void broadcast(String msg) {

        for (ClientThread connection: this.clients) {
            System.out.println("USUARIO CONECTADO: "+connection.getUserName());
            connection.sendResponse(msg);
        }
    }

    private void toggleChannel(String channel, String user) {
        String response;
        if(this.currentChannel.equals(channel)) {
            this.currentChannel = "";
            response = ClientThread.UNSUBSCRIBE_CHANNEL;
        }
        else {
            this.currentChannel = channel;
            response = ClientThread.SUBSCRIBE_CHANNEL;
        }

        response += ClientThread.SEPARATOR + channel + ClientThread.SEPARATOR + user;

        this.broadcast(response);
    }

    public String getUserName() {
        return this.userNick;
    }

    private void toggleUser(String channel, String user) {

        if(this.currentChannel.equals(channel))
            return;

        this.currentUser = user;
        this.currentChannel = channel;

        String response = ClientThread.CLEAR + ClientThread.SEPARATOR + user;

        this.broadcast(response);
    }

    public String getCurrentChannel() {
        return this.currentChannel;
    }

    public String getCurrentUser() {
        return this.currentUser;
    }

    //Para que los mensajes lleguen en orden consistente.
    public synchronized void sendMsgInChannel(String channel, String user, String message) {
        for(ClientThread client : this.clients) {
            if(client.getCurrentChannel().equals(channel))
                client.sendResponse(ClientThread.MESSAGE + ClientThread.SEPARATOR + channel + ClientThread.SEPARATOR + user + ClientThread.SEPARATOR + message);
        }
    }

    public synchronized void sendPrivateMessage(String channel, String from, String to, String message) {
        for(ClientThread client : this.clients) {
            if(client.getCurrentChannel().equals(channel) && client.getCurrentUser().equals(from))
                client.sendResponse(ClientThread.MESSAGE + ClientThread.SEPARATOR + channel + ClientThread.SEPARATOR + from + ClientThread.SEPARATOR + to + ClientThread.SEPARATOR + message);
            break;
        }
    }

    private boolean routeMsg(String request) {
        String[] chunks = request.split(ClientThread.SEPARATOR);

        if (chunks.length >= 2) {
            String command = chunks[0];
            String msg = chunks[1];

            switch (command) {
                case ClientThread.NEW_CHANNEL:
                    this.server.addChannel(msg);
                    this.broadcast(request);
                    break;
                case ClientThread.TOGGLE_CHANNEL:
                    if(chunks.length < 3)
                        return true;

                    this.server.toggleUserFromChannel(msg, chunks[2]);
                    this.toggleChannel(msg, chunks[2]);
                    break;
                case ClientThread.TOGGLE_USER:
                    if(chunks.length < 3)
                        return true;

                    this.server.toggleUserFromChannel(msg, chunks[2]);
                    this.toggleUser(msg, chunks[2]);
                    break;
                case ClientThread.MESSAGE:
                    if(chunks.length == 3) {
                        System.out.println("MENSAJE DIRECTO");
                        this.sendMsgInChannel(msg, chunks[1], chunks[2]);
                    }
                    else if(chunks.length == 4) {
                        System.out.println("MENSAJE GLOBAL");
                        this.sendPrivateMessage(msg, chunks[1], chunks[2], chunks[4]);
                    }
                    break;
                case ClientThread.REGISTER_USER:
                    System.out.println("SE REGISTRA EL USUARIO: "+msg);
                    this.userNick = msg;
                    break;
                case ClientThread.LOGOUT:
                    System.out.println("SE VA EL USUARIO: "+msg);
                    this.broadcast(request);
                    this.server.logoutUser(msg);
                    return false; //Para cerrar todo.
            }
        }
        return true;
    }

    private void sendChannels() {
        String names = ClientThread.LOAD_CHANNELS+ ClientThread.SEPARATOR;
        for (String channel:this.server.getChannelsName()) {
            names += channel + ClientThread.SEP_ITEMS;
        }
        names = names.substring(0, names.length()-1); //Last ;
        this.sendResponse(names);
    }

    private void sendUsers() {
        String names;
        for (String channel:this.server.getChannelsName()) {
            names = ClientThread.LOAD_USERS;
            names += ClientThread.SEPARATOR + channel + ClientThread.SEPARATOR;

            for(String user : this.server.getUsersFromChannel(channel)) {
                names += user + ClientThread.SEP_ITEMS;
            }

            names = names.substring(0,names.length()-1);

            this.sendResponse(names);
        }
    }

    private void disconnect() {
        try {
            this.reader.close();
            this.writter.close();
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            this.reader = new DataInputStream(this.socket.getInputStream());
            this.writter = this.socket.getOutputStream();

            this.sendChannels();
            this.sendUsers();

            boolean finish = false;

            while(!finish) {
                try {
                    String request;
                    while ((request = this.reader.readLine()) != null) {
                        if (!this.routeMsg(request)) {
                            finish = true;
                            break;
                        }
                    }
                }
                catch (IOException e) {
                    System.out.println(e.getMessage());
                }
            }
            this.disconnect();
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
            return;
        }
    }
}
