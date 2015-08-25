/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fcnprojectoption1;

import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author suresh
 */
public class FcnProjectServer {

    public static void main(String ar[]) {
        try {
            DatagramSocket server = new DatagramSocket(1234);
            File outPutFile = new File(System.getProperty("user.dir") + File.separator + "TftpUploads");
            boolean status = outPutFile.mkdir();
            while (true) {
                byte[] chunkFile = new byte[512];
                byte[] finalFile = new byte[0];
                byte[] msgs = new byte[512];
                boolean completed = false;
                int min = 0;
                int max = 512;
                //int chunk=0;
                //int dataSent=0;
                //DatagramPacket receiveData=new DatagramPacket(chunkFile,chunkFile.length);
                boolean fileCompleted = false;
                int index = 0;
                DatagramPacket receiveMsg = new DatagramPacket(msgs, msgs.length);
                server.receive(receiveMsg);
                byte[] req = new byte[receiveMsg.getLength()];
                req = receiveMsg.getData();
                //write request
                if (new String(req).substring(0, receiveMsg.getLength()).equals("wrq")) {
                    System.out.println("received WRQ ");
                    DatagramPacket receiveFileName = new DatagramPacket(msgs, msgs.length);
                    server.receive(receiveFileName);
                    //System.out.println("received file name to be uploaded "+new String(receiveFileName.getData()));
                    byte[] fileName = new byte[receiveFileName.getLength()];
                    fileName = receiveFileName.getData();
                    String name = new String(fileName).substring(0, receiveFileName.getLength());
                    System.out.println("receiving FILE");
                    while (fileCompleted == false) {
                        DatagramPacket receiveData = new DatagramPacket(chunkFile, chunkFile.length);
                        server.receive(receiveData);
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
                            byte[] ackMsg = "ack".getBytes();
                            DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length, receiveData.getAddress(), receiveData.getPort());
                            server.send(ack);
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
                            byte[] ackMsg = "ack".getBytes();
                            DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length, receiveData.getAddress(), receiveData.getPort());
                            server.send(ack);
                        }
                    }
                    FileOutputStream fout = new FileOutputStream(System.getProperty("user.dir") + File.separator + "TftpUploads" + File.separator + name);
                    fout.write(finalFile);
                    System.out.println("File uploaded");
                } //read request
                else if (new String(req).substring(0, receiveMsg.getLength()).equals("rrq")) {
                    System.out.println("received RRQ");
                    DatagramPacket receiveFileName = new DatagramPacket(msgs, msgs.length);
                    server.receive(receiveFileName);
                    String name = new String(receiveFileName.getData()).substring(0, receiveFileName.getLength());
                    //System.out.println("file requested "+name);
                    //if the given filename exists then send the file 
                    if (new File(System.getProperty("user.dir") + File.separator + "TftpUploads" + File.separator + name).exists()) {
                        //System.out.println("requested file exists");
                        Path fileToBeSent = Paths.get(System.getProperty("user.dir") + File.separator + "TftpUploads" + File.separator + name);
                        byte[] file = Files.readAllBytes(fileToBeSent);
                        byte[] sMsg = "success".getBytes();
                        DatagramPacket succMsg = new DatagramPacket(sMsg, sMsg.length, receiveMsg.getAddress(), receiveMsg.getPort());
                        server.send(succMsg);
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
                                DatagramPacket sendData = new DatagramPacket(chunkFile, chunkFile.length, receiveMsg.getAddress(), receiveMsg.getPort());
                                server.send(sendData);
                                //chunk=chunk+1;
                                byte[] ackMsg = "ack".getBytes();
                                DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length);
                                server.receive(ack);
                            } //when the last part of the file is less than 512 bytes create a packet to remaining size and send
                            else {
                                int in = 0;
                                chunkFile = new byte[file.length - min];
                                for (int i = min; i < file.length; i++) {
                                    chunkFile[in] = file[i];
                                    in++;
                                    //dataSent++;
                                }
                                DatagramPacket sendData = new DatagramPacket(chunkFile, chunkFile.length, receiveMsg.getAddress(), receiveMsg.getPort());
                                server.send(sendData);
                                //System.out.println("Data sent "+dataSent);
                                //chunk=chunk+1;
                                //System.out.println("Chunk sent "+chunk);
                                completed = true;
                                byte[] ackMsg = "ack".getBytes();
                                DatagramPacket ack = new DatagramPacket(ackMsg, ackMsg.length);
                                server.receive(ack);
                                System.out.println("File successfully sent");
                            }
                        }
                    } else {
                        byte[] errorMsg = "error".getBytes();
                        DatagramPacket sendData = new DatagramPacket(errorMsg, errorMsg.length, receiveMsg.getAddress(), receiveMsg.getPort());
                        server.send(sendData);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
