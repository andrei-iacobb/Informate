package com.example.informate;
import java.util.ArrayList;
import java.util.List;

public class user {
    private String email;
    private String password;
    private List<articles> submittedArticles;

    public user(String email, String password){
        this.email = email;
        this.password = password;
        this.submittedArticles = new ArrayList<>();
    }

}
