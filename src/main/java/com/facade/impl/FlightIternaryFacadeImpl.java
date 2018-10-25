package com.facade.impl;

import java.util.List;

import org.springframework.stereotype.Component;

import com.datatype.Flight;
import com.datatype.FlightFilter;
import com.datatype.FlightIternary;
import com.facade.FlightIternaryFacade;

@Component
public class FlightIternaryFacadeImpl implements FlightIternaryFacade{

	public void create(FlightIternary flightIternary) {
		// TODO Auto-generated method stub
		
	}

	public Flight read(Long flightIternaryId) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FlightIternary> filter(FlightFilter filter) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FlightIternary> filterByDepartureCity(String departureCity) {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FlightIternary> filterByArrivalCity(String departureCity) {
		// TODO Auto-generated method stub
		return null;
	}

	public void update(FlightIternary flight) {
		// TODO Auto-generated method stub
		
	}

	public void delete(FlightIternary flight) {
		// TODO Auto-generated method stub
		
	}

}
