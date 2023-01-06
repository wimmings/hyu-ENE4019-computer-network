package tcp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private ServerSocket serverSocket1;
    private ServerSocket serverSocket2;
    public Server(ServerSocket serverSocket1, ServerSocket serverSocket2) {
        this.serverSocket1 = serverSocket1;
        this.serverSocket2 = serverSocket2;
    }

    public void runServer() {
        try {
            while (!serverSocket1.isClosed() && !serverSocket2.isClosed()) {
                // 접속 대기
                Socket socket = serverSocket1.accept();
                System.out.println("New client connected!");

                ClientHandler clientHandler = new ClientHandler(socket, serverSocket2);
                Thread thread = new Thread(clientHandler);
                thread.start();

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//
//    public void closeServerSocket() {
//        try {
//            if (serverSocket1 != null) {
//                serverSocket.close();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void main(String[] args) throws IOException {

        // 서버와 채팅 메세지를 주고 받는 용도, #로 시작하는 명령어 전송
        int portNo1 = Integer.parseInt(args[0]);

        // #PUT #GET 의 동작만을 위해 사용
        int portNo2 = Integer.parseInt(args[1]);

        ServerSocket serverSocket1 = new ServerSocket(portNo1);
        ServerSocket serverSocket2 = new ServerSocket(portNo2);

        Server server = new Server(serverSocket1, serverSocket2);
        server.runServer();

     }
}
