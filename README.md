# Java Chat Application

A **real-time peer-to-peer chat system** built using **Java**, **Socket Programming**, and **JavaFX**.  
Supports public and private messaging, nicknames, and a simple text-based GUI.

---

## Features

- Real-time chat between multiple clients
- **Public messages** broadcasted to all connected users
- **Private messaging** using `/w <nickname> <message>`
- **Nickname management** using `/nick <newname>`
- Simple **JavaFX GUI**:
  - Chat area
  - Input field
  - Send button
  - Online users list
- Connection logs and user join/leave notifications

---

## Prerequisites

- **Java 21** installed  
- **JavaFX SDK 21** installed (or downloaded separately)
  - [Download JavaFX](https://gluonhq.com/products/javafx/)
- Set `--module-path` to your JavaFX `lib` folder when running the client

---

## How to Run

 1. Start the Server

```bash

javac ChatServer.java
java ChatServer


 2. Start the Client

# Compile
javac --module-path /path/to/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.fxml ChatClient.java

# Run
java --module-path /path/to/javafx-sdk-21.0.2/lib --add-modules javafx.controls,javafx.fxml ChatClient


Example Chat
--------------
Your nickname: Megha
[Server] Megha has joined the chat.
Megha: Hello everyone!
[Private to Akash] Hi Akash!
[Server] Akash has joined the chat.
Akash: Hi Megha!


Project Structure
-----------------

Java_Chat_Application/
│
├─ ChatServer.java       # Server-side code
├─ ChatClient.java       # Client-side JavaFX GUI
├─ README.md            # Project documentation
└─ .gitignore           # Recommended to exclude compiled files and IDE configs

***Notes***
-Make sure each client connects to the correct server    host (localhost by default) and port (5555).

-The project uses UTF-8 encoding for messages.

-Encryption is optional and can be implemented later.



