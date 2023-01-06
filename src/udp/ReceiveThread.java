package udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class ReceiveThread implements Runnable{
    private MulticastSocket multicastSocket;
    private byte[] receiveData = new byte[512];

    public ReceiveThread(MulticastSocket multicastSocket) {
        this.multicastSocket = multicastSocket;
    }

    @Override
    public void run() {
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while (true) {
            try {
                multicastSocket.receive(receivePacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String sentence = new String(receivePacket.getData()).substring(0, receivePacket.getLength());
            System.out.println(sentence);
        }
    }
}
