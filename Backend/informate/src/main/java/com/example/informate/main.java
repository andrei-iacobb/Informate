package com.example.informate;

import java.io.IOException;

public class main {
    static scraper scrap = new scraper();
    articles art = new articles();
    public static void main(String[] args) {
        try {
            scrap.scrapeForTitle();
            scrap.scrapePageForText();
            scrap.scrapePageForImages();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
