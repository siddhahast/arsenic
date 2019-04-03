package com.service;

import com.datatype.Booking;
import com.datatype.VoidData;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface BookingService 
{
	
	public ResponseEntity<List<Booking>> filterByUser(Long userid);
	
	public ResponseEntity<Booking> bookTravel(Booking booking);
	
	public ResponseEntity<Booking> filterByBookingId(Long bookingId);
	
	public ResponseEntity<VoidData> updateBookingDate(Booking booking);

}
