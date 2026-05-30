package NewChat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//접속 대기 및 스레드 구동 역할
public class ChatServer {

	public static void main(String[] args) {

		try (ServerSocket serversocket = new ServerSocket(5000)) {
			System.out.println("***서버 시작됨***");

			while (true) {

				Socket client = serversocket.accept();
				System.out.println("***클라이언트 접속***");

				ClientHandlerT handler = new ClientHandlerT(client);

				UserManager.clientsAdd(handler);

				new Thread(handler).start();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}