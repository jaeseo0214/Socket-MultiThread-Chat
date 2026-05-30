package NewChat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandlerT implements Runnable {

	private final Socket socket;
	private BufferedReader in;

	private PrintWriter out;

	private String clientName;

	public ClientHandlerT(Socket socket) {
		this.socket = socket;
		try {
			this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// 외부에 clientName을 안전하게 꺼내주기 위해
	public String getClientName() {
		return this.clientName;
	}

	// 외부(UserManager)에서 나한테 출력을 요청할 때 씀
	public void sendMessage(String msg) {
		if (out != null) {
			out.println(msg);
		}
	}

	@Override
	public void run() {

		try {
			String msg;

			clientName = in.readLine();

			UserManager.broadcast("CHAT//📢 [" + clientName + "]님이 입장하셨습니다.");

			UserManager.updateUserList();

			while ((msg = in.readLine()) != null) {
				System.out.println(" [" + clientName + "] " + msg);

				UserManager.broadcast("CHAT//[" + clientName + "] " + msg);
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("⚠️ " + clientName + " 사용자와의 연결이 예기치 않게 끊어졌습니다.");
		} finally {
			UserManager.clientsRemove(this);

			if (clientName != null) {
				UserManager.broadcast("CHAT//❌ [" + clientName + "]님이 퇴장하셨습니다.");
			}

			UserManager.updateUserList();

			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
				if (socket != null) {
					socket.close();
				}

				System.out.println("***클라이언트 종료 및 자원 해제***");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}