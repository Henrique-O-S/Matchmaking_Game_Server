# Distributed Systems Assignment

## Instructions for Running the Program

---

This guide provides step-by-step instructions on how to compile and run this Java program consisting of 5 files: Server.java, Client.java, User.java, Database.java and Game.java. The program allows running a game on the console, with one console running the Server and other consoles running the Client.

---

### Prerequisites

---

Before proceeding, make sure you have the following installed on your system:

Java Development Kit (JDK) - Version 8 or higher
Command-line interface (e.g., Terminal, Command Prompt)

---

### Compilation

---

Follow these steps to compile the Java program:

1. Open a command-line interface.

2. Navigate to the directory (src folder) containing the Java source files (Server.java, Client.java, User.java, Database.java and Game.java).

3. Compile all the Java files at once using the javac command:

```
javac Server.java Client.java User.java Database.java Game.java
```

- This command compiles all the Java files and generates corresponding bytecode files (.class files) in the same directory.

---

### Running the Server

---

To run the Server program, follow these steps:

1. In the command-line interface, ensure you are still in the same directory where the Java files are located.

2. Start the server by running the following command:

```
java Server
```

- This command launches the Server program, and the server starts listening for incoming connections on port 5002.

- Note: Keep the server running throughout the duration of the game.

---

### Running the Clients

---

To run the Client program and connect to the server, perform the following steps for each client:

1. Open a new command-line interface or a new console window.

2. Navigate to the directory containing the Java source files (same as above).

3. Start a client by running the following command:

```
java Client
```

- This command launches the Client program, and the client attempts to establish a connection with the server.

- Note: You can run multiple client instances in separate consoles to simulate multiple players.

4. Repeat steps 1-3 for each client you want to connect to the server.

---

### User Registration and Authentication

---

The client logic follows these processes:

1. The client connects to the server.

2. The client has the option to register (as a new user), login (as an existing user) or quit.

3. After the authentication, the user is added to a wait queue in order to find an available game.

4. The server groups users with similar global scores (rank based matchmaking) to start a new game and the user confirms if he wishes to join the game.

5. After the game, the user is added again to the wait queue to possibly find the next game.

6. The user can quit at any given moment by typing CONTROL+C.

---

### Gameplay

---

Once the server and clients are running and connected, the gameplay can commence. The specific gameplay mechanics are determined by the logic implemented in the Game class.

- Each game has 3 players.

- The game has 5 rounds.

- On each round, each player rolls a random number between 1 and 12.

- The player(s) with the higher result wins the round.

- Finally, the player(s) with the most rounds won wins the game and his global score increases by 100 points.

- The players who lose have their global score decreased by 20 points.

---

Project made by [t06-g10]:
- Diogo Silva, up202004288
- Henrique Silva, up202007242
- Tiago Branquinho, up202005567

---