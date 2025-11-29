package com.gic.cinema.order;

import com.gic.cinema.order.menu.ConfigMenu;
import com.gic.cinema.order.menu.MainMenu;
import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.service.CinemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.BufferedReader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for OrderSystemApplication (CommandLineRunner).
 *
 * Pure unit test:
 *  - No Spring context
 *  - Dependencies are mocked
 */
@ExtendWith(MockitoExtension.class)
class OrderSystemApplicationTest {

	@Mock
	private CinemaService cinemaService;

	@Mock
	private ConfigMenu configMenu;

	@Mock
	private MainMenu mainMenu;

	private OrderSystemApplication app;

	/**
	 * init value before run test case
	 */
	@BeforeEach
	void setUp() {
		// inject mocks via constructor, same as Spring would do
		app = new OrderSystemApplication(cinemaService, configMenu, mainMenu);
	}

	/**
	 * Test Invoke Config Menu And Main Menu In Order
	 * @throws Exception
	 */
	@Test
	void testInvokeConfigMenuAndMainMenuInOrder() throws Exception {
		// arrange
		CinemaHall hall = new CinemaHall("Test Movie", 3, 4);
		when(configMenu.process(any(BufferedReader.class))).thenReturn(hall);

		// act
		app.run();  // CommandLineRunner entry

		// assert
		verify(configMenu, times(1)).process(any(BufferedReader.class));
		verify(mainMenu, times(1)).process(any(BufferedReader.class), eq(hall));
		verifyNoMoreInteractions(mainMenu);
	}

	/**
	 * Test Catch Exceptions And Not Propagate
	 * @throws Exception
	 */
	@Test
	void testCatchExceptionsAndNotPropagate() throws Exception{
		// arrange
		// simulate some runtime failure inside config menu
		try {
			when(configMenu.process(any(BufferedReader.class)))
					.thenThrow(new RuntimeException("boom"));
		} catch (Exception e) {
			// process(...) in your code signature doesn't declare checked exceptions,
			// so this catch is just to make compiler happy if method changes.
		}

		// act + assert: run() should handle it internally (try/catch) and not throw
		assertDoesNotThrow(() -> {
			try {
				app.run();
			} catch (Exception e) {
				// run signature throws Exception, but your implementation catches its own
				// so if we get here, it's a failure.
				throw new RuntimeException(e);
			}
		});

		// mainMenu should not be called if configMenu failed
		verify(configMenu, times(1)).process(any(BufferedReader.class));
		verify(mainMenu, never()).process(any(), any());
	}
}