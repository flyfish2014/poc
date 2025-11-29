package com.gic.cinema.order.service;

import com.gic.cinema.order.exception.NotEnoughSeatsException;
import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.model.Order;
import com.gic.cinema.order.model.Seat;
import com.gic.cinema.order.model.SeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cinema Service -- allocate seat etc
 */
@Service
public class CinemaService {
    public static final int MIN_INT = 1;
    @Value("${com.gic.cinema.order.hall.maxrow:26}")
   private int cinemaHallMaxRow=26;

    @Value("${com.gic.cinema.order.hall.maxseatsperrow:50}")
    private int cinemaHallMaxSeatsPerRow=50;

    /*
     * CinemaHalls Map
     */
    private Map<String,CinemaHall> cinemaHalls = new ConcurrentHashMap<>();
    /*
     * current CinemaHall
     */
    private CinemaHall currenHall;

    /**
     * Build one CinemaHall
     * @param title - movie name
     * @param rows  - cinema rows
     * @param seatsPerRow - cinema seat per row
     * @return
     */
    public CinemaHall configureHall(String title, int rows, int seatsPerRow) {
        //check movie name
        if(!StringUtils.hasLength(title)) throw new IllegalArgumentException("Movie Name can't be empty.");
        //check row
        if (rows < MIN_INT || rows > cinemaHallMaxRow) throw new IllegalArgumentException("Rows must be 1 - "+cinemaHallMaxRow+".");
        //check seat per row
        if (seatsPerRow < MIN_INT || seatsPerRow> cinemaHallMaxSeatsPerRow )
            throw new IllegalArgumentException("Seats Per Row must be 1 – "+cinemaHallMaxSeatsPerRow+".");
        //generate key
        String key = title+"_"+CinemaHall.DEFAULT_HALL_NAME+"_row_"+rows+"_col_"+seatsPerRow;
        //get CinemaHall from map
        CinemaHall hall = cinemaHalls.get(key);
        //first time generate cinema hall, build CinemaHall and put to map
        if(hall == null){
          hall = new CinemaHall(title, rows, seatsPerRow);
            cinemaHalls.put(key,hall);
        }
        this.currenHall=hall;
        return hall;
    }

    /**
     * get current cinema hall
     * @return
     */
    public CinemaHall getHall() {
        if (currenHall == null) throw new IllegalStateException("Hall not configured.");
        return currenHall;
    }

    /**
     * Book ticket default seats
     * @param tickets
     * @return
     */
    public List<Seat> bookDefault(int tickets) {
        //get current hall
        CinemaHall h = getHall();
        //check booking tickets number, it should be between 0 and current hall available seat count
        if (tickets <= 0) throw new IllegalArgumentException("Tickets must be > 0.");
        if (tickets > h.getAvailableSeatCount())
            throw new NotEnoughSeatsException("Not enough seats.");
         //allocate default seats
         return allocateDefault(h, tickets);
    }

    /**
     * Book ticket for special seat
     * @param tickets
     * @param rowChar
     * @param seatNumber
     * @return
     */
    public List<Seat> bookFromPosition(int tickets, char rowChar, int seatNumber) {
        //get current hall
        CinemaHall h = getHall();
        //check booking tickets number, it should be between 0 and current hall available seat count
        if (tickets <= 0) throw new IllegalArgumentException("Tickets must be > 0.");
        if (tickets > h.getAvailableSeatCount())
            throw new NotEnoughSeatsException("Not enough seats.");

        //get custom row index
        int rowIndex = h.getRows() - (Character.toUpperCase(rowChar) - 'A')-1;
        //get customer seat index
        int colIndex = seatNumber - 1;
        // check row/column index
        if (rowIndex < 0 || rowIndex >= h.getRows() || colIndex < 0 || colIndex >= h.getSeatsPerRow())
            throw new IllegalArgumentException("Seat out of bounds.");
        //allocate seats custom position
        return allocateFromPosition(h, tickets, rowIndex, colIndex);
    }

