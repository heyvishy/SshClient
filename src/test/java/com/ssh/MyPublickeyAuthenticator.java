package com.ssh;

import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import java.security.PublicKey;


/**Very basic PublickeyAuthenticator used for unit tests.
 * @author vs053a
 *
 */
public class MyPublickeyAuthenticator implements PublickeyAuthenticator {
    public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) {
        return false;
    }
}