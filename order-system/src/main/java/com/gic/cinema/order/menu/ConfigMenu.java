package com.gic.cinema.order.menu;

import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.service.CinemaService;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.util.Arrays;

/**
 * ConfigMenu: Operate movie configure input
 */
@Service
public class ConfigMenu {
    private CinemaService cinemaService;
    public ConfigMenu(CinemaService cinemaService){
        this.cinemaService=cinemaService;
    }

    /**
     * process movie configure
     * @param scanner
     * @return
     * @throws Exception
     */
    public CinemaHall process(BufferedReader scanner) throws Exception{
        // ------- Application start: ask for movie + map -------
        System.out.println("Please define movie title and seating map in [Title] [Row] [SeatsPerRow] format:");
        System.out.print("> ");
        /*
         * loop read input till input not empty
         */
        String line = scanner.readLine().trim();
        while (line.isEmpty()) {
            System.out.print("> ");
            line = scanner.readLine().trim();
        }

        /*
         * parse input and validate input format
         */
        String[] tokens = line.split("\\s+");
        if (tokens.length < 3) {
            throw new RuntimeException("Invalid format. Example: Inception 8 10");
        }

        /*
         * Get movie hall row and seats per row
         */
        int rows, seatsPerRow;
        try {
            rows = Integer.parseInt(tokens[tokens.length - 2]);
            seatsPerRow = Integer.parseInt(tokens[tokens.length - 1]);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Last two values must be integers. Example: Inception 8 10");
        }
        //movie name
        String title = String.join(" ", Arrays.copyOfRange(tokens, 0, tokens.length - 2));

        try {
            return cinemaService.configureHall(title, rows, seatsPerRow);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error: " + e.getMessage());
        }
    }
}
