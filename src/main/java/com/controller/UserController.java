package com.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.datatype.LoginRequest;
import com.datatype.User;
import com.datatype.UserFilter;
import com.facade.UserFacade;
import com.service.UserService;

@Controller
public class UserController implements UserService{

	@Autowired
	private UserFacade userFacade;
	
	@RequestMapping(value="/userDummy", method=RequestMethod.GET)
	public ResponseEntity<User> filterDummy() 
	{
		User user = new User();
		user.setId(1L);
		user.setUsername("Siddhahast");
		user.setEmail("siddhahast.nitr@gmail.com");
		return new ResponseEntity<User>(user, HttpStatus.OK);
	}

	@RequestMapping(value="/users", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<User>> filter(@RequestParam UserFilter filter) 
	{
		List<User> users = userFacade.filter(filter);
		return new ResponseEntity<List<User>>(users, HttpStatus.OK);
	}

	@RequestMapping(value="/user/login", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<User> login(@RequestBody LoginRequest loginRequest) 
	{
		User loggedInUser = userFacade.login(loginRequest);
		return new ResponseEntity<User>(loggedInUser, HttpStatus.OK);
	}

	@RequestMapping(value="/user/login", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<User> filterUserByAuthToken(UserFilter filter) 
	{
		User loggedInUser = userFacade.searchByUserToken(filter.getAuthToken());
		return new ResponseEntity<User>(loggedInUser, HttpStatus.OK);
	}
	
}
