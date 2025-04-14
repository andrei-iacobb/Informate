package com.example.informate;

import java.io.IOException;
import java.util.Scanner;

import javax.swing.plaf.basic.BasicSplitPaneUI;

public class main {
    static scraper scrap = new scraper();
    articles art = new articles();
    public static void main(String[] args) {
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
