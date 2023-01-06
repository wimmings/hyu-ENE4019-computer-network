package udp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class Non1 {

    public static void main(String[] args) throws IOException {
        serve(1234);
    }

    public static void serve(int port) throws IOException {
        // 1. 채널들을 처리할 셀렉터 열기, 하나의 셀렉터에는 여러 종류 채널 포함.
        Selector selector = Selector.open();

        // 2. 서버의 채널을 열고, 지정된 포트로 바인딩
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        ServerSocket serverSocket = serverChannel.socket();

        InetSocketAddress address = new InetSocketAddress(port);
        serverSocket.bind(address);

        // 3. 셀렉터에, 연결을 수락할 서버의 채널을 등록
        // 채널이 셀렉터에 등록될 때는 해당 채널이 선택될 수 있는 조건(selectionkey)을 파라미터로 지정
        // 해당 조건이 만족될 경우 select()에 의해 활성화 될 수 있음
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // 클라에게 보내줄 메세지를 미리 선언
        final ByteBuffer msg = ByteBuffer.wrap("Hi!\r\n".getBytes());

        for (; ; ) {
            // 4. 처리할 새로운 이벤트가 발생하기를 기다림
            selector.select();

            // 5. 셀렉터에 새롭게 발생한 이벤트들의 키를 얻음
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            
            // 키들을 처리하는 루프 시작
            Iterator<SelectionKey> iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();

                try {
                    // 6. 발생한 이벤트 종류가 수락할 수 있는 새로운 연결인지 확인
                    if (key.isAcceptable()) {
                        // 7. 서버의 채널로부터 클라 채널을 수락
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();

                        // 8. 셀렉터에 새롭게 들어온 클라의 채널을 등록
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ, msg.duplicate());

                        System.out.println("Accepted connection from" + client);

                    }

                    // 9. 발생한 이벤트 종류가 출력할 수 있는 연결인지 확인
                    if (key.isWritable()) {
                        // 10. 클라의 채널로부터 데이터를 출력할 버퍼를 가져옴
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = (ByteBuffer) key.attachment();

                        while (buffer.hasRemaining()) {
                            // 11. 연결된 클라로 메세지 출력
                            if (client.write(buffer) == 0) {
                                break;
                            }
                        }

                        // 12. 연결을 닫음
                        client.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    key.channel();
                    try {key.channel().close();} catch (IOException cex) {}
                    throw new RuntimeException(e);
                }
            }
        }

    }
}
