package com.example.informate;
import org.jsoup.*;
import org.jsoup.nodes.*;
import java.util.Random;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.jsoup.select.Elements;

public class scraper {
    articles art = new articles();

    public PrintWriter pw;

    public scraper() {
        try {
            pw = new PrintWriter(new FileOutputStream(new File("output.csv"), true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void scrapePageForText(String pageURL)throws IOException{
        System.out.println("text got called!");
        Document doc = Jsoup.connect(pageURL).get();
        Elements divs = doc.select("[data-component]");
        StringBuilder fullText = new StringBuilder();
        for (Element element : divs) {
            fullText.append(element.text()).append(" ");
        }
        for (String sentence : fullText.toString().split("\\.")){
            String cleaned = sentence.trim();
            if(!cleaned.isEmpty()){
                pw.append(cleaned + ".\n");
            }
        }
        pw.flush();
    }

    public void scrapePageForImages(String pageURL)throws IOException{

        System.out.println("image got called!");
        Document doc = Jsoup.connect(pageURL).get();
        Elements images = doc.getElementsByTag("img");
        Files.createDirectories(Paths.get("SiteImages/"));
        int counter = 0;
        for (Element element :images) {
            if(!element.equals("")){
                counter ++;
            }
            System.out.println(counter);   
        }
        for (Element element : images){
            String src = element.absUrl("image");
            if(!src.equals("")){
                InputStream inputStream = new URL(src).openStream();
                Files.copy(inputStream, Paths.get("SiteImages/"+ imageNamer() + ".jpg"), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
    public String imageNamer(){
        String characterSet = "1234567890QWERTYUIOPASDFGHJKLZXCVBNM";
        StringBuilder namer = new StringBuilder();
        Random random = new Random();
        int length = 8;
        for (int i = 0; i < length; i ++){
            int index = random.nextInt(characterSet.length());
            namer.append(characterSet.charAt(index));
        }
        return namer.toString();
    }

    public void scrapeForTitle(String pageURL) throws IOException{
        System.out.println("title got called!");
        Document doc = Jsoup.connect(pageURL).get();
        Elements title = doc.getElementsByTag("h1");
        pw.println("Output taken from " + pageURL);
        pw.println(title.text());
        pw.flush();
    }
}
