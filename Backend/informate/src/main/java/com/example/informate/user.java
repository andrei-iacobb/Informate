package com.example.informate;
import java.util.ArrayList;
import java.util.List;

public class user {
    private String username;
    private String email;
    private String password;
    private List<articles> submittedArticles;

    user(String username, String email, String password){
        this.username = username;
        this.email = email;
        this.password = password;
        this.submittedArticles = new ArrayList<>();
    }

    

}
