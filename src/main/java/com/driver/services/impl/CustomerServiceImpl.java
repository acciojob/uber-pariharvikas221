package com.driver.services.impl;

import com.driver.model.*;
import com.driver.services.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.driver.repository.CustomerRepository;
import com.driver.repository.DriverRepository;
import com.driver.repository.TripBookingRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.driver.model.TripStatus.*;

@Service
public class CustomerServiceImpl implements CustomerService {

	@Autowired
	CustomerRepository customerRepository2;

	@Autowired
	DriverRepository driverRepository2;

	@Autowired
	TripBookingRepository tripBookingRepository2;

	@Override
	public void register(Customer customer) {
		//Save the customer in database
		customerRepository2.save(customer);
	}

	@Override
	public void deleteCustomer(Integer customerId) {
		// Delete customer without using deleteById function
		Customer customer = customerRepository2.findById(customerId).get();
		List<TripBooking> bookedTrip = customer.getTripBookingList();

		for (TripBooking tripBookedByCustomer : bookedTrip) {
			Driver driver = tripBookedByCustomer.getDriver();
			Cab cab = driver.getCab();
			cab.setAvailable(true);
			driverRepository2.save(driver);
			tripBookedByCustomer.setStatus(CANCELED);
		}

		customerRepository2.delete(customer);
	}

	@Override
	public TripBooking bookTrip(int customerId, String fromLocation, String toLocation, int distanceInKm) throws Exception{
		//Book the driver with lowest driverId who is free (cab available variable is Boolean.TRUE). If no driver is available, throw "No cab available!" exception
		//Avoid using SQL query


		// finding a nearest driver
		List<Driver> driverList = driverRepository2.findAll();
		Driver driver = null;
		for(Driver currDriver : driverList){
			if(currDriver.getCab().getAvailable()){
				if((driver == null) || (currDriver.getDriverId() < driver.getDriverId())){
					driver = currDriver;
				}
			}
		}
//		for (Driver driver: driverList) {
//			if(driver != null && driver.getDriverId() < lowestDriverId){
//				if(driver.getCab().getAvailable()){
//					availableDriver = driver;
//				}
//			}
//		}
		if (driver == null) throw new Exception("No cab available!");


		// finding trip details
		TripBooking bookedTrip = new TripBooking();
		Customer customer = customerRepository2.findById(customerId).get();
		bookedTrip.setCustomer(customer);
		bookedTrip.setFromLocation(fromLocation);
		bookedTrip.setToLocation(toLocation);
		bookedTrip.setDistanceInKm(distanceInKm);
		bookedTrip.setBill(distanceInKm * 10);
		bookedTrip.setStatus(CONFIRMED);
		bookedTrip.setDriver(driver);
		driver.getCab().setAvailable(false);
		driverRepository2.save(driver);

		customer.getTripBookingList().add(bookedTrip);
		customerRepository2.save(customer);

		tripBookingRepository2.save(bookedTrip);
		return null;
	}

	@Override
	public void cancelTrip(Integer tripId){
		//Cancel the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripBooked = tripBookingRepository2.findById(tripId).get();
		tripBooked.setStatus(CANCELED);
		tripBooked.setBill(0);
		tripBooked.getDriver().getCab().setAvailable(true);
		tripBookingRepository2.save(tripBooked);
	}

	@Override
	public void completeTrip(Integer tripId){
		//Complete the trip having given trip Id and update TripBooking attributes accordingly
		TripBooking tripBooked = tripBookingRepository2.findById(tripId).get();
		tripBooked.setStatus(COMPLETED);
		tripBooked.getDriver().getCab().setAvailable(true);
		tripBookingRepository2.save(tripBooked);
	}
}