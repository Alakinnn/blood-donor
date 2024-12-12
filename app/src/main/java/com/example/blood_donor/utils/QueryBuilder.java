package com.example.blood_donor.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class QueryBuilder {
    private StringBuilder query;
    private List<String> args;

    public QueryBuilder() {
        this.query = new StringBuilder();
        this.args = new ArrayList<>();
    }

    public QueryBuilder select(String columns) {
        query.append("SELECT ").append(columns);
        return this;
    }

    public QueryBuilder from(String table) {
        query.append(" FROM ").append(table);
        return this;
    }

    public QueryBuilder join(String joinClause) {
        query.append(" JOIN ").append(joinClause);
        return this;
    }

    public QueryBuilder where(String condition, String... params) {
        query.append(" WHERE ").append(condition);
        if (params != null) {
            for (String param : params) {
                args.add(param);
            }
        }
        return this;
    }

    public QueryBuilder and(String condition, String... params) {
        query.append(" AND ").append(condition);
        if (params != null) {
            for (String param : params) {
                args.add(param);
            }
        }
        return this;
    }

    public QueryBuilder orderBy(String orderClause) {
        query.append(" ORDER BY ").append(orderClause);
        return this;
    }

    public QueryBuilder limit(int limit) {
        query.append(" LIMIT ").append(limit);
        return this;
    }

    public QueryBuilder offset(int offset) {
        query.append(" OFFSET ").append(offset);
        return this;
    }

    public Cursor execute(SQLiteDatabase db) {
        return db.rawQuery(query.toString(), args.toArray(new String[0]));
    }

    @Override
    public String toString() {
        return query.toString();
    }
}