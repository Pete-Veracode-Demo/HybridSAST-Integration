package com.metrowest.services.search;

import com.metrowest.entity.Order;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs customer order searches. The controller hands off the raw search
 * criteria here; this service builds the query and executes it.
 */
@Service
public class OrderSearchService
{
    private final OrderQueryBuilder queryBuilder;

    @PersistenceContext
    private EntityManager entityManager;

    public OrderSearchService(OrderQueryBuilder queryBuilder)
    {
        this.queryBuilder = queryBuilder;
    }

    @SuppressWarnings("unchecked")
    public List<Order> search(OrderSearchCriteria criteria, long customerId)
    {
        String sql = queryBuilder.buildOrderSearchSql(criteria, customerId);
        return entityManager.createNativeQuery(sql, Order.class).getResultList();
    }
}
