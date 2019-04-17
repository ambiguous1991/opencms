package com.jba.opencms.type.base;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.OffsetDateTime;

@MappedSuperclass
@Data
public abstract class BaseTypeSimpleKey<T extends BaseType> extends BaseType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    protected Long id;

}
