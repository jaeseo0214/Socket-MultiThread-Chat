package NewChat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserManager {
	private final static List<ClientHandlerT> clients = Collections.synchronizedList(new ArrayList<>());

	public static void updateUserList() {

		synchronized (clients) {

			StringBuilder sb = new StringBuilder("USERLIST//");

			for (int i = 0; i < clients.size(); i++) {

				ClientHandlerT handler = clients.get(i);

				sb.append(handler.getClientName());

				if (i < clients.size() - 1) {
					sb.append(",");
				}
			}

			String userList = sb.toString();

			for (ClientHandlerT hd : clients) {
				hd.sendMessage(userList);
			}
		}
	}

	public static void clientsAdd(ClientHandlerT handler) {
		clients.add(handler);
	}

	public static void clientsRemove(ClientHandlerT handler) {
		clients.remove(handler);
	}

	public static void broadcast(String msg) {

		synchronized (clients) {
			for (ClientHandlerT clientHandler : clients) {
				clientHandler.sendMessage(msg);
			}
		}
	}

	// 서버가 꽉 찼을 때 소켓을 받아 직접 처리
	public static void sendRejectedMessage(Socket client) {
		try {
			// PrintWriter를 직접 생성하여 메시지 전달
			PrintWriter out = new PrintWriter(client.getOutputStream(), true);
			out.println("SERVER_FULL//서버 정원이 초과되었습니다.");

			// 전송 후 소켓 닫기
			client.close();
			System.out.println("* 접속 거부 처리 완료: " + client.getInetAddress());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
