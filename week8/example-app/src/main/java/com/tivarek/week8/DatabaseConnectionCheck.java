package com.tivarek.week8;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("dbcheck")
public class DatabaseConnectionCheck implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public DatabaseConnectionCheck(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        var result = jdbc.queryForMap("""
                SELECT DB_NAME() AS DatabaseName,
                       USER_NAME() AS DatabaseUser
                """);

        System.out.println("Database connection: " + result);
    }
}