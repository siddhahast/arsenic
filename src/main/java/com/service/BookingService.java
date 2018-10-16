package com.service;

import java.util.List;

import org.springframework.http.ResponseEntity;

import com.datatype.Booking;
import com.datatype.VoidData;

public interface BookingService {
	
	public ResponseEntity<List<Booking>> filterByUser(Long userid);
	
	public ResponseEntity<Booking> bookTravel(Booking booking);
	
	public ResponseEntity<Booking> filterByBookingId(Long bookingId);
	
	public ResponseEntity<VoidData> updateBookingDate(Booking booking);

}
