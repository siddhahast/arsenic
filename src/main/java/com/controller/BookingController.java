package com.controller;

import com.facade.BookingFacade;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.datatype.Booking;
import com.datatype.VoidData;
import com.service.BookingService;

@Controller
public class BookingController implements BookingService
{
	
	@Autowired
	private BookingFacade bookingFacade;
	
	@RequestMapping(value="/booking/{userId}", method=RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<List<Booking>> filterByUser(Long userid) 
	{
		List<Booking> userBookings = bookingFacade.filterByUser(userid);
		return new ResponseEntity<List<Booking>>(userBookings, HttpStatus.OK);
	}

	public ResponseEntity<Booking> bookTravel(Booking booking) 
	{
		Booking userTravelBooking = bookingFacade.bookTravel(booking);
		return new ResponseEntity<Booking>(userTravelBooking, HttpStatus.OK);
	}

	public ResponseEntity<Booking> filterByBookingId(Long bookingId) 
	{
		Booking bookingById = bookingFacade.filterByBookingId(bookingId);
		return new ResponseEntity<Booking>(bookingById, HttpStatus.OK);
	}

	public ResponseEntity<VoidData> updateBookingDate(Booking booking) 
	{
		bookingFacade.updateBookingDate(booking);
		return new ResponseEntity<VoidData>(VoidData.VOID, HttpStatus.OK);
	}

}
