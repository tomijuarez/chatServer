import com.sun.security.ntlm.Client;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

/**
 * Created by tomi on 17/06/17.
 */
public class ChatServer {
    private int port;

    private ServerSocket socket;
    private List<ClientThread> clients = new ArrayList<>();
    private Map<String, List<String>> channels = new HashMap<>(); //Channels as key, users as value.;


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

        try {
            this.socket.setReuseAddress(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public synchronized void addChannel(String channel) {
        this.channels.put(channel, new ArrayList<>());
    }

    public synchronized void toggleUserFromChannel(String channel, String user) {
        if(!this.channels.containsKey(channel))
            return;

        List<String> users = this.channels.get(channel);

        if(users.contains(user))
            users.remove(user);
        else
            users.add(user);
    }

    public synchronized List<String> getChannelsName() {
        return new ArrayList<>(this.channels.keySet());
    }

    public synchronized List<String> getUsersFromChannel(String channel) {
        return (this.channels.containsKey(channel))
                ? this.channels.get(channel)
                : new ArrayList<>();
    }

    public synchronized void logoutUser(String userName) {
        List<String> users;
        for(String channel : this.channels.keySet()) {
            users = this.channels.get(channel);
            if (users.contains(userName))
                users.remove(userName);
        }

        ClientThread client;
        for (Iterator<ClientThread> iterator = this.clients.iterator(); iterator.hasNext(); ) {
            client = iterator.next();
            if (userName.equals(client.getUserName()))
                iterator.remove();
        }

    }

    public void serve() {
        this.connect();

        Socket clientSocket;
        ClientThread client;

        while(true) {
            try {
                clientSocket = this.socket.accept();
                client = new ClientThread(this, clientSocket);
                this.clients.add(client);
                client.setClients(this.clients);
                client.start();
            }
            catch(IOException e) {
                System.out.println(e.getMessage());
            }

        }
    }
}
