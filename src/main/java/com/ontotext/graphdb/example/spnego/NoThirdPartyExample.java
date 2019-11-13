package com.ontotext.graphdb.example.spnego;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.security.PrivilegedAction;

/**
 * This example shows how to use the built-in SPNEGO support in {@link org.apache.http.client.HttpClient}
 * for Kerberos/SPNEGO authentication. This supports only non-preemptive authentication, which means that
 * the server must first request Kerberos/SPNEGO via "WWW-Authenticate: Negotiate".
 */
public class NoThirdPartyExample extends AbstractExample {
    private LoginContext loginContext;

    private CallbackHandler getUsernamePasswordHandler(String username, String password) {
        return callbacks -> {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback) {
                    ((NameCallback) callback).setName(username);
                } else if (callback instanceof PasswordCallback) {
                    ((PasswordCallback) callback).setPassword(password.toCharArray());
                }
            }
        };
    }

    @Override
    protected CloseableHttpClient createKerberosHttpClient() {
        AuthScope authScope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);

        Credentials creds = new Credentials() {
            public String getPassword() {
                return null;
            }

            public Principal getUserPrincipal() {
                return null;
            }
        };

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(authScope, creds);

        return HttpClientBuilder.create()
                .setDefaultCredentialsProvider(credentialsProvider)
                .build();
    }

    @Override
    protected void login(boolean useTicketCache) throws LoginException {
        if (useTicketCache) {
            // Uses previous login from Kerberos ticket cache. You can login with "kinit".
            loginContext = new LoginContext("MyClientCache");
        } else {
            // Performs Kerberos login by username and password
            loginContext = new LoginContext("MyClient", getUsernamePasswordHandler(USERNAME, PASSWORD));
        }

        // Try to login with the configured login context
        loginContext.login();
    }

    @Override
    protected void run() throws Exception {
        if (loginContext == null) {
            throw new IllegalStateException("You must login first");
        }

        // Subject.doAs() is how you request something to be executed in the context of a given login
        Exception e = Subject.doAs(loginContext.getSubject(), (PrivilegedAction<Exception>) () -> {
            try {
                runImpl();
            } catch (Exception x) {
                return x;
            }
            return null;
        });

        if (e != null) {
            throw e;
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.auth.login.config", "login.conf");
        // Uncomment this and set to a custom krb5.conf if needed, otherwise the system one will be used
        //System.setProperty("java.security.krb5.conf", "krb5.conf");
        // Uncomment if you want Kerberos to be verbose
        //System.setProperty("sun.security.krb5.debug", "true");

        AbstractExample example = new NoThirdPartyExample();
        // If parameter is true login will use ticket cache, if false it will use username/password
        // Use "kinit" to create a ticket in the cache.
        example.login(false);
        example.run();
    }
}
