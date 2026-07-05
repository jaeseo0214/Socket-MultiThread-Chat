package NewChat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

// 접속 대기 및 스레드 구동 역할
public class ChatServer {

	// 스레드풀 도입
	private static final ExecutorService threadPool = new ThreadPoolExecutor(30, // corePoolSize -> 기본 유지 스레드 개수 (내 PC
																					// 코어 수가 8개)
			100, // maximumPoolSize -> 안정적으로 돌아가는 임계점 100
			60L, TimeUnit.SECONDS, // keepAliveTime -> 안 쓰는 스레드 해제 대기 시간
			new ArrayBlockingQueue<>(100), // 나머지 대기자들이 대기할 큐
			new ThreadPoolExecutor.AbortPolicy() // handler -> 200명 넘으면 깔끔하게 거절
	);

	public static void main(String[] args) {

		try (ServerSocket serversocket = new ServerSocket(5000)) {
			System.out.println("***서버 시작됨***");

			while (true) {

				Socket client = serversocket.accept();
				System.out.println("***클라이언트 접속***");

				ClientHandlerT handler = new ClientHandlerT(client);

				// 삭제 -> 스레드 할당되기도 전에 등록하면 오류 발생 가능
				// 큐에 있을 땐 등록되면 안됨
				// UserManager.clientsAdd(handler);

				// 스레드풀에 작업 던짐
				try {
					threadPool.execute(handler);
					
					// 스레드풀 로그
					ThreadPoolExecutor pool = (ThreadPoolExecutor) threadPool;
					System.out.println("[서버 모니터링] 접속 유저 추가됨 | " + "활성 스레드: " + pool.getActiveCount() + "개 | " + "대기열 큐: "
							+ pool.getQueue().size() + "명");
				} catch (RejectedExecutionException e) {
					// 서버가 가득 찼을 시, 거절 위임
					UserManager.sendRejectedMessage(client);
				}

				/*
				 * // 현재 서버의 실시간 스레드 상태 출력 int activeThreads = Thread.activeCount();
				 * System.out.println("[서버 모니터링] 접속 유저 추가됨 | 현재 총 스레드 수: " + activeThreads +
				 * "개");
				 */
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}