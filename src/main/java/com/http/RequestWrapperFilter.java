package com.http;

import javax.servlet.*;
import java.io.IOException;

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
