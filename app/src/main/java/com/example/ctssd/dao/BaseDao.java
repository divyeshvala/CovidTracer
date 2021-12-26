package com.example.ctssd.dao;

import java.util.List;

public interface BaseDao<T>
{
    public void save(T object);
    public List<T> getAll();
}