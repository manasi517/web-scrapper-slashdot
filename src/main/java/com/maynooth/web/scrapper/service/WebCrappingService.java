package com.maynooth.web.scrapper.service;

import java.io.IOException;
import java.text.ParseException;

import org.springframework.stereotype.Service;

@Service
public interface WebCrappingService {

	public void readData(int from, int to) throws IOException, ParseException;
	
}
