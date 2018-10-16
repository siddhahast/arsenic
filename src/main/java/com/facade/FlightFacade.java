package com.facade;

import com.datatype.FlightFilter;
import java.util.List;

import com.datatype.Flight;

public interface FlightFacade 
{
	
	public void create(Flight flight);
	
	public Flight read(Long flightId);
	
	public List<Flight> filter(FlightFilter filter);
	
	public void update(Flight flight);
	
	public void delete(Flight flight);

}
