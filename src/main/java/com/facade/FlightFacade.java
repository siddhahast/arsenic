package com.facade;

import com.datatype.Flight;
import com.datatype.FlightFilter;

import java.util.List;

public interface FlightFacade 
{
	
	public void create(Flight flight);
	
	public Flight read(Long flightId);
	
	public List<Flight> filter(FlightFilter filter);
	
	public void update(Flight flight);
	
	public void delete(Flight flight);

}
