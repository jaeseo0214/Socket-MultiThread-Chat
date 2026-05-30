package ChatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.*;

public class ChatServer {

	// static List<ChatHandler> clients = new ArrayList<>();
	/*
	 * [1]. 일반 ArrayList는 동기화 처리가 되어있지 않아서 여러 스레드가 동시에 접근(add, remove)하면 데이터가 깨지거나
	 * ConcurrentModificationException 에러가 발생함.(Thread-Unsafe)
	 * 
	 * [2]. Collections.synchronizedList는 내부적으로 모든 메서드에 모니터 락(Monitor Lock)을 걸어 한 번에
	 * 오직 하나의 스레드만 리스트에 접근하도록 강제함.(Thread-Safe 보장)
	 * 
	 * [3]. 이를 통해 여러 클라이언트(ChatHandler 스레드)가 동시에 들어오거나 나갈 때, 명단 데이터의 원자성과 상호 배제를
	 * 유지함.
	 * 
	 */
	static List<ChatHandler> clients = Collections.synchronizedList(new ArrayList<>());

	public static void main(String[] args) {

		try {
			// [네트워크 표준 - 포트 바인딩]
			// OS로부터 5000번 포트를 할당받아 연결 요청 대기 상태로 들어감
			ServerSocket socket = new ServerSocket(5000);

			System.out.println("***서버 시작됨***");

			// [서버의 무한 루프 구조]
			// 사용자가 눈으로 보는 화면 뒤에서 돌아가는 백그라운드 프로그램
			while (true) {

				/*
				 * [블로킹 입출력 매커니즘]
				 * 
				 * [1]. socket.accept()는 클라이언트의 연결 신호가 올 때까지 무한정 대기.
				 * 
				 * [2]. 이 때, 블로킹 상태가 되어서 CPU자원을 소모하지 않음. -> 휴식(Sleep)상태 -> while 이어도 CPU점유율이
				 * 폭증하지 않는 이유
				 * 
				 * [3]. 클라이언트가 접속하는 순간 락이 풀리고, 통신용 일반 소켓 객체 리턴.
				 */
				Socket client = socket.accept();

				System.out.println("***클라이언트 접속***");
				/*
				 * [1]. 메인스레드는 단순히 문지기(accept)역할. 실제 대화를 나누는 로직은 클라이언트마다 독립된 전용
				 * 스레드(ChatHandler)를 생성해 위임.
				 */
				ChatHandler handler = new ChatHandler(client); // 연결 담당자

				// 동기화 리스트이므로 이 시점에 다른 핸들러 스레드가 리스트를 만지면 대기하게 됨
				clients.add(handler); // 전체 클라이언트 목록에 추가

				/*
				 * [1]. start 호출 시, JVM 내부의 스케쥴러에 등록 -> 메인 스레드와는 별개로 독립적인 실행 흐름(Runnable ->
				 * Running)을 가짐.
				 */
				handler.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void updateUserList() {
		/*
		 * [1]. clients 는 이미 이 클래스 내부에 전역변수로 선언되어 있음
		 * 
		 * [2]. 메소드 전체를 감싸는 것보다 리스트 하나를 감싸는 게 서버 성능적으로 이득 -> 병목현상을 최대한 줄이기 위해서.
		 * 
		 */
		synchronized (clients) {

			// 프로토콜 규격인 헤더를 먼저 달고 시작
			StringBuilder sb = new StringBuilder("USERLIST//");

			// 클라이언트의 이름들을 추가
			for (int i = 0; i < clients.size(); i++) {

				ChatHandler handler = clients.get(i);

				sb.append(handler.clientName);

				// 맨 마지막 클라이언트가 아니라면 콤마(,)를 붙여 구분자로 활용
				if (i < clients.size() - 1) {
					sb.append(",");
				}
			}

			String userList = sb.toString();

			// 조립이 완료된 클라이언트 리스트를 모든 클라이언트에게 전송
			for (ChatHandler hd : clients) {
				hd.out.println(userList);
			}
		}
	}
}
