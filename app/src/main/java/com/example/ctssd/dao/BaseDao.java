package com.example.ctssd.dao;

import java.util.List;

public interface BaseDao<T>
{
    public void save(T object);
    public T findById(String id);
    public void update(T object);
    public void delete(String id);
    public List<T> getAll();
}