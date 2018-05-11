package com.speechanalytics.siprecorder.utils;


import com.speechanalytics.siprecorder.sip.SipSession;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DBHelper {

    static String dbUrl = "jdbc:mysql://192.168.8.125:3306/sap";
    static String userName = "user1";
    static String password = "pass";

    public static void main(String[] args){
        InsertData("dsfsd", "bajdf", "basj", "C:\\sdfsd\\faraz.wav");
    }

    public static boolean InsertData(String from, String to, String call_id, String filePath) {
        try {
            Connection conn = DriverManager.getConnection(dbUrl, userName, password);


            String query = "INSERT INTO caller(Call_from, Call_to, Call_c_id, Call_recorded_file) VALUES ("
                    + from + ", " + to + "," + call_id +",\""+filePath+"\")";
            String sql = "INSERT INTO caller(Call_from, Call_to, Call_c_id, Call_recorded_file) values (?, ?, ?, ?)";
            PreparedStatement statement = conn.prepareStatement(sql);
            statement.setString(1, from);
            statement.setString(2, to);
            statement.setString(3, call_id);
            statement.setString(4, filePath);

            int row = statement.executeUpdate();
            if (row > 0) {
                System.out.println("A contact was inserted with photo image.");
                conn.close();
                return true;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

}
