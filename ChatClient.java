import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class ChatClient extends Application {

    private static String host = "localhost";
    private static int port = 5555;

    private TextArea chatArea;
    private TextField inputField;
    private Button sendButton;
    private ListView<String> usersListView;
    private ObservableList<String> users = FXCollections.observableArrayList();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nickname = "Guest";

    public static void main(String[] args) {
        if (args.length >= 1) host = args[0];
        if (args.length >= 2) port = Integer.parseInt(args[1]);
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        stage.setTitle("JavaFX Chat Client");

        // Chat area
        chatArea = new TextArea();
        chatArea.setEditable(false);
        chatArea.setWrapText(true);

        // Input field
        inputField = new TextField();
        inputField.setPromptText("Type a message… (/nick <name>, /w <user> <msg>)");
        inputField.setTooltip(new Tooltip(
                "Commands:\n" +
                "/nick <newname>  - Change your nickname\n" +
                "/w <user> <msg>  - Send private message"
        ));
        inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) sendMessage();
        });

        // Send button
        sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        // Users list
        usersListView = new ListView<>(users);
        usersListView.setPrefWidth(150);

        // Input bar
        HBox inputBar = new HBox(8, inputField, sendButton);
        inputBar.setPadding(new Insets(8));
        HBox.setHgrow(inputField, Priority.ALWAYS);

        // Root layout
        BorderPane root = new BorderPane();
        root.setCenter(chatArea);
        root.setBottom(inputBar);
        root.setRight(usersListView);
        BorderPane.setMargin(usersListView, new Insets(8));

        stage.setScene(new Scene(root, 700, 400));
        stage.show();

        // Connect to server
        new Thread(this::connect).start();
    }

    private void connect() {
        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);

            // Nickname popup
            Platform.runLater(() -> {
                TextInputDialog dialog = new TextInputDialog("Guest");
                dialog.setTitle("Nickname");
                dialog.setHeaderText("Enter your nickname:");
                dialog.setContentText("Nickname:");
                dialog.showAndWait().ifPresent(name -> {
                    nickname = name.trim();
                    out.println("/nick " + nickname);
                });
            });

            // Reader thread
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        handleIncoming(line);
                    }
                } catch (IOException e) {
                    Platform.runLater(() -> chatArea.appendText("[Disconnected]\n"));
                }
            });
            reader.setDaemon(true);
            reader.start();

        } catch (IOException e) {
            Platform.runLater(() -> chatArea.appendText("Connection failed: " + e.getMessage() + "\n"));
        }
    }

    private void handleIncoming(String line) {
        // Server sends: "USERS user1,user2,user3"
        if (line.startsWith("USERS ")) {
            String payload = line.substring(6).trim();
            List<String> list = payload.isEmpty() ? List.of() : Arrays.asList(payload.split(","));
            Platform.runLater(() -> users.setAll(list));
        } 
        // Private message marker from server: "DM sender: message"
        else if (line.startsWith("DM ")) {
            String msg = line.substring(3);
            Platform.runLater(() -> chatArea.appendText("[Private] " + msg + "\n"));
        }
        // System messages: "SYS ..."
        else if (line.startsWith("SYS ")) {
            String msg = line.substring(4);
            Platform.runLater(() -> chatArea.appendText("[Server] " + msg + "\n"));
        }
        // Public messages
        else {
            Platform.runLater(() -> chatArea.appendText(line + "\n"));
        }
    }

    private void sendMessage() {
        String text = inputField.getText();
        if (text == null || text.isBlank()) return;

        inputField.clear();

        // Private message validation
        if (text.startsWith("/w ")) {
            String[] parts = text.split(" ", 3);
            if (parts.length < 3 || parts[1].isBlank() || parts[2].isBlank()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Private Message");
                    alert.setHeaderText(null);
                    alert.setContentText("Private message format:\n/w <nickname> <message>");
                    alert.showAndWait();
                });
                return;
            }
        }

        // Nickname change command validation
        if (text.startsWith("/nick ")) {
            String[] parts = text.split(" ", 2);
            if (parts.length < 2 || parts[1].isBlank()) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Invalid Nickname");
                    alert.setHeaderText(null);
                    alert.setContentText("Nickname format:\n/nick <newname>");
                    alert.showAndWait();
                });
                return;
            }
            nickname = parts[1].trim();
        }

        out.println(text);
    }
}
