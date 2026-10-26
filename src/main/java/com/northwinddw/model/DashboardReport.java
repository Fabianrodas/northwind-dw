package com.northwinddw.model;

public enum DashboardReport {
    QUARTERLY_SALES("01  Ventas por trimestre", "Compara la evolución de cada año."),
    CATEGORY_REVENUE("02  Ventas por categoría", "Participación de cada grupo de productos."),
    COUNTRY_REVENUE("03  Ventas por país", "Los diez países con más ingresos."),
    CUSTOMER_REVENUE("04  Ventas por cliente", "Los diez clientes con mayor importe."),
    EMPLOYEE_REVENUE("05  Ventas por empleado", "Todos los empleados con actividad, por año."),
    SUPPLIERS_BY_COUNTRY("06  Proveedores por país", "Cantidad registrada por país de origen.");

    private final String title;
    private final String context;

    DashboardReport(String title, String context) {
        this.title = title;
        this.context = context;
    }

    public String title() {
        return title;
    }

    public String context() {
        return context;
    }
}
