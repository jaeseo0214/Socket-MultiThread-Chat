package ChatServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/*
	[1]. Thread 클래스를 상속받아, 이 객체 자체가 하나의 독립된 작업 흐름(스레드)으로 동작하도록 설계함.
	
	[2]. 클라이언트 1명당 1개의 ChatHandler 스레드가 할당되어 1:1 전임 마킹을 수행.
*/
public class ChatHandler extends Thread {
	// 소켓 하나 당 핸들러 하나
	// 이 핸들러가 담당하는 클라이언트와의 통신 채널
	Socket socket;
	BufferedReader in;

	/*
	 * [1]. BufferedWriter는 데이터를 보낸 후 반드시 .flush()를 호출해야 상대방에게 전송
	 * 
	 * [2]. PrintWriter(..., true)는 두 번째 인자로 'true(Auto-flush)'를 주어, println()이 호출될
	 * 때마다 자동으로 스트림을 밀어내어 실시간 채팅 전송을 보장
	 */
	PrintWriter out;

	String clientName; // 이 핸들러가 담당하는 클라이언트의 닉네임을 저장할 변수

	public ChatHandler(Socket socket) {
		this.socket = socket;

		try {
			/*
			 * [자바 I/O 표준 -> 데코레이터 패턴]
			 * 
			 * [1]. 네트워크 선로에서 들어오는 가공되지 않은 0과 1로 이루어진 바이트 스트림을 가져옴.
			 * 
			 * [2]. 바이트 데이터를 인코딩을 통해 글자로 바꿔주는 문자 스트림으로 변환.
			 * 
			 * [3]. 메모리에 버퍼를 두어 매번 네트워크를 읽는 게 아닌, 버퍼에 쌓아두고 한 줄 단위로 읽을 수 있게 확장
			 */
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

			out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// handler.start()가 호출되면 메인 스레드로부터 완전히 독립하여 JVM 스케줄러에 의해 이 run() 메서드가 실행.
	@Override
	public void run() {

		try {
			String msg;

			// [프로토콜 약속] 클라이언트 앱에 접속하자마자 가장 첫 줄로 보낸 문자열을 '닉네임'으로 규정하고 변수에 저장
			clientName = in.readLine();

			// 모든 사람에게 이 사람이 들어왔다고 알림
			broadcast("CHAT//📢 [" + clientName + "]님이 입장하셨습니다.");

			ChatServer.updateUserList();

			/*
			 * [클라이언트 입력 대기 무한 루프]
			 * 
			 * [1]. in.readLine() -> 블로킹 IO 메소드
			 * 
			 * [2]. 클라이언트가 엔터를 치기 전까지 자원 소비 X -> 대기 상태
			 * 
			 * [3]. 상대방이 연결을 정상적으로 끊으면 null 리턴, 루프 탈출
			 */
			while ((msg = in.readLine()) != null) {
				// 단순 콘솔 로그
				System.out.println(" [" + clientName + "] " + msg);

				// 서버가 닉네임까지 조립해서 보냄
				broadcast("CHAT//[" + clientName + "] " + msg);
			}
		} catch (IOException e) {
			// 클라이언트가 강제 종료(창 강제 닫기 등)되었을 때 예외 처리
			e.printStackTrace();
			System.out.println("⚠️ " + clientName + " 사용자와의 연결이 예기치 않게 끊어졌습니다.");
		} finally {
			/* 클라이언트가 나갔을 때의 로직 */

			// 1. 전체 유저 목록에서 현재 핸들러 삭제
			ChatServer.clients.remove(this);

			// 2. 다른 사람들에게 퇴장을 알림
			if (clientName != null) {
				broadcast("CHAT//❌ [" + clientName + "]님이 퇴장하셨습니다.");
			}

			ChatServer.updateUserList();

			// 3. 사용했던 소켓, 스트림 닫기(메모리 누수 방지)
			// 객체가 안전하게 생성되어 열린 적이 있던 것만 닫아라.
			try {
				if (in != null)
					in.close();
				if (out != null)
					out.close();
				if (socket != null)
					socket.close();
				System.out.println("***클라이언트 종료 및 자원 해제***");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void broadcast(String msg) {
		// 동기화 블록을 사용하여 여러 스레드가 동시에 리스트를 순회할 때 생기는 에러 방지
		// ConcurrentModificationException -> 순회 중 리스트 구조가 바뀌었다는 에러 방지

		/*
		 * [1]. Collections.synchronizedList로 만든 리스트라 할지라도, 향상된 for 문(Iterator)을 사용해 리스트
		 * 전체를 순회할 때는 내부 락이 자동으로 풀림.
		 * 
		 * [2]. 이를 막기 위해 순회할 때만큼은 전체 유저 목록 객체 자체를 synchronized 블록으로 수동 잠금(Lock)해야 안전
		 */
		synchronized (ChatServer.clients) {
			for (ChatHandler handler : ChatServer.clients) {
				handler.out.println(msg);
			}
		} // 블록을 벗어나는 순간 자동으로 잠금(Lock)이 해제되어 다른 스레드가 리스트를 만질 수 있게 됨
	}
}
