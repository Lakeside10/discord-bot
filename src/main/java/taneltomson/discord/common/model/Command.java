package taneltomson.discord.common.model;


import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;


@Getter
@Setter
@Accessors(chain = true)
@Entity
@Table(name = "command")
public class Command {
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "command_seq", sequenceName = "command_seq", allocationSize = 1)
    @GeneratedValue(generator = "command_seq", strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "call_key", unique = true, nullable = false)
    private String callKey;

    @Column(name = "value", nullable = false)
    private String value;

    @Column(name = "created", nullable = false)
    private LocalDate created;
}
