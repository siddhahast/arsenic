package com.datatype;

import java.io.Serializable;
import java.util.Date;

public class Booking implements Serializable{

	private User traveller;
	
	private FlightSeat seatBooked;
	
	private Date travelDate;
	
	private Date bookingDate;

	public User getTraveller() {
		return traveller;
	}

	public void setTraveller(User traveller) {
		this.traveller = traveller;
	}

	public FlightSeat getSeatBooked() {
		return seatBooked;
	}

	public void setSeatBooked(FlightSeat seatBooked) {
		this.seatBooked = seatBooked;
	}

	public Date getTravelDate() {
		return travelDate;
	}

	public void setTravelDate(Date travelDate) {
		this.travelDate = travelDate;
	}

	public Date getBookingDate() {
		return bookingDate;
	}

	public void setBookingDate(Date bookingDate) {
		this.bookingDate = bookingDate;
	}
	
}
