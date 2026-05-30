package ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {

	public static void main(String[] args) {
		try {
			// 서버 접속 시도
			Socket socket = new Socket("localhost", 5000);

			System.out.println("***서버에 연결되었습니다.***");
			
			Scanner scanner = new Scanner(System.in);
			
	        System.out.print("사용할 닉네임을 입력하세요: ");
	        
	        String nickname = scanner.nextLine(); 
	        
	        System.out.println(nickname + "님, 채팅방에 입장하셨습니다.");

			// 수신 전용 스레드
			Thread receiveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

						String serveMsg;

						while ((serveMsg = in.readLine()) != null) {
							// 서버에게 받은 메시지
							System.out.println(">> " + serveMsg);
						}
					} catch (IOException e) {
						System.out.println("서버와 연결이 끊겼습니다.");
					}
				}
			});

			// 수신 스레드 시작
			receiveThread.start();

			// 송신 로직
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			Scanner sc = new Scanner(System.in);

			//System.out.print(">>>");

			while (true) {
				String myMsg = sc.nextLine();
				// 서버로 전송
				out.println("[" + nickname + "] " + myMsg);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
