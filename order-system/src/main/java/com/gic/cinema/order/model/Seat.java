package com.gic.cinema.order.model;

import lombok.*;

/**
 * Seat - Seat information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Seat {
    /*
     * Row index of Seat - 0 = A (front), rows-1 = furthest  Screen
     */
    private int rowIndex;
    /*
     * Column index of Seat - 0-base (left)
     */
    private  int colIndex;
    /*
     * Seat Status : default Available
     */
    private SeatStatus status = SeatStatus.AVAILABLE;
    /*
     * Order ID : null/empty if never booked
     */
    private String orderId;

    /*
     * max row
     */
    private int maxRow;

    /**
     * inti seat
     * @param rowIndex
     * @param colIndex
     */
    public Seat(int rowIndex, int colIndex,int maxRow) {
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;
        this.maxRow=maxRow;
    }

    /**
     * return Seat Label - the furthest screen row is A
     * @return
     */
    public String getLabel() {
        char rowChar = (char) ('A' + (maxRow-rowIndex-1));
        int seatNo = colIndex + 1;
        return "%c%02d".formatted(rowChar, seatNo);
    }


}
