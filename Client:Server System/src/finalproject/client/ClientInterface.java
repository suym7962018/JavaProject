package finalproject.client;

import java.util.ArrayList;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import javax.swing.*;

//import finalproject.client.ClientInterface.ComboBoxItem;
import finalproject.db.DBInterface;
import finalproject.entities.Person;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class ClientInterface extends JFrame {

	private static final long serialVersionUID = 1L;

	public static final int DEFAULT_PORT = 8001;
	
	private static final int FRAME_WIDTH = 600;
	private static final int FRAME_HEIGHT = 400;
	final int AREA_ROWS = 10;
	final int AREA_COLUMNS = 40;

	Connection conn;
	JPanel panel;
	JTextArea text;
	JLabel dbLabel;
	JLabel dbName;
	JLabel connLabel;
	JLabel connName;
	JButton openConn;
	JButton closeConn;
	JButton send;
	JButton query;
	
	JComboBox<String> peopleSelect;
	JFileChooser jFileChooser = new JFileChooser();
	Socket socket;
	int port;
	String host;
	InputStreamReader in;
	ObjectOutputStream out;
	
	public ClientInterface() {
		this(DEFAULT_PORT);
		setSize(FRAME_WIDTH, FRAME_HEIGHT);
		createMenus();
		createPanel();
		this.add(panel, BorderLayout.NORTH);
		text = new JTextArea(AREA_ROWS, AREA_COLUMNS);
		text.setEditable(false);
		this.add(text);
		query.addActionListener(new QueryListener());
	}
	
	public ClientInterface(int port) {
		this.port = port;
		
	}
	
	private void createMenus() {	
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		menuBar.add(createFileMenu());	
	}

	public JMenu createFileMenu() {
		JMenu menu = new JMenu("File");
		menu.add(createFileOpenItem());
		menu.add(createFileExitItem());
		return menu;
	}
   
	private void fillComboBox() throws SQLException {
		List<ComboBoxItem> l = getNames();
		peopleSelect.setModel(new DefaultComboBoxModel(l.toArray()));
	}
	
	private List<ComboBoxItem> getNames() throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("SELECT id, first, last FROM People WHERE sent = 0");
		//stmt.setBoolean(1, false);
		ResultSet rs = stmt.executeQuery();
		List<ComboBoxItem> list = new ArrayList<>();
		while(rs.next()) {
			int id = rs.getInt("id");
			String name = rs.getString("first") + " " + rs.getString("last");
			ComboBoxItem item = new ComboBoxItem(id, name);
			list.add(item);
		}
		return list;
	}
	
	class ComboBoxItem {
		private int id;
		private String name;
		
		public ComboBoxItem(int id, String name) {
			this.id = id;
			this.name = name;
		}
		
		public int getId() {
			return this.id;
		}
		
		public String getName() {
			return this.name;
		}
		
		public String toString() {
			return this.name;
		}
	}
	
	private JMenuItem createFileOpenItem() {
		JMenuItem item = new JMenuItem("Open DB");
		class OpenDBListener implements ActionListener {
			public void actionPerformed(ActionEvent event) {
				int returnVal = jFileChooser.showOpenDialog(getParent());
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					System.out.println("You chose to open this file: " + jFileChooser.getSelectedFile().getAbsolutePath());
					String dbFileName = jFileChooser.getSelectedFile().getAbsolutePath();
					try {
						connectToDB(dbFileName);
						dbName.setText(dbFileName.substring(dbFileName.lastIndexOf("/") + 1));
						fillComboBox();
						//System.out.println("Connected" + dbFileName);
					} catch (Exception e) {
						System.err.print("error connection to db: " + e.getMessage());
						e.printStackTrace();
						dbName.setText("<None>");
					}
				}
			}
		}
		item.addActionListener(new OpenDBListener());
		return item;
	}
   
	private JMenuItem createFileExitItem() {
		JMenuItem item = new JMenuItem("Exit");      
		class MenuItemListener implements ActionListener {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		}      
		ActionListener listener = new MenuItemListener();
		item.addActionListener(listener);
		return item;
	}
   
	private JPanel createPanel() {
		panel = new JPanel();
		panel.setLayout(new GridLayout(5, 1));
		
		JPanel line1 = new JPanel();
		dbLabel = new JLabel("Active DB:");
		dbName = new JLabel("<None>");
		line1.add(dbLabel);
		line1.add(dbName);
		
		JPanel line2 = new JPanel();
		connLabel = new JLabel("Active Connection:");
		connName = new JLabel("<None>");
		line2.add(connLabel);
		line2.add(connName);
		
		JPanel line3 = new JPanel();
		peopleSelect = new JComboBox<String>(new String[] {"<Empty>"});
		//peopleSelect.addItem("<Empty>");
		line3.add(peopleSelect);
		
		JPanel line4 = new JPanel();
		openConn = new JButton("Open Connection");
		openConn.addActionListener(new OpenConnListener());
		closeConn = new JButton("Close Connection");
		closeConn.addActionListener(new CloseConnListener());
		line4.add(openConn);
		line4.add(closeConn);
		
		JPanel line5 = new JPanel();
		send = new JButton("Send Data");
		send.addActionListener(new SendButtonListener());
		query = new JButton("Query DB Data");
		query.addActionListener(new QueryListener());
		line5.add(send);
		line5.add(query);
		
		panel.add(line1);
		panel.add(line2);
		panel.add(line3);
		panel.add(line4);
		panel.add(line5);
		return panel;
	}
	
	private void connectToDB (String dbFileName) {
		String url = "jdbc:sqlite:" + dbFileName;
		try {
			conn = DriverManager.getConnection(url);
		} catch (SQLException e) {
			System.err.println("Connection error: " + e);
			System.exit(1);
		}
	}
   
	class SendButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
	        try {
				
	        	// responses are going to come over the input as text, and that's tricky,
	        	// which is why I've done that for you:
				//BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
				// now, get the person on the object dropdownbox we've selected
				ComboBoxItem personEntry = (ComboBoxItem)peopleSelect.getSelectedItem();
				
				// That's tricky which is why I have included the code. the personEntry
				// contains an ID and a name. You want to get a "Person" object out of that
				// which is stored in the database
				Person p = new Person(personEntry.getId(), personEntry.getName());
				System.out.println(p.getName());
				// Send the person object here over an output stream that you got from the socket.
				//out = new ObjectOutputStream(socket.getOutputStream());
				out.writeObject(p);
				System.out.println("sent");
				BufferedReader br = new BufferedReader(in);
				String response = br.readLine();
				System.out.println(response);

				if (response.contains("Success")) {
					System.out.println("Success");
					PreparedStatement stmt = conn.prepareStatement("UPDATE People SET sent = 1 WHERE id =" + personEntry.getId());
					//stmt.setInt(1, personEntry.getId());
					stmt.execute();
					fillComboBox();
					// what do you do after we know that the server has successfully
					// received the data and written it to its own database?
					// you will have to write the code for that.
				} else {
					System.out.println("Failed");
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} 
				catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	        
			
		}
		
	}
	
	class QueryListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				text.setText("");
				PreparedStatement stmt = conn.prepareStatement("SELECT * FROM People");
				ResultSet result = stmt.executeQuery();
				ResultSetMetaData rsmd = result.getMetaData(); 
				int numColumns = rsmd.getColumnCount();
				String rowString = "";
				boolean isHead = true;
				while (result.next()) {
					if(isHead) {
						for(int i = 1; i <= numColumns; i++) {
							rowString += rsmd.getColumnName(i) + "\t";
						}
						rowString += "\n";
						for(int i = 1; i <= numColumns; i++) {
							rowString += "----" + "\t";
						}
						rowString += "\n";
						isHead = false;
					}
					for (int i=1;i<=numColumns;i++) {
						Object o = result.getObject(i);
						rowString += o.toString() + "\t";
					}
					rowString += "\n";
				}
				System.out.print("rowString  is  " + rowString);
				text.setText(rowString);
			}
			catch (SQLException sql) {
				sql.printStackTrace();
			}
		}
	}
	
	class OpenConnListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			port = 8001;
			host = "localhost";
			try {
				socket = new Socket(host, port);
				out = new ObjectOutputStream(socket.getOutputStream());
				
				in = new InputStreamReader(socket.getInputStream());
				System.out.println("success connect");
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	class CloseConnListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				//socket.shutdownInput();
				socket.close();
				System.out.println("close conn");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static void main(String[] args) {
		ClientInterface ci = new ClientInterface();
		ci.setVisible(true);
	}
}
