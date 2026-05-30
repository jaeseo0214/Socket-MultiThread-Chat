package ChatServer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class ChatClientGUI extends JFrame {

	private JTextField txtInput; // 메시지 입력창
	private JTextArea txtDisplay; // 채팅 내용 출력창
	private JTextArea txtUserList; // 오른쪽 접속자 명단 출력창
	private PrintWriter out;
	private String nickname;
	private JLabel lblName; // 이름 표시할 레이블
	private JScrollPane scrollPane; // 수신 스레드에서 접근할 수 있도록 멤버 변수로 승격

	/*
	 * [1]. 생성자 실행 시 내부에서 선언된 지역변수(이름표)들은 스택 영역에 생성.
	 * 
	 * [2]. new 키워드로 생성된 객체들은 힙 영역에 독립적으로 할당.
	 * 
	 * [3]. 생성자 종료 시, 지역변수(이름표)들은 사라짐.
	 * 
	 * [4]. 힙에 생성된 객체들은 최상위 컨테이너인 JFrame에 add 되어 '강한 참조' 관계를 유지하고 있기 때문에, 가비지 컬렉터에
	 * 의해 파기되지 않음.
	 * 
	 * [5]. 이벤트 리스너 또한 컴포넌트(txtInput)가 힙 메모리에 살아있는 한 기능 계속해서 작동.
	 */
	public ChatClientGUI() {
		// 1. GUI 화면 구성하기
		setTitle("채팅 프로그램");
		setSize(550, 500);
		// 창 닫기 버튼을 누르면 프로세스 자체를 완전히 종료하라는 JVM명령
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// [중앙] -> 대화창
		txtDisplay = new JTextArea();
		txtDisplay.setEditable(false); // 대화창은 수정 불가 설정
		// JTextArea를 스크롤이 가능한 컴포넌트로 감싸기
		// scrollPane은 클래스의 멤버 변수(필드)이므로 생성자가 끝나도 이름표와 객체 모두 영구 생존 (수신 스레드 접근용)
		scrollPane = new JScrollPane(txtDisplay);
		add(scrollPane, BorderLayout.CENTER);

		// [오른쪽] -> 접속자 명단
		txtUserList = new JTextArea();
		txtUserList.setEditable(false);
		// 초기 뼈대 확인용 가상 데이터 (나중에 서버 연동으로 지우기)
		txtUserList.setText("👥 접속자 목록\n-----------\n- (나)\n");
		// userScrollPane은 '지역 변수' -> 생성자 종료 시 스택에서 사라짐
		JScrollPane userScrollPane = new JScrollPane(txtUserList);
		// 명단 창이 가로로 너무 넓어지지 않게 가로 크기를 130픽셀로 제한
		userScrollPane.setPreferredSize(new Dimension(130, 0));
		// 지역 변수명(userScrollPane)은 곧 사라지지만 add 에 의해 JFrame에 고정됨
		add(userScrollPane, BorderLayout.EAST); // 화면 오른쪽에 배치

		// [아래] -> 입력 영역
		JPanel southPanel = new JPanel(new BorderLayout());
		lblName = new JLabel(" [미정] : "); // 기본값
		txtInput = new JTextField();

		southPanel.add(lblName, BorderLayout.WEST); // 왼쪽에 이름 표시
		southPanel.add(txtInput, BorderLayout.CENTER); // 가운데에 입력창
		add(southPanel, BorderLayout.SOUTH);

		// 엔터 키를 누르면 서버로 메시지 전송하는 이벤트
		/*
		 * [1]. new ActionListener()를 통해 힙에 이벤트 핸들러 객체가 독립적으로 생성.
		 * 
		 * [2]. addActionListener() 메소드를 통해 이 객체를 txtInput 컴포넌트 내부의 '리스너 리스트'에 등록(설치).
		 * 
		 * [3]. 생성자가 끝나더라도 OS가 감지한 키 입력 신호를 자바의 GUI 전용 스레드인 Event Dispatch Thread(EDT)가
		 * 전달받아, 힙에 살아있는 익명 객체의 actionPerformed() 메서드를 콜백 호출
		 * 
		 * 
		 * ??? - 익명 객체, 이름이 없는 객체인데 EDT가 어떻게 힙에서 찾아 호출 -???
		 * 
		 * -> 객체 자체는 이름이 없지만, 생성 직후 addActionListener()를 통해 txtInput 컴포넌트 내부의 **리스너
		 * 리스트(배열/컬렉션)**에 그 객체의 메모리 주소(참조값)가 등록되었기 때문. EDT는 txtInput에 저장된 그 주소를 보고 찾아가
		 * 호출.
		 */
		txtInput.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String myMsg = txtInput.getText();
				if (!myMsg.isEmpty()) {
					out.println(myMsg);
					txtInput.setText("");
				}
			}
		});

		setVisible(true);
	}

	public void startClient() {
		try {
			// 닉네임 먼저 입력 받기 (팝업창)
			nickname = JOptionPane.showInputDialog(this, "사용할 닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
			if (nickname == null || nickname.trim().isEmpty()) {
				nickname = "익명";
			}

			// 화면 입력창 왼쪽에 내 닉네임 반영하기
			lblName.setText(" [" + nickname + "] : ");

			// 내 닉네임 확정되었으니 명단 창 업데이트
			txtUserList.setText("👥 접속자 목록\n-----------\n- " + nickname + " (나)\n");

			// 서버 접속
			Socket socket = new Socket("localhost", 5000);
			out = new PrintWriter(socket.getOutputStream(), true);

			// 서버에 접속하자마자 내 닉네임을 첫 줄로 가장 먼저 전송
			out.println(nickname);

			// 수신 전용 스레드 (서버가 보낸 말을 받아 대화창에 추가)
			Thread receiveThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String serverMsg;
						while ((serverMsg = in.readLine()) != null) {
							// 콘솔 출력이 아니라 화면(TextArea)에 한 줄씩 추가
							// txtDisplay.append(serverMsg + "\n");

							/* [1]. 서버가 보낸 메시지가 USERLIST 형식일 때 */
							if (serverMsg.startsWith("USERLIST//")) {

								// 헤더 부분 떼어내고 이름만 받기
								// USERLIST// 의 글자 수 = 10
								String payLoad = serverMsg.substring(10);

								// 콤마 기준으로 잘라서 배열로 만들기
								String[] users = payLoad.split(",");

								// 오른쪽 접속자 명단 갱신 시작
								txtUserList.setText("👥 접속자 목록\n-----------\n");

								for (String user : users) {
									if (user.equals(nickname)) {
										txtUserList.append("- " + user + " (나)\n");
									} else {
										txtUserList.append("- " + user + "\n");
									}
								}
							}

							/* [2]. 서버가 보낸 메시지가 CHAT 형식일 때 */
							else if (serverMsg.startsWith("CHAT//")) {

								// 마찬가지로 CHAT// 의 글자 수가 6
								String chatContent = serverMsg.substring(6);

								/* 스크롤바 강제 이동 제거 로직 */

								// [1]. 스크롤바 부품 가져오기
								JScrollBar verticalBar = scrollPane.getVerticalScrollBar();

								// [2]. 현재 스크롤바의 값들 측정하기
								
								// 현재 화면 크기 위의, 가려져서 안보이는 영역
								int pastHeight = verticalBar.getValue();
								// 내가 보고 있는 영역, 즉 채팅창의 크기
								int visibleHeight = verticalBar.getVisibleAmount();
								// 가변 변수, 영수증마냥 채팅을 칠 때마다 늘어나서, 그 채팅의 총 길이
								int maxHeight = verticalBar.getMaximum();

								// [3]. 사용자가 채팅 기록을 보는 중인지 판독
								// (완전 소수점 차이나 오차를 방지하기 위해 보통 '값 + 화면높이 >= 전체높이' 범위)
								boolean isAtBottom = (pastHeight + visibleHeight >= maxHeight);

								// [4]. 채팅 추가
								txtDisplay.append(chatContent + "\n");

								// 5. 사용자가 맨 밑에 있었을 때만 스크롤을 강제로 내려즘
								if (isAtBottom) {
									// Swing의 렌더링 스레드가 갱신된 텍스트 높이를 인지한 뒤
									// 맨 아래로 내리도록 안전하게 이벤트를 큐에 밀어 넣는 정석적인 방법
									SwingUtilities.invokeLater(new Runnable() {
										@Override
										public void run() {
											verticalBar.setValue(verticalBar.getMaximum());
										}
									});
								}

							}
						}
					} catch (IOException e) {
						txtDisplay.append("서버와 연결이 끊겼습니다.\n");
					}
				}
			});
			receiveThread.start();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		ChatClientGUI client = new ChatClientGUI();
		client.startClient();
	}
}
