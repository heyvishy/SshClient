package com.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class SSHClient {


    private String user;
    private String host;
    private byte[] privateKey;
    private Channel channel = null;
    private int exitStatus;

    private void setExitStatus(int exitStatus) {
		this.exitStatus = exitStatus;
	}

	public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    private Session session = null;

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    /**
     * constructor to initialize SSHClient using user,host and byte[] pvt key
     * 
     * @param user
     *            username used to login to remote machine
     * @param host
     *            IP address of remote machine
     * @param privateKey
     *            byte[] pvt key to remote machine
     */
    public SSHClient(String user, String host, byte[] privateKey) {
        this.user = user;
        this.host = host;
        this.privateKey = privateKey;
    }


    public void connect() throws JSchException, IOException {
        connect(20000);
    }

    /**
     * This method is used to connect to remote machine using user,host and byte[] pvt key. It initializes session to
     * remote machine and opens a channel to execute commands. Once the remote operation is done, call disconnect()
     * method to close the session.
     * 
     * @throws JSchException
     *             If the connection cannot be established
     * @throws IOException 
     */
    public void connect(int connectTimeout) throws JSchException, IOException {
        JSch jsch = new JSch();
        final byte[] prvkey = privateKey;
        final byte[] emptyPassPhrase = new byte[0];
        // setup identity(i.e pvt key)
        jsch.addIdentity(user, prvkey, null, emptyPassPhrase);
        session = jsch.getSession(user, host, 22);
        Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        session.setConfig(config);
        session.setTimeout(connectTimeout);
        session.connect();
    }

    /**
     * disconnect from remote machine after operation is completed
     */
    public void disconnect() {
        if (session != null) {
            try {
                if (channel != null)
                    channel.disconnect();
                session.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * read file at remote path as InputStream <dd>The filePath value will be absolute directory path at remote machine
     * , for e.g "/root/cdp/etc"</dd> <dd>The fileName value will be name of file , at the filePath.For e.g "log4j.xml"</dd>
     * 
     * @param filePath
     * @param fileName
     * @return inputstream
     * @throws JSchException
     * @throws SftpException
     */
    public InputStream readFile(String filePath, String fileName) throws JSchException, SftpException {
        ChannelSftp chSftp = null;
        InputStream is = null;
        StringBuffer source = new StringBuffer();
        Channel lchannel = session.openChannel("sftp");
        lchannel.connect();
        chSftp = (ChannelSftp) lchannel;
        source.append(filePath).append("/").append(fileName);
        is = chSftp.get(source.toString());
        return is;
    }


    /**
     * write file at remote path using InputStream <dd>The filePath value will be absolute directory path at remote
     * machine , for e.g "/root/cdp/etc"</dd> <dd>The fileName value will be name of file , at the filePath.For e.g
     * "log4j.xml"</dd>
     * 
     * @param filePath
     * @param fileName
     * @return
     * @throws JSchException
     * @throws SftpException
     */
    public void writeFile(InputStream inputStream, String filePath, String fileName) throws JSchException,
        SftpException {
        ChannelSftp chSftp = null;
        InputStream is = null;
        StringBuffer destinationFilePath = new StringBuffer();
        Channel lchannel = session.openChannel("sftp");
        lchannel.connect();
        chSftp = (ChannelSftp) lchannel;
        destinationFilePath.append(filePath).append("/").append(fileName);
        chSftp.put(inputStream, destinationFilePath.toString());
        chSftp.disconnect();
    }

    /**
     * Executes command on remote host and returns response as String. This function could be used to perform unix
     * commands on remote machine For e.g use command "mkdir -p /path/of/new/dir" , to create a directory
     * 
     <dd> This method should only be used,if you want to execute command in 'exec' mode.</dd>
     <dd>That means the session variables won't be retained from one command execution to another command execution.
     </dd>
     * @param command
     *            A unix command to be executed on remote vm e.g 'pwd'
     * @return String A string response of command output on remote vm
     * @throws JSchException
     * @throws IOException
     */
    public String executeCommand(String command) throws JSchException, IOException {
    	System.out.println("EXEC Mode : Command being executed -->"+command);
    	
    	StringBuffer commandOutput = new StringBuffer();
        Channel execChannel = null;
        execChannel = session.openChannel("exec");

        ((ChannelExec) execChannel).setPty(true);
        ((ChannelExec) execChannel).setCommand(command);
        execChannel.setInputStream(null);
        ((ChannelExec) execChannel).setErrStream(System.err);
        InputStream in = execChannel.getInputStream();
        execChannel.connect();

        byte[] tmp = new byte[1024];
        while (true) {
            while (in.available() > 0) {
                int i = in.read(tmp, 0, 1024);
                if (i < 0)
                    break;
                commandOutput.append(new String(tmp, 0, i));
            }
            if (execChannel.isClosed()) {
                if (in.available() > 0)
                    continue;
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception ee) {
            }
        }
        exitStatus = execChannel.getExitStatus();
        execChannel.disconnect();
        System.out.println("Command response -->"+commandOutput.toString());
        return commandOutput.toString();
    }

    /**
     * Returns the status code from the execution of the last command.
     * 
     * @return The exit status from the last command.
     */
    public int getExitStatus() {
        return exitStatus;
    }

}
