package com.gic.cinema.order;

import com.gic.cinema.order.menu.ConfigMenu;
import com.gic.cinema.order.menu.MainMenu;
import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.service.CinemaService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * OrderSystemApplication  -  CommandLineRunner
 */
@SpringBootApplication
@ComponentScan(basePackages={"com.gic.cinema.order"})
public class OrderSystemApplication implements CommandLineRunner {

	private CinemaService cinemaService;
    private ConfigMenu configMenu;
	private MainMenu mainMenu ;
	public OrderSystemApplication(CinemaService cinemaService,ConfigMenu configMenu,MainMenu mainMenu) {
		this.configMenu=configMenu;
		this.cinemaService = cinemaService;
		this.mainMenu=mainMenu;
	}

	public static void main(String[] args) {
		SpringApplication.run(OrderSystemApplication.class, args);
	}
	@Override
	public void run(String... args) {
		try {
			//Line read from System input
			BufferedReader scanner = new BufferedReader(new InputStreamReader(System.in));
	        //run config menu
			CinemaHall hall =configMenu.process(scanner);
			mainMenu.process(scanner,hall);

		}catch(Exception ex){
			System.out.println(ex.getMessage());
		}
	}
}
