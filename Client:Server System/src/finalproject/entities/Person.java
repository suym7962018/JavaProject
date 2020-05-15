package finalproject.entities;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Person implements java.io.Serializable {

	private static final long serialVersionUID = 4190276780070819093L;

	// this is a person object that you will construct with data from the DB
	// table. The "sent" column is unnecessary. It's just a person with
	// a first name, last name, age, city, and ID.
	
	private String first;
	private String last;
	private int age;
	private String city;
	private int id;
	
	public Person(int id, String name) {
		this.id = id;
		this.first = name.split(" ")[0];
		this.last = name.split(" ")[1];
		String url = "jdbc:sqlite:client.db";
		try {
			Connection conn = DriverManager.getConnection(url);
			PreparedStatement stmt = conn.prepareStatement("SELECT age, city FROM People WHERE id = ? AND first = ? AND last = ?");
			stmt.setInt(1, id);
			stmt.setString(2, first);
			stmt.setString(3, last);
			ResultSet rs = stmt.executeQuery();
			this.age = rs.getInt("age");
			this.city = rs.getString("city");
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public String getName() {
		return this.first + " " + this.last;
	}
	public String getFirst(){
		return this.first;
	}
	public String getLast(){
		return this.last;
	}
	public String getCity(){
		return this.city;
	}
	public int getAge(){
		return this.age;
	}
	public int getId(){
		return this.id;
	}
}
