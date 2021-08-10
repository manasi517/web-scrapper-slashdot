package com.maynooth.web.scrapper.controller;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.maynooth.web.scrapper.service.WebCrappingService;



@RestController
public class WebScrapperController {

	@Autowired
	WebCrappingService webCrappingService;

	@RequestMapping("/")
	public String readData(@RequestParam(required = false, defaultValue = "0") int from, 
						   @RequestParam(required = false, defaultValue = "0") int to) throws IOException, ParseException {

		webCrappingService.readData(from, to);
		
		return "Web Scraping done";
	}




}
