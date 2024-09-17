package com.example.tienda104.ventas.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tienda104.ventas.domain.Venta;

public interface JpaVentaRepository extends JpaRepository<Venta, Long> {
}
