package com.facade;

import com.datatype.LoginRequest;
import com.datatype.User;
import com.datatype.UserFilter;

import java.util.List;

public interface UserFacade 
{

	public List<User> filter(UserFilter userFilter);
	
	public User login(LoginRequest loginRequest);
	
	public User searchByUserToken(String userToken);
	
}
