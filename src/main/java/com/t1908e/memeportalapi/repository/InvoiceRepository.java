package com.t1908e.memeportalapi.repository;

import com.t1908e.memeportalapi.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Integer> {
}
