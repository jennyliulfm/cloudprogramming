package edu.utas.util;

import java.sql.*;
import java.sql.DriverManager;

public class DBAdaptor {

    public static Connection getConnection() {
        Connection con = null;

        try {
            // load the Driver Class
            Class.forName(PropertiesCache.getInstance().getProperty("DB_DRIVER_CLASS"));

            // create the connection now
            con = DriverManager.getConnection(PropertiesCache.getInstance().getProperty("DB_URL"),
                    PropertiesCache.getInstance().getProperty("DB_USERNAME"),
                    PropertiesCache.getInstance().getProperty("DB_PASSWORD"));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return con;
    }


}
