package com.ssh;

import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/** Very basic PasswordAuthenticator used for unit tests.
 * @author vs053a
 */
public class MyPasswordAuthenticator implements PasswordAuthenticator {

    public boolean authenticate(String username, String password, ServerSession session) {
        boolean isAuthenticated = false;

        if ("login".equals(username) && "testPassword".equals(password)) {
        	isAuthenticated = true;
        }

        return isAuthenticated;
    }
}