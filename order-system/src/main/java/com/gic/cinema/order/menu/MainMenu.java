package com.gic.cinema.order.menu;

import com.gic.cinema.order.exception.NotEnoughSeatsException;
import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.model.Order;
import com.gic.cinema.order.model.Seat;
import com.gic.cinema.order.model.SeatStatus;
import com.gic.cinema.order.service.CinemaService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MainMenu - process main menu - movie order system
 */
@Component
public class MainMenu {
    private CinemaService cinemaService;
    public MainMenu(CinemaService cinemaService){
        this.cinemaService=cinemaService;
    }

    /**
     * process movie main menu - order flow
     *  1: Booking movie
     *  2: Check Booking
     *  3: Exit movie booking system
     * @param scanner
     * @param hall
     * @throws Exception
     */
    public void process(BufferedReader scanner, CinemaHall hall) throws Exception{
        // ------- Main menu loop -------
        while (true) {
            // get available seat count in current movie hall
            int available = hall.getAvailableSeatCount();
            System.out.println();
            System.out.printf("Welcome to GIC Cinemas%n");
            System.out.printf("[1] Book tickets for %s (%d seats available)%n", hall.getMovieName(), available);
            System.out.println("[2] Check bookings");
            System.out.println("[3] Exit");
            //System.out.println("[4] Go Movie Seat Map Define");
            System.out.println("Please enter your selection: ");
            System.out.print("> ");
            String choice = scanner.readLine().trim();
            if (choice.equals("1")) {
                //process booking workflow
                handleBookingWorkflow(scanner, hall);
            } else if (choice.equals("2")) {
                //check booking
                showBookings(scanner,hall);
            } else if (choice.equals("3")) {
                System.out.println("Thank you for using GIC Cinemas system. Bye!");
                break;
            } else {
                System.out.println("Invalid option.");
            }
        }

    }

