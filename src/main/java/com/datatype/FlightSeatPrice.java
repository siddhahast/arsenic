package com.datatype;

import java.util.Date;

public class FlightSeatPrice {

	private FlightSeat flightSeat;
	
	private Long basePrice;
	
	private Date startDate;
	
	private Date endDate;

	public FlightSeat getFlightSeat() {
		return flightSeat;
	}

	public void setFlightSeat(FlightSeat flightSeat) {
		this.flightSeat = flightSeat;
	}

	public Long getBasePrice() {
		return basePrice;
	}

	public void setBasePrice(Long basePrice) {
		this.basePrice = basePrice;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	
}
