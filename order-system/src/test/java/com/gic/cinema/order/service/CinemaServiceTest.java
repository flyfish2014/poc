package com.gic.cinema.order.service;

import com.gic.cinema.order.exception.NotEnoughSeatsException;
import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.model.Order;
import com.gic.cinema.order.model.Seat;
import com.gic.cinema.order.model.SeatStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CinemaService.
 *
 * These are pure unit tests – we instantiate the service directly
 * without starting the Spring container.
 */
class CinemaServiceTest {

    private CinemaService cinemaService;

    /**
     * init value before start junit test
     */
    @BeforeEach
    void setUp() {
        cinemaService = new CinemaService();
    }

    // ==============================
    // configureHall / getHall tests
    // ==============================

    /**
     * Test Configure Hall With Valid Inputs Builds Hall And Sets Current Hall
     */
    @Test
    void testConfigureHallWithValidInputsBuildsHallAndSetsCurrentHall() {
        CinemaHall hall = cinemaService.configureHall("Avengers", 5, 10);

        assertNotNull(hall);
        assertEquals(5, hall.getRows());
        assertEquals(10, hall.getSeatsPerRow());

        // getHall() should return the same instance
        CinemaHall current = cinemaService.getHall();
        assertSame(hall, current);
    }

    /**
     * Test Configure Hall With Empty Title Throws IllegalArgumentException
     */
    @Test
    void testConfigureHallWithEmptyTitleThrowsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.configureHall("", 5, 10)
        );
        assertTrue(ex.getMessage().contains("Movie Name"));
    }

    /**
     * Test Configure Hall With Invalid Row Count Throws IllegalArgumentException
     */
    @Test
    void testConfigureHallWithInvalidRowCountThrowsIllegalArgumentException() {
        // rows < 1
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.configureHall("Movie", 0, 10)
        );
    }

    /**
     * Test Get Hall Before Configure Hall Throws IllegalStateException
     */
    @Test
    void testGetHallBeforeConfigureHallThrowsIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> cinemaService.getHall());
    }

    // ==================
    // bookDefault tests
    // ==================

    /**
     * Test Book Default With NonPositive Tickets Throws IllegalArgumentException
     */
    @Test
    void testBookDefaultWithNonPositiveTicketsThrowsIllegalArgumentException() {
        cinemaService.configureHall("Movie", 5, 5);
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.bookDefault(0)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.bookDefault(-3)
        );
    }

    /**
     * Test Book Default With More Tickets Than Available Seats Count, Throws NotEnoughSeatsException
     */
    @Test
    void testBookDefaultWithMoreTicketsThanAvailableThrowsNotEnoughSeatsException() {
        CinemaHall hall = cinemaService.configureHall("Movie", 1, 3);

        // Mark all seats as BOOKED to simulate full hall
        for (Seat s : hall.getSeats()[0]) {
            s.setStatus(SeatStatus.BOOKED);
        }

        assertEquals(0, hall.getAvailableSeatCount());
        assertThrows(
                NotEnoughSeatsException.class,
                () -> cinemaService.bookDefault(1)
        );
    }

    /**
     * Test Book Default With Valid Request, Returns Correct Number Of Seats
     */
    @Test
    void testBookDefaultWithValidRequestReturnsCorrectNumberOfSeats() {
        cinemaService.configureHall("Movie", 3, 4);
        List<Seat> seats = cinemaService.bookDefault(5);

        assertEquals(5, seats.size());
        // At this point service only allocates seats, does NOT mark them BOOKED
        assertTrue(seats.stream().allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE));
    }

    // =======================
    // bookFromPosition tests
    // =======================

    /**
     * Test Book From Position With NonPositive Tickets Throws IllegalArgumentException
     */
    @Test
    void testBookFromPositionWithNonPositiveTicketsThrowsIllegalArgumentException() {
        cinemaService.configureHall("Movie", 5, 5);
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.bookFromPosition(0, 'C', 2)
        );
    }

    /**
     * Test Book From Position With Too Many Tickets Throws NotEnoughSeatsException
     */
    @Test
    void testBookFromPositionWithTooManyTicketsThrowsNotEnoughSeatsException() {
        CinemaHall hall = cinemaService.configureHall("Movie", 1, 3);

        // Mark some seats BOOKED, leaving only 1 available
        Seat[][] seats = hall.getSeats();
        seats[0][0].setStatus(SeatStatus.BOOKED);
        seats[0][1].setStatus(SeatStatus.BOOKED);
        assertEquals(1, hall.getAvailableSeatCount());

        assertThrows(
                NotEnoughSeatsException.class,
                () -> cinemaService.bookFromPosition(2, 'A', 1)
        );
    }

    /**
     * Test Book From Position With Seat Out Of Bounds Throws IllegalArgumentException
     */
    @Test
    void testBookFromPositionWithSeatOutOfBoundsThrowsIllegalArgumentException() {
        cinemaService.configureHall("Movie", 3, 4);

        // row 'Z' definitely out of range for 3 rows
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.bookFromPosition(1, 'Z', 1)
        );

        // seat number 0 (less than 1)
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.bookFromPosition(1, 'A', 0)
        );

        // seat number 999 clearly > seatsPerRow
        assertThrows(
                IllegalArgumentException.class,
                () -> cinemaService.bookFromPosition(1, 'A', 999)
        );
    }

    /**
     * Test Book From Position With Valid Inputs - Allocates Seats Starting From Position
     */
    @Test
    void testBookFromPositionWithValidInputsAllocatesSeatsStartingFromPosition() {
        CinemaHall hall = cinemaService.configureHall("Movie", 3, 5);

        // start from row 'A' (front row, closest to screen) seat 2
        List<Seat> seats = cinemaService.bookFromPosition(3, 'A', 2);

        assertEquals(3, seats.size());

        // Seats should still be AVAILABLE (only allocated, not confirmed)
        assertTrue(seats.stream().allMatch(s -> s.getStatus() == SeatStatus.AVAILABLE));
    }

    // ====================
    // confirmOrder / getBookings
    // ====================

    /**
     * Test Confirm Order Marks Seats Booked And Stores Order In Hall
     */
    @Test
    void testConfirmOrderMarksSeatsBookedAndStoresOrderInHall() {
        CinemaHall hall = cinemaService.configureHall("Movie", 2, 4);

        // allocate, then confirm
        List<Seat> allocated = cinemaService.bookDefault(3);

        Order order = new Order();
        // assuming standard setter exists
        order.setId("ORD-1");

        cinemaService.confirmOrder(hall, order, allocated);

        // all seats should be BOOKED now
        assertTrue(
                allocated.stream()
                        .allMatch(s -> s.getStatus() == SeatStatus.BOOKED)
        );

        // order should be saved in hall orders map
        assertTrue(hall.getOrders().containsKey("ORD-1"));
        assertSame(order, hall.getOrders().get("ORD-1"));

        // getBookings() should expose this order
        Collection<?> bookings = cinemaService.getBookings();
        assertEquals(1, bookings.size());
        assertTrue(bookings.contains(order));
    }
}