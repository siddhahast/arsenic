package com.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.datatype.User;

@Controller
public class UserController {

	@RequestMapping(value="/user", method=RequestMethod.GET)
	public ResponseEntity filter() {
		User user = new User();
		user.setId(1L);
		user.setUsername("Siddhahast");
		user.setEmail("siddhahast.nitr@gmail.com");
		return new ResponseEntity(user, HttpStatus.OK);
	}
	
}
