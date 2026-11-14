package com.hrms.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

public class TestDBConnection {
    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            System.out.println("Ket noi database thanh cong!");
            System.out.println("Database: " + connection.getCatalog());
            System.out.println("Driver: " + metaData.getDriverName());
            System.out.println("URL: " + metaData.getURL());
        } catch (SQLException e) {
            System.out.println("Ket noi database that bai!");
            e.printStackTrace();
        }
    }
}
