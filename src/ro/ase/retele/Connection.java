package ro.ase.retele;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.TreeMap;

class Connection extends Thread {
    DataInputStream fromClient;
    DataOutputStream toClient;
    Socket clientSocket;

    private static TreeMap<String, Room> rooms = new TreeMap<>();
    private static ArrayList<User> users = new ArrayList<>();
    private static IPAddress ipRoom = new IPAddress("224.0.0.0");
    private Room currentRoom = null;

    public Connection(Socket aClientSocket) {
        try {
            clientSocket = aClientSocket;
            fromClient = new DataInputStream(clientSocket.getInputStream());
            toClient = new DataOutputStream(clientSocket.getOutputStream());
            this.start();
        } catch (IOException e) {
            System.out.println("Connection: " + e.getMessage());
        }
    }

    public String createRoom() throws UnknownHostException {
        try {
            Room room = new Room(ipRoom.toString());
            ipRoom = ipRoom.next();
            rooms.put(room.getAddress().toString(), room);
            return (room.toString() + " created!");
        } catch (IOException e) {
            return ("Error: " + e.getMessage());
        }
    }

    public String deleteRoom(String roomAddress) throws UnknownHostException {
        rooms.remove(roomAddress);
        return (roomAddress + " deleted!");
    }

    public String joinRoom(String roomAddress, User user) {
        try {
            this.currentRoom = rooms.get(roomAddress);
            currentRoom.join(user);
            return ("Connected on " + roomAddress + "\nYou are ready to send a message!");
        } catch (NullPointerException e) {
            return ("Can't join to " + roomAddress + " address");
        }
    }

    public String exitRoom(User user) {
        try {
            String address = currentRoom.getAddress().toString();
            currentRoom.exit(user);
            this.currentRoom = null;
            return (user.getName() + " exited of " + address);
        } catch (NullPointerException e) {
            return ("Impossible to exit of this room");
        }
    }

    public String listRooms() {
        try {
            if (rooms.size() != 0) return rooms.toString();
            return ("\nNo rooms created! Type :create_room to create a room.");
        } catch (NullPointerException e) {
            return ("Error: " + e.getMessage());
        }
    }

    public void connectUser(User user) {
        users.add(user);
    }

    public void disconnectUser(User user) {
        users.remove(user);
    }


    @Override
    public void run() {
        try {
            String username = fromClient.readUTF();
            System.out.println("Received: " + username);

            User user = new User(username);
            connectUser(user);
            toClient.writeUTF("OK");

            String command;
            do {
                command = fromClient.readUTF();
                switch (command) {
                    case ":list_rooms":
                        toClient.writeUTF(listRooms());
                        System.out.println("Rooms listed");
                        break;

                    case ":create_room":
                        toClient.writeUTF(createRoom());
                        System.out.println("Room created");
                        break;

                    case ":delete_room":
                        toClient.writeUTF("\nType the room address");
                        toClient.writeUTF(deleteRoom(fromClient.readUTF()));
                        System.out.println("Room deleted");
                        break;


                    case ":join_room":
                        toClient.writeUTF("\nType your room address");
                        toClient.writeUTF(joinRoom(fromClient.readUTF(), user));
                        if (currentRoom != null) {
                            System.out.println(user.getName() + " joined into " + currentRoom.getAddress());
                        }
                        break;

                    case ":exit_room":
                        if (currentRoom != null) toClient.writeUTF(exitRoom(user));
                        else toClient.writeUTF("You are not into a room.");
                        break;

                    case "#$%&disconnect$@#$#@":
                        disconnectUser(user);
                        System.out.println("Disconnected: " + user.getName());
                        toClient.close();
                        fromClient.close();
                        break;
                }

            } while (!command.equals(":exit"));

            clientSocket.close();
        } catch (EOFException e) {
            System.out.println("EOF: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("ReadLine: " + e.getMessage());
        }
    }
}
