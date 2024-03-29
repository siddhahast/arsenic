package com.service;

import com.common.def.ServiceResponse;
import com.datatype.LoginRequest;
import com.datatype.User;
import com.datatype.UserFilter;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface UserService 
{

	public ServiceResponse<User> filterDummy();
	
	public ResponseEntity<List<User>> filter(UserFilter filter);
	
	public ResponseEntity<User> login(LoginRequest loginRequest);
	
	public ResponseEntity<User> filterUserByAuthToken(UserFilter filter);
	
}
