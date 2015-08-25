/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fcnprojectoption1;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 *
 * @author suresh
 */
public class FcnProject {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        try {
            String tftpServerIp = "";
            DatagramSocket client = new DatagramSocket(1234);
            //InetAddress add=InetAddress.getByName(tftpServerIp);
            InetAddress add = InetAddress.getByName("192.168.72.162");
            File outPutFile = new File(System.getProperty("user.dir") + File.separator + "TftpDownloads");
            boolean status = outPutFile.mkdir();
            while (true) {
                //convert a file to byte array and byte array to file
                //Path pa=Paths.get("/home/suresh/FiletransferTry/index.jpeg");
                //divide the file into 512 bytes each part 
                int min = 0;
                int max = 512;
                boolean completed = false;
                boolean fileCompleted = false;
                //int dataSent=0;
                byte[] chunkFile = new byte[512];
                byte[] ackMsg = new byte[100];
                byte[] statusFile = new byte[100];
                byte[] finalFile = new byte[0];
                //int chunk=0;
                int index = 0;
                Scanner input = new Scanner(System.in);
                String msg = input.next();
                //byte[] req=msg.getBytes();
                //System.out.println("wrq bytes length "+req.length);
                //DatagramPacket sendData=new DatagramPacket(req,req.length,add,1234);
                //client.send(sendData);
                if (msg.equals("put")) {
                    byte[] req = "wrq".getBytes();
                    DatagramPacket sendData = new DatagramPacket(req, req.length, add, 1234);
                    client.send(sendData);
                    //System.out.println("Enter the upload file location");
                    String path = input.next();
                    Path pa = Paths.get(path);
                    byte[] file = Files.readAllBytes(pa);
                    System.out.println("Name of the file on server ?");
                    String fileName = input.next();
                    byte[] fileNameInBytes = fileName.getBytes();
                    DatagramPacket sendFileName = new DatagramPacket(fileNameInBytes, fileNameInBytes.length, add, 1234);
                    client.send(sendFileName);
                    System.out.println("Uploading WAIT");
                    while (completed == false) {
                        //transfer only 512 bytes at a time when the file length is greater than 512. This splits the file into 512 chunks
                        if ((max) < file.length) {
                            int in = 0;
                            for (int i = min; i < max; i++) {
                                chunkFile[in] = file[i];
                                in++;
                                //dataSent++;
                            }
                            min = max;
                            max = max + 512;
                            sendData = new DatagramPacket(chunkFile, chunkFile.length, add, 1234);
                            client.send(sendData);
                            //chunk=chunk+1;
                            DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length);
                            client.receive(ack);
                        } //when the last part of the file is less than 512 bytes create a packet to remaining size and send
                        else {
                            int in = 0;
                            chunkFile = new byte[file.length - min];
                            for (int i = min; i < file.length; i++) {
                                chunkFile[in] = file[i];
                                in++;
                                //dataSent++;
                            }
                            sendData = new DatagramPacket(chunkFile, chunkFile.length, add, 1234);
                            client.send(sendData);
                            //System.out.println("Data sent "+dataSent);
                            //chunk=chunk+1;
                            //System.out.println("Chunk sent "+chunk);
                            completed = true;
                            DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length);
                            client.receive(ack);
                            System.out.println("File successfully uploaded ");
                        }
                    }
                } //read request
                else if (msg.equals("get")) {
                    byte[] req = "rrq".getBytes();
                    DatagramPacket sendData = new DatagramPacket(req, req.length, add, 1234);
                    client.send(sendData);
                    String fileName = input.next();
                    byte[] fileNameInBytes = fileName.getBytes();
                    DatagramPacket sendFileName = new DatagramPacket(fileNameInBytes, fileNameInBytes.length, add, 1234);
                    client.send(sendFileName);
                    DatagramPacket fileStatus = new DatagramPacket(statusFile, statusFile.length, add, 1234);
                    client.receive(fileStatus);
                    if (new String(fileStatus.getData()).substring(0, fileStatus.getLength()).equals("success")) {
                        System.out.println("receiving FILE");
                        while (fileCompleted == false) {
                            DatagramPacket receiveData = new DatagramPacket(chunkFile, chunkFile.length);
                            client.receive(receiveData);
                            chunkFile = receiveData.getData();
                            //receive the data chunks and add it to a final array if the chunk is 512 bytes
                            if (receiveData.getLength() == 512) {
                                byte[] tempFile = new byte[finalFile.length];
                                tempFile = finalFile;
                                finalFile = new byte[finalFile.length + 512];
                                for (int i = 0; i < tempFile.length; i++) {
                                    finalFile[i] = tempFile[i];
                                }
                                for (int i = 0; i < receiveData.getLength(); i++) {
                                    finalFile[index] = chunkFile[i];
                                    index++;
                                }
                                ackMsg = "ack".getBytes();
                                DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length, receiveData.getAddress(), receiveData.getPort());
                                client.send(ack);
                            } //if the data chunk is less the 512 bytes 
                            else {
                                byte[] tempFile = new byte[finalFile.length];
                                tempFile = finalFile;
                                finalFile = new byte[finalFile.length + receiveData.getLength()];
                                for (int i = 0; i < tempFile.length; i++) {
                                    finalFile[i] = tempFile[i];
                                }
                                for (int i = 0; i < receiveData.getLength(); i++) {
                                    finalFile[index] = chunkFile[i];
                                    index++;
                                }
                                fileCompleted = true;
                                ackMsg = "ack".getBytes();
                                DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length, receiveData.getAddress(), receiveData.getPort());
                                client.send(ack);
                            }
                        }
                        FileOutputStream fout = new FileOutputStream(System.getProperty("user.dir") + File.separator + fileName);
                        fout.write(finalFile);
                        System.out.println("File recieved");
                    } else {
                        System.out.println("No such file found");
                    }
                } else if (msg.equals("connect")) {
                    String iP = input.next();
                    tftpServerIp = iP;
                    System.out.println("Connected to Server at " + iP);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
