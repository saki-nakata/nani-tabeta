package com.nanitabeta.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DbConnectionTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void MySQLに接続できる() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            String version = connection.getMetaData().getDatabaseProductVersion();
            System.out.println("接続先の MySQL: " + version);
            assertThat(version).startsWith("8.4");
        }
    }
}