package udp;

import java.net.*;
import java.io.*;

import static udp.HashTest.getSHA256;


public class UDPp2pChatRoom {

    public static void main(String[] args) throws IOException {

        // portNo 받아서 multisocket 만들기
        int portNo = Integer.parseInt(args[0]);
        MulticastSocket multicastSocket = new MulticastSocket(portNo);


        Thread receiveThread = new Thread(new ReceiveThread(multicastSocket));
        receiveThread.start();

        Thread sendThread = new Thread(new SendThread(multicastSocket, portNo));
        sendThread.start();

    }
}
