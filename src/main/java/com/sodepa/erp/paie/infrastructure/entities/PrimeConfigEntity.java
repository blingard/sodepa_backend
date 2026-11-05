package com.sodepa.erp.paie.infrastructure.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "paie_primes_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrimeConfigEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 150)
    private String libelle;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_rubrique", nullable = false, length = 40)
    private TypeRubrique typeRubrique;

    @Column(nullable = false)
    @Builder.Default
    private boolean imposable = true;

    @Column(name = "cotisable_cnps", nullable = false)
    @Builder.Default
    private boolean cotisableCnps = true;

    @Column(name = "montant_defaut", precision = 18, scale = 2)
    @Builder.Default
    private BigDecimal montantDefaut = BigDecimal.ZERO;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;
}
