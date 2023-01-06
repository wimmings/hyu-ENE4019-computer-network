package tcp;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

class Pair<U, V>
{
    public final U first;
    public final V second;

    private Pair(U first, V second)
    {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Pair<?, ?> pair = (Pair<?, ?>) o;

        if (!first.equals(pair.first)) {
            return false;
        }
        return second.equals(pair.second);
    }

    @Override
    public int hashCode()
    {
        return 31 * first.hashCode() + second.hashCode();
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

    public static <U, V> Pair <U, V> of(U a, V b)
    {
        return new Pair<>(a, b);
    }
}
public class ClientHandler implements Runnable{

    public static HashMap<Pair<String, String>, ClientHandler> clientHandlers = new HashMap<>();
    private Socket socket;
    private ServerSocket serverSocketForFile;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String chatRoomName;
    private String clientName;
    private boolean isRoomExists = false;
    private InputStream inputStream;
    private OutputStream outputStream;

    private String fileName;


    public ClientHandler(Socket socket, ServerSocket serverSocketForFile) throws IOException {

        this.socket = socket;
        this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        this.serverSocketForFile = serverSocketForFile;
    }

    @Override
    public void run() {
        String msgFromClient;
        while (!socket.isClosed()) {
            try {
                msgFromClient = bufferedReader.readLine();
                if(msgFromClient == null) {
                    System.out.println("null");
                    closeEverything(socket, bufferedReader, bufferedWriter);
                    break;
                }
                if (msgFromClient.charAt(0) == '#') {
                    String[] msgFromClients = msgFromClient.split("\\s");

                    if (msgFromClients[0].equals("#STATUS")) {
                        String chatRoomInfo = "chatRoomName : "+chatRoomName +", clients [ ";
                        for (Pair<String, String> key : clientHandlers.keySet()) {
                            if (key.first.equals(this.chatRoomName))
                                chatRoomInfo = chatRoomInfo + key.second +" ";
                        }
                        chatRoomInfo += "]";
                        sendPersonalMessage(chatRoomInfo);
                    }
                    else if (msgFromClients[0].equals("#CREATE")) {
                        for (Pair<String, String> key : clientHandlers.keySet()) {
                            // 해쉬맵 돌면서 채팅방이름이 같으면
                            if (key.first.equals(msgFromClients[1])) {
                                isRoomExists = true;
                                break;
                            }
                        }
                        if (isRoomExists) {
                            // 이미 존재
                            sendPersonalMessage("FAIL: chatroom [ " + msgFromClients[1]  + " ] is already exists.");
                            isRoomExists = false;
                        }
                        else {
                            // 새 채팅방 생성
                            this.chatRoomName = msgFromClients[1];
                            this.clientName = msgFromClients[2];

                            clientHandlers.put(Pair.of(msgFromClients[1], msgFromClients[2]), this);

                            // 성공메세지 전송
                            sendPersonalMessage("SUCCESS: chatroom [ "+chatRoomName+" ] is successfully created.");
                        }

                    }
                    else if (msgFromClients[0].equals("#JOIN")) {
                        for (Pair<String, String> key : clientHandlers.keySet()) {
                            // 해쉬맵 돌면서 채팅방이름이 같으면
                            if (key.first.equals(msgFromClients[1])) {
                                isRoomExists = true;
                                break;
                            }
                        }
                        if (isRoomExists) {
                            // 있는 채팅방이여야 성공
                            this.chatRoomName = msgFromClients[1];
                            this.clientName = msgFromClients[2];

                            clientHandlers.put(Pair.of(msgFromClients[1], msgFromClients[2]), this);

                            // 성공메세지 전송
                            sendPersonalMessage("SUCCESS: client [ "+clientName+" ] successfully joins chat room [ "+chatRoomName+" ].");
                            isRoomExists = false;
                        }
                        else {
                            // 없는 채팅방에 join 하면 실패메세지 전송
                            sendPersonalMessage("FAIL: chatroom [ " + msgFromClients[1]  + " ] doesn't exists.");
                        }
                    }
                    else if (msgFromClients[0].equals("#PUT")) {
                        Socket socketForFile = this.serverSocketForFile.accept();
                        fileName = msgFromClients[1];

                        FileOutputStream fileOutputStream = new FileOutputStream(fileName);
                        InputStream fileInputStream = socketForFile.getInputStream();
                        int l;
                        int byteSize = 64000;
                        byte[] data = new byte[byteSize];
                        while ((l = fileInputStream.read(data)) != -1) {
                            fileOutputStream.write(data, 0, l);
                            fileOutputStream.flush();
                        }

                        fileInputStream.close();
                        fileOutputStream.close();
                        socketForFile.close();

                        broadcastMessage("SERVER : [ " + clientName + " ] sends file [ " + fileName +" ]");
                    }
                    else if (msgFromClients[0].equals("#GET")) {
                        Socket socketForFile = this.serverSocketForFile.accept();
                        fileName = msgFromClients[1];

                        FileInputStream fileInputStream = new FileInputStream(fileName);
                        OutputStream fileOutputStream = socketForFile.getOutputStream();

                        int l;
                        int byteSize = 64000;
                        byte[] data = new byte[byteSize];
                        while ((l = fileInputStream.read(data)) > 0) {
                            fileOutputStream.write(data, 0, l);
                            fileOutputStream.flush();
                        }
                        fileInputStream.close();
                        fileOutputStream.close();
                        socketForFile.close();
                    }
                    else if (msgFromClients[0].equals("#EXIT")) {
                        removeClientHandler();
                    }
                } else {
                    // 메세지 전송
                    broadcastMessage(msgFromClient);
                }
            } catch (IOException e) {
                closeEverything(socket, bufferedReader, bufferedWriter);
                break;
            }
        }
    }
    public void sendPersonalMessage(String message) {
        try {
            bufferedWriter.write(message);
            bufferedWriter.newLine();
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public void broadcastMessage(String message) {
        for (Pair<String, String> key : clientHandlers.keySet()) {
            try {
                // 채팅방 이름은 같고, 클라이언트 이름은 다를 때
                if (key.first.equals(chatRoomName) && !key.second.equals(clientName)) {
                    clientHandlers.get(key).bufferedWriter.write(message);
                    clientHandlers.get(key).bufferedWriter.newLine();
                    clientHandlers.get(key).bufferedWriter.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void removeClientHandler() {

        if (this.chatRoomName != null && this.clientName != null && clientHandlers.containsKey(Pair.of(this.chatRoomName, this.clientName))) {
            clientHandlers.remove(Pair.of(this.chatRoomName, this.clientName));
            broadcastMessage("SERVER : " + clientName + " has left the chat");
        }
    }

    public void closeEverything(Socket socket, BufferedReader bufferedReader, BufferedWriter bufferedWriter) {
        removeClientHandler();
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
}
