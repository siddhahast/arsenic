package com.datatype;

import java.io.Serializable;

public class Flight implements Serializable{

	private Long id;
	private String name;
	private String operator;
	private Long seats;
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getOperator() {
		return operator;
	}
	public void setOperator(String operator) {
		this.operator = operator;
	}
	
}
