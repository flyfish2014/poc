package com.gic.cinema.order.model;

/**
 * Seat Status
 */
public enum SeatStatus {
    /*
     * never booked yet
     */
    AVAILABLE,
    /*
     * taken by previous bookings
     */
    BOOKED,
    /*
     * seats being reserved in the current booking
     */
    RESERVED
}
