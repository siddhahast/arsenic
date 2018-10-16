package com.facade;

import java.util.List;

import com.datatype.Flight;
import com.datatype.FlightFilter;
import com.datatype.FlightIternary;

public interface FlightIternaryFacade 
{

	public void create(FlightIternary flightIternary);
	
	public Flight read(Long flightIternaryId);
	
	public List<FlightIternary> filter(FlightFilter filter);
	
	public List<FlightIternary> filterByDepartureCity(String departureCity);
	
	public List<FlightIternary> filterByArrivalCity(String departureCity);
	
	public void update(FlightIternary flight);
	
	public void delete(FlightIternary flight);
	
}
