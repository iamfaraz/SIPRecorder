package com.speechanalytics.siprecorder.utils;

import java.sql.*;
import java.util.Properties;
public class TestConnection {

    public static void main(String[] args) {

        Connection conn1 = null;
        Connection conn2 = null;
        Connection conn3 = null;

        try {
            // connect way #1
            String dbUrl = "jdbc:mysql://192.168.8.125:3306/sap";
            String userName = "user";
            String password = "";

            String url1 = "jdbc:mysql://192.168.8.125:3306/sap";
            String user = "user";

//            conn1 = DriverManager.getConnection(url1, "root@192.168.8.125", "");
//            if (conn1 != null) {
//                System.out.println("Connected to the database test1");
//            }else {
//            	System.err.println("Error");
//            }

//             connect way #2
            String url2 = "jdbc:mysql://192.168.8.125:3306/sap?user=root";
            conn2 = DriverManager.getConnection(url2);
            if (conn2 != null) {
                System.out.println("Connected to the database test2");
            }else {
                System.err.println("Error Occurred");
            }
//
//            // connect way #3
            String url3 = "jdbc:mysql://192.168.8.125:3306/sap";
            Properties info = new Properties();
            info.put("user", "root");
            info.put("password", "");

            conn3 = DriverManager.getConnection(url3, info);
            if (conn3 != null) {
                System.out.println("Connected to the database test3");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

//        String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
//		// TODO Auto-generated method stub
//		 DB db = new DB();
//	        db.dbConnect(JDBC_DRIVER, "jdbc:sqlserver://localhost", "root", "" );
    }

}
class DB{
    public void dbConnect(  String Driver, String db_connect_string,
                            String db_userid,
                            String db_password){
        try{
            Class.forName(Driver);
            Connection conn = DriverManager.getConnection(
                    db_connect_string,
                    db_userid,
                    db_password);
            System.out.println( "connected" );
        }
        catch( Exception e ){
            e.printStackTrace();
        }
    }
}
