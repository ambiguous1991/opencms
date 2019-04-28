package com.jba.opencms.base;

import com.jba.opencms.dao.GenericDao;
import com.jba.opencms.type.base.BaseType;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;

@Transactional
public abstract class AbstractService<T extends BaseType> implements BaseService<T> {

    protected GenericDao<T> dao;

    public AbstractService(GenericDao<T> dao) {
        this.dao = dao;
    }

    @Override
    public void create(T entity) {
        dao.create(entity);
    }

    @Override
    public void update(T entity) {
        dao.update(entity);
    }

    @Override
    public T findOne(Long id, boolean initialize) {
        T element = dao.findOne(id);
        initialize(element);
        return element;
    }

    @Override
    public List<T> findAll(boolean initialize) {
        List<T> all = dao.findAll();
        if(initialize) {
            all.forEach(this::initialize);
        }
        return all;
    }

    @Override
    public void delete(T entity) {
        dao.delete(entity);
    }

    @Override
    public void delete(Long id) {
        dao.deleteById(id);
    }

    /**
     * Initializes the element's collections.
     * If the element has collections such as Set, List, Map etc, it calls Hibernate#initialize
     * @param element
     */
    private void initialize(T element){
        Field[] declaredFields = element.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if(isAssignableToCollection(declaredField)){
                declaredField.setAccessible(true);
                try {
                    initialize((Collection) declaredField.get(element));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (LazyInitializationException e){
                    throw e;
                }
                declaredField.setAccessible(false);
            }
        }
    }

    private boolean isAssignableToCollection(Field field){
        return Collection.class.isAssignableFrom(field.getType());
    }

    private void initialize(Collection collection){
        Hibernate.initialize(collection);
    }
}
