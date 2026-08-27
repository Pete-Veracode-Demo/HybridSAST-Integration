package com.metrowest.services.search;

/**
 * Carries the parameters for a customer order search. The search term is the
 * text the customer typed into the dashboard search box.
 */
public class OrderSearchCriteria
{
    private final String term;

    public OrderSearchCriteria(String term)
    {
        this.term = term;
    }

    public String getTerm()
    {
        return term;
    }
}