    /*
     * process booking workflow
     * @param scanner
     * @param hall
     * @throws Exception
     */
    private void handleBookingWorkflow(BufferedReader scanner, CinemaHall hall) throws Exception{
        //book info
        String bookingId = "GIC"+UUID.randomUUID().toString().substring(0, 8);
        Order booking =  Order.builder().id(bookingId).movieName(hall.getMovieName()).build();
        String line =null;
        List<Seat> seatsToBook=null;
        List<String> labels=null;
        int tickets;
        // loop order workflow menu
        while (true) {
            System.out.println("Enter number of tickets to book, or enter blank to go back to main menu: ");
            System.out.print("> ");
            //read line, if empty return to main menu
            line = scanner.readLine().trim();
            if (!StringUtils.hasLength(line)) {
               return;
            }
            //get booking tickets number, if invalid input, return to booking workflow menu
            try {
                tickets = Integer.parseInt(line);
                if (tickets <= 0) {
                    System.out.println("Tickets must be > 0.");
                    continue;
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
                continue;
            }
            //if input tickets number is large than hall available seat count, return to booking workflow menu
            if (tickets > hall.getAvailableSeatCount()) {
                System.out.println("Not enough seats available. Try a smaller number.");
                continue; // let user try again
            }
             // allocate default seats first input
             seatsToBook = cinemaService.bookDefault(tickets);
            //generate seats labels
             labels = seatsToBook.stream()
                    .map(Seat::getLabel)
                    .sorted()
                    .collect(Collectors.toList());
            booking.setTickets(tickets);
            booking.setSeatLabels(labels);
            //print current booking ticket info
            printMapHeader(booking);
            // print map with current booking highlighted
            printSeatingMap(hall, new HashSet<>(booking.getSeatLabels()));
            break; // finish workflow
        }
        //loop booking confirm or booking custom seats
        while(true){
            try {
                System.out.println("\n\nEnter blank to accept seat selection, or enter new seating position (B04) ");

                System.out.print("> ");
                line = scanner.readLine().trim();
                //confirm booking if input is empty, return main menu
                if (!StringUtils.hasLength(line)) {
                    cinemaService.confirmOrder(hall, booking, seatsToBook);
                    System.out.println("Booking id: " + bookingId + " confirmed.");
                    break;
                }
                // custom starting position format B02, first position is row char
                String seatStr = line.toUpperCase(Locale.ROOT);
                if (seatStr.length() < 2) {
                    System.out.println("Invalid seat format.");
                    continue;
                }
                //get row char
                char rowChar = seatStr.charAt(0);
                int seatNo;
                //get seat position, if invalid, return to booking workflow menu
                try {
                    seatNo = Integer.parseInt(seatStr.substring(1));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid seat number.");
                    continue;
                }
                //allocate seats for custom position
                seatsToBook = cinemaService.bookFromPosition(tickets, rowChar, seatNo);
                //generate booking seats label
                labels = seatsToBook.stream()
                        .map(Seat::getLabel)
                        .sorted()
                        .collect(Collectors.toList());
                booking.setSeatLabels(labels);
                //print booking seat info
                printMapHeader(booking);
                // print map with current booking highlighted
                printSeatingMap(hall, new HashSet<>(booking.getSeatLabels()));
            } catch (NotEnoughSeatsException | IllegalArgumentException ex) {
                System.out.println("Error: " + ex.getMessage());
                break;
            }
        }

    }


    /**
     * print map header
     * @param booking
     */
     private void printMapHeader(Order booking){
         System.out.println();
         System.out.printf("Booking successful! ID: %s%n", booking.getId());
         System.out.println("Selected seats: " + String.join(", ", booking.getSeatLabels()));
         //System.out.println("Selected seats in Cinema Map: ");
     }
    /**
     * Print seating map:
     *  '.' = available
     *  '#' = previously booked
     *  'O' = seats in current booking (highlight)
     * Back row (furthest from screen) appears at top.
     */
    private void printSeatingMap(CinemaHall hall, Set<String> highlightSeats) {
        int totalLen=hall.getSeatsPerRow()*3+1;
        //display Screen front top, display Screen in the middle
        if(totalLen <=6){
          System.out.println("Screen");
        }else {
            //display empty before "Screen"
            int middle = totalLen/2+1;
            for(int i=0;i<middle-3;i++){
                System.out.print(" ");
            }
            System.out.print("Screen");
            //display empty after "Screen"
            for(int i=middle+6;i<totalLen;i++){
                System.out.print(" ");
            }
            System.out.println("");
        }
        //hall seats
        Seat[][] seats = hall.getSeats();
        //display one line between Screen and seats map
        System.out.print("-");
        for (int c = 0; c < hall.getSeatsPerRow(); c++) {
            System.out.print(" - ");
        }
        System.out.println("-");
        //display seats map, loop row
        for (int r = hall.getRows() ; r >= 1; r--) {
            StringBuilder sb = new StringBuilder();
            //append row char (A-Z)
            char rowChar = (char) ('A' + r-1);
            sb.append(rowChar).append(' ');
            //loop seat per row
            for (int c = 0; c < hall.getSeatsPerRow(); c++) {
                Seat s = seats[hall.getRows()-r][c];
                String label = s.getLabel();
                String ch ="";
                //display seat status
                if (highlightSeats.contains(label)) {  //seating booking
                    ch = " O ";
                } else if (s.getStatus() == SeatStatus.BOOKED) { //seating booked
                    ch = " # ";
                } else {
                    ch = " . ";  //seat available
                }
                sb.append(ch);
            }
            System.out.println(sb);
        }
        //display seats number in the bottom
        StringBuilder sb1 = new StringBuilder("  ");
        for (int c = 1; c <= hall.getSeatsPerRow(); c++) {
            if(c<11){
            sb1.append(" "+c+" ");
            }else {
                sb1.append(c+" ");
            }
        }
        System.out.println(sb1);
        System.out.println("Legend: '.'=available, '#'=booked, 'O'=this booking");
    }
    /**
     * show current cinema hall bookings
     * @param hall
     */
    public void showBookings(BufferedReader scanner,CinemaHall hall) throws Exception{
        //check order status
        if (hall.getOrders().isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }
        //print booking info
        System.out.println("Existing bookings:");
        for (Order b : hall.getOrders().values()) {
            System.out.printf("  ID: %s | Tickets: %d | Seats: %s%n",
                    b.getId(), b.getTickets(), String.join(", ", b.getSeatLabels()));
        }
        //loop check order
        while(true){
            System.out.println("Enter booking id, or enter blank to go back to main menu:");
            System.out.print("> ");
            String line = scanner.readLine().trim();
            //if empty return main menu
            if (!StringUtils.hasLength(line)) {
                return;
            }
            //get order/booking id
            Order booking = hall.getOrders().get(line);
            if(booking == null){
                System.out.println("Invalid bookings!");
            }else {
                System.out.println();
                System.out.printf("Booking id: %s%n", booking.getId());
                System.out.println("Selected seats: " + String.join(", ", booking.getSeatLabels()));
                // print map with current booking highlighted
                printSeatingMap(hall, new HashSet<>(booking.getSeatLabels()));
            }
        }
    }
}
