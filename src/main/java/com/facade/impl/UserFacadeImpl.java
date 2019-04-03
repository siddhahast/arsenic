package com.facade.impl;

import com.datatype.LoginRequest;
import com.datatype.User;
import com.datatype.UserFilter;
import com.facade.UserFacade;
import org.springframework.stereotype.Component;

import java.util.List;

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
