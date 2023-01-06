package tcp;

import java.io.*;
import java.net.Socket;

import static java.lang.System.exit;

public class Client {

    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;

    private String clientCommand;
    private String chatRoomName;
    private String clientName;

    public Client(String ip, int port, String clientCommand, String chatRoomName, String clientName) {
        try {
            this.socket = new Socket(ip, port);

            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            this.clientCommand = clientCommand;
            this.chatRoomName = chatRoomName;
            this.clientName = clientName;

        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }
    public void sendMessage(String message) {
        try {
            if (!socket.isClosed()) {
                bufferedWriter.write("FROM "+ clientName + " : " + message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            }
        } catch (IOException e) {
            closeEverything(socket, bufferedReader, bufferedWriter);
        }
    }
    public boolean sendClientInfo() {
        if (!socket.isClosed()) {
            try {
                // 서버한테 전송
                bufferedWriter.write(clientCommand + " " + chatRoomName + " " + clientName);
                bufferedWriter.newLine();
                bufferedWriter.flush();

                // 서버한테 받음
                String fromServer = bufferedReader.readLine();
                System.out.println(fromServer);
                String[] fromServers = fromServer.split(":");
                if (fromServers[0].equals("SUCCESS")) {
                    return true;
                }
                else return false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
    public void sendCMD(String str) {
        if (!socket.isClosed()) {
            try {
                bufferedWriter.write(str);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void listenForMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msgFromGroupChat;

                while (!socket.isClosed()) {
                    try {
                        // 서버로 부터 온 것 읽어들임.
                        msgFromGroupChat = bufferedReader.readLine();
                        System.out.println(msgFromGroupChat);
                    } catch (IOException e) {
                        closeEverything(socket, bufferedReader, bufferedWriter);
                    }
                }
            }
        }).start();
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        try {
            if (socket != null) {
                socket.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        String serverIPaddress = args[0];
        int portNo1 = Integer.parseInt(args[1]);
        int portNo2 = Integer.parseInt(args[2]);

        Socket socketForFile;

        BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));

        Client client = null;
        String[] cmd;
        while (true) {
            // 채팅방 입장
            while (true) {
                // 입력 받기
                String sentence = inFromUser.readLine();
                // 다양한 명령어들
                if (sentence.charAt(0) == '#') {
                    cmd = sentence.split("\\s");

                    if (cmd[0].equals("#CREATE") || cmd[0].equals("#JOIN")) {
                        // 이름 겹치는 거 있으면 안 됨
                        client = new Client(serverIPaddress, portNo1, cmd[0], cmd[1], cmd[2]);
                        if (client.sendClientInfo()) break;
                        else client.closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
                    }
                }
            }
            // 서버로 부터 내용 받는 스레드 실행
            client.listenForMessage();


            // 파일 전송 명령어
            while (true) {
                String str = inFromUser.readLine();

                if (str.charAt(0) == '#') {
                    String[] strs = str.split("\\s");

                    if (strs[0].equals("#STATUS")) {
                        client.sendCMD(str);
                    } else if (strs[0].equals("#PUT")) {
                        client.sendCMD(str);
                        //#PUT (FileName) 파일의 송신자는 #PUT 명령어를 이용해서 Server로 파일을 전송
                        String fileName = System.getProperty("user.dir") + "/tcp/" + strs[1];

                        socketForFile = new Socket(serverIPaddress, portNo2);
                        FileInputStream fileInputStream = new FileInputStream(fileName);
                        OutputStream fileOutputStream = socketForFile.getOutputStream();

                        int l;
                        byte[] buffer= new byte[64000];

                        while ((l = fileInputStream.read(buffer)) != -1) {
                            fileOutputStream.write(buffer, 0, l);
                            fileOutputStream.flush();
                            System.out.printf("#");
                        }
                        fileInputStream.close();
                        socketForFile.close();
                        fileOutputStream.close();
                        System.out.println("file send success");

                    } else if (strs[0].equals("#GET")) {
                        client.sendCMD(str);
                        //#GET (FileName)파일의 수신자는 Server로부터 전달 받은 파일 이름을 통해
                        //#GET 명령어를 수행하고 Server 로부터 파일을 다운로드
                        String fileName = System.getProperty("user.dir") + "/tcp/new" + strs[1];

                        socketForFile = new Socket(serverIPaddress, portNo2);
                        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                        InputStream fileInputStream = socketForFile.getInputStream();

                        int l;
                        byte[] buffer= new byte[64000];

                        while ((l = fileInputStream.read(buffer)) > 0) {
                            fileOutputStream.write(buffer, 0, l);
                            System.out.printf("#");
                        }

                        fileInputStream.close();
                        socketForFile.close();
                        fileOutputStream.close();
                        System.out.println("file receive success");

                    } else if (strs[0].equals("#EXIT")) {
                        client.sendCMD(str);
                        client.closeEverything(client.socket, client.bufferedReader, client.bufferedWriter);
                        break;
                    } else {
                        exit(1);
                    }
                } else {
                    // 메세지 전송
                    client.sendMessage(str);
                }
            }

        }
    }
}
