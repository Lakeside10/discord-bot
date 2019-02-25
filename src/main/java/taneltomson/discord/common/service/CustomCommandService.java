package taneltomson.discord.common.service;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import lombok.extern.slf4j.Slf4j;
import taneltomson.discord.common.model.Command;


@Slf4j
public class CustomCommandService {
    private static final String SELECT_FROM_COMMAND_WHERE_NAME =
            "SELECT c FROM Command c WHERE lower(c.callKey) = lower(:call_key)";
    private final EntityManager em;
    private final EntityManagerFactory emf;

    public CustomCommandService(String puName) {
        emf = Persistence.createEntityManagerFactory(puName);
        em = emf.createEntityManager();
    }

    public void addCommand(Command command) {
        em.getTransaction().begin();
        em.persist(command);
        em.getTransaction().commit();
    }

    public Command findCommand(String name) {
        log.info("Finding command with name {}", name);
        return (Command) em.createQuery(SELECT_FROM_COMMAND_WHERE_NAME)
                           .setParameter("call_key", name).getSingleResult();
    }

    public void close() {
        em.clear();
        em.close();
        emf.close();
    }

    public boolean hasCommand(String name) {
        return !em.createQuery(SELECT_FROM_COMMAND_WHERE_NAME)
                  .setParameter("call_key", name)
                  .getResultList().isEmpty();
    }

    public void deleteCommand(String name) {
        em.getTransaction().begin();
        em.createQuery("DELETE FROM Command c WHERE lower(c.callKey) = lower(:call_key)")
          .setParameter("call_key", name)
          .executeUpdate();
        em.getTransaction().commit();
    }
}
