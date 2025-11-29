package com.gic.cinema.order.menu;

import com.gic.cinema.order.exception.NotEnoughSeatsException;
import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.model.Order;
import com.gic.cinema.order.model.Seat;
import com.gic.cinema.order.service.CinemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MainMenu
 */
class MainMenuTest {

    private CinemaService cinemaService;
    private MainMenu mainMenu;
    private CinemaHall hall;

    /**
     * init value before run test case
     */
    @BeforeEach
    void setUp() {
        cinemaService = mock(CinemaService.class);
        mainMenu = new MainMenu(cinemaService);
        hall = mock(CinemaHall.class);

        // Safe defaults for all tests
        when(hall.getMovieName()).thenReturn("Inception");
        when(hall.getAvailableSeatCount()).thenReturn(10);

        // Make seating map “do nothing” (0 rows/cols => no array indexing)
        when(hall.getRows()).thenReturn(0);
        when(hall.getSeatsPerRow()).thenReturn(0);
        when(hall.getSeats()).thenReturn(new Seat[0][0]);
    }
   /*
    * mock seat
    */
    private Seat mockSeat(String label) {
        Seat seat = mock(Seat.class);
        when(seat.getLabel()).thenReturn(label);
        return seat;
    }

    // --- Main menu basic behaviour ---

    /**
     * Test Option 3 Exits Without Error
     * @throws Exception
     */
    @Test
    void testOption3ExitsWithoutError() throws Exception {
        // user immediately chooses Exit
        String input = "3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        // no booking workflow triggered
        verifyNoInteractions(cinemaService);
    }

    /**
     * Test Invalid Option Then Exit Does Not Call Service
     * @throws Exception
     */
    @Test
    void testInvalidOptionThenExitDoesNotCallService() throws Exception {
        // invalid option "5" then exit "3"
        String input = "5\n3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        verifyNoInteractions(cinemaService);
    }

    // --- Booking workflow: cancel before tickets ---

    /**
     * Test Booking Cancelled Before Ticket Entry Does Not Call Service
     * @throws Exception
     */
    @Test
    void testBookingCancelledBeforeTicketEntryDoesNotCallService() throws Exception {
        // 1 = book, then blank at ticket prompt, then 3 = exit
        String input = "1\n\n3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        verifyNoInteractions(cinemaService);
    }

    // --- Booking workflow: default seats + confirm ---

    /**
     * Test Default Booking Confirmed Calls Book Default And Confirm
     * @throws Exception
     */
    @Test
    void testDefaultBookingConfirmedCallsBookDefaultAndConfirm() throws Exception {
        // 1 = book, "2" tickets, blank to accept default seats, then 3 = exit
        String input = "1\n2\n\n3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        List<Seat> defaultSeats = Arrays.asList(
                mockSeat("A01"),
                mockSeat("A02")
        );

        when(cinemaService.bookDefault(2)).thenReturn(defaultSeats);

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        // booking from default seat allocation
        verify(cinemaService).bookDefault(2);

