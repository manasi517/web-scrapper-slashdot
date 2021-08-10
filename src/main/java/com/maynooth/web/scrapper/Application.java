package com.maynooth.web.scrapper;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	public static void main(String[] args) throws ParseException, IOException {
		
		SpringApplication.run(Application.class, args);
	}
}
