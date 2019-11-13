package com.ontotext.graphdb.example.spnego;

import org.apache.http.client.HttpClient;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import javax.security.auth.login.LoginException;

/**
 * Common code for both {@link NoThirdPartyExample} and {@link SpnegoClientExample}.
 */
public abstract class AbstractExample {
    protected final static String USERNAME = "pavel";
    protected final static String PASSWORD = "xxx";
    private final static String REPOSITORY_URL = "http://hostname.example.com:7200/repositories/test";

    protected abstract HttpClient createKerberosHttpClient();

    protected abstract void login(boolean useTicketCache) throws LoginException;

    protected abstract void run() throws Exception;

    protected void runImpl() {
        HTTPRepository httpRepository = new HTTPRepository(REPOSITORY_URL);
        httpRepository.setHttpClient(createKerberosHttpClient());

        try (RepositoryConnection connection = httpRepository.getConnection()) {
            System.out.println("Size before update: " + connection.size());
            System.out.println("Transaction begin");
            connection.begin();
            System.out.println("Insert data");
            connection.prepareUpdate("insert data { [] <urn:test> [] }").execute();
            System.out.println("Transaction commit");
            connection.commit();
            System.out.println("Size after update: " + connection.size());
        }
    }
}
