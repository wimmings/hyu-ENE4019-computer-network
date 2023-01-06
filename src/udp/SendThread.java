package udp;

import java.net.*;
import java.io.*;

import static java.lang.System.exit;
import static udp.HashTest.getSHA256;
public class SendThread implements Runnable {
    private MulticastSocket multicastSocket;
    private InetAddress IPAddress = null;
    private int portNo;
    private String roomName, peerName;
    private boolean isJoin = false;
    private DatagramPacket packet;
    private byte[] sendData;

    public SendThread(MulticastSocket multicastSocket, int portNo) {
        this.multicastSocket = multicastSocket;
        this.portNo = portNo;
    }

    @Override
    public void run() {
        // #JOIN
        while (true) {
            try {
                // join 상태 아닐 때만
                if (!isJoin) {
                    BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

                    String sentence = null;
                    try {
                        sentence = inFromUser.readLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    String[] cmd = sentence.split(" ");

                    if(sentence.equals("#QUIT")) {
                        exit(0);
                    }
                    // #JOIN으로 시작하도록

                    try {
                        while (!cmd[0].equals("#JOIN")||cmd[2]==null) {
                            System.out.println("[ERROR] Wrong command : Cannot start chatting room");
                            sentence = inFromUser.readLine();
                            cmd = sentence.split(" ");
                        }
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println("[ERROR] Too few arguments : Cannot start chatting room");
                        try {
                            sentence = inFromUser.readLine();
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        cmd = sentence.split(" ");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }

                    roomName = cmd[1];
                    peerName = cmd[2];

                    byte[] digest = getSHA256(roomName);
                    String IP = getIPAddress(digest);

                    IPAddress = InetAddress.getByName(IP);
                    multicastSocket.joinGroup(IPAddress);

                    sentence = "\"" + peerName + "\"" + " enters the [ " + roomName + " ] chatting room";
                    sendData = sentence.getBytes();

                    packet = new DatagramPacket(sendData, sendData.length, IPAddress, portNo);
                    multicastSocket.send(packet);

                    isJoin = true;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // 사용자 입력값 받기 (#EXIT or #QUIT or sendData)
            BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
            String sentence = null;
            try {
                sentence = inFromUser.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // #으로 시작할 때 - #EXIT or #QUIT
            if (sentence.charAt(0) == '#') {
                if (sentence.equals("#EXIT")) {
                    sentence = "\"" + peerName + "\"" + " leaves the [ " + roomName + " ] chatting room";
                    sendData = sentence.getBytes();

                    packet = new DatagramPacket(sendData, sendData.length, IPAddress, portNo);

                    try {
                        multicastSocket.send(packet);
                        multicastSocket.leaveGroup(IPAddress);

                        isJoin = false;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                else {
                    System.out.println("[ERROR] only #EXIT possible");
                }
            }
            // 채팅 보낼 때 - sendData
            else {
                sentence = "Peer " + peerName + " : " + sentence;
                sendData = sentence.getBytes();
                // data 크기 512보다 작거나 같을 때
                if (sendData.length <= 512) {
                    packet = new DatagramPacket(sendData, sendData.length, IPAddress, portNo);

                    try {
                        multicastSocket.send(packet);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                // data 크기 512보다 클 때
                else {
                    int offset = 0;
                    int bytesSent = 0;
                    packet = new DatagramPacket(sendData, offset, 512, IPAddress, portNo);

                    while (bytesSent < sendData.length) {
                        try {
                            multicastSocket.send(packet);

                            bytesSent += packet.getLength();
                            int bytesToSend = sendData.length - bytesSent;
                            int size = (bytesToSend > 512) ? 512 : bytesToSend;

                            packet.setData(sendData, bytesSent, size);

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

    }
    public static String getIPAddress(byte[] bytes) {
        StringBuffer sbuffer = new StringBuffer();
        sbuffer.append("225.");

        int[] positive = new int[bytes.length];

        for (int i = 0; i < bytes.length; ++i) {
            positive[i] = bytes[i] & 0xff;
        }
        String x = Integer.toString(positive[29]);
        String y = Integer.toString(positive[30]);
        String z = Integer.toString(positive[31]);

        sbuffer.append(x + ".");
        sbuffer.append(y + ".");
        sbuffer.append(z);

        return sbuffer.toString();
    }

}
