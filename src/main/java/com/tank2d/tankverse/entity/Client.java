package com.tank2d.tankverse.entity;
import java.io.*;
import java.net.*;

public class Client {
    public static void main(String[] args) {
        try {
            // đổi host + port theo Ngrok của bạn
            String host = "0.tcp.ap.ngrok.io";
            int port = 19383;  // <--- sửa tại đây

            Socket socket = new Socket(host, port);
            System.out.println("Kết nối thành công tới Server!");

            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

            System.out.println("Server: " + in.readLine());

            String msg;
            while (true) {
                System.out.print("Bạn: ");
                msg = keyboard.readLine();
                out.println(msg);
                System.out.println("Server trả lời: " + in.readLine());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}