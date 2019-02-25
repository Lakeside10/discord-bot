package taneltomson.discord.common.service;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.hibernate.Session;
import org.junit.After;
import org.junit.Before;
import taneltomson.discord.TestConfiguration;


public class DatabaseTestHelper {
    private static EntityManagerFactory emf;
    private static EntityManager em;

    private static void updateLiquibase() {
        Session session = (Session) em.getDelegate();
        session.doWork(conn -> {
            try {
                Database database = DatabaseFactory
                        .getInstance()
                        .findCorrectDatabaseImplementation(new JdbcConnection(conn));
                Liquibase liquibase = new Liquibase("changelog.xml",
                                                    new ClassLoaderResourceAccessor(),
                                                    database);
                liquibase.update("test");
            } catch (LiquibaseException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Before
    public void setUp() {
        emf = Persistence.createEntityManagerFactory(TestConfiguration.getTestPUName());
        em = emf.createEntityManager();

        updateLiquibase();
    }

    @After
    public void tearDown() {
        em.clear();
        em.close();
        emf.close();
    }
}
