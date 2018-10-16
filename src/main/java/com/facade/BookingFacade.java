package com.facade;

import java.util.List;

import com.datatype.Booking;

public interface BookingFacade {

	public List<Booking> filterByUser(Long userid);
	
	public Booking bookTravel(Booking booking);
	
	public Booking filterByBookingId(Long bookingId);
	
	public void updateBookingDate(Booking booking);
}
