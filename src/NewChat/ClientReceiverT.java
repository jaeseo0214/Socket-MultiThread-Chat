package NewChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientReceiverT implements Runnable {

	private final Socket socket;
	private final ClientGUI gui; // 화면을 조작하기 위해
	private BufferedReader in;

	public ClientReceiverT(Socket socket, ClientGUI gui) {
		this.socket = socket;
		this.gui = gui;
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		String serverMsg;

		try {
			while ((serverMsg = in.readLine()) != null) {

				if (serverMsg.startsWith("USERLIST//")) {
					String payLoad = serverMsg.substring(10);
					gui.updateUserList(payLoad);
				}

				else if (serverMsg.startsWith("CHAT//")) {
					String chatContent = serverMsg.substring(6);
					gui.appendChat(chatContent);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			gui.appendChat("서버와 연결이 끊겼습니다.");
		}
	}
}
