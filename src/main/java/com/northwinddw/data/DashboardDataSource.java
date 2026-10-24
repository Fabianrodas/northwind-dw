package com.northwinddw.data;

import com.northwinddw.model.DashboardData;

import java.sql.SQLException;

@FunctionalInterface
public interface DashboardDataSource {
    DashboardData loadDashboard(Integer year) throws SQLException;
}
