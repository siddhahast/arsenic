package com.datatype;

import java.io.Serializable;

public class UserFilter implements Serializable{

	private Long[] userIds;
	private UserType[] userTypes;
	private String authToken;
	
	public Long[] getUserIds() {
		return userIds;
	}
	public void setUserIds(Long[] userIds) {
		this.userIds = userIds;
	}
	public UserType[] getUserTypes() {
		return userTypes;
	}
	public void setUserTypes(UserType[] userTypes) {
		this.userTypes = userTypes;
	}
	public String getAuthToken() {
		return authToken;
	}
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	
}
