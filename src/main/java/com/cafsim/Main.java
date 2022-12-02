package com.cafsim;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cafsim.constants.GlobalConstants;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Objects;

import static java.lang.Thread.sleep;
import static org.slf4j.LoggerFactory.getLogger;

public class Main {
    private final static Logger LOGGER = getLogger(Main.class);

    public static String GetJson() {
        StringBuilder result = new StringBuilder();
        BufferedReader in = null;
        try {
            String urlName = GlobalConstants.WHAZZUP_FILE;
            URL realUrl = new URL(urlName);
            //打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            //设置通用的请求属性
            conn.setRequestProperty("accept", "charset=UTF-8");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            //建立实际的连接
            conn.connect();
            //定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            System.out.println("发送GET请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }

    public static void main(String[] args) {
        Connection conn;
        Statement stmt;
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(GlobalConstants.MYSQL_URL, GlobalConstants.MYSQL_USER, GlobalConstants.MYSQL_PASSWORD);
            stmt = conn.createStatement();
            String sql;
            while (true) {
                JSONObject object = JSONObject.parseObject(GetJson());
                JSONArray results = object.getJSONArray("clients");
                try {
                    for (int i = 0; i < results.size(); i++) {
                        String type = results.getJSONObject(i).getString("type");
                        String cid = results.getJSONObject(i).getString("cid");
                        if (Objects.equals(type, "ATC")) {
                            sql = "SELECT * FROM " + GlobalConstants.MYSQL_TABLE + " WHERE username = '" + cid + "'";
                            ResultSet rs = stmt.executeQuery(sql);
                            rs.next();
                            int str = rs.getInt("atc_time");
                            double time = str + 30;
                            sql = "UPDATE " + GlobalConstants.MYSQL_TABLE + " SET atc_time = " + Math.round(time) + " WHERE " + GlobalConstants.MYSQL_TABLE + ".username = " + cid;
                            stmt.executeUpdate(sql);
                        } else if (Objects.equals(type, "PILOT")) {
                            sql = "SELECT * FROM " + GlobalConstants.MYSQL_TABLE + " WHERE username = '" + cid + "'";
                            ResultSet rs = stmt.executeQuery(sql);
                            rs.next();
                            int str = rs.getInt("pilot_time");
                            double time = str + 30;
                            sql = "UPDATE " + GlobalConstants.MYSQL_TABLE + " SET pilot_time = " + Math.round(time) + " WHERE " + GlobalConstants.MYSQL_TABLE + ".username = " + cid;
                            stmt.executeUpdate(sql);
                        }
                        String callsign = results.getJSONObject(i).getString("callsign");
                        sql = "UPDATE " + GlobalConstants.MYSQL_TABLE + " SET last_login_callsign = '" + callsign + "' WHERE " + GlobalConstants.MYSQL_TABLE + ".username = " + cid;
                        stmt.executeUpdate(sql);
                    }
                } catch (NullPointerException e) {
                    Date date = new Date(System.currentTimeMillis());
                    LOGGER.info(date + " - CHECKING WHAZZUP FILE");
                }
                try {
                    sleep(30000);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}