    /**
     * get booking list
     * @return
     */
    public Collection<Order> getBookings() {
        return getHall().getOrders().values();
    }

    /**
     * Confirm booking seats
     * @param h
     * @param order
     * @param seatsToBook
     * @return
     */
    public Order confirmOrder(CinemaHall h, Order order, List<Seat> seatsToBook) {
        //loop to set seat of booking status to BOOKED
        for (Seat s : seatsToBook) {
            s.setStatus(SeatStatus.BOOKED);
            s.setOrderId(order.getId());
        }
        //set order
        h.getOrders().put(order.getId(), order);
        return order;
    }


    /**
     * Default rule:
     *  - Start from the furthest row (the highest index).
     *  - Within a row, choose from middle-most column outwards.
     *  - If row cannot fit, overflow to next row closer to screen.
     */
    private List<Seat> allocateDefault(CinemaHall h, int tickets) {
        List<Seat> result = new ArrayList<>();
        int remaining = tickets;
        //loop start the furthest row
        for (int row = h.getRows() - 1; row >= 0 && remaining > 0; row--) {
            List<Seat> rowSeats = pickFromRowMiddleOut(h.getSeats()[row], remaining);
            result.addAll(rowSeats);
            remaining = tickets - result.size();
        }

        if (result.size() != tickets)
            throw new NotEnoughSeatsException("Cannot allocate seats with default rule.");
        return result;
    }

    /**
     * Starting-position rule:
     *  - from starting seat, fill to the right in same row;
     *  - if not enough, overflow to next row closer to screen using default rule.
     */
    private List<Seat> allocateFromPosition(CinemaHall h, int tickets,
                                            int startRow, int startCol) {
        List<Seat> result = new ArrayList<>();
        int remaining = tickets;

        // fill to right in custom row
        Seat[] rowSeats = h.getSeats()[startRow];
        for (int c = startCol; c < h.getSeatsPerRow() && remaining > 0; c++) {
            Seat s = rowSeats[c];
            if (s.getStatus() == SeatStatus.AVAILABLE) {
                result.add(s);
                remaining--;
            }
        }

        // overflow to rows closer to screen
        if (remaining > 0) {
            for (int row = startRow - 1; row >= 0 && remaining > 0; row--) {
                List<Seat> rowPick = pickFromRowMiddleOut(h.getSeats()[row], remaining);
                result.addAll(rowPick);
                remaining = tickets - result.size();
            }
        }

        if (result.size() != tickets)
            throw new NotEnoughSeatsException("Cannot allocate seats from position.");
        return result;
    }

    /*
     * Pick up to max seats from a row using middle-out strategy.
     * @param rowSeats
     * @param max
     * @return
     */
    private List<Seat> pickFromRowMiddleOut(Seat[] rowSeats, int max) {
        List<Seat> picked = new ArrayList<>();
        //row index
        int n = rowSeats.length;
        //middle index
        int center = (n - 1) / 2+1; // for even, left-middle
        boolean[] used = new boolean[n];
        int explored = 0;
        //loop follow middle most possible
        while (explored < n && picked.size() < max) {
            //offset to middle
            int offset = explored;
            //left index of middle
            int left = center - offset;
            //right index of middle
            int right = center + offset;
            //check left
            if (left >= 0 && !used[left]) {
                used[left] = true;
                if (rowSeats[left].getStatus() == SeatStatus.AVAILABLE) {
                    picked.add(rowSeats[left]);
                    if (picked.size() == max) break;
                }
            }
            //check right
            if (right < n && !used[right]) {
                used[right] = true;
                if (rowSeats[right].getStatus() == SeatStatus.AVAILABLE) {
                    picked.add(rowSeats[right]);
                    if (picked.size() == max) break;
                }
            }
            explored++;
        }
        return picked;
    }
}
