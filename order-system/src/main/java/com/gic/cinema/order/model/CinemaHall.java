package com.gic.cinema.order.model;
import lombok.*;

import java.util.LinkedHashMap;
import java.util.Map;
/**
 * Cinema Hall info
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class CinemaHall {
    public static final String DEFAULT_HALL_NAME = "Hall_1";
    /*
     * movie name
     */
    private String movieName;
    /*
     * total rows
     */
    private int rows;
    /*
     * total seats per row
     */
    private int seatsPerRow;
    /*
     * total seats in cinema hall - [row][col]
     */
    private Seat[][] seats;
    /*
     * Orders info
     */
    private final Map<String, Order> orders = new LinkedHashMap<>();

    /*
     * Hall name
     */
    private String hallName;

    /**
     * init CinemaHall include seat info
     * @param movieName
     * @param rows
     * @param seatsPerRow
     */
    public CinemaHall(String movieName, int rows, int seatsPerRow) {
        this.movieName = movieName;
        this.rows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seats = new Seat[rows][seatsPerRow];
        //loop to init each seat per row
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < seatsPerRow; c++) {
                seats[r][c] = new Seat(r, c,rows);
            }
        }
        this.hallName= DEFAULT_HALL_NAME;
    }

    /**
     * init CinemaHall include seat info
     * @param movieName
     * @param rows
     * @param seatsPerRow
     * @param hallName
     */
    public CinemaHall(String movieName, int rows, int seatsPerRow,String hallName) {
        this(movieName,rows,seatsPerRow);
        this.hallName=hallName;
    }

    /**
     * get available seat count
     * @return
     */
    public int getAvailableSeatCount() {
        int count = 0;
        for (Seat[] row : seats) {
            for (Seat s : row) {
                if (s.getStatus() == SeatStatus.AVAILABLE) count++;
            }
        }
        return count;
    }
}
