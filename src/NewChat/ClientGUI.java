package NewChat;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
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

public class ClientGUI extends JFrame {

	private JTextField txtInput;
	private JTextArea txtDisplay;
	private JTextArea txtUserList;
	private PrintWriter out;
	private String nickname;
	private JLabel lblName;
	private JScrollPane scrollPane;

	public ClientGUI() {
		setTitle("채팅 프로그램");
		setSize(550, 500);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		txtDisplay = new JTextArea();
		txtDisplay.setEditable(false);
		scrollPane = new JScrollPane(txtDisplay);
		add(scrollPane, BorderLayout.CENTER);

		txtUserList = new JTextArea();
		txtUserList.setEditable(false);
		txtUserList.setText("👥 접속자 목록\n-----------\n- (나)\n");
		JScrollPane userScrollPane = new JScrollPane(txtUserList);
		userScrollPane.setPreferredSize(new Dimension(130, 0));
		add(userScrollPane, BorderLayout.EAST);

		JPanel southPanel = new JPanel(new BorderLayout());
		lblName = new JLabel(" [미정] : ");
		txtInput = new JTextField();

		southPanel.add(lblName, BorderLayout.WEST);
		southPanel.add(txtInput, BorderLayout.CENTER);
		add(southPanel, BorderLayout.SOUTH);

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
			nickname = JOptionPane.showInputDialog(this, "사용할 닉네임을 입력하세요:", "닉네임 설정", JOptionPane.PLAIN_MESSAGE);
			if (nickname == null || nickname.trim().isEmpty()) {
				nickname = "익명";
			}

			lblName.setText(" [" + nickname + "] : ");
			txtUserList.setText("👥 접속자 목록\n-----------\n- " + nickname + " (나)\n");

			Socket socket = new Socket("localhost", 5000);
			out = new PrintWriter(socket.getOutputStream(), true);
			out.println(nickname);

			// 내부 스레드 압축
			ClientReceiverT receiver = new ClientReceiverT(socket, this);
			new Thread(receiver).start();

		} catch (IOException e) {
			// 서버 연결 실패 시 사용자에게 알림
			JOptionPane.showMessageDialog(this, "서버에 접속할 수 없습니다.\n정원이 초과되었거나 서버가 점검 중입니다.", "접속 실패",
					JOptionPane.ERROR_MESSAGE);
			
			// 앱종료
			// 근데 실제 서비스라면 바로 종료보단 나가기 버튼 누르면 끄게 하거나
			// 닉네임 다시 입력받거나
			// 초기 화면으로 돌아가는 그런 로직으로 하면 좋을 듯
			System.exit(0);
		}
	}

	// 외부(ClientReceiver)에서 서버 메시지를 받으면 호출할 채팅 추가 메소드
	public void appendChat(String chatContent) {
		JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
		int pastHeight = verticalBar.getValue();
		int visibleHeight = verticalBar.getVisibleAmount();
		int maxHeight = verticalBar.getMaximum();

		boolean isAtBottom = (pastHeight + visibleHeight >= maxHeight);

		txtDisplay.append(chatContent + "\n");

		if (isAtBottom) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					verticalBar.setValue(verticalBar.getMaximum());
				}
			});
		}
	}

	// 외부(ClientReceiver)에서 유저 목록 데이터를 주면 호출할 명단 갱신 메소드
	public void updateUserList(String payLoad) {
		String[] users = payLoad.split(",");
		txtUserList.setText("👥 접속자 목록\n-----------\n");

		for (String user : users) {
			if (user.equals(nickname)) {
				txtUserList.append("- " + user + " (나)\n");
			} else {
				txtUserList.append("- " + user + "\n");
			}
		}
	}

	public static void main(String[] args) {
		ClientGUI client = new ClientGUI();
		client.startClient();
	}
}