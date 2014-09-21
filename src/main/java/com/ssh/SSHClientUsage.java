package com.ssh;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class SSHClientUsage {

	// Change the IP as per your local unix machine
	public static String host = "130.3.192.111";
	// user name for the unix machine
    public static String user = "cloud-user";

	public static void main(String[] args) {

		try{
			// the ppk file should be in classpath 
			// In case you are using a password to connect , you can use 'session.setPassword(passwd)' , in connect() method
		    InputStream in = ClassLoader.class.getResourceAsStream("/centos.ppk");
		    byte[] identity=null;
			identity = IOUtils.toByteArray(in);

			SSHClient sshClient = new SSHClient(user,host,identity);
			//connects to Unix machine
			sshClient.connect();
			//execute command and returns respons in String
			String fileContent = sshClient.executeCommand("cat /home/cloud-user/test.txt");
			System.out.println("Content -->"+fileContent);
			//disconnect from remote machine
			sshClient.disconnect();
			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

}
