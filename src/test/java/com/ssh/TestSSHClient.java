package com.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

/**
 * @author vs053a
 *
 */
public class TestSSHClient {
	
	private static SshServer sshd;
    
    private static String password = "testPassword";
    private static String server = "localhost";
    private static String user = "login";
	
    /**
	 * initializes test environment. 
	 * Creates a testSSH server and copies some file on it. 
	 */
	@BeforeClass
	public static void setup() {
        // Initializing ssh server
        sshd = SshServer.setUpDefaultServer();
        sshd.setPasswordAuthenticator(new MyPasswordAuthenticator());
        sshd.setPublickeyAuthenticator(new MyPublickeyAuthenticator());
        sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
        sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystem.Factory()));
        sshd.setShellFactory(new ProcessShellFactory(new String[] { "/bin/sh", "-i", "-l" }));
        
        //starting ssh server
        try {
			sshd.start();
		} catch (IOException e) {
			fail("Test SSH Server should have started");
			e.printStackTrace();
		}
        
        //create a file on SSH server
        putFileOnServer("src/test/resources/test.properties", "target/test.properties");
        putFileOnServer("src/test/resources/upload.txt", "target/upload.txt");
	}
	
	/**
	 * This method will create a test file on ssh server
	 * @param sourceFile
	 * @param destinationFile
	 */
	public static void putFileOnServer(String sourceFile,String destinationFile){
        Session session =null;
        Channel channel=null;
        ChannelSftp chSftp = null;
        JSch jsch=new JSch();
        try {
        	session=jsch.getSession(user, server, sshd.getPort());
        	session.setPassword(password);
            Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            logger.info("Connecting to " + server + ":" + sshd.getPort());
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            chSftp = (ChannelSftp) channel;
            logger.info("Channel sftp opened");
		} 
        catch (JSchException e) {
			fail("Should have opened a session to SSHD server");
			e.printStackTrace();
		}

		if (chSftp == null || session == null || !session.isConnected() || !chSftp.isConnected()) {
        	fail("Connection to server is closed. Open it first.");
        }
        
        try {
            logger.info("Uploading file to server");
            chSftp.put(sourceFile, destinationFile);
            logger.info("Upload successfull.");
        } catch (SftpException e) {
            fail("Should have been able copy file on server");
            e.printStackTrace();
        }
        finally{
        	if(chSftp!=null)
        		chSftp.disconnect();
        	if(session!=null)
        		session.disconnect();
        }
	}


	/**
	 * This test is to verify, whether we are able to read file as inputstream from a remote location
	 * @throws IOException 
	 *  
	 */
	@Test
	public void testReadFile() throws IOException{
		SSHClientUtil sshclientUtil = new SSHClientUtil(user, server, null);
		sshclientUtil.connectWithPassword(password,sshd.getPort());
		InputStream is = null;
		try {
			is = sshclientUtil.readFile("target", "test.properties");
		} catch (JSchException | SftpException e) {
			fail("Should have been able to read file");
		}
		assertNotNull(is);
		sshclientUtil.disconnect();
	}
	
	/**
	 * This test is to verify, whether we are able to read file as properties from a remote location
	 */
	@Test
	public void testReadFileAsProperties(){
		SSHClientUtil sshclientUtil = new SSHClientUtil(user, server, null);
		sshclientUtil.connectWithPassword(password,sshd.getPort());
		Properties properties = null;
		try {
			properties = sshclientUtil.readFileAsProperties("target", "test.properties");
		} catch (IOException | JSchException | SftpException e) {
			fail("Should have been able to read file");
		}
		assertNotNull(properties);
		assertTrue(properties.size()>0);
		sshclientUtil.disconnect();
	}

	/**
	 * This test is to verify, whether we are able to write file on a remote location
	 */
	@Test
	public void testWriteFile(){
		SSHClientUtil sshclientUtil = new SSHClientUtil(user, server, null);
		sshclientUtil.connectWithPassword(password,sshd.getPort());
		
        InputStream isContent =  ClassLoader.class.getResourceAsStream("/test.properties");
        String filePath = "target";
        String fileName ="testCopy.properties";
        //write file to remote path	
		try {
			sshclientUtil.writeFile(isContent, filePath, fileName);
		} catch (JSchException | SftpException e) {
			fail("Should have been able to write file");
		}
		//validate the file is actually written
		InputStream inputStreamContent = null;
		try {
			inputStreamContent = sshclientUtil.readFile(filePath, fileName);
		} catch (JSchException | SftpException e) {
			fail("Should have been able to read file");
		}
		assertNotNull(inputStreamContent);
		sshclientUtil.disconnect();
		
	}
	
//	@Test
//    public void testExecuteCommand() throws Exception {
//		
//		SSHClientUtil sshclientUtil = new SSHClientUtil(user, server, null);
//		sshclientUtil.connectWithPassword(password,sshd.getPort());
//		//String res = sshclientUtil.executeCommand("ssh -p "+ sshd.getPort() +" localhost ls -l");
//		String res = sshclientUtil.executeCommand("pwd");
//		assertNotNull(res);
//		sshclientUtil.disconnect();
//    }
	
	@Ignore
	public void testExecuteCommand() throws IOException, JSchException {
//	    String user = "cloud-user";
        String user = "root";
//	    String host = "130.3.192.96";
        String host = "130.3.192.99";
	    byte[] privateKey;

//        InputStream in = ClassLoader.class.getResourceAsStream("/BWI1_private_key.ppk");
        InputStream in = ClassLoader.class.getResourceAsStream("/dewayne.ppk");
        privateKey = IOUtils.toByteArray(in);

        SSHClientUtil client = new SSHClientUtil(user, host, privateKey);
        boolean connected = false; 
        for (int i = 0; i < 60; i++) {
            try {
                client.connect();
                connected = true;
                break;
            } catch (JSchException e) {
                System.out.println(e.getMessage());
            }

            // wait a little while
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        
        assertTrue(connected);
        client.executeCommand("cd /tmp"); 
        client.executeCommand("cd /");
        client.executeCommand("ls");
        client.disconnect();
	}
	
	/**
	 * 
	 */
	@AfterClass
	public static void cleanup(){
		try {
			sshd.stop();
		} catch (InterruptedException e) {
			fail("Test SSH Server should have stopped");
			e.printStackTrace();
		}
	}
	
}
