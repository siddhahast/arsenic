package com.facade;

import com.datatype.Flight;
import com.datatype.FlightFilter;
import com.datatype.FlightIternary;

import java.util.List;

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
