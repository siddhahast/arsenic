package com.http;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class RequestWrapperFilter implements Filter{

	private boolean sanitize;
	
	public static final boolean DEFAULT_WRAP_REQUEST = true;
	public static final boolean DEFAULT_WRAP_RESPONSE = true;
	
	public void init(FilterConfig filterConfig) throws ServletException 
	{
		sanitize = filterConfig.getInitParameter("sanitize").equals("true");
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException 
	{
		
	}

	public void destroy()
	{
		
	}

}
