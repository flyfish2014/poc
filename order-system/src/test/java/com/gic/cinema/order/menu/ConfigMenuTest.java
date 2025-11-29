package com.gic.cinema.order.menu;

import com.gic.cinema.order.model.CinemaHall;
import com.gic.cinema.order.service.CinemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConfigMenu
 */
class ConfigMenuTest {

    private CinemaService cinemaService;
    private ConfigMenu configMenu;

    /**
     * init value before run test case
     */
    @BeforeEach
    void setUp() {
        cinemaService = mock(CinemaService.class);
        configMenu = new ConfigMenu(cinemaService);
    }

    /**
     * Test Simple Title
     * @throws Exception
     */
    @Test
    void testSimpleTitle() throws Exception {
        // given
        String input = "Inception 8 10\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        CinemaHall expectedHall = new CinemaHall(); // or use a real constructor if you have one
        when(cinemaService.configureHall("Inception", 8, 10)).thenReturn(expectedHall);

        // when
        CinemaHall result = configMenu.process(reader);

        // then
        assertSame(expectedHall, result);
        verify(cinemaService).configureHall("Inception", 8, 10);
    }

    /**
     * Test Title With Spaces
     * @throws Exception
     */
    @Test
    void testTitleWithSpaces() throws Exception {
        // given
        String input = "The Dark Knight 12 20\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        CinemaHall expectedHall = new CinemaHall();
        when(cinemaService.configureHall("The Dark Knight", 12, 20)).thenReturn(expectedHall);

        // when
        CinemaHall result = configMenu.process(reader);

        // then
        assertSame(expectedHall, result);
        verify(cinemaService).configureHall("The Dark Knight", 12, 20);
    }

    /**
     * Test Skips Empty Lines Until Non Empty
     * @throws Exception
     */
    @Test
    void testSkipsEmptyLinesUntilNonEmpty() throws Exception {
        // given: first two lines empty, third is valid
        String input = "\n\nAvatar 5 9\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        CinemaHall expectedHall = new CinemaHall();
        when(cinemaService.configureHall("Avatar", 5, 9)).thenReturn(expectedHall);

        // when
        CinemaHall result = configMenu.process(reader);

        // then
        assertSame(expectedHall, result);
        verify(cinemaService).configureHall("Avatar", 5, 9);
    }

    /**
     * Test Invalid Format Not Enough Tokens Throws RuntimeException
     */
    @Test
    void testInvalidFormatNotEnoughTokensThrowsRuntimeException() {
        // given
        String input = "Inception 8\n"; // only 2 tokens -> invalid
        BufferedReader reader = new BufferedReader(new StringReader(input));

        // when
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configMenu.process(reader));

        // then (optional: check message)
        assertTrue(ex.getMessage().startsWith("Invalid format"));
        verifyNoInteractions(cinemaService);
    }

    /**
     * Test Last Two Values Not Integers Throws RuntimeException
     */
    @Test
    void testLastTwoValuesNotIntegersThrowsRuntimeException() {
        // given
        String input = "Inception eight ten\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        // when
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configMenu.process(reader));

        // then
        assertTrue(ex.getMessage().startsWith("Last two values must be integers"));
        verifyNoInteractions(cinemaService);
    }

    /**
     * Test Service Throws IllegalArgument Wraps In RuntimeException
     */
    @Test
    void testServiceThrowsIllegalArgumentWrapsInRuntimeException() {
        // given
        String input = "Inception 0 10\n";
        BufferedReader reader = new BufferedReader(new StringReader(input));

        when(cinemaService.configureHall("Inception", 0, 10))
                .thenThrow(new IllegalArgumentException("Rows must be > 0"));

        // when
        RuntimeException ex = assertThrows(RuntimeException.class, () -> configMenu.process(reader));

        // then
        assertEquals("Error: Rows must be > 0", ex.getMessage());
        verify(cinemaService).configureHall("Inception", 0, 10);
    }
}