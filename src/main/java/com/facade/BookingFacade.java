package com.facade;

import com.datatype.Booking;

import java.util.List;

public interface BookingFacade {

	public List<Booking> filterByUser(Long userid);
	
	public Booking bookTravel(Booking booking);
	
	public Booking filterByBookingId(Long bookingId);
	
	public void updateBookingDate(Booking booking);
}
