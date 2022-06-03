package ro.ase.retele;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Client {

    private final static int port = 6000;
    private static InetAddress groupIp;
    private static MulticastSocket mSocket = null;

    private static void startChat(String roomAddress, User user) {
        try {
            groupIp = InetAddress.getByName(roomAddress);
            mSocket = new MulticastSocket(port);
            mSocket.joinGroup(groupIp);
            String joinMessage = user.getName() + " joined";
            byte[] message = joinMessage.getBytes();
            DatagramPacket messageOut = new DatagramPacket(message, message.length, groupIp, port);
            mSocket.send(messageOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        final Scanner inReader = new Scanner(System.in);

        List<String> commands = Arrays.asList(":list_rooms", ":create_room", ":delete_room", ":join_room", ":exit_room", ":exit -> Leave server");
        String menuOption;

        Socket s = null;
        try {
            s = new Socket("localhost", port);
            DataInputStream fromServer = new DataInputStream(s.getInputStream());
            DataOutputStream toServer = new DataOutputStream(s.getOutputStream());

            System.out.println("Welcome!");
            System.out.println();

            System.out.println("Type your name: ");
            System.out.print("> ");
            String name;

            name = inReader.nextLine();
            User user = new User(name);
            toServer.writeUTF(name);

            if (fromServer.readUTF().equals("OK")) {
                do {
                    System.out.println();
                    System.out.println("\n=========== Menu ===========");
                    System.out.println("1 - Commands");
                    System.out.print("> ");
                    menuOption = inReader.nextLine();

                    switch (menuOption) {
                        case "1":
                            System.out.println();
                            commands.forEach(System.out::println);
                            break;

                        case ":list_rooms":
                        case ":create_room":
                            toServer.writeUTF(menuOption);
                            System.out.println(fromServer.readUTF());
                            break;

                        case ":delete_room":
                            toServer.writeUTF(menuOption);
                            String responseDelete, roomAddressDelete;
                            System.out.println(fromServer.readUTF());
                            System.out.print("> ");
                            roomAddressDelete = inReader.nextLine();
                            toServer.writeUTF(roomAddressDelete);
                            System.out.println();
                            responseDelete = fromServer.readUTF();
                            System.out.println(responseDelete);
                            break;

                        case ":join_room":
                            toServer.writeUTF(menuOption);
                            String response, roomAddress;
                            System.out.println(fromServer.readUTF());
                            System.out.print("> ");
                            roomAddress = inReader.nextLine();
                            toServer.writeUTF(roomAddress);
                            System.out.println();
                            response = fromServer.readUTF();
                            System.out.println(response);

                            if (response.startsWith("Can't join to")) {
                                break;
                            }

                            roomAddress = roomAddress.split("/")[1];
                            startChat(roomAddress, user);

                            Thread thread = new Thread(() -> {
                                try {
                                    while (true) {
                                        byte[] buffer = new byte[1000];
                                        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length, groupIp, port);
                                        mSocket.receive(messageIn);
                                        System.out.println(new String(messageIn.getData()).trim());
                                    }
                                } catch (IOException e) {
                                    System.out.println("IO: " + e.getMessage());
                                }
                            });
                            thread.start();

                            boolean chatting = true;

                            while (chatting) {
                                String userInput;
                                userInput = inReader.nextLine();
                                if (userInput.startsWith(":exit_room")) {
                                    String leftMessage = user.getName() + " left";
                                    byte[] message = leftMessage.getBytes();
                                    DatagramPacket messageOut = new DatagramPacket(message, message.length, groupIp, port);
                                    mSocket.send(messageOut);
                                    mSocket.leaveGroup(groupIp);
                                    toServer.writeUTF(userInput);
                                    System.out.println(fromServer.readUTF());
                                    chatting = false;
                                } else {
                                    userInput = user.getName() + ":  " + userInput;
                                    byte[] msg = userInput.getBytes();
                                    DatagramPacket messageOut = new DatagramPacket(msg, msg.length, groupIp, port);
                                    mSocket.send(messageOut);
                                }
                            }
                            break;

                        default:
                            System.out.println("\nCommand not recognized!");
                            System.out.println();
                            System.out.println("List of commands: ");
                            commands.forEach(System.out::println);

                    }
                } while (menuOption.startsWith(":exit"));
            }

            toServer.writeUTF("#$%&disconnect$@#$#@");
            toServer.close();
            fromServer.close();
        } catch (UnknownHostException e) {
            System.out.println("Socket:" + e.getMessage());
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline:" + e.getMessage());
        } finally {
            assert s != null;
            s.close();
        }
    }
}
