import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    private static final int PORT = 5555;
    private static Map<String, ClientHandler> clients = Collections.synchronizedMap(new HashMap<>());

    public static void main(String[] args) {
        System.out.println("Chat server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler client = new ClientHandler(socket);
                Thread thread = new Thread(client);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast public message to all users
    static void broadcast(String message) {
        synchronized (clients) {
            for (ClientHandler c : clients.values()) {
                c.send(message);
            }
        }
    }

    // Send private message
    static void privateMessage(String target, String message) {
        ClientHandler c;
        synchronized (clients) {
            c = clients.get(target);
        }
        if (c != null) {
            c.send("[Private] " + message);
        }
    }

    // Update all clients with the user list
    static void updateUserList() {
        String users = String.join(",", clients.keySet());
        broadcast("USERS " + users);
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private String nickname;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void send(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

                // Ask for nickname first
                out.println("SYS Welcome! Set nickname with /nick <name>");
                String line = in.readLine();
                if (line == null) return;

                if (line.startsWith("/nick ")) {
                    nickname = line.substring(6).trim();
                } else {
                    nickname = line.trim();
                }

                // Ensure unique nickname
                synchronized (clients) {
                    while (clients.containsKey(nickname)) {
                        out.println("SYS Nickname already taken. Enter another:");
                        nickname = in.readLine().trim();
                    }
                    clients.put(nickname, this);
                }

                broadcast("SYS " + nickname + " has joined the chat.");
                updateUserList();

                // Main message loop
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("/nick ")) {
                        // Change nickname
                        String oldNick = nickname;
                        String newNick = line.substring(6).trim();
                        if (newNick.isEmpty() || clients.containsKey(newNick)) {
                            out.println("SYS Invalid or duplicate nickname.");
                        } else {
                            synchronized (clients) {
                                clients.remove(nickname);
                                nickname = newNick;
                                clients.put(nickname, this);
                            }
                            broadcast("SYS " + oldNick + " changed nickname to " + nickname);
                            updateUserList();
                        }
                    } else if (line.startsWith("/w ")) {
                        // Private message
                        String[] parts = line.split(" ", 3);
                        if (parts.length < 3) {
                            out.println("SYS Private message format: /w <nickname> <message>");
                        } else {
                            String target = parts[1];
                            String msg = parts[2];
                            privateMessage(target, nickname + ": " + msg);
                            // Optional: show sender's copy
                            if (!target.equals(nickname)) {
                                out.println("[Private to " + target + "] " + msg);
                            }
                        }
                    } else {
                        // Public message
                        broadcast(nickname + ": " + line);
                    }
                }

            } catch (IOException e) {
                System.out.println("User disconnected: " + nickname);
            } finally {
                try {
                    if (nickname != null) {
                        synchronized (clients) {
                            clients.remove(nickname);
                        }
                        broadcast("SYS " + nickname + " has left the chat.");
                        updateUserList();
                    }
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
