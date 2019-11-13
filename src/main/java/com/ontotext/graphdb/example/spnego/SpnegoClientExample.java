package com.ontotext.graphdb.example.spnego;

import com.kerb4j.client.SpnegoClient;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.ietf.jgss.GSSException;

import javax.security.auth.login.LoginException;
import java.net.URL;
import java.security.PrivilegedActionException;

/**
 * This example shows how to use {@link SpnegoClient} from the kerb4j library to inject Authorization headers
 * with SPNEGO tokens. This mechanism uses preemptive authentication, which means the client will send the necessary
 * Authorization header even if the server hasn't requested Kerberos/SPNEGO via "WWW-Authenticate: Negotiate".
 */
public class SpnegoClientExample extends AbstractExample {
    private SpnegoClient spnegoClient;

    @Override
    protected CloseableHttpClient createKerberosHttpClient() {
        if (spnegoClient == null) {
            throw new IllegalStateException("You must login first");
        }

        return HttpClientBuilder.create()
                .addInterceptorFirst((HttpRequestInterceptor) (httpRequest, httpContext) -> {
                    URL url = new URL(((HttpClientContext) httpContext).getHttpRoute().getTargetHost().toURI());
                    try {
                        httpRequest.addHeader("Authorization", spnegoClient.createAuthroizationHeader(url));
                    } catch (PrivilegedActionException | GSSException e) {
                        throw new RuntimeException(e);
                    }
                })
                .build();
    }

    @Override
    protected void login(boolean useTicketCache) throws LoginException {
        if (useTicketCache) {
            spnegoClient = SpnegoClient.loginWithTicketCache(USERNAME);
        } else {
            spnegoClient = SpnegoClient.loginWithUsernamePassword(USERNAME, PASSWORD);
        }
    }

    @Override
    protected void run() throws Exception {
        runImpl();
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("java.security.auth.login.config", "login.conf");
        // Uncomment this and set to a custom krb5.conf if needed, otherwise the system one will be used
        //System.setProperty("java.security.krb5.conf", "krb5.conf");
        // Uncomment if you want Kerberos to be verbose
        //System.setProperty("sun.security.krb5.debug", "true");
        
        AbstractExample example = new SpnegoClientExample();
        // If parameter is true login will use ticket cache, if false it will use username/password.
        // Use "kinit" to create a ticket in the cache.
        example.login(false);
        example.run();
    }
}
