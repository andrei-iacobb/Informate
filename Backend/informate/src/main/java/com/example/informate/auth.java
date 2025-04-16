package com.example.informate;
import java.awt.im.InputContext;
import java.net.ConnectException;
import java.sql.*;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Scanner;


public class auth {
    Connection conn;
    
    public void initialiseDB(){
        try{
            conn = DriverManager.getConnection("jdbc:sqlite:data.db");
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE TABLE IF NOT EXISTS user (username STRING PRIMARY KEY ,email STRING, password STRING)");
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    public void insertDetails(String username, String password, String email ){
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO user (username, email, password) VALUES (?,?,?) ")){
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, password);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean checkUsername(String username){
        try (PreparedStatement ps = conn.prepareStatement("SELECT username FROM user WHERE username = (?)")){
            ps.setString(1, username);
            ResultSet result = ps.executeQuery();
            if (result.next()){
                System.out.println("Username correct");
                System.out.println("Enter password");
                Scanner input = new Scanner(System.in);
                String password = input.nextLine();
                try {
                    if(verifyPassword(password, username)){
                        System.out.println("Password correct");
                        return true;
                    }
                    else{
                        System.err.println("incorrect password");
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
            else return false;
        } catch (SQLException e) {
            System.err.println("Error " + e);
            return false;
        }
    }


    public boolean checkEmail(String email) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT email FROM user WHERE email = ?")) {
            ps.setString(1, email);
            ResultSet result = ps.executeQuery();
            if (result.next()) {
                System.out.println("Email found");
                System.out.println("Enter password:");
                Scanner input = new Scanner(System.in);
                String password = input.nextLine();
                try {
                    if (verifyPassword(password, email)) {
                        System.out.println("Password correct");
                        return true;
                    } else {
                        System.err.println("Incorrect password");
                        return false;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                System.err.println("Email not found");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("Error: " + e);
            return false;
        }
    }
    

    public boolean verifyPassword(String inputPassword, String login) throws Exception {
        String storedHash = null;
        byte[] salt = null;
        try(PreparedStatement ps = conn.prepareStatement("SELECT password FROM user WHERE username = (?)")) {
            ps.setString(1, login);
            ResultSet result = ps.executeQuery();
            if(result.next()){
                String dbResult = result.getString("password");
                String[] split = dbResult.split(":");
                storedHash = split[0];
                salt = Base64.getDecoder().decode(split[1]);
            }
        } catch (SQLException e){
            System.err.println("Error " + e);
        }

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);

        byte[] hashedInputPassword = md.digest(inputPassword.getBytes());
        String newHash = Base64.getEncoder().encodeToString(hashedInputPassword);

        return newHash.equals(storedHash);
    }

    public static String hashPassword(String password) throws Exception{
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);

        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(salt);
        
        byte[] hashedPassword = md.digest(password.getBytes());

        String hashBase64 = Base64.getEncoder().encodeToString(hashedPassword);
        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        return hashBase64 + ":" + saltBase64;
    }
}
