package NewChat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class DummyClient {
	// 더미 클라이언트 수
	private static final int DUMMY_COUNT = 300;

	public static void main(String[] args) {

		System.out.println(DUMMY_COUNT + "명의 더미 클라이언트 테스트");

		for (int i = 1; i <= DUMMY_COUNT; i++) {

			int clientId = i;

			// 각 클라이언트가 동시에 접속하도록 스레드 사용
			new Thread(() -> {
				try {
					Socket socket = new Socket("localhost", 5000);
					PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

					// * 서버가 보내는 신호 받기 위해서
					BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

					System.out.println("더미 " + clientId + " 서버 연결 완료!");

					// 1. 프로토콜에 맞게 첫 줄에 '닉네임' 전송
					out.println("DummyUser-" + clientId);

					// 2. 간단한 메시지 전송
					out.println("접속 테스트 중입니다.");

					// 3. 바로 끊지 않고 연결을 유지해서 서버 스레드를 'I/O 대기' 상태로 묶어두기
					// 큐에 갇혀 있는 100명은 서버가 바빠서 신호를 안 주므로 여기서 계속 기다리기

					String serverSignal = in.readLine();

					if ("SERVER_START_OK".equals(serverSignal)) {
						System.out.println("더미 " + clientId + " 큐 탈출! 실제 통신 시작.");

						// 4. 서버가 나를 처리하기 시작한 시점부터
						Thread.sleep(10000);
					} else if (serverSignal != null && serverSignal.startsWith("SERVER_FULL")) {
						System.out.println("더미 " + clientId + " 접속 거부: 서버가 만석입니다.");
					}

					socket.close();
				} catch (SocketException e) {
					// 서버가 강제로 연결을 끊었을 때
					System.out.println("더미 " + clientId + " 연결 실패: 서버에 의해 접속이 거부되었습니다.");
				} catch (Exception e) {
					System.out.println("더미 " + clientId + " 연결 실패");
					e.printStackTrace();
				}
			}).start();

			// 서버 연결이 너무 한 번에 몰려서 OS에서 막히는 걸 방지 (10ms 간격)
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
		}

		System.out.println("모든 더미 클라이언트 요청 완료");
	}
}
