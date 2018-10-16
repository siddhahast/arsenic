package com.datatype;

public class FlightSeat {

	private Flight flight;
	
	private SeatType seatType;
	
	private Long seatNumber;
	
	private SeatAttribute seatAttribute;

	public Flight getFlight() {
		return flight;
	}

	public void setFlight(Flight flight) {
		this.flight = flight;
	}

	public SeatType geteatType() {
		return seatType;
	}

	public void setSeatType(SeatType SeatType) {
		this.seatType = SeatType;
	}

	public Long getSeatNumber() {
		return seatNumber;
	}

	public void setSeatNumber(Long seatNumber) {
		this.seatNumber = seatNumber;
	}

	public SeatType getSeatType() {
		return seatType;
	}

	public SeatAttribute getSeatAttribute() {
		return seatAttribute;
	}

	public void setSeatAttribute(SeatAttribute seatAttribute) {
		this.seatAttribute = seatAttribute;
	}
	
	
}
