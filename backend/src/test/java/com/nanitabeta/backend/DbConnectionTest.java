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

    @Test
    void DBのタイムゾーンが日本時間になっている() throws Exception {
        try (Connection connection = dataSource.getConnection();
             var statement = connection.createStatement();
             var rs = statement.executeQuery(
                 "SELECT @@session.time_zone, NOW(), TIMEDIFF(NOW(), UTC_TIMESTAMP())")) {
            rs.next();
            System.out.println("session.time_zone = " + rs.getString(1));
            System.out.println("NOW() = " + rs.getString(2));
            assertThat(rs.getString(3)).isEqualTo("09:00:00");
        }
    }

}