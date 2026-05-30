package NewChat;

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
}
