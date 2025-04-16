package com.example.informate;
import java.io.IOException;
import java.util.Scanner;
import java.io.Console;
import java.util.Arrays;

public class main {
    static scraper scrap = new scraper();
    articles art = new articles();
    public static void main(String[] args) {
        auth au = new auth();
        au.initialiseDB();
        
        Console console = System.console();
        if (console == null) {
            System.out.println("Couldn't get Console instance");
            System.exit(0);
        }

        System.out.println("Please choose between the following:");
        System.out.println("1. Login");
        System.out.println("2. Register");
        Scanner input = new Scanner(System.in);
        String option = input.next(); 
        

        switch(option){
            case "1":
                System.out.println("Please input the following details");
                input.nextLine();
                System.out.println("Enter email or username");
                String inputText = input.nextLine(); 
                if(inputText.contains("@")){
                    System.out.println("Email recognised");
                    if(au.checkEmail(inputText)){
                        System.out.println("Email correct");
                    }
                    else{
                        System.err.println("Error email");
                    }
                }
                else{
                    System.out.println("Username recognised");
                    if(au.checkUsername(inputText)){
                        
                    }
                    else{
                        System.err.println("Error username");
                    }
                }


            break;
            case "2":
            System.out.println("Please enter the following details.");
            System.out.println("Choose a username:");
            String username = input.next();
            System.out.println("Choose an email");
            String email = input.next();
            System.out.println("Choose a secure password");
            char[] passwordChar = console.readPassword("Password: ");
            String password = new String(passwordChar);
            String hashedPassword = null;
            try {
                hashedPassword = au.hashPassword(password);
            } catch (Exception e) { 
                System.err.println("Error " + e );
            }
            Arrays.fill(passwordChar, ' ');
            au.insertDetails(username, hashedPassword, email);
            break;

            default:
            System.err.println("Invalid Input!");
            break;
        }
        
        String URL = "https://www.bbc.com/news/articles/cd6jn5exv24o";
        System.err.println(URL);
        try {
            scrap.scrapeForTitle(URL);
            scrap.scrapePageForText(URL);
            scrap.scrapePageForImages(URL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
