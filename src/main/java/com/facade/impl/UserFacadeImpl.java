package com.facade.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.datatype.LoginRequest;
import com.datatype.User;
import com.datatype.UserFilter;
import com.facade.UserFacade;

@Component
public class UserFacadeImpl implements UserFacade{

	public List<User> filter(UserFilter userFilter) {
		// TODO Auto-generated method stub
		return null;
	}

	public User login(LoginRequest loginRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	public User searchByUserToken(String userToken) {
		// TODO Auto-generated method stub
		return null;
	}

}
