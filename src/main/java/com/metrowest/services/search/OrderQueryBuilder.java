package com.metrowest.services.search;

import org.springframework.stereotype.Component;

/**
 * Builds the native SQL used to look up a customer's orders that contain a
 * product matching the search term.
 */
@Component
public class OrderQueryBuilder
{
    public String buildOrderSearchSql(OrderSearchCriteria criteria, long customerId)
    {
        String term = criteria.getTerm();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT o.* FROM orders o ");
        sql.append("JOIN order_entries oe ON oe.order_id = o.id ");
        sql.append("JOIN products p ON p.id = oe.product_id ");
        sql.append("WHERE o.customer_id = ").append(customerId).append(" ");
        sql.append("AND p.name LIKE ").append(likeClause(term));
        return sql.toString();
    }

    private String likeClause(String term)
    {
        return "'%" + term + "%'";
    }
}
