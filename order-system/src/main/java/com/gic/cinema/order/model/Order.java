package com.gic.cinema.order.model;

/**
 * Order : include book information
 */
import lombok.*;

import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Order {
    /*
     * Order ID
     */
    private String id;
    /*
     * Order Movie Name
     */
    private String movieName;
    /*
     * Order ticket number
     */
    private int tickets;
    /*
     * Order seats info
     */
    private List<String> seatLabels;

    /*
     * Hall name
     */
    private String hallName;
}
