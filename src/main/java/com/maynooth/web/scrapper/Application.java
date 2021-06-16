package com.maynooth.web.scrapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws ParseException, IOException {
		SpringApplication.run(Application.class, args);
		/*
		 * Document doc = Jsoup.connect(
		 * "https://entertainment.slashdot.org/comments.pl?sid=19069242&amp;cid=61468774"
		 * ).get(); //writeToFile(doc.toString(),"comment_"+61468744); File yourFile =
		 * new File("comment_"+61468774+".txt"); yourFile.createNewFile();
		 * BufferedWriter writer = new BufferedWriter(new FileWriter(yourFile));
		 * writer.write(doc.toString()); writer.close();
		 */
	}
}
