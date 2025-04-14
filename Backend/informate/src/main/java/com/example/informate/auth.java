package com.example.informate;
import java.sql.*;

public class auth {
    Connection conn;
    
    public void DB(){
        try{
            conn = DriverManager.getConnection("jdbc:sqlite:data.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXIST user (username STRING PRIMARY KEY ,email STRING, password STRING)");
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertDetails(String username, String password, String email ){
        try (PreparedStatement ps = conn.prepareStatement("INSTERT INTO user (username, email, password) VALUES (?,?,?) ")){
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void checkUsername(String username){
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:data.db");
            //Statement stmt = conn.prepareStatement();
        } catch (SQLException e) {

        }
    }
}