        // confirmOrder should be called with the defaultSeats list
        verify(cinemaService).confirmOrder(eq(hall), any(Order.class), eq(defaultSeats));
        verify(cinemaService, never()).bookFromPosition(anyInt(), anyChar(), anyInt());
    }

    // --- Booking workflow: custom starting position + confirm ---

    /**
     * Test Custom Starting Seat Confirmed Uses Book From Position Seats
     * @throws Exception
     */
    @Test
    void testCustomStartingSeatConfirmedUsesBookFromPositionSeats() throws Exception {
        // Flow:
        // 1 -> book
        // 2 -> tickets
        // B04 -> custom starting position
        // "" -> accept and confirm
        // 3 -> exit
        String input = "1\n2\nB04\n\n3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        List<Seat> defaultSeats = Arrays.asList(
                mockSeat("A01"),
                mockSeat("A02")
        );
        List<Seat> customSeats = Arrays.asList(
                mockSeat("B04"),
                mockSeat("B05")
        );

        when(cinemaService.bookDefault(2)).thenReturn(defaultSeats);
        when(cinemaService.bookFromPosition(2, 'B', 4)).thenReturn(customSeats);

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        verify(cinemaService).bookDefault(2);
        verify(cinemaService).bookFromPosition(2, 'B', 4);

        // confirmOrder should use the latest seatsToBook i.e. customSeats
        verify(cinemaService).confirmOrder(eq(hall), any(Order.class), eq(customSeats));
    }

    // --- Booking workflow: tickets larger than available => retry ---

    /**
     * Test Tickets More Than Available Re-prompts And Only Books Once
     * @throws Exception
     */
    @Test
    void testTicketsMoreThanAvailableRePromptsAndOnlyBooksOnce() throws Exception {
        // First try 20 (too many), then 2, then confirm, then exit
        String input = "1\n20\n2\n\n3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        when(hall.getAvailableSeatCount()).thenReturn(10);

        List<Seat> defaultSeats = Arrays.asList(mockSeat("A01"), mockSeat("A02"));
        when(cinemaService.bookDefault(2)).thenReturn(defaultSeats);

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        // bookDefault should only be called for the valid second attempt (2 tickets)
        verify(cinemaService, times(1)).bookDefault(2);
    }

    // --- Booking workflow: service errors (NotEnoughSeatsException) ---

    /**
     * Test Book From Position Throws Not Enough Seats Stops Booking Loop
     * @throws Exception
     */
    @Test
    void testBookFromPositionThrowsNotEnoughSeatsStopsBookingLoop() throws Exception {
        // 1 -> book
        // 2 -> tickets
        // B04 -> custom starting position that fails
        // 3 -> exit
        String input = "1\n2\nB04\n3\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        List<Seat> defaultSeats = Arrays.asList(
                mockSeat("A01"),
                mockSeat("A02")
        );

        when(cinemaService.bookDefault(2)).thenReturn(defaultSeats);
        when(cinemaService.bookFromPosition(2, 'B', 4))
                .thenThrow(new NotEnoughSeatsException("Not enough seats in that area"));

        assertDoesNotThrow(() -> mainMenu.process(reader, hall));

        verify(cinemaService).bookDefault(2);
        verify(cinemaService).bookFromPosition(2, 'B', 4);
        // confirmOrder should NOT be called because we break out on exception
        verify(cinemaService, never()).confirmOrder(any(), any(), anyList());
    }

    // --- showBookings: no bookings ---

    /**
     * Test Show Bookings No Bookings Prints Message And Returns
     * @throws Exception
     */
    @Test
    void testShowBookingsNoBookingsPrintsMessageAndReturns() throws Exception {
        when(hall.getOrders()).thenReturn(Collections.emptyMap());

        BufferedReader reader = new BufferedReader(new StringReader(""));

        assertDoesNotThrow(() -> mainMenu.showBookings(reader, hall));

        // no interaction with service layer
        verifyNoInteractions(cinemaService);
    }

    // --- showBookings: has bookings and user exits immediately ---

    /**
     * Test Show Bookings Has Bookings User Immediately Exits
     * @throws Exception
     */
    @Test
    void testShowBookingsHasBookingsUserImmediatelyExits() throws Exception {
        Map<String, Order> orders = new HashMap<>();
        Order order = Order.builder()
                .id("GIC123456")
                .movieName("Inception")
                .tickets(2)
                .seatLabels(Arrays.asList("A01", "A02"))
                .build();
        orders.put(order.getId(), order);

        when(hall.getOrders()).thenReturn(orders);

        // user just presses enter (blank) to go back
        String input = "\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        assertDoesNotThrow(() -> mainMenu.showBookings(reader, hall));

        verifyNoInteractions(cinemaService);
    }

    // --- showBookings: user enters invalid ID then valid ID then exits ---

    /**
     * Test Show Bookings Invalid Id Then Valid Id Shows Map Then Returns
     * @throws Exception
     */
    @Test
    void testShowBookingsInvalidIdThenValidIdShowsMapThenReturns() throws Exception {
        Map<String, Order> orders = new HashMap<>();
        Order order = Order.builder()
                .id("GIC999999")
                .movieName("Inception")
                .tickets(3)
                .seatLabels(Arrays.asList("B01", "B02", "B03"))
                .build();
        orders.put(order.getId(), order);

        when(hall.getOrders()).thenReturn(orders);

        // "wrong" -> invalid, then "GIC999999" -> valid, then "" -> back to menu
        String input = "wrong\nGIC999999\n\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        assertDoesNotThrow(() -> mainMenu.showBookings(reader, hall));

        // still no calls into service layer
        verifyNoInteractions(cinemaService);
    }
